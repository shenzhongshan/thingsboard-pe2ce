/** 文件职责：把电气安全设备、告警和资产树接口结果转换为看板展示模型。 */
import type {AlarmInfo} from '@shared/models/alarm.models';
import type {PageData} from '@shared/models/page/page-data';
import type {SmartLightningMapDevice} from '../../smart-lightning/shared/map/smart-lightning-device-map.models';
import type {DeviceAlarmDisplayColumn, DeviceAlarmDisplayRow} from '@home/pages/deviceAlarms/shared/alarm-list/device-alarm-records.model';
import type {DeviceAlarmInformationChartItem} from '@home/pages/deviceAlarms/shared/alarm-information/device-alarm-information-chart.model';
import {getSmartLightningPhaseLabel} from '../../smart-lightning/dashboard/smart-lightning-summary.data';
import type {
  ElectricalSafetyAssetTreeNode,
  ElectricalSafetyDeviceContext
} from '../electrical-safety-api.service';
import {
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
  formatElectricalSafetyActivityTime,
  formatElectricalSafetyRealtimeMetricValue,
  getElectricalSafetyRealtimeMetricDefinitions
} from '../monitoring/electrical-safety-monitoring.data';
import type {
  ElectricalSafetyMonitoringTelemetryValue,
  ElectricalSafetyRealtimeMetricDefinition
} from '../monitoring/electrical-safety-monitoring.data';

/** 看板右侧电气安全设备卡片的分组标识。 */
export type ElectricalSafetyDashboardCategory = 'fire' | 'safe-electricity' | 'power-quality' | 'arc-fault';

/** 单张电气安全类别卡片的设备状态统计及火灾类周期告警事件数。 */
export interface ElectricalSafetyDashboardCard {
  /** 稳定的分组键，用于模板追踪及对应样式。 */
  key: ElectricalSafetyDashboardCategory;
  /** 卡片显示名称。 */
  title: string;
  /** 项目图标库中的图标名称。 */
  icon: string;
  /** 该类别的设备总数；设备上下文未加载时为空。 */
  totalCount: number | null;
  /** 该类别在线设备数；设备上下文未加载时为空。 */
  onlineCount: number | null;
  /** 当前没有活动告警的设备数；告警接口未加载时为空。 */
  normalCount: number | null;
  /** 该类别离线设备数；设备上下文未加载时为空。 */
  offlineCount: number | null;
  /** 当前至少有一条未清除告警的设备数；告警接口未加载时为空。 */
  alarmDeviceCount: number | null;
  /** 在线设备占该类别设备总数的百分比。 */
  onlinePercent: number | null;
  /** 离线设备占该类别设备总数的百分比。 */
  offlinePercent: number | null;
  /** 活动告警设备占该类别设备总数的百分比。 */
  alarmDevicePercent: number | null;
  /** 无活动告警设备占该类别设备总数的百分比。 */
  normalPercent: number | null;
  /** 火灾卡片所含功能分类在所选自然周期内的告警事件数；其他卡片不显示。 */
  alarmEventCount: number | null;
  /** 电弧设备中已上报 DevStatus 状态的设备数；非电弧卡片不使用。 */
  arcFaultStatusDeviceCount: number | null;
  /** 电弧设备中 DevStatus 映射为正常的设备数；非电弧卡片不使用。 */
  arcFaultNormalStatusCount: number | null;
  /** 兼容原防雷卡片既有口径；当前不从未确认的遥测键推算动作次数。 */
  totalArcFaultActionCount: number | null;
  /** 电弧设备最近的活动或遥测时间，单位为毫秒。 */
  latestArcFaultReportTime: number | null;
}

