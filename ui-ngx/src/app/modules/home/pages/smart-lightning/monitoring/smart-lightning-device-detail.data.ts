/** 文件职责：数据监测模块的 smart-lightning-device-detail.data，定义数据模型、接口结果映射与纯函数。 */
import {SMART_LIGHTNING_UNITS} from '../shared/telemetry/smart-lightning-measurements';

export interface SmartLightningDeviceDetailState {
  title: string;
  metrics: Array<{label: string; value: string}>;
  alarms: Array<{item: string; severity: string; status: string; time: string}>;
}

export type SmartLightningChartPeriod = 'day' | 'week' | 'month';

/** 详情趋势图各周期的时间轴点数配置，仅用于生成横轴标签。 */
const SMART_LIGHTNING_CHART_POINT_COUNTS: Readonly<Record<SmartLightningChartPeriod, number>> = {
  day: 24,
  week: 7,
  month: 30
};

/**
 * 以当前时间为终点生成趋势图横轴。
 * 日：最近 24 小时；周：最近 7 天；月：最近 30 天。
 */
export const buildSmartLightningChartTimes = (
  period: SmartLightningChartPeriod,
  now = new Date()
): string[] => {
  const pointCount = SMART_LIGHTNING_CHART_POINT_COUNTS[period];
  const interval = period === 'day' ? 60 * 60 * 1000 : 24 * 60 * 60 * 1000;
  const pad = (value: number): string => String(value).padStart(2, '0');
  return Array.from({length: pointCount}, (_, index) => {
    const time = new Date(now.getTime() - (pointCount - 1 - index) * interval);
    const date = `${pad(time.getMonth() + 1)}/${pad(time.getDate())}`;
    return period === 'day' ? `${date} ${pad(time.getHours())}:${pad(time.getMinutes())}` : date;
  });
};

/**
 * 一类设备共享的实时数据卡片定义。
 *
 * key 和 label 以《thingsboard平台设备遥测数据测点_含转换后全称.xlsx》
 * 的“转换后全称”和“测点名称”为准；这里不再把原始协议键当作兼容键读取。
 */
export interface SmartLightningRealtimeMetricDefinition {
  key: string;
  label: string;
  unit: string;
  valueType: 'status' | 'telemetry';
  fallbackValue: string;
  /** 状态/枚举值的页面显示文本，不改变后端保存的原始数值。 */
  valueMap?: Readonly<Record<string, string>>;
}

/**
 * 原始测点字典（来源：防雷设备型号与原始测点对照表）。
 * 状态类保持 0/1 数值读取，只在页面显示时转换为中文，避免破坏规则链告警条件。
 */
const ALARM_STATUS_VALUES: Readonly<Record<string, string>> = {0: '正常', 1: '异常'};
const POLARITY_VALUES: Readonly<Record<string, string>> = {0: '负极', 1: '正极'};

const statusMetric = (
  key: string,
  label: string,
  fallbackValue = '__',
  valueMap: Readonly<Record<string, string>> = ALARM_STATUS_VALUES
): SmartLightningRealtimeMetricDefinition => ({key, label, unit: '', valueType: 'status', fallbackValue, valueMap});

/**
 * 只为页面状态文案查找映射键，不改变实时遥测快照中的原始字符串。
 * 设备上报的状态可能是 0、0.0、1 或 1.0，而状态字典统一使用 0/1。
 */
const resolveStatusDisplayValue = (
  rawText: string,
  valueMap: Readonly<Record<string, string>>
): string => {
  const trimmedValue = rawText.trim();
  const directValue = valueMap[trimmedValue];
  if (directValue !== undefined) {
    return directValue;
  }
  if (/^[+-]?(?:\d+\.?\d*|\.\d+)$/.test(trimmedValue)) {
    const normalizedKey = String(Number(trimmedValue));
    const normalizedValue = valueMap[normalizedKey];
    if (normalizedValue !== undefined) {
      return normalizedValue;
    }
    // 二态状态只有数值零代表正常；其余已上报值统一按异常展示，不改变原始遥测值。
    if (valueMap === ALARM_STATUS_VALUES) {
  return Number(trimmedValue) === 0 ? '正常' : '异常';
  }
  }
};

