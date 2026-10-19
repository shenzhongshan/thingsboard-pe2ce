/** 文件职责：定义电气安全设备页面模型，并把模块接口数据转换为页面展示数据。 */
import type {ElectricalSafetyDeviceContext} from '../electrical-safety-api.service';
import {getSmartLightningMonitoringAssetPaths} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import type {
  SmartLightningMonitoringAssetTree,
  SmartLightningMonitoringTreeResponseNode
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';

/** 设备总览分类筛选项；other 仅承接未知旧数据，不单独显示为可选分类。 */
export type ElectricalSafetyMonitoringCategoryKey =
  | 'all'
  | 'fire'
  | 'safe-electricity'
  | 'power-quality'
  | 'arc-fault'
  | 'other';

/** 顶部专项分析卡片按指标领域统计，和设备的四类产品分类分别维护。 */
export type ElectricalSafetyMonitoringSummaryCategoryKey = 'fire' | 'leakage' | 'power-quality' | 'arc-fault';

/** 设备在线状态由 active 遥测决定；告警状态需独立告警接口提供。 */
export type ElectricalSafetyConnectionState = 'online' | 'offline';

/** 电气安全产品分类服务器属性值，与 thingsboard.yml 的型号映射一致。 */
export const ELECTRICAL_SAFETY_PRODUCT_CATEGORY = 'electrical_safety';

/** 顶部监测卡片中的单个指标。无对应接口数据时显示 --。 */
export interface ElectricalSafetyMonitoringMetric {
  label: string;
  value: string;
  unit?: string;
  tone?: 'healthy' | 'warning' | 'critical' | 'neutral';
  /** 有遥测来源时显示原生悬浮说明；来源设备、参数和时间不常驻占用卡片空间。 */
  title?: string;
}

/** 设备卡片中已有 WebSocket 最新值对应的遥测项；不含本页未订阅或未经确认的键。 */
export interface ElectricalSafetyMonitoringRealtimeMetric {
  key: string;
  label: string;
  value: string;
  title: string;
  abnormal: boolean;
}

/** 单个型号已经确认的遥测定义，详情、回顾与监测卡片共享这份键名和展示信息。 */
export interface ElectricalSafetyRealtimeMetricDefinition {
  readonly key: string;
  readonly label: string;
  readonly unit: string;
  /** 状态测点只在展示层映射中文；原始遥测仍保留在实时缓存中。 */
  readonly valueType: 'status' | 'telemetry';
  /** 与防雷详情页一致，配置测点缺值时使用的占位文本。 */
  readonly fallbackValue: string;
  /** 状态值的展示映射；不改变 ThingsBoard 返回的原始遥测。 */
  readonly valueMap?: Readonly<Record<string, string>>;
  /** 指明该键可参与哪一类看板摘要；不参与的详情测点保持未设置。 */
  readonly dashboardMetric?: ElectricalSafetyDashboardMetricKey;
}

/** 看板摘要使用的已核实指标语义；型号数组仍负责声明各键的单位和来源。 */
export type ElectricalSafetyDashboardMetricKey =
  | 'fire-residual-current'
  | 'fire-temperature'
  | 'safe-leakage-current'
  | 'safe-total-current'
  | 'quality-voltage-thd'
  | 'quality-negative-sequence';

/** 电气安全页面可直接复用的 WebSocket 遥测快照结构。 */
export interface ElectricalSafetyMonitoringTelemetryValue {
  readonly ts: number;
  readonly value: string;
}

/** 顶部电气安全专项监测卡片的展示结构。 */
export interface ElectricalSafetyMonitoringSummaryCard {
  key: ElectricalSafetyMonitoringSummaryCategoryKey;
  title: string;
  icon: string;
  metrics: readonly ElectricalSafetyMonitoringMetric[];
}

/** 设备总览卡片所需字段；告警相关字段尚未接入时为 null。 */
export interface ElectricalSafetyMonitoringDevice {
  id: string;
  name: string;
  productName: string;
  model: string;
  protocol: string;
  productCategory: string;
  functionCategory: string;
  categoryKey: Exclude<ElectricalSafetyMonitoringCategoryKey, 'all'>;
  categoryLabel: string;
  assetId: string;
  assetPath: string;
  connection: ElectricalSafetyConnectionState;
  lastReportTime: string;
  realtimeMetrics: ElectricalSafetyMonitoringRealtimeMetric[];
  todayAlarmCount: number | null;
  unprocessedAlarmCount: number | null;
  dispatchedAlarmCount: number | null;
  processedAlarmCount: number | null;
}

/** 资产范围统计；在线数可由 active 计算，告警状态与告警数量保持未知。 */
export interface ElectricalSafetyMonitoringScopeSummary {
  totalDevices: number | null;
  normalDevices: number | null;
  onlineDevices: number | null;
  alarmDevices: number | null;
  offlineDevices: number | null;
  todayAlarmCount: number | null;
  unprocessedAlarmCount: number | null;
  dispatchedAlarmCount: number | null;
  processedAlarmCount: number | null;
}

/** 设备筛选项对应电气安全四类产品功能；未知历史值仍可在“全部”中查看。 */
export const ELECTRICAL_SAFETY_MONITORING_CATEGORIES: ReadonlyArray<{
  key: ElectricalSafetyMonitoringCategoryKey;
  label: string;
}> = [
  {key: 'all', label: '全部'},
  {key: 'fire', label: '电气火灾监测'},
  {key: 'safe-electricity', label: '安全用电监测'},
  {key: 'power-quality', label: '电力质量监测'},
  {key: 'arc-fault', label: '故障电弧监测'}
];

/**
 * 将当前四类编码及数据库中历史功能分类统一映射为四个筛选组。
 * 未知值保留原标签并落入不可选的 other，避免根据设备名称猜测业务分类。
 */
const ELECTRICAL_SAFETY_FUNCTION_CATEGORY_DEFINITIONS: Record<string, {
  key: Exclude<ElectricalSafetyMonitoringCategoryKey, 'all'>;
  label: string;
}> = {
  electrical_fire: {key: 'fire', label: '电气火灾监测'},
  temperature_monitor: {key: 'fire', label: '电气火灾监测'},
  leakage_monitor: {key: 'fire', label: '电气火灾监测'},
  safe_electricity: {key: 'safe-electricity', label: '安全用电监测'},
  input_monitor: {key: 'safe-electricity', label: '安全用电监测'},
  smart_breaker: {key: 'safe-electricity', label: '安全用电监测'},
  mains_monitor: {key: 'safe-electricity', label: '安全用电监测'},
  neutral_ground_voltage: {key: 'safe-electricity', label: '安全用电监测'},
  power_quality: {key: 'power-quality', label: '电力质量监测'},
  phase_unbalance: {key: 'power-quality', label: '电力质量监测'},
  arc_fault: {key: 'arc-fault', label: '故障电弧监测'}
};

/** 将设备功能分类映射为页面筛选项和显示文字。 */
function resolveFunctionCategory(functionCategory: string | null | undefined): {
  key: Exclude<ElectricalSafetyMonitoringCategoryKey, 'all'>;
  label: string;
} {
  const normalizedCategory = functionCategory?.trim() || '';
  return ELECTRICAL_SAFETY_FUNCTION_CATEGORY_DEFINITIONS[normalizedCategory] || {
    key: 'other',
    label: normalizedCategory || '--'
  };
}

/**
 * 顶部专项指标卡片按设备实际型号维持独立统计，不随四类产品分类合并漏电卡片。
 * E2C3 在新分类中属于电气火灾组，但仍单独计入漏电保护监测卡片。
 */
export function isElectricalSafetyMonitoringSummaryDevice(
  device: Pick<ElectricalSafetyMonitoringDevice, 'functionCategory' | 'model'>,
  category: ElectricalSafetyMonitoringSummaryCategoryKey
): boolean {
  const functionCategory = device.functionCategory.trim();
  const model = resolveElectricalSafetyDeviceModel(device.model);
  switch (category) {
    case 'fire':
      return model !== 'E2C3'
        && (functionCategory === 'electrical_fire' || functionCategory === 'temperature_monitor');
    case 'leakage':
      return model === 'E2C3' || functionCategory === 'leakage_monitor';
    case 'power-quality':
      return functionCategory === 'power_quality' || functionCategory === 'phase_unbalance';
    case 'arc-fault':
      return functionCategory === 'arc_fault';
    default:
      return false;
  }
}

/** 将平台毫秒时间戳格式化为卡片使用的本地日期时间；无效值显示 --。 */
export function formatElectricalSafetyActivityTime(timestamp: number | null | undefined): string {
  if (timestamp === null || timestamp === undefined || !Number.isFinite(timestamp) || timestamp <= 0) {
    return '--';
  }
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) {
    return '--';
  }
  const pad = (part: number) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** 电气安全型号集合与防雷一样以型号编码作为测点数组索引。 */
export type ElectricalSafetyDeviceModel =
  | 'LDH1' | 'FAP1' | 'ESF1' | 'E1T2' | 'E2C3' | 'E1I2'
  | 'CBP4' | 'CBP3' | 'CBP2' | 'CBP1' | 'LDA1' | 'E1B2'
  | 'E1P1' | 'E1E2' | 'SFE' | 'SFA';

/** 状态测点与防雷保持一致，默认按 0 正常、1 异常显示。 */
const ELECTRICAL_SAFETY_STATUS_VALUES: Readonly<Record<string, string>> = {0: '正常', 1: '异常'};

/** 以小构造函数维持防雷的测点声明方式，状态类和数值类在同一型号数组中明确区分。 */
const statusMetric = (
  key: string,
  label: string,
  fallbackValue = '__',
  valueMap: Readonly<Record<string, string>> = ELECTRICAL_SAFETY_STATUS_VALUES
): ElectricalSafetyRealtimeMetricDefinition => ({
  key, label, unit: '', valueType: 'status', fallbackValue, valueMap
});

/** 声明数值类遥测，并可附加与防雷详情趋势图一致的分组元数据。 */
const telemetryMetric = (
  key: string,
  label: string,
  unit = '',
  fallbackValue = '__',
  dashboardMetric?: ElectricalSafetyDashboardMetricKey
): ElectricalSafetyRealtimeMetricDefinition => ({
  key, label, unit, valueType: 'telemetry', fallbackValue, dashboardMetric
});

/** 型号中文别名与 thingsboard.yml 产品名称一致，测点数组仍按型号分开维护。 */
const ELECTRICAL_SAFETY_MODEL_ALIASES: Readonly<Record<ElectricalSafetyDeviceModel, readonly string[]>> = {
  LDH1: ['LDH1', 'FA故障电弧监测模块'],
  FAP1: ['FAP1', 'FAP故障电弧探测器（一体式）'],
  ESF1: ['ESF1', 'ESF电气火灾监测模组'],
  E1T2: ['E1T2', 'EST温度监测模组（有线测温）'],
  E2C3: ['E2C3', 'ESC漏电监测模组'],
  E1I2: ['E1I2', 'ESI输入监测模组'],
  CBP4: ['CBP4', 'FECB智能断路器（4P）'],
  CBP3: ['CBP3', 'FECB智能断路器（3P）'],
  CBP2: ['CBP2', 'FECB智能断路器（2P）'],
  CBP1: ['CBP1', 'FECB智能断路器（1P）'],
  LDA1: ['LDA1', 'FD市电监测模块'],
  E1B2: ['E1B2', 'ESB三相不平衡监测器'],
  E1P1: ['E1P1', 'ESP零地电压监测器'],
  E1E2: ['E1E2', 'ESE电能质量监测器'],
  SFE: ['SFE', 'SFE电能质量监测器'],
  SFA: ['SFA', 'SFA三相智能测控模组']
};

/**
 * 每个型号单独声明一个测点数组，状态类使用 statusMetric，数字类使用 telemetryMetric。
 * 未核对的型号保留空数组；SFA 按已确认的测点表配置，数组顺序用于详情页展示。
 */
export const electricalSafetyRealtimeMetricDefinitions: Record<
  ElectricalSafetyDeviceModel, ElectricalSafetyRealtimeMetricDefinition[]
> = {
  LDH1: [statusMetric('DevStatus', '设备状态')],
  FAP1: [statusMetric('DevStatus', '设备状态')],
  ESF1: [
    telemetryMetric('ResCurr', '剩余电流', 'mA', '__', 'fire-residual-current'),
    telemetryMetric('Temp1', '温度1', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp2', '温度2', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp3', '温度3', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp4', '温度4', '°C', '__', 'fire-temperature')
  ],
  E1T2: [
    telemetryMetric('Temp1', '温度1', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp2', '温度2', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp3', '温度3', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp4', '温度4', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp5', '温度5', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp6', '温度6', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp7', '温度7', '°C', '__', 'fire-temperature'),
    telemetryMetric('Temp8', '温度8', '°C', '__', 'fire-temperature')
  ],
  E2C3: [
    telemetryMetric('ResCurr1', '剩余电流1', 'mA', '__', 'fire-residual-current'),
    telemetryMetric('ResCurr2', '剩余电流2', 'mA', '__', 'fire-residual-current'),
    telemetryMetric('ResCurr3', '剩余电流3', 'mA', '__', 'fire-residual-current')
  ],
  E1I2: [],
  CBP4: [
    telemetryMetric('LeakValue', '漏电流', 'mA', '__', 'safe-leakage-current'),
    telemetryMetric('CurrTot', '总电流', 'A', '__', 'safe-total-current')
  ],
  CBP3: [
    telemetryMetric('LeakValue', '漏电流', 'mA', '__', 'safe-leakage-current'),
    telemetryMetric('CurrTot', '总电流', 'A', '__', 'safe-total-current')
  ],
  CBP2: [
    telemetryMetric('LeakValue', '漏电流', 'mA', '__', 'safe-leakage-current'),
    telemetryMetric('CurrTot', '总电流', 'A', '__', 'safe-total-current')
  ],
  CBP1: [
    telemetryMetric('LeakValue', '漏电流', 'mA', '__', 'safe-leakage-current'),
    telemetryMetric('CurrTot', '总电流', 'A', '__', 'safe-total-current')
  ],
  LDA1: [],
  E1B2: [
    telemetryMetric('NSeqVUnb', '负序电压不平衡度', '%', '__', 'quality-negative-sequence')
  ],
  E1P1: [],
  E1E2: [
    telemetryMetric('VoltThdA', 'A相电压总谐波畸变率', '%', '__', 'quality-voltage-thd'),
    telemetryMetric('VoltThdB', 'B相电压总谐波畸变率', '%', '__', 'quality-voltage-thd'),
    telemetryMetric('VoltThdC', 'C相电压总谐波畸变率', '%', '__', 'quality-voltage-thd'),
    telemetryMetric('NSeqVUnb', '负序电压不平衡度', '%', '__', 'quality-negative-sequence')
  ],
  SFE: [],
  SFA: [
    telemetryMetric('Longitude', '经度', '°'),
    telemetryMetric('Latitude', '纬度', '°'),
    telemetryMetric('PhAVolt', 'A相电压', 'V'),
    telemetryMetric('PhBVolt', 'B相电压', 'V'),
    telemetryMetric('PhCVolt', 'C相电压', 'V'),
    telemetryMetric('LineVoltAB', 'AB线电压', 'V'),
    telemetryMetric('LineVoltBC', 'BC线电压', 'V'),
    telemetryMetric('LineVoltCA', 'CA线电压', 'V'),
    telemetryMetric('Freq', '频率', 'Hz'),
    telemetryMetric('PhACurr', 'A相电流', 'A'),
    telemetryMetric('PhBCurr', 'B相电流', 'A'),
    telemetryMetric('PhCCurr', 'C相电流', 'A'),
    telemetryMetric('CurrVecSum', '三相电流矢量和', 'A'),
    telemetryMetric('ResCurr', '剩余电流', 'mA', '__', 'fire-residual-current'),
    telemetryMetric('ActPwrA', 'A相有功功率', 'kW'),
    telemetryMetric('ActPwrB', 'B相有功功率', 'kW'),
    telemetryMetric('ActPwrC', 'C相有功功率', 'kW'),
    telemetryMetric('ActPwrTot', '总有功功率', 'kW'),
    telemetryMetric('RctPwrA', 'A相无功功率', 'kvar'),
    telemetryMetric('RctPwrB', 'B相无功功率', 'kvar'),
    telemetryMetric('RctPwrC', 'C相无功功率', 'kvar'),
    telemetryMetric('RctPwrTot', '总无功功率', 'kvar'),
    telemetryMetric('AppPwrA', 'A相视在功率', 'kVA'),
    telemetryMetric('AppPwrB', 'B相视在功率', 'kVA'),
    telemetryMetric('AppPwrC', 'C相视在功率', 'kVA'),
    telemetryMetric('AppPwrTot', '总视在功率', 'kVA'),
    telemetryMetric('PwrFactA', 'A相功率因数', '无'),
    telemetryMetric('PwrFactB', 'B相功率因数', '无'),
    telemetryMetric('PwrFactC', 'C相功率因数', '无'),
    telemetryMetric('PwrFactT', '总功率因数', '无'),
    telemetryMetric('FwdActEA', 'A相正向有功电能', 'kWh'),
    telemetryMetric('FwdActEB', 'B相正向有功电能', 'kWh'),
    telemetryMetric('FwdActEC', 'C相正向有功电能', 'kWh'),
    telemetryMetric('FwdActET', '总正向有功电能', 'kWh'),
    telemetryMetric('RevRctEA', 'A相反向无功电能', 'kvarh'),
    telemetryMetric('RevRctEB', 'B相反向无功电能', 'kvarh'),
    telemetryMetric('RevRctEC', 'C相反向无功电能', 'kvarh'),
    telemetryMetric('RevRctET', '总反向无功电能', 'kvarh'),
    telemetryMetric('FwdRctEA', 'A相正向无功电能', 'kvarh'),
    telemetryMetric('FwdRctEB', 'B相正向无功电能', 'kvarh'),
    telemetryMetric('FwdRctEC', 'C相正向无功电能', 'kvarh'),
    telemetryMetric('FwdRctET', '总正向无功电能', 'kvarh')
  ]
};

/** 设备卡片挑选规则独立于型号测点数组，和防雷卡片测点布局保持分层。 */
type ElectricalSafetyCardMetricLayout = {kind: 'key'; key: string};
const cardMetric = (key: string): ElectricalSafetyCardMetricLayout => ({kind: 'key', key});
const ELECTRICAL_SAFETY_CARD_METRICS: Readonly<
  Record<ElectricalSafetyDeviceModel, readonly ElectricalSafetyCardMetricLayout[]>
> = {
  LDH1: [cardMetric('DevStatus')], FAP1: [cardMetric('DevStatus')], ESF1: [], E1T2: [], E2C3: [], E1I2: [],
  CBP4: [], CBP3: [], CBP2: [], CBP1: [], LDA1: [], E1B2: [],
  E1P1: [], E1E2: [], SFE: [],
  SFA: [cardMetric('PhAVolt'), cardMetric('PhBVolt'), cardMetric('PhCVolt')]
};

/** 将中文括号和英文大小写规范化，只按已有型号编码/别名精确匹配。 */
function normalizeElectricalSafetyModelName(value: string): string {
  return value.trim().replace(/（/g, '(').replace(/）/g, ')').toUpperCase();
}

/** 按设备型号编码或产品中文名称解析测点配置键，未匹配时返回 undefined。 */
export function resolveElectricalSafetyDeviceModel(
  deviceProfileName?: string | null
): ElectricalSafetyDeviceModel | undefined {
  const normalizedName = normalizeElectricalSafetyModelName(deviceProfileName || '');
  const matchedModel = (Object.keys(ELECTRICAL_SAFETY_MODEL_ALIASES) as ElectricalSafetyDeviceModel[])
    .find((model) => ELECTRICAL_SAFETY_MODEL_ALIASES[model]
      .some((alias) => normalizeElectricalSafetyModelName(alias) === normalizedName));
  return matchedModel;
}

/** 按型号返回防雷同形态的独立测点数组；未匹配型号不借用其他产品定义。 */
export function getElectricalSafetyRealtimeMetricDefinitions(
  deviceProfileName?: string | null
): ElectricalSafetyRealtimeMetricDefinition[] {
  const model = resolveElectricalSafetyDeviceModel(deviceProfileName);
  return model ? electricalSafetyRealtimeMetricDefinitions[model] : [];
}

/** 设备最新遥测变更是否影响任一看板关键指标，命中后复用共享缓存重算摘要。 */
export function hasElectricalSafetyDashboardMetricUpdate(
  deviceProfileName: string | null | undefined,
  changedValues: ReadonlyMap<string, unknown>
): boolean {
  const definitions = getElectricalSafetyRealtimeMetricDefinitions(deviceProfileName);
  for (const definition of definitions) {
    if (definition.dashboardMetric && changedValues.has(definition.key)) {
      return true;
    }
  }
  return false;
}

/** 按防雷的状态映射规则格式化状态文本；二态状态中数值零为正常，其余数值为异常。 */
function resolveElectricalSafetyStatusDisplayValue(
  rawValue: string,
  valueMap: Readonly<Record<string, string>>
): string | undefined {
  const trimmedValue = rawValue.trim();
  const directValue = valueMap[trimmedValue];
  if (directValue !== undefined) {
    return directValue;
  }
  if (/^[+-]?(?:\d+\.?\d*|\.\d+)$/.test(trimmedValue)) {
    const normalizedValue = valueMap[String(Number(trimmedValue))];
    if (normalizedValue !== undefined) {
      return normalizedValue;
    }
    if (valueMap === ELECTRICAL_SAFETY_STATUS_VALUES) {
      return Number(trimmedValue) === 0 ? '正常' : '异常';
    }
  }
  return undefined;
}

/** 用型号测点定义格式化展示值；状态只转显示文案，数值仍保留后端精度。 */
export function formatElectricalSafetyMetricDisplayValue(
  definition: ElectricalSafetyRealtimeMetricDefinition,
  rawValue: unknown
): string {
  if (rawValue === null || rawValue === undefined || String(rawValue).trim() === '') {
    return '--';
  }
  const rawText = String(rawValue);
  const displayValue = definition.valueMap
    ? resolveElectricalSafetyStatusDisplayValue(rawText, definition.valueMap) || rawText
    : rawText;
  return displayValue;
}

/** 卡片值按防雷方式将单位接在显示值后；详情表格继续把单位单独渲染。 */
export function formatElectricalSafetyRealtimeMetricValue(
  definition: ElectricalSafetyRealtimeMetricDefinition,
  rawValue: unknown
): string {
  const displayValue = formatElectricalSafetyMetricDisplayValue(definition, rawValue);
  const displayUnit = definition.unit === '无' ? '' : definition.unit;
  return displayValue === '--' ? displayValue : displayValue + displayUnit;
}

/** 从当前型号数组和独立卡片布局生成最多三项设备卡片遥测。 */
function buildElectricalSafetyRealtimeMetrics(
  deviceProfileName: string,
  latestValues: ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>
): ElectricalSafetyMonitoringRealtimeMetric[] {
  const model = resolveElectricalSafetyDeviceModel(deviceProfileName);
  const layout = model ? ELECTRICAL_SAFETY_CARD_METRICS[model] : undefined;
  if (!layout?.length) {
    return [];
  }
  const metrics: ElectricalSafetyMonitoringRealtimeMetric[] = [];
  const definitions = getElectricalSafetyRealtimeMetricDefinitions(deviceProfileName);
  const definitionsByKey = new Map(definitions.map((definition) => [definition.key, definition]));
  for (const item of layout) {
    const definition = definitionsByKey.get(item.key);
    if (!definition) {
      continue;
    }
    const sample = latestValues.get(item.key);
    const value = formatElectricalSafetyRealtimeMetricValue(definition, sample?.value);
    metrics.push({
      key: item.key,
      label: definition.label,
      value,
      title: `测点：${item.key}；原始值：${sample?.value ?? '--'}；时间：${formatElectricalSafetyActivityTime(sample?.ts)}`,
      abnormal: definition.valueType === 'status' && value === '异常'
    });
  }
  return metrics.slice(0, 3);
}

/** 将 active 遥测解析为明确的在线布尔值；未识别值由调用方保留原有状态。 */
export function parseElectricalSafetyActiveValue(value: unknown): boolean | undefined {
  if (value === true || value === 1) {
    return true;
  }
  if (value === false || value === 0) {
    return false;
  }
  if (typeof value !== 'string') {
    return undefined;
  }
  const normalizedValue = value.trim().toLowerCase();
  if (normalizedValue === 'true' || normalizedValue === '1') {
    return true;
  }
  if (normalizedValue === 'false' || normalizedValue === '0') {
    return false;
  }
  return undefined;
}

/**
 * 将接口设备上下文转换为卡片数据，并从真实资产树中解析设备绝对层级路径。
 * 设备分类以接口返回的 productCategory 和 functionCategory 为准。
 */
export function buildElectricalSafetyMonitoringDevices(
  contexts: readonly ElectricalSafetyDeviceContext[],
  assetTree: SmartLightningMonitoringAssetTree,
  telemetryByDeviceId: ReadonlyMap<string, ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>> = new Map()
): ElectricalSafetyMonitoringDevice[] {
  const assetPaths = getSmartLightningMonitoringAssetPaths(assetTree);
  const devices: ElectricalSafetyMonitoringDevice[] = [];
  for (const context of contexts) {
    if (context.productCategory !== ELECTRICAL_SAFETY_PRODUCT_CATEGORY || !context.deviceId) {
      continue;
    }
    const category = resolveFunctionCategory(context.functionCategory);
    const deviceTreePath = assetPaths.get(context.deviceId);
    const pathParts = deviceTreePath?.split('/') || [];
    const assetPath = pathParts[pathParts.length - 1] === context.deviceName
      ? pathParts.slice(0, -1).join('/') || '--'
      : deviceTreePath || '--';
    devices.push({
      id: context.deviceId,
      name: context.deviceName || '--',
      productName: context.productName?.trim() || '--',
      model: context.deviceProfileName?.trim() || '--',
      protocol: context.deviceProtocol?.trim() || '--',
      productCategory: context.productCategory,
      functionCategory: context.functionCategory?.trim() || '',
      categoryKey: category.key,
      categoryLabel: category.label,
      // 资产树的设备叶节点 ID 与设备上下文 ID 相同，用于资产范围筛选。
      assetId: context.deviceId,
      assetPath,
      connection: context.active === true ? 'online' : 'offline',
      lastReportTime: formatElectricalSafetyActivityTime(context.lastActivityTime),
      realtimeMetrics: buildElectricalSafetyRealtimeMetrics(
        context.deviceProfileName || '', telemetryByDeviceId.get(context.deviceId) || new Map()
      ),
      // 该阶段只接入设备基础资料，未查询告警，因此不伪造告警状态或统计值。
      todayAlarmCount: null,
      unprocessedAlarmCount: null,
      dispatchedAlarmCount: null,
      processedAlarmCount: null
    });
  }
  return devices;
}

/**
 * 生成顶部电气安全分析卡片。
 * 设备总数、在线数和告警数由实时资产范围统计区提供；未接入的测点值显示 --。
 */
export function buildElectricalSafetyMonitoringSummaryCards(): ElectricalSafetyMonitoringSummaryCard[] {
  return [
    {
      key: 'fire',
      title: '火灾预警监测',
      icon: 'warning_amber',
      metrics: [
        {label: '平均温度', value: '--', unit: '℃', tone: 'neutral'},
        {label: '风险等级', value: '--', tone: 'neutral'}
      ]
    },
    {
      key: 'leakage',
      title: '漏电保护监测',
      icon: 'electrical_services',
      metrics: [
        {label: '动作次数', value: '--', unit: '次', tone: 'neutral'},
        {label: '漏电电流', value: '--', unit: 'mA', tone: 'neutral'},
        {label: '误报率', value: '--', unit: '%', tone: 'neutral'}
      ]
    },
    {
      key: 'power-quality',
      title: '电力质量监测',
      icon: 'memory',
      metrics: [
        {label: '谐波含量', value: '--', unit: '%', tone: 'neutral'},
        {label: '功率因数', value: '--', tone: 'neutral'},
        {label: '质量等级', value: '--', tone: 'neutral'}
      ]
    },
    {
      key: 'arc-fault',
      title: '故障电弧监测',
      icon: 'crisis_alert',
      metrics: [
        {label: '设备状态', value: '--', tone: 'neutral'},
        {label: '累计动作次数', value: '--', unit: '次', tone: 'neutral'},
        {label: '最近上报', value: '--', tone: 'neutral'}
      ]
    }
  ];
}

/**
 * 汇总所选资产范围内电弧设备的设备状态、既有动作次数口径和最近上报时间。
 * DevStatus 使用电气安全型号配置中的 0/1 状态映射；当前协议没有单独的动作次数遥测，
 * 因而延续原防雷卡片由后端固定返回零的展示口径，不从未知字段推算动作次数。
 */
export function buildElectricalSafetyArcFaultSummaryMetrics(
  contexts: readonly ElectricalSafetyDeviceContext[],
  telemetryByDeviceId: ReadonlyMap<
    string,
    ReadonlyMap<string, ElectricalSafetyMonitoringTelemetryValue>
  >
): ElectricalSafetyMonitoringMetric[] {
  let statusDeviceCount = 0;
  let normalStatusCount = 0;
  let latestReportTime: number | null = null;
  let latestReportDeviceName: string | null = null;

  for (const context of contexts) {
    if (context.productCategory !== ELECTRICAL_SAFETY_PRODUCT_CATEGORY
      || context.functionCategory?.trim() !== 'arc_fault'
      || !context.deviceId) {
      continue;
    }

    const latestValues = telemetryByDeviceId.get(context.deviceId);
    const statusSample = latestValues?.get('DevStatus');
    if (statusSample && statusSample.value.trim() !== '') {
      const statusDefinition = getElectricalSafetyRealtimeMetricDefinitions(context.deviceProfileName)
        .find((definition) => definition.key === 'DevStatus');
      const statusValue = statusDefinition
        ? formatElectricalSafetyRealtimeMetricValue(statusDefinition, statusSample.value)
        : statusSample.value;
      statusDeviceCount++;
      if (statusValue === '正常') {
        normalStatusCount++;
      }
    }

    const activityTime = Number(context.lastActivityTime);
    if (Number.isFinite(activityTime) && activityTime > 0
      && (latestReportTime === null || activityTime > latestReportTime)) {
      latestReportTime = activityTime;
      latestReportDeviceName = context.deviceName || null;
    }
    latestValues?.forEach((sample) => {
      if (Number.isFinite(sample.ts) && sample.ts > 0
        && (latestReportTime === null || sample.ts > latestReportTime)) {
        latestReportTime = sample.ts;
        latestReportDeviceName = context.deviceName || null;
      }
    });
  }

  const statusTone = statusDeviceCount === 0
    ? 'neutral'
    : normalStatusCount === statusDeviceCount ? 'healthy' : 'critical';
  const latestReportText = formatElectricalSafetyActivityTime(latestReportTime);
  return [
    {
      label: '设备状态',
      value: `${normalStatusCount} / ${statusDeviceCount} 正常`,
      tone: statusTone,
      title: `正常设备数 / 已上报设备状态数：${normalStatusCount} / ${statusDeviceCount}`
    },
    {
      label: '累计动作次数',
      value: '0',
      unit: '次',
      tone: 'neutral',
      title: '当前型号未提供独立动作次数遥测，沿用原卡片统计口径。'
    },
    {
      label: '最近上报',
      value: latestReportText,
      tone: 'neutral',
      title: latestReportTime === null
        ? undefined
        : `设备：${latestReportDeviceName || '--'}；时间：${latestReportText}`
    }
  ];
}

/** 汇总当前资产子树的设备连接状态，并按活动告警设备 ID 统计告警设备数。 */
export function buildElectricalSafetyMonitoringScopeSummary(
  devices: readonly ElectricalSafetyMonitoringDevice[],
  activeAlarmDeviceIds: ReadonlySet<string> | null = null
): ElectricalSafetyMonitoringScopeSummary {
  const filteredDevices = devices.filter(
    (device) => device.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY
  );
  return {
    totalDevices: filteredDevices.length,
    normalDevices: null,
    onlineDevices: filteredDevices.filter((device) => device.connection === 'online').length,
    alarmDevices: activeAlarmDeviceIds === null
      ? null
      : filteredDevices.filter((device) => activeAlarmDeviceIds.has(device.id)).length,
    offlineDevices: filteredDevices.filter((device) => device.connection === 'offline').length,
    todayAlarmCount: null,
    unprocessedAlarmCount: null,
    dispatchedAlarmCount: null,
    processedAlarmCount: null
  };
}

/** 按资产范围、产品功能分类和关键词筛选真实设备上下文。 */
export function filterElectricalSafetyMonitoringDevices(
  devices: readonly ElectricalSafetyMonitoringDevice[],
  selectedAssetIds: ReadonlySet<string>,
  category: ElectricalSafetyMonitoringCategoryKey,
  search: string
): ElectricalSafetyMonitoringDevice[] {
  const normalizedSearch = search.trim().toLocaleLowerCase();
  return devices.filter((device) => {
    const matchesProduct = device.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY;
    const matchesAsset = selectedAssetIds.has(device.assetId);
    const matchesCategory = category === 'all' || device.categoryKey === category;
    const searchableText = `${device.name} ${device.productName} ${device.model} ${device.assetPath}`;
    const matchesSearch = !normalizedSearch || searchableText.toLocaleLowerCase().includes(normalizedSearch);
    return matchesProduct && matchesAsset && matchesCategory && matchesSearch;
  });
}