/** 火灾卡片中按已核实型号遥测汇总的单个风险测点最大值。 */
export interface ElectricalSafetyFireRiskMetric {
  /** 当前已接收的最大原始数值；没有配置或没有遥测时为空。 */
  value: string | null;
  /** 单位来自该设备型号的规范测点定义；未配置时为空。 */
  unit: string | null;
  /** 该最大值来自的设备名称；没有可用遥测时为空。 */
  sourceDeviceName: string | null;
  /** 只有规范遥测键明确标出 A/B/C 相时才填写，不根据设备型号或中文标签推测。 */
  phase: string | null;
  /** 产生该最大值的最新遥测样本时间戳，单位为毫秒。 */
  timestamp: number | null;
  /** 来源遥测的原始规范键；无法从键名解析相位时用于提示具体参数。 */
  parameterKey: string | null;
  /** 型号配置中是否存在该类测点，用于区分未配置与暂时无数据。 */
  configured: boolean;
}

/** 电气火灾卡片展示的剩余电流和温度实时最大值。 */
export interface ElectricalSafetyFireRiskMeasurements {
  readonly residualCurrentMax: ElectricalSafetyFireRiskMetric;
  readonly highestTemperature: ElectricalSafetyFireRiskMetric;
}

/** 设备总览中的设备数和在线率；不同数据源加载失败时对应字段保留未知。 */
export interface ElectricalSafetyDeviceOverview {
  total: number | null;
  online: number | null;
  offline: number | null;
  alarm: number | null;
  onlineRate: number | null;
}

/** 接口尚未返回设备上下文或活动告警范围时统一显示为未知。 */
export const EMPTY_ELECTRICAL_SAFETY_DEVICE_OVERVIEW: ElectricalSafetyDeviceOverview = {
  total: null,
  online: null,
  offline: null,
  alarm: null,
  onlineRate: null
};

/** 电气安全告警统计行遵循共享图表契约，标签按功能大类显示。 */
export type ElectricalSafetyAlarmChartItem = DeviceAlarmInformationChartItem;

/** 电气安全告警列表沿用共享表格列契约和本模块原有列顺序。 */
export const ELECTRICAL_SAFETY_ALARM_COLUMNS: readonly DeviceAlarmDisplayColumn[] = [
  {field: 'deviceName', label: '设备名称', widthPercent: 21},
  {field: 'deviceType', label: '设备类型', widthPercent: 19},
  {field: 'source', label: '设备位置', widthPercent: 14},
  {field: 'severity', label: '报警级别', widthPercent: 9},
  {field: 'type', label: '报警情况', widthPercent: 21},
  {field: 'startTs', label: '异常发生时间', widthPercent: 16}
];

/** 看板内固定的类别及项目配色图标，不包含任何演示统计值。 */
const ELECTRICAL_SAFETY_DASHBOARD_CATEGORIES: ReadonlyArray<{
  key: ElectricalSafetyDashboardCategory;
  title: string;
  icon: string;
}> = [
  {key: 'fire', title: '电气火灾监测', icon: 'local_fire_department'},
  {key: 'safe-electricity', title: '安全用电监测', icon: 'electrical_services'},
  {key: 'power-quality', title: '电力质量监测', icon: 'monitoring'},
  {key: 'arc-fault', title: '故障电弧监测', icon: 'crisis_alert'}
];

/** 火灾卡片汇总当前四类编码和未迁移数据库中保留的旧分类告警事件。 */
const ELECTRICAL_SAFETY_FIRE_FUNCTION_CATEGORIES = [
  'electrical_fire', 'temperature_monitor', 'leakage_monitor'
] as const;