/** 将雷电流极性遥测值转换为中文显示文本，未识别值保留原文且不改遥测快照。 */
export const formatSmartLightningPolarityLabel = (value: unknown): string => {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '--';
  }
  const rawText = String(value);
  return resolveStatusDisplayValue(rawText, POLARITY_VALUES) || rawText;
};

const telemetryMetric = (
  key: string,
  label: string,
  unit: string,
  fallbackValue = '__'
): SmartLightningRealtimeMetricDefinition => ({key, label, unit, valueType: 'telemetry', fallbackValue});

/**
 * 设备配置名称可能保存型号编码，也可能保存产品中文名称。
 * 实时指标、趋势图和产品参数统一通过该方法归一化，避免 DEP4 有遥测却查不到页面定义。
 */
const SMART_LIGHTNING_MODEL_ALIASES: Readonly<Record<string, readonly string[]>> = {
  DES4: ['DES4', 'FS防雷监测模块（四要素）', 'FS防雷监测模块(四要素)', 'FS防雷器监测模块（四要素）', 'FS防雷器监测模块(四要素)'],
  DES9: ['DES9', 'FS防雷监测模块（九要素）', 'FS防雷监测模块(九要素)', 'FS防雷器监测模块（九要素）', 'FS防雷器监测模块(九要素)'],
  DESA: ['DESA', 'FS防雷监测模块（全要素）', 'FS防雷监测模块(全要素)', 'FS防雷器监测模块（全要素）', 'FS防雷器监测模块(全要素)'],
  DEM2: ['DEM2', 'FSS智能型电涌保护器（单相无漏流）', 'FSS智能型电涌保护器(单相无漏流)'],
  DEM4: ['DEM4', 'FSS智能型电涌保护器（三相无漏流）', 'FSS智能型电涌保护器(三相无漏流)'],
  DEA2: ['DEA2', 'FSS智能型电涌保护器（单相全电流）', 'FSS智能型电涌保护器(单相全电流)'],
  DEA4: ['DEA4', 'FSS智能型电涌保护器（三相全电流）', 'FSS智能型电涌保护器(三相全电流)'],
  ESM0: ['ESM0', 'ESM防雷监测模组（基础版）', 'ESM防雷监测模组(基础版)'],
  ESM3: ['ESM3', 'ESM防雷监测模组（旗舰版）', 'ESM防雷监测模组(旗舰版)'],
  G1A3: ['G1A3', 'FR接地电阻监测模块'],
  M1AI: ['M1AI', 'FL雷电流监测模块（峰值型）', 'FL雷电流监测模块(峰值型)'],
  L2AI: ['L2AI', 'FL瞬态电流监测模块'],
  DEP4: ['DEP4', 'FSP防雷监测底座']
};

/** 将设备配置名称归一化为测点表中的型号编码。 */
export const resolveSmartLightningDeviceModel = (deviceProfileName?: string): string | undefined => {
  const value = deviceProfileName?.trim();
  if (!value) {
    return undefined;
  }
  const upperValue = value.toUpperCase();
  return Object.entries(SMART_LIGHTNING_MODEL_ALIASES)
    .find(([, aliases]) => aliases.some((alias) => alias.toUpperCase() === upperValue))?.[0];
};

