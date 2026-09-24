/** 文件职责：根据已订阅的设备最新遥测快照，计算数据监测页的实时汇总。 */
import type {
  SmartLightningRealtimeAnalysis,
  SmartLightningRealtimeArcFault,
  SmartLightningRealtimeCurrent,
  SmartLightningRealtimeGround,
  SmartLightningRealtimeOverall,
  SmartLightningRealtimeSpd
} from './smart-lightning-monitoring-api.service';

/** 页面当前权限范围内的一台设备及其最新遥测快照。 */
export interface SmartLightningMonitoringRealtimeDeviceSnapshot {
  readonly deviceId: string;
  readonly deviceName: string;
  readonly functionCategory: string;
  readonly active: unknown;
  readonly values: ReadonlyMap<string, {readonly ts: number; readonly value: unknown}>;
}

interface StoredValue {
  readonly ts: number;
  readonly value: unknown;
}

interface NumericSource {
  readonly value: number;
  readonly deviceName: string;
  readonly parameter?: string;
  readonly ts: number | null;
}

interface StatusCount {
  readonly participating: number;
  readonly normal: number;
  readonly abnormal: number;
}

/**
 * 按后端当前实时分析的统计口径，从 WebSocket 最新值计算整页实时汇总。
 *
 * <p>传入设备由当前已加载的可见设备上下文与已选资产子树交集产生；告警设备 ID
 * 也由当前页面已加载的活动告警按设备去重得到。本函数只替代重复的实时汇总请求，
 * 不读取历史周期数据，也不自行扩张用户可见范围。</p>
 */
export function buildSmartLightningMonitoringRealtimeAnalysis(
  snapshots: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>,
  fallbackLatestArcReportTime: number | null = null
): SmartLightningRealtimeAnalysis {
  const spdDevices = snapshots.filter((device) => device.functionCategory === 'spd_monitor');
  const groundDevices = snapshots.filter((device) => device.functionCategory === 'ground_resistance');
  const currentDevices = snapshots.filter((device) => device.functionCategory === 'lightning_current');
  const arcFaultDevices = snapshots.filter((device) => device.functionCategory === 'arc_fault');

  const overall = buildOverall(snapshots, activeAlarmDeviceIds);
  const spd = buildSpd(spdDevices, activeAlarmDeviceIds);
  const ground = buildGround(groundDevices, activeAlarmDeviceIds);
  const lightningCurrent = buildCurrent(currentDevices, activeAlarmDeviceIds);
  const arcFault = buildArcFault(arcFaultDevices, activeAlarmDeviceIds, fallbackLatestArcReportTime);

  return {overall, spd, ground, lightningCurrent, arcFault};
}

/** 汇总整体设备数、在线数和有活动告警的设备数。 */
function buildOverall(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>
): SmartLightningRealtimeOverall {
  const counts = countDevices(devices, activeAlarmDeviceIds);
  return {...counts, status: resolveStatus(counts.totalDevices, counts.offlineDevices, counts.alarmDeviceCount, false)};
}

/** 按设备统计空开、接地、温度、寿命和最大漏流，保持后端原有阈值与异常规则。 */
function buildSpd(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>
): SmartLightningRealtimeSpd {
  const counts = countDevices(devices, activeAlarmDeviceIds);
  const breakerStatus = countBinaryStatus(devices, 'McbStatus');
  const groundStatus = countBinaryStatus(devices, 'GndStatus');
  const temperatureStatus = countTemperatureStatus(devices);
  const lifeStatus = countLifeStatus(devices);
  let maximumLeakage: NumericSource | null = null;

  for (const device of devices) {
    for (const parameter of ['LeakCurrA', 'LeakCurrB', 'LeakCurrC']) {
      const value = parseFiniteNumber(device.values.get(parameter));
      if (value == null) {
        continue;
      }
      const absoluteValue = Math.abs(value);
      if (maximumLeakage == null || absoluteValue > maximumLeakage.value) {
        maximumLeakage = createNumericSource(absoluteValue, device, parameter, device.values.get(parameter));
      }
    }
  }

  const hasAbnormalTelemetry = breakerStatus.abnormal > 0 || groundStatus.abnormal > 0 ||
    temperatureStatus.abnormal > 0 || lifeStatus.abnormal > 0;
  let status = resolveStatus(counts.totalDevices, counts.offlineDevices, counts.alarmDeviceCount, hasAbnormalTelemetry);
  if (counts.totalDevices > 0 && counts.alarmDeviceCount === 0 && counts.offlineDevices === 0 &&
      breakerStatus.participating === 0 && groundStatus.participating === 0 &&
      temperatureStatus.participating === 0 && lifeStatus.participating === 0 && maximumLeakage == null) {
    status = '数据不足';
  }

  return {
    ...counts,
    breakerStatusNormalCount: breakerStatus.normal,
    breakerStatusDeviceCount: breakerStatus.participating,
    groundStatusNormalCount: groundStatus.normal,
    groundStatusDeviceCount: groundStatus.participating,
    temperatureNormalCount: temperatureStatus.normal,
    temperatureDeviceCount: temperatureStatus.participating,
    lifeNormalCount: lifeStatus.normal,
    lifeDeviceCount: lifeStatus.participating,
    maximumLeakage: maximumLeakage?.value ?? null,
    maximumLeakageDeviceName: maximumLeakage?.deviceName ?? null,
    maximumLeakageParameter: maximumLeakage?.parameter ?? null,
    maximumLeakageTime: maximumLeakage?.ts ?? null,
    status
  };
}