/** 未取得设备上下文时构造四张空卡片；所有业务计数均保持未知而不预置模拟数字。 */
export function buildElectricalSafetyDashboardCards(
  contexts: readonly ElectricalSafetyDeviceContext[] | null,
  activeAlarmDeviceIds: ReadonlySet<string> | null,
  alarmEventCountsByFunctionCategory: ReadonlyMap<string, number> | null = null,
  telemetryByDeviceId: ReadonlyMap<
    string,
    ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>
  > = new Map()
): ElectricalSafetyDashboardCard[] {
  return ELECTRICAL_SAFETY_DASHBOARD_CATEGORIES.map((category) => {
    const devices = contexts === null
      ? null
      : contexts.filter((device) => isElectricalSafetyDevice(device)
        && resolveDashboardCategory(device.functionCategory) === category.key);
    const categoryAlarmIds = activeAlarmDeviceIds === null || devices === null
      ? null
      : new Set(devices.filter((device) => activeAlarmDeviceIds.has(device.deviceId))
        .map((device) => device.deviceId));
    const total = devices?.length ?? null;
    const onlineCount = devices?.filter((device) => device.active === true).length ?? null;
    const offlineCount = total === null || onlineCount === null ? null : total - onlineCount;
    const alarmDeviceCount = categoryAlarmIds?.size ?? null;
    const normalCount = total === null || alarmDeviceCount === null ? null : total - alarmDeviceCount;
    const arcFaultMetrics = category.key === 'arc-fault' && devices !== null
      ? buildArcFaultDashboardMetrics(devices, telemetryByDeviceId)
      : {statusDeviceCount: null, normalStatusCount: null, latestReportTime: null};
    return {
      ...category,
      totalCount: total,
      onlineCount,
      normalCount,
      offlineCount,
      alarmDeviceCount,
      onlinePercent: calculatePercentage(onlineCount, total),
      offlinePercent: calculatePercentage(offlineCount, total),
      alarmDevicePercent: calculatePercentage(alarmDeviceCount, total),
      normalPercent: calculatePercentage(normalCount, total),
      alarmEventCount: category.key === 'fire'
        ? sumElectricalSafetyFireAlarmEvents(alarmEventCountsByFunctionCategory)
        : null,
      arcFaultStatusDeviceCount: arcFaultMetrics.statusDeviceCount,
      arcFaultNormalStatusCount: arcFaultMetrics.normalStatusCount,
      totalArcFaultActionCount: category.key === 'arc-fault' && devices !== null ? 0 : null,
      latestArcFaultReportTime: arcFaultMetrics.latestReportTime
    };
  });
}

/** 汇总电弧设备已映射的 DevStatus 和最近数据时间，不额外发起遥测请求。 */
function buildArcFaultDashboardMetrics(
  devices: readonly ElectricalSafetyDeviceContext[],
  telemetryByDeviceId: ReadonlyMap<
    string,
    ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>
  >
): {statusDeviceCount: number; normalStatusCount: number; latestReportTime: number | null} {
  let statusDeviceCount = 0;
  let normalStatusCount = 0;
  let latestReportTime: number | null = null;

  for (const device of devices) {
    const latestValues = telemetryByDeviceId.get(device.deviceId);
    const statusSample = latestValues?.get('DevStatus');
    if (statusSample && statusSample.value.trim() !== '') {
      const statusDefinition = getElectricalSafetyRealtimeMetricDefinitions(device.deviceProfileName)
        .find((definition) => definition.key === 'DevStatus');
      const statusValue = statusDefinition
        ? formatElectricalSafetyRealtimeMetricValue(statusDefinition, statusSample.value)
        : statusSample.value;
      statusDeviceCount++;
      if (statusValue === '正常') {
        normalStatusCount++;
      }
    }

    const activityTime = Number(device.lastActivityTime);
    if (Number.isFinite(activityTime) && activityTime > 0
      && (latestReportTime === null || activityTime > latestReportTime)) {
      latestReportTime = activityTime;
    }
    latestValues?.forEach((sample) => {
      if (Number.isFinite(sample.ts) && sample.ts > 0
        && (latestReportTime === null || sample.ts > latestReportTime)) {
        latestReportTime = sample.ts;
      }
    });
  }

  return {statusDeviceCount, normalStatusCount, latestReportTime};
}

/**
 * 汇总火灾卡片覆盖设备的实时剩余电流最大值和温度最大值。
 * 只消费各型号已配置的测点定义，不从未确认的遥测键推断单位或业务含义。
 */