export const smartLightningRealtimeMetricDefinitions: Record<string, SmartLightningRealtimeMetricDefinition[]> = {
  DES4: [
    statusMetric('RemoteSig', '遥信状态'),
    statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count, '__')
  ],
  DES9: [
    statusMetric('RemoteSig', '遥信状态'),
    statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count, '__'),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature)
  ],
  DESA: [
    statusMetric('RemoteSig', '遥信状态'),
    statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count, '__'),
    telemetryMetric('PhAVolt', 'A相电压', SMART_LIGHTNING_UNITS.voltage, '__'),
    telemetryMetric('PhBVolt', 'B相电压', SMART_LIGHTNING_UNITS.voltage, '__'),
    telemetryMetric('PhCVolt', 'C相电压', SMART_LIGHTNING_UNITS.voltage, '__'),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent, '__'),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent, '__'),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent, '__'),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature, '__'),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature, '__'),
    telemetryMetric('LifeLeft', '预估寿命', SMART_LIGHTNING_UNITS.percentage, '__')
  ],
  DEM2: [
    statusMetric('VoltStsA', 'A相电压状态'), statusMetric('GndStatus', '接地状态'),
    statusMetric('Trip1', '脱扣1'), statusMetric('Trip2', '脱扣2'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('LifeLeft', '预估寿命', SMART_LIGHTNING_UNITS.percentage)
  ],
  DEM4: [
    statusMetric('VoltStsA', 'A相电压状态'), statusMetric('VoltStsB', 'B相电压状态'),
    statusMetric('VoltStsC', 'C相电压状态'), statusMetric('GndStatus', '接地状态'),
    statusMetric('Trip1', '脱扣1'), statusMetric('Trip2', '脱扣2'),
    statusMetric('Trip3', '脱扣3'), statusMetric('Trip4', '脱扣4'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp3', '温度3', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp4', '温度4', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('LifeLeft', '预估寿命', SMART_LIGHTNING_UNITS.percentage)
  ],
  DEA2: [
    statusMetric('VoltStsA', 'A相电压状态'), statusMetric('GndStatus', '接地状态'),
    statusMetric('Trip1', '脱扣1'), statusMetric('Trip2', '脱扣2'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('LifeLeft', '预估寿命', SMART_LIGHTNING_UNITS.percentage)
  ],
  DEA4: [
    statusMetric('VoltStsA', 'A相电压状态'), statusMetric('VoltStsB', 'B相电压状态'),
    statusMetric('VoltStsC', 'C相电压状态'), statusMetric('GndStatus', '接地状态'),
    statusMetric('Trip1', '脱扣1'), statusMetric('Trip2', '脱扣2'),
    statusMetric('Trip3', '脱扣3'), statusMetric('Trip4', '脱扣4'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp3', '温度3', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp4', '温度4', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('LifeLeft', '预估寿命', SMART_LIGHTNING_UNITS.percentage)
  ],
  ESM0: [
    statusMetric('RemoteSig', '遥信状态'), statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'), telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('SpdTemp', 'SPD温度', SMART_LIGHTNING_UNITS.temperature)
  ],
  ESM3: [
    statusMetric('RemoteSig', '遥信状态'), statusMetric('McbStatus', '空开状态'), statusMetric('GndStatus', '接地状态'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('PhAVolt', 'A相电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('PhBVolt', 'B相电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('PhCVolt', 'C相电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('AmbTemp', '环境温度', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('SpdTemp', 'SPD温度', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('LifeLeft', '预估寿命', SMART_LIGHTNING_UNITS.percentage)
  ],
  G1A3: [telemetryMetric('SampleCnt', '采集次数', SMART_LIGHTNING_UNITS.count), telemetryMetric('SamplePrd', '采集周期', SMART_LIGHTNING_UNITS.interval), telemetryMetric('GndGridRes', '地网阻值', SMART_LIGHTNING_UNITS.resistance)],
  M1AI: [
    telemetryMetric('SurgeCnt', '电涌次数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('PeakValue', '峰值', SMART_LIGHTNING_UNITS.lightningCurrent),
    statusMetric('Polarity', '极性', '__', POLARITY_VALUES),
    telemetryMetric('StrikeDur', '持续时间', SMART_LIGHTNING_UNITS.duration),
    telemetryMetric('Charge', '电荷量', SMART_LIGHTNING_UNITS.charge),
    telemetryMetric('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage),
    telemetryMetric('SpecEnergy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy),
    telemetryMetric('WaveImp', '波阻抗', SMART_LIGHTNING_UNITS.resistance)
  ],
  L2AI: [
    telemetryMetric('SurgeCnt', '电涌次数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('PeakValue', '峰值', SMART_LIGHTNING_UNITS.lightningCurrent),
    statusMetric('Polarity', '极性', '__', POLARITY_VALUES),
    telemetryMetric('StrikeDur', '持续时间', SMART_LIGHTNING_UNITS.duration),
    telemetryMetric('Charge', '电荷量', SMART_LIGHTNING_UNITS.charge),
    telemetryMetric('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage),
    telemetryMetric('SpecEnergy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy),
    telemetryMetric('WaveImp', '波阻抗', SMART_LIGHTNING_UNITS.resistance)
  ],
  DEP4: [
    statusMetric('RemoteSig', '遥信状态'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('Temp', '温度', SMART_LIGHTNING_UNITS.temperature)
  ],
};

/** 按设备配置名称返回实时指标定义；未匹配的型号返回空列表。 */
export const getSmartLightningRealtimeMetricDefinitions = (deviceProfileName?: string): SmartLightningRealtimeMetricDefinition[] =>
  resolveSmartLightningDeviceModel(deviceProfileName)
    ? smartLightningRealtimeMetricDefinitions[resolveSmartLightningDeviceModel(deviceProfileName)!] || []
    : [];

/**
 * 按设备详情页现有测点定义格式化原始遥测值。
 * 状态只转换为界面中文文案，普通数值保留原始精度，并沿用集中配置的单位。
 */
export const formatSmartLightningRealtimeMetricValue = (
  definition: SmartLightningRealtimeMetricDefinition,
  rawValue: unknown
): string => {
  if (rawValue === null || rawValue === undefined || String(rawValue).trim() === '') {
    return '--';
  }
  const rawText = String(rawValue);
  const displayValue = definition.valueMap
    ? resolveStatusDisplayValue(rawText, definition.valueMap) || rawText
    : rawText;
  return displayValue + definition.unit;
};

/** 只读取规则链输出的规范遥测键。 */
export const buildSmartLightningRealtimeMetrics = (
  deviceProfileName: string | undefined,
  latestValues: Map<string, unknown>,
  defaultMetrics: Array<{label: string; value: string}>
): Array<{label: string; value: string}> => {
  const definitions = getSmartLightningRealtimeMetricDefinitions(deviceProfileName);
  if (!definitions.length) {
    return defaultMetrics;
  }
  const metrics = definitions.map((definition) => {
    const rawValue = latestValues.get(definition.key);
    // 缺失键、空字符串及文本化的空值都统一显示占位符，避免模板渲染 null/undefined。
    const normalizedValue = rawValue == null ? '' : String(rawValue).trim();
    if (!normalizedValue || /^(null|undefined)$/i.test(normalizedValue)) {
      return {label: definition.label, value: definition.fallbackValue, isAbnormal: false};
    }
    const rawText = String(rawValue);
    const value = definition.valueMap
      ? resolveStatusDisplayValue(rawText, definition.valueMap) ?? rawText
      : rawText;
    return {
      label: definition.label,
      value: `${value}${definition.unit}`,
      isAbnormal: definition.valueType === 'status' && value === '异常'
    };
  });
  // 只把已识别为异常的状态项置顶；普通遥测数值即使等于 1 也保持原顺序。
  const abnormalMetrics = metrics.filter((metric) => metric.isAbnormal);
  const remainingMetrics = metrics.filter((metric) => !metric.isAbnormal);
  return [...abnormalMetrics, ...remainingMetrics]
    .map(({label, value}) => ({label, value}));
};

/** 创建设备详情初始状态；实时遥测到达前仅展示占位符，不生成设备业务数据。 */
export const createSmartLightningDeviceDetailState = (deviceId?: string): SmartLightningDeviceDetailState => ({
  title: deviceId ? `设备监测 · ${deviceId.slice(0, 8)}` : '设备监测',
  metrics: [
    {label: '防雷器状态', value: '__'}, {label: '温度1', value: '__'},
    {label: '温度2', value: '__'}, {label: '温度3', value: '__'},
    {label: 'A相电压', value: '__'}, {label: 'B相电压', value: '__'},
    {label: 'C相电压', value: '__'}, {label: 'A相电流', value: '__'},
    {label: 'B相电流', value: '__'}, {label: 'C相电流', value: '__'},
    {label: '剩余电流', value: '__'}, {label: '接地状态', value: '__'}
  ],
  // 告警表由设备告警接口填充，初始为空，避免接口返回前显示虚构告警。
  alarms: []
});