/** 汇总接地电阻的当前最大、最小值及对应设备和时间。 */
function buildGround(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>
): SmartLightningRealtimeGround {
  const counts = countDevices(devices, activeAlarmDeviceIds);
  let maximum: NumericSource | null = null;
  let minimum: NumericSource | null = null;

  for (const device of devices) {
    const entry = device.values.get('GndGridRes');
    const value = parseFiniteNumber(entry);
    if (value == null) {
      continue;
    }
    if (maximum == null || value > maximum.value) {
      maximum = createNumericSource(value, device, undefined, entry);
    }
    if (minimum == null || value < minimum.value) {
      minimum = createNumericSource(value, device, undefined, entry);
    }
  }

  const reportingDeviceCount = devices.filter((device) => parseFiniteNumber(device.values.get('GndGridRes')) != null).length;
  let status = resolveStatus(counts.totalDevices, counts.offlineDevices, counts.alarmDeviceCount, false);
  if (counts.totalDevices > 0 && counts.alarmDeviceCount === 0 && counts.offlineDevices === 0 && reportingDeviceCount === 0) {
    status = '数据不足';
  }

  return {
    ...counts,
    reportingDeviceCount,
    maximumResistance: maximum?.value ?? null,
    maximumResistanceDeviceName: maximum?.deviceName ?? null,
    maximumResistanceTime: maximum?.ts ?? null,
    minimumResistance: minimum?.value ?? null,
    minimumResistanceDeviceName: minimum?.deviceName ?? null,
    minimumResistanceTime: minimum?.ts ?? null,
    status
  };
}

/** 汇总各设备当前累计雷击次数及最新峰值，并保留峰值设备、极性和时间。 */
function buildCurrent(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>
): SmartLightningRealtimeCurrent {
  const counts = countDevices(devices, activeAlarmDeviceIds);
  let totalSurgeCount = 0;
  let latestPeak: NumericSource | null = null;
  let latestPeakPolarity: string | null = null;

  for (const device of devices) {
    const surgeCount = parseFiniteNumber(device.values.get('SurgeCnt'));
    if (surgeCount != null && surgeCount >= 0) {
      // 与 Java Math.round 对非负累计值的结果一致。
      totalSurgeCount += Math.floor(surgeCount + 0.5);
    }

    const peakEntry = device.values.get('PeakValue');
    const peak = parseFiniteNumber(peakEntry);
    const peakTime = getTimestamp(peakEntry);
    if (peak != null && peakTime != null && (latestPeak == null || peakTime > (latestPeak.ts ?? 0))) {
      latestPeak = createNumericSource(Math.abs(peak), device, undefined, peakEntry);
      const polarity = device.values.get('Polarity');
      latestPeakPolarity = hasStoredValue(polarity) ? String(polarity.value) : null;
    }
  }

  return {
    ...counts,
    totalSurgeCount,
    latestPeakCurrent: latestPeak?.value ?? null,
    latestPeakDeviceName: latestPeak?.deviceName ?? null,
    latestPeakPolarity,
    latestPeakTime: latestPeak?.ts ?? null,
    peakCurrentUnit: 'kA',
    status: resolveStatus(counts.totalDevices, counts.offlineDevices, counts.alarmDeviceCount, false)
  };
}

/** 汇总故障电弧状态；动作次数维持后端当前固定为零的既有口径。 */
function buildArcFault(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>,
  fallbackLatestReportTime: number | null
): SmartLightningRealtimeArcFault {
  const counts = countDevices(devices, activeAlarmDeviceIds);
  let statusDeviceCount = 0;
  let normalStatusCount = 0;
  let latestReportTime: number | null = null;
  let hasAbnormalTelemetry = false;

  for (const device of devices) {
    const status = device.values.get('DevStatus');
    if (hasStoredValue(status)) {
      statusDeviceCount++;
      if (String(status.value).trim() === '正常') {
        normalStatusCount++;
      } else {
        hasAbnormalTelemetry = true;
      }
    }
    for (const entry of device.values.values()) {
      const timestamp = getTimestamp(entry);
      if (timestamp != null && (latestReportTime == null || timestamp > latestReportTime)) {
        latestReportTime = timestamp;
      }
    }
  }

  if (fallbackLatestReportTime != null && Number.isFinite(fallbackLatestReportTime) &&
      (latestReportTime == null || fallbackLatestReportTime > latestReportTime)) {
    latestReportTime = fallbackLatestReportTime;
  }

  let status = resolveStatus(counts.totalDevices, counts.offlineDevices, counts.alarmDeviceCount, hasAbnormalTelemetry);
  if (counts.totalDevices > 0 && counts.alarmDeviceCount === 0 && counts.offlineDevices === 0 &&
      statusDeviceCount === 0 && latestReportTime == null) {
    status = '数据不足';
  }

  return {
    ...counts,
    normalStatusCount,
    statusDeviceCount,
    totalArcFaultCount: 0,
    latestReportTime,
    status
  };
}