export function buildElectricalSafetyFireRiskMeasurements(
  contexts: readonly ElectricalSafetyDeviceContext[] | null,
  telemetryByDeviceId: ReadonlyMap<
    string,
    ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>
  > = new Map()
): ElectricalSafetyFireRiskMeasurements {
  const residualCurrent: ElectricalSafetyFireRiskMetric = {
    value: null,
    unit: null,
    sourceDeviceName: null,
    phase: null,
    timestamp: null,
    parameterKey: null,
    configured: false
  };
  const temperature: ElectricalSafetyFireRiskMetric = {
    value: null,
    unit: null,
    sourceDeviceName: null,
    phase: null,
    timestamp: null,
    parameterKey: null,
    configured: false
  };
  let residualCurrentNumericValue: number | null = null;
  let temperatureNumericValue: number | null = null;

  for (const context of contexts || []) {
    if (!isElectricalSafetyDevice(context) || !isElectricalSafetyFireFunctionCategory(context.functionCategory)) {
      continue;
    }
    const latestValues = telemetryByDeviceId.get(context.deviceId);
    const definitions = getElectricalSafetyRealtimeMetricDefinitions(context.deviceProfileName);
    for (const definition of definitions) {
      if (!isElectricalSafetyFireRiskMetricDefinition(definition)) {
        continue;
      }
      const isResidualCurrent = definition.label === '剩余电流';
      if (isResidualCurrent) {
        residualCurrent.configured = true;
      } else {
        temperature.configured = true;
      }
      const sample = latestValues?.get(definition.key);
      const numericValue = parseElectricalSafetyTelemetryNumber(sample?.value);
      if (sample === undefined || numericValue === null) {
        continue;
      }
      if (isResidualCurrent
        && (residualCurrentNumericValue === null || numericValue > residualCurrentNumericValue)) {
        residualCurrentNumericValue = numericValue;
        residualCurrent.value = sample.value;
        residualCurrent.unit = definition.unit || null;
        residualCurrent.sourceDeviceName = context.deviceName?.trim() || null;
        residualCurrent.phase = getElectricalSafetyMetricPhase(definition.key);
        residualCurrent.timestamp = Number.isFinite(sample.ts) ? sample.ts : null;
        residualCurrent.parameterKey = definition.key;
      }
      if (isElectricalSafetyTemperatureUnit(definition.unit)
        && (temperatureNumericValue === null || numericValue > temperatureNumericValue)) {
        temperatureNumericValue = numericValue;
        temperature.value = sample.value;
        temperature.unit = definition.unit || null;
        temperature.sourceDeviceName = context.deviceName?.trim() || null;
        temperature.phase = getElectricalSafetyMetricPhase(definition.key);
        temperature.timestamp = Number.isFinite(sample.ts) ? sample.ts : null;
        temperature.parameterKey = definition.key;
      }
    }
  }

  return {
    residualCurrentMax: residualCurrent,
    highestTemperature: temperature
  };
}

/** 只在已配置的火灾风险测点发生变化时触发实时最大值重算。 */
export function hasElectricalSafetyFireRiskMetricUpdate(
  deviceProfileName: string | null | undefined,
  changedValues: ReadonlyMap<string, unknown>
): boolean {
  const definitions = getElectricalSafetyRealtimeMetricDefinitions(deviceProfileName);
  for (const definition of definitions) {
    if (isElectricalSafetyFireRiskMetricDefinition(definition) && changedValues.has(definition.key)) {
      return true;
    }
  }
  return false;
}

/**
 * 依据真实电气安全设备上下文和活动告警设备集合构建总览。
 * active=true 计为在线，其他状态计为离线；告警数按有未清除告警的设备去重。
 */
