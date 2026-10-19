import {formatSmartLightningPolarityLabel, getSmartLightningRealtimeMetricDefinitions, resolveSmartLightningDeviceModel} from './smart-lightning-device-detail.data';
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
  /** 设备档案用于选择型号协议，不能将不同型号极性编码混用。 */
  readonly deviceProfileName?: string;
  /** 档案为自定义名称时，用明确的产品名称匹配既有型号定义。 */
  readonly productName?: string;
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
  const spdDevices = snapshots.filter((device) => device.functionCategory === 'spd_monitor')
    .map(filterSupportedSpdTelemetry);
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

/** 复用实时卡片的型号测点定义裁剪快照，防止其他型号的历史键参与WebSocket重算。 */
function filterSupportedSpdTelemetry(
  device: SmartLightningMonitoringRealtimeDeviceSnapshot
): SmartLightningMonitoringRealtimeDeviceSnapshot {
  const profile = resolveSmartLightningDeviceModel(device.deviceProfileName)
    ? device.deviceProfileName : device.productName;
  const supported = new Set(getSmartLightningRealtimeMetricDefinitions(profile).map((item) => item.key));
  return {...device, values: new Map([...device.values].filter(([key]) => supported.has(key)))};
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
  // 与Java计算器使用相同状态口径；只读取已按型号裁剪的快照。
  const protocolStatus = summarizeSpdProtocolStatus(devices);
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

  const hasAbnormalTelemetry = protocolStatus.abnormal || breakerStatus.abnormal > 0 || groundStatus.abnormal > 0 ||
    temperatureStatus.abnormal > 0 || lifeStatus.abnormal > 0;
  let status = resolveStatus(counts.totalDevices, counts.offlineDevices, counts.alarmDeviceCount, hasAbnormalTelemetry);
  if (status === '正常' && protocolStatus.needsAttention) {
    status = '需关注';
  }
  if (counts.totalDevices > 0 && counts.alarmDeviceCount === 0 && counts.offlineDevices === 0 &&
      breakerStatus.participating === 0 && groundStatus.participating === 0 &&
      temperatureStatus.participating === 0 && lifeStatus.participating === 0 && maximumLeakage == null && !protocolStatus.reported) {
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

/** 与SpdMonitoringCalculator一致的协议状态键，不将诊断说明字段当作报警开关。 */
const SPD_PROTOCOL_STATUS_KEYS = [
  'RemoteSig', 'McbStatus', 'GndStatus', 'PhAStatus', 'PhBStatus', 'PhCStatus',
  'TripSW1', 'TripSW2', 'TripSW3', 'TripSW4', 'SPD_Status', 'SPD_Alarm'
] as const;

/**
 * 前端只计算遥测诊断提示，平台告警设备数量仍来自活动告警ID集合。
 * 初始化／学习／事件及未知编码提示关注；缺失字段不算正常，也不算异常。
 */
function summarizeSpdProtocolStatus(devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[]): {
  reported: boolean; abnormal: boolean; needsAttention: boolean;
} {
  let reported = false;
  let abnormal = false;
  let needsAttention = false;
  for (const device of devices) {
    for (const key of SPD_PROTOCOL_STATUS_KEYS) {
      const entry = device.values.get(key);
      if (!hasStoredValue(entry)) {
        continue;
      }
      reported = true;
      const value = parseFiniteNumber(entry);
      if (value == null || !Number.isInteger(value)) {
        needsAttention = true;
        continue;
      }
      if (key === 'SPD_Status') {
        if (value >= 6 && value <= 8) {
          abnormal = true;
        } else if (value !== 4) {
          needsAttention = true;
        }
      } else if (key === 'SPD_Alarm') {
        if (value >= 1 && value <= 14) {
          abnormal = true;
        } else if (value !== 0) {
          needsAttention = true;
        }
      } else if (value === 1) {
        abnormal = true;
      } else if (value !== 0) {
        needsAttention = true;
      }
    }
  }
  return {reported, abnormal, needsAttention};
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
    const countKey = resolveSmartLightningDeviceModel(device.deviceProfileName) === 'M1AI' ? 'StrikeCnt' : 'SurgeCnt';
    const surgeCount = parseFiniteNumber(device.values.get(countKey));
    if (surgeCount != null && surgeCount >= 0) {
      // 与 Java Math.round 对非负累计值的结果一致。
      totalSurgeCount += Math.floor(surgeCount + 0.5);
    }

    // 两种雷电流型号均使用实际报文PeakCur，不回退旧峰值键。
    const peakKey = 'PeakCur';
    const peakEntry = device.values.get(peakKey);
    const peak = parseFiniteNumber(peakEntry);
    const peakTime = getTimestamp(peakEntry);
    if (peak != null && peakTime != null && (latestPeak == null || peakTime > (latestPeak.ts ?? 0))) {
      latestPeak = createNumericSource(Math.abs(peak), device, undefined, peakEntry);
      const polarity = device.values.get('Polarity');
      latestPeakPolarity = hasStoredValue(polarity) ? formatSmartLightningPolarityLabel(polarity.value, device.deviceProfileName) : null;
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
      .filter(([key, entry]) => /^Temp[1-4]?$/.test(key) && hasStoredValue(entry))
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

/** 仅采用新协议SPD_RUL；剩余寿命达到20%计为正常，与后端统计保持一致。 */
function countLifeStatus(devices: readonly SmartLightningMonitoringRealtimeDeviceSnapshot[]): StatusCount {
  let participating = 0;
  let normal = 0;
  let abnormal = 0;
  for (const device of devices) {
    // 缺少新协议寿命时保持缺失，不使用历史旧键补值。
    const entry = device.values.get('SPD_RUL');
    if (!hasStoredValue(entry)) {
      continue;
    }
    participating++;
    const value = parseFiniteNumber(entry);
    if (value != null && value >= 20) {
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