/** 统计分类设备数、在线设备数及告警设备数；告警以设备 ID 去重。 */
function countDevices(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  activeAlarmDeviceIds: ReadonlySet<string>
): SmartLightningRealtimeOverall {
  const onlineDevices = devices.filter(isOnline).length;
  const alarmDeviceCount = devices.filter((device) => activeAlarmDeviceIds.has(device.deviceId)).length;
  return {
    totalDevices: devices.length,
    onlineDevices,
    offlineDevices: devices.length - onlineDevices,
    alarmDeviceCount,
    status: resolveStatus(devices.length, devices.length - onlineDevices, alarmDeviceCount, false)
  };
}

/** 按后端约定识别在线值 true 或 1；其他值均视为离线。 */
function isOnline(device: SmartLightningMonitoringRealtimeDeviceSnapshot): boolean {
  const active = device.values.get('active');
  const value = hasStoredValue(active) ? active.value : device.active;
  if (value === true || value === 1) {
    return true;
  }
  return typeof value === 'string' && (value.trim().toLowerCase() === 'true' || value.trim() === '1');
}

/** 对指定二元状态键统计有值设备数、正常数和异常数。 */
function countBinaryStatus(
  devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[],
  key: string
): StatusCount {
  let participating = 0;
  let normal = 0;
  let abnormal = 0;
  for (const device of devices) {
    const entry = device.values.get(key);
    if (!hasStoredValue(entry)) {
      continue;
    }
    participating++;
    const value = String(entry.value).trim();
    if (value === '0' || value === '0.0') {
      normal++;
    } else {
      abnormal++;
    }
  }
  return {participating, normal, abnormal};
}

/** 任意 Temp 键都参与统计；任一温度达到 65 ℃ 或无法解析即判该设备异常。 */
function countTemperatureStatus(devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[]): StatusCount {
  let participating = 0;
  let normal = 0;
  let abnormal = 0;
  for (const device of devices) {
    const temperatures = [...device.values.entries()]
      .filter(([key, entry]) => key.includes('Temp') && hasStoredValue(entry))
      .map(([, entry]) => parseFiniteNumber(entry));
    if (!temperatures.length) {
      continue;
    }
    participating++;
    if (temperatures.some((value) => value == null || value >= 65)) {
      abnormal++;
    } else {
      normal++;
    }
  }
  return {participating, normal, abnormal};
}

/** LifeLeft 键存在即计入设备总数；有效值不低于 60 才计为正常。 */
function countLifeStatus(devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[]): StatusCount {
  let participating = 0;
  let normal = 0;
  let abnormal = 0;
  for (const device of devices) {
    const entry = device.values.get('LifeLeft');
    if (!hasStoredValue(entry)) {
      continue;
    }
    participating++;
    const value = parseFiniteNumber(entry);
    if (value != null && value >= 60) {
      normal++;
    } else {
      abnormal++;
    }
  }
  return {participating, normal, abnormal};
}

/** 判断遥测键是否确实带有存储值；空字符串仍视为已上报，沿用后端统计约定。 */
function hasStoredValue(entry: StoredValue | undefined): entry is StoredValue {
  return entry != null && entry.value != null;
}

/** 解析有限数值；空文本、非法文本和无穷值与后端一样不参与数值最值。 */
function parseFiniteNumber(entry: StoredValue | undefined): number | null {
  if (!hasStoredValue(entry)) {
    return null;
  }
  const text = String(entry.value).trim();
  if (!text) {
    return null;
  }
  const value = Number(text);
  return Number.isFinite(value) ? value : null;
}

/** 提取有效遥测毫秒时间戳。 */
function getTimestamp(entry: StoredValue | undefined): number | null {
  if (!entry) {
    return null;
  }
  const timestamp = Number(entry.ts);
  return Number.isFinite(timestamp) && timestamp > 0 ? timestamp : null;
}

/** 保留最值来源设备、参数与原始遥测时间，供页面显示和悬浮提示复用。 */
function createNumericSource(
  value: number,
  device: SmartLightningMonitoringRealtimeDeviceSnapshot,
  parameter: string | undefined,
  entry: StoredValue | undefined
): NumericSource {
  return {
    value,
    deviceName: device.deviceName,
    ...(parameter ? {parameter} : {}),
    ts: getTimestamp(entry)
  };
}

/** 生成与后端相同优先级的整体状态：告警/异常、离线、正常、无设备。 */
function resolveStatus(total: number, offline: number, alarmDevices: number, abnormalTelemetry: boolean): string {
  if (total === 0) {
    return '数据不足';
  }
  if (alarmDevices > 0 || abnormalTelemetry) {
    return '异常';
  }
  if (offline > 0) {
    return '需关注';
  }
  return '正常';
}