export function buildElectricalSafetyDeviceOverview(
  contexts: readonly ElectricalSafetyDeviceContext[] | null,
  activeAlarmDeviceIds: ReadonlySet<string> | null
): ElectricalSafetyDeviceOverview {
  if (contexts === null) {
    return {...EMPTY_ELECTRICAL_SAFETY_DEVICE_OVERVIEW};
  }
  const devices = contexts.filter(isElectricalSafetyDevice);
  const total = devices.length;
  const online = devices.filter((device) => device.active === true).length;
  const alarm = activeAlarmDeviceIds === null
    ? null
    : new Set(devices.filter((device) => activeAlarmDeviceIds.has(device.deviceId))
      .map((device) => device.deviceId)).size;
  return {
    total,
    online,
    offline: total - online,
    alarm,
    onlineRate: total === 0 ? 0 : Math.round(online / total * 100)
  };
}

/** 根据已解析的实时或服务器坐标生成地图模型；无合法坐标的设备不发送给地图。 */
export function buildElectricalSafetyMapDevices(
  contexts: readonly ElectricalSafetyDeviceContext[] | null,
  assetRoots: readonly ElectricalSafetyAssetTreeNode[] | null,
  activeAlarmDeviceIds: ReadonlySet<string> | null
): SmartLightningMapDevice[] {
  if (contexts === null) {
    return [];
  }
  const assetPaths = buildElectricalSafetyAssetPaths(assetRoots || []);
  const mapDevices: SmartLightningMapDevice[] = [];
  for (const context of contexts) {
    if (!isElectricalSafetyDevice(context)
      || !isValidLatitude(context.latitude)
      || !isValidLongitude(context.longitude)) {
      continue;
    }
    const deviceId = context.deviceId;
    const assetPath = getDeviceParentPath(assetPaths.get(deviceId), context.deviceName);
    mapDevices.push({
      id: deviceId,
      name: context.deviceName || '--',
      productName: context.productName?.trim() || '--',
      profileName: context.deviceProfileName?.trim() || '--',
      productCategory: context.productCategory,
      active: context.active === true,
      latitude: context.latitude,
      longitude: context.longitude,
      assetPath,
      // 地图仅需要说明设备当前有未清除告警；不在没有告警详情时伪造具体告警名称。
      alarmNames: activeAlarmDeviceIds?.has(deviceId) ? ['存在未清除告警'] : []
    });
  }
  return mapDevices;
}

/** 按告警记录的原始 type 名称统计当前活动告警；不改写或归并告警名称。 */
export function buildElectricalSafetyAlarmChart(
  alarms: readonly AlarmInfo[] | null
): ElectricalSafetyAlarmChartItem[] {
  if (!alarms?.length) {
    return [];
  }
  const totals = new Map<string, number>();
  for (const alarm of alarms) {
    const alarmType = alarm.type;
    if (!alarmType || !alarmType.trim()) {
      continue;
    }
    totals.set(alarmType, (totals.get(alarmType) || 0) + 1);
  }
  const items: ElectricalSafetyAlarmChartItem[] = [];
  for (const [label, count] of totals) {
    items.push({label, count});
  }
  items.sort((left, right) => (right.count ?? 0) - (left.count ?? 0)
    || left.label.localeCompare(right.label));
  return items;
}

/**
 * 将平台原生告警分页映射为共享告警记录表契约。
 * 设备名称和型号取电气安全设备上下文，设备位置取本模块权限裁剪后的资产树路径。
 */
export function mapElectricalSafetyAlarms(
  alarmPage: PageData<AlarmInfo> | null,
  contexts: readonly ElectricalSafetyDeviceContext[] | null,
  assetRoots: readonly ElectricalSafetyAssetTreeNode[] | null
): DeviceAlarmDisplayRow[] {
  if (!alarmPage?.data?.length) {
    return [];
  }
  const devicesById = new Map<string, ElectricalSafetyDeviceContext>();
  for (const context of contexts || []) {
    if (isElectricalSafetyDevice(context)) {
      devicesById.set(context.deviceId, context);
    }
  }
  const assetPaths = buildElectricalSafetyAssetPaths(assetRoots || []);
  return alarmPage.data.map((alarm) => {
    const originatorId = alarm.originator?.id || '';
    const device = devicesById.get(originatorId);
    const deviceName = device?.deviceName || alarm.originatorName || '--';
    const severity = mapAlarmSeverity(alarm.severity);
    const id = alarm.id?.id || `${originatorId}-${alarm.startTs}`;
    return {
      source: getDeviceParentPath(assetPaths.get(originatorId), deviceName),
      secondaryLocation: '--',
      device: id,
      deviceType: device?.deviceProfileName?.trim() || device?.productName?.trim() || '--',
      deviceName,
      type: alarm.type || '--',
      severity: severity.label,
      severityTone: severity.tone,
      status: mapAlarmStatus(alarm.status),
      startTs: Number.isFinite(alarm.startTs) ? alarm.startTs : Number.NaN
    };
  });
}

/** 判断设备上下文是否属于本模块产品大类，并具备设备标识。 */
function isElectricalSafetyDevice(context: ElectricalSafetyDeviceContext): boolean {
  return context.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY && !!context.deviceId;
}

/** 根据设备配置中的规范功能类型归入看板卡片，不依据设备名称或型号猜测。 */
function resolveDashboardCategory(functionCategory: string | null | undefined): ElectricalSafetyDashboardCategory {
  if (isElectricalSafetyFireFunctionCategory(functionCategory)) {
    return 'fire';
  }
  if (functionCategory === 'power_quality' || functionCategory === 'phase_unbalance') {
    return 'power-quality';
  }
  if (functionCategory === 'arc_fault') {
    return 'arc-fault';
  }
  // 新的安全用电分类及未迁移的输入、断路器、市电和零地电压分类归入安全用电卡片。
  return 'safe-electricity';
}

/** 判断设备功能分类是否属于火灾卡片已确认的覆盖范围。 */
export function isElectricalSafetyFireFunctionCategory(functionCategory: string | null | undefined): boolean {
  return ELECTRICAL_SAFETY_FIRE_FUNCTION_CATEGORIES.some((category) => category === functionCategory);
}

/** 测点定义的规范单位用于识别温度值，避免在页面层另行猜测字段名。 */
function isElectricalSafetyTemperatureUnit(unit: string): boolean {
  return unit === '℃' || unit === '°C';
}

/** 优先复用防雷的参数键相位解析规则，并补充 SFA 的规范相电流键。 */
function getElectricalSafetyMetricPhase(parameterKey: string): string | null {
  const lightningPhase = getSmartLightningPhaseLabel(parameterKey);
  if (lightningPhase !== '--') {
    return lightningPhase;
  }

  // SFA 使用 PhACurr、PhBCurr、PhCCurr 表示 A/B/C 相电流；相位必须由键名明确给出。
  const phaseMatch = parameterKey.trim().match(/^Ph([ABC])Curr$/i);
  return phaseMatch ? `${phaseMatch[1].toUpperCase()}相` : null;
}

/** 剩余电流按规范中文测点名识别；温度仅接受明确标注为温度单位的型号测点。 */
function isElectricalSafetyFireRiskMetricDefinition(
  definition: ElectricalSafetyRealtimeMetricDefinition
): boolean {
  return definition.valueType === 'telemetry'
    && (definition.label === '剩余电流' || isElectricalSafetyTemperatureUnit(definition.unit));
}

/** 将已接收的数值遥测解析为有限数字；空值、非数值和无穷值不参与最大值统计。 */
function parseElectricalSafetyTelemetryNumber(value: string | undefined): number | null {
  if (value === undefined || value.trim() === '') {
    return null;
  }
  const parsedValue = Number(value);
  return Number.isFinite(parsedValue) ? parsedValue : null;
}

/** 生成火灾风险最大值的原生提示；按防雷规则显示可解析相位，否则显示原始参数键。 */
export function formatElectricalSafetyFireRiskMetricTooltip(
  label: string,
  metric: ElectricalSafetyFireRiskMetric
): string {
  const formattedValue = metric.value === null
    ? '--'
    : `${metric.value}${metric.unit ? ` ${metric.unit}` : ''}`;
  const sourceDetail = metric.phase
    ? `相位：${metric.phase}`
    : `参数：${metric.parameterKey || '--'}`;
  return [
    `${label}：${formattedValue}`,
    `设备：${metric.sourceDeviceName || '--'}`,
    sourceDetail,
    `时间：${formatElectricalSafetyActivityTime(metric.timestamp)}`
  ].join('\n');
}

/** 统计成功但某分类没有事件时按零计；请求未成功时保留 null 表示未知。 */
function sumElectricalSafetyFireAlarmEvents(
  alarmEventCountsByFunctionCategory: ReadonlyMap<string, number> | null
): number | null {
  if (alarmEventCountsByFunctionCategory === null) {
    return null;
  }
  let total = 0;
  for (const functionCategory of ELECTRICAL_SAFETY_FIRE_FUNCTION_CATEGORIES) {
    total += alarmEventCountsByFunctionCategory.get(functionCategory) || 0;
  }
  return total;
}

/** 将没有设备总数的分类占比设为 0；数据源未加载时保留 null 表示未知。 */
function calculatePercentage(value: number | null, total: number | null): number | null {
  if (value === null || total === null) {
    return null;
  }
  return total === 0 ? 0 : Math.round(value / total * 100);
}

/** 递归建立资产/设备 ID 到从根节点开始的绝对名称路径映射。 */
function buildElectricalSafetyAssetPaths(
  roots: readonly ElectricalSafetyAssetTreeNode[]
): Map<string, string> {
  const paths = new Map<string, string>();
  const visit = (node: ElectricalSafetyAssetTreeNode, ancestorNames: readonly string[]): void => {
    const currentName = node.name?.trim() || node.id;
    const currentPath = [...ancestorNames, currentName];
    paths.set(node.id, currentPath.join('/'));
    for (const child of node.children || []) {
      visit(child, currentPath);
    }
  };
  for (const root of roots) {
    visit(root, []);
  }
  return paths;
}

/** 设备地图和告警表显示父资产路径；路径末尾若是设备名则只显示资产祖先。 */
function getDeviceParentPath(devicePath: string | undefined, deviceName: string): string {
  if (!devicePath) {
    return '--';
  }
  const pathParts = devicePath.split('/');
  if (pathParts[pathParts.length - 1] === deviceName) {
    pathParts.pop();
  }
  return pathParts.join('/') || '--';
}

/** 校验经纬度范围，避免把非法设备属性传给地图投影。 */
function isValidLatitude(value: number | null | undefined): value is number {
  return value !== null && value !== undefined && Number.isFinite(value) && value >= -90 && value <= 90;
}

/** 校验经度范围，避免把非法设备属性传给地图投影。 */
function isValidLongitude(value: number | null | undefined): value is number {
  return value !== null && value !== undefined && Number.isFinite(value) && value >= -180 && value <= 180;
}

/** 把平台严重程度映射成表格中文文案和既有共享色调。 */
function mapAlarmSeverity(value: string): {
  label: string;
  tone: DeviceAlarmDisplayRow['severityTone'];
} {
  switch (value) {
    case 'CRITICAL': return {label: '严重', tone: 'critical'};
    case 'MAJOR': return {label: '重要', tone: 'major'};
    case 'MINOR': return {label: '次要', tone: 'minor'};
    case 'WARNING': return {label: '警告', tone: 'warning'};
    default: return {label: '--', tone: 'indeterminate'};
  }
}

/** 把平台告警状态转换为可读文案；共享表格不处理告警业务状态。 */
function mapAlarmStatus(value: string): string {
  switch (value) {
    case 'ACTIVE_UNACK': return '待确认';
    case 'ACTIVE_ACK': return '已确认';
    case 'CLEARED_UNACK': return '已清除待确认';
    case 'CLEARED_ACK': return '已清除';
    default: return '--';
  }
}
