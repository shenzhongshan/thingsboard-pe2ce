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
/** M1AI新版协议与L2AI分开维护极性方向，不改变未更新型号的定义。 */
const M1AI_POLARITY_VALUES: Readonly<Record<string, string>> = {0: '正极性', 1: '负极性'};

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
export const formatSmartLightningPolarityLabel = (value: unknown, deviceProfileName?: string): string => {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '--';
  }
  const rawText = String(value);
  const valueMap = resolveSmartLightningDeviceModel(deviceProfileName) === 'M1AI' ? M1AI_POLARITY_VALUES : POLARITY_VALUES;
  return resolveStatusDisplayValue(rawText, valueMap) || rawText;
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
  M1AI: ['M1AI', 'FL雷电流监测模块', 'FL雷电流监测模块（峰值型）', 'FL雷电流监测模块(峰值型)'],
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

/** DES9／DESA 共用的新协议诊断项；枚举和告警序号保持原始整数，仅在展示时解释。 */
const DES_DEVICE_STATES: Readonly<Record<string, string>> = {
  0: '初始化', 1: '学习中', 2: '学习保持', 3: '事件', 4: '正常',
  5: '观察', 6: '预警', 7: '危急', 8: '故障'
};
const DES_DEGRADATION_REASONS: Readonly<Record<string, string>> = {0: '无', 1: '漏流', 2: '温升', 3: '趋势', 4: '雷击'};
/**
 * 所有已接入SPD诊断型号统一按整数告警序号查表，不仅针对编码6；禁止按位组合解释。
 * 0按协议“正常”处理，不将旧表bit0的遥信脱扣误当作编码0异常；未知码保留原值。
 */
const SPD_ALARM_CODE_LABELS: Readonly<Record<string, string>> = {
  0: '正常', 1: 'PE接地异常', 2: '电压状态报警',
  3: 'L1漏流偏高', 4: 'L2漏流偏高', 5: 'L3漏流偏高',
  6: '温升异常', 7: '剩余寿命偏低', 8: '雷击后漏流上升',
  9: '健康度偏低', 10: '建议更换', 11: '漏流接近饱和',
  12: '温度高危', 13: '传感器异常', 14: '基准学习失败'
};
/** 按实际设备的单个告警序号查表，不执行位运算或组合告警解释。 */
const formatSpdAlarmCode = (rawText: string): string => {
  const code = Number(rawText);
  if (!Number.isInteger(code)) {
    return rawText;
  }
  return SPD_ALARM_CODE_LABELS[String(code)] ?? rawText;
};
const desDiagnosticStatusMetrics = (): SmartLightningRealtimeMetricDefinition[] => [
  statusMetric('SPD_Status', '设备状态', '__', DES_DEVICE_STATES),
  statusMetric('SPD_Alarm', '告警码', '__', SPD_ALARM_CODE_LABELS),
  statusMetric('SPD_WorstPh', '最差相', '__', {0: 'A相', 1: 'B相', 2: 'C相'}),
  statusMetric('SPD_Reason', '退化原因', '__', DES_DEGRADATION_REASONS)
];
// 平均漏流键以实际整包报文为准：SPD_I1hA/B/C、SPD_I24hA/B/C，不带额外的L。
const desDiagnosticNumericMetrics = (): SmartLightningRealtimeMetricDefinition[] => [
  telemetryMetric('SPD_RUL', '剩余寿命', SMART_LIGHTNING_UNITS.percentage),
  telemetryMetric('SPD_RULDay', '剩余寿命天数', '天'),
  telemetryMetric('SPD_RULA', 'A相健康度', SMART_LIGHTNING_UNITS.percentage),
  telemetryMetric('SPD_RULB', 'B相健康度', SMART_LIGHTNING_UNITS.percentage),
  telemetryMetric('SPD_RULC', 'C相健康度', SMART_LIGHTNING_UNITS.percentage),
  telemetryMetric('SPD_I1hA', 'A相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
  telemetryMetric('SPD_I1hB', 'B相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
  telemetryMetric('SPD_I1hC', 'C相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
  telemetryMetric('SPD_I24hA', 'A相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
  telemetryMetric('SPD_I24hB', 'B相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
  telemetryMetric('SPD_I24hC', 'C相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
  telemetryMetric('SPD_DeltaT', '温升指标', SMART_LIGHTNING_UNITS.temperature)
];

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
    ...desDiagnosticStatusMetrics(),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count, '__'),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    ...desDiagnosticNumericMetrics()
  ],
  DESA: [
    statusMetric('RemoteSig', '遥信状态'),
    statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'),
    ...desDiagnosticStatusMetrics(),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count, '__'),
    telemetryMetric('PhAVolt', 'A相电压', SMART_LIGHTNING_UNITS.voltage, '__'),
    telemetryMetric('PhBVolt', 'B相电压', SMART_LIGHTNING_UNITS.voltage, '__'),
    telemetryMetric('PhCVolt', 'C相电压', SMART_LIGHTNING_UNITS.voltage, '__'),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent, '__'),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent, '__'),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent, '__'),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature, '__'),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature, '__'),
    ...desDiagnosticNumericMetrics()
  ],
  // DEM2新版14项：仅使用本型号已确认的键，状态优先；诊断枚举及告警序号解释与DES共用，数值不换算。
  DEM2: [
    statusMetric('PhAStatus', 'A相电压状态'),
    statusMetric('GndStatus', '接地状态'),
    statusMetric('TripSW1', '脱扣1'),
    statusMetric('TripSW2', '脱扣2'),
    statusMetric('SPD_Status', '设备状态', '__', DES_DEVICE_STATES),
    statusMetric('SPD_Alarm', '告警码', '__', SPD_ALARM_CODE_LABELS),
    statusMetric('SPD_Reason', '退化原因', '__', DES_DEGRADATION_REASONS),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('SPD_RUL', '剩余寿命', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULDay', '剩余寿命天数', '天'),
    telemetryMetric('SPD_RULA', 'A相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_DeltaT', '温升指标', SMART_LIGHTNING_UNITS.temperature)
  ],
  // DEM4新版23项：状态优先，复用已确认的SPD枚举及告警序号解释，数值保持原值。
  DEM4: [
    statusMetric('PhAStatus', 'A相电压状态'),
    statusMetric('PhBStatus', 'B相电压状态'),
    statusMetric('PhCStatus', 'C相电压状态'),
    statusMetric('GndStatus', '接地状态'),
    statusMetric('TripSW1', '脱扣1'), statusMetric('TripSW2', '脱扣2'),
    statusMetric('TripSW3', '脱扣3'), statusMetric('TripSW4', '脱扣4'),
    ...desDiagnosticStatusMetrics(),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp3', '温度3', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp4', '温度4', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('SPD_RUL', '剩余寿命', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULDay', '剩余寿命天数', '天'),
    telemetryMetric('SPD_RULA', 'A相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULB', 'B相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULC', 'C相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_DeltaT', '温升指标', SMART_LIGHTNING_UNITS.temperature)
  ],
  // DEA2新版17项：单相测点独立配置，平均漏流键以实际报文为准，不带L。
  DEA2: [
    statusMetric('PhAStatus', 'A相电压状态'), statusMetric('GndStatus', '接地状态'),
    statusMetric('TripSW1', '脱扣1'), statusMetric('TripSW2', '脱扣2'),
    statusMetric('SPD_Status', '设备状态', '__', DES_DEVICE_STATES),
    statusMetric('SPD_Alarm', '告警码', '__', SPD_ALARM_CODE_LABELS),
    statusMetric('SPD_Reason', '退化原因', '__', DES_DEGRADATION_REASONS),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('SPD_RUL', '剩余寿命', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULDay', '剩余寿命天数', '天'),
    telemetryMetric('SPD_RULA', 'A相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_I1hA', 'A相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_I24hA', 'A相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_DeltaT', '温升指标', SMART_LIGHTNING_UNITS.temperature)
  ],
  // DEA4新版32项：三相状态、诊断和数值按型号独立配置，平均漏流键按用户确认不带L。
  DEA4: [
    statusMetric('PhAStatus', 'A相电压状态'), statusMetric('PhBStatus', 'B相电压状态'),
    statusMetric('PhCStatus', 'C相电压状态'), statusMetric('GndStatus', '接地状态'),
    statusMetric('TripSW1', '脱扣1'), statusMetric('TripSW2', '脱扣2'),
    statusMetric('TripSW3', '脱扣3'), statusMetric('TripSW4', '脱扣4'),
    ...desDiagnosticStatusMetrics(),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp3', '温度3', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp4', '温度4', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('SPD_RUL', '剩余寿命', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULDay', '剩余寿命天数', '天'),
    telemetryMetric('SPD_RULA', 'A相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULB', 'B相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_RULC', 'C相健康度', SMART_LIGHTNING_UNITS.percentage),
    telemetryMetric('SPD_I1hA', 'A相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_I1hB', 'B相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_I1hC', 'C相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_I24hA', 'A相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_I24hB', 'B相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_I24hC', 'C相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('SPD_DeltaT', '温升指标', SMART_LIGHTNING_UNITS.temperature)
  ],
  // ESM基础版仅有5项，温度协议键为Temp，同名原值读取。
  ESM0: [
    statusMetric('RemoteSig', 'SPD状态'), statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('Temp', '温度', SMART_LIGHTNING_UNITS.temperature)
  ],
  // ESM旗舰版使用28个实际协议键，温度直接显示为温度1／温度2，平均漏流键不带L。
  ESM3: [
    statusMetric('RemoteSig', 'SPD状态'), statusMetric('McbStatus', '空开状态'),
    statusMetric('GndStatus', '接地状态'),
    ...desDiagnosticStatusMetrics(),
    telemetryMetric('StrikeCnt', '雷击计数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('PhAVolt', 'A相电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('PhBVolt', 'B相电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('PhCVolt', 'C相电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('LeakCurrA', 'A相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrB', 'B相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('LeakCurrC', 'C相漏流', SMART_LIGHTNING_UNITS.leakageCurrent),
    telemetryMetric('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
    telemetryMetric('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature),
    ...desDiagnosticNumericMetrics()
  ],
  // G1A3只展示已确认的5个新键；小数位字段按表中单位展示，不驱动阻值缩放。
  G1A3: [
    telemetryMetric('SampleCnt', '采集次数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('SamplePrd', '采集周期', SMART_LIGHTNING_UNITS.interval),
    telemetryMetric('GndVolt', '地网电压', SMART_LIGHTNING_UNITS.voltage),
    telemetryMetric('GndGridRes', '地网阻值', SMART_LIGHTNING_UNITS.resistance),
    telemetryMetric('ResDecimal', '阻值小数位', SMART_LIGHTNING_UNITS.resistance)
  ],
  // M1AI仅使用实际报文8项；峰值键PeakCur，极性0正1负，不作倍率换算。
  M1AI: [
    statusMetric('Polarity', '雷电极性', '__', M1AI_POLARITY_VALUES),
    telemetryMetric('StrikeCnt', '雷击次数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('PeakCur', '雷电峰值', SMART_LIGHTNING_UNITS.lightningCurrent),
    telemetryMetric('KeepTime', '持续时间', SMART_LIGHTNING_UNITS.duration),
    telemetryMetric('CharImpe', '波阻抗值', SMART_LIGHTNING_UNITS.resistance),
    telemetryMetric('EleCharge', '电荷量', SMART_LIGHTNING_UNITS.electricCharge),
    telemetryMetric('Energy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy),
    telemetryMetric('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage)
  ],
  // L2AI实际报文8项：PeakCur按瞬态电流产品定义使用A，计数键为SurgeCnt。
  L2AI: [
    statusMetric('Polarity', '雷电极性', '__', POLARITY_VALUES),
    telemetryMetric('SurgeCnt', '电涌次数', SMART_LIGHTNING_UNITS.count),
    telemetryMetric('PeakCur', '峰值电流', SMART_LIGHTNING_UNITS.transientCurrent),
    telemetryMetric('KeepTime', '持续时间', SMART_LIGHTNING_UNITS.duration),
    telemetryMetric('CharImpe', '波阻抗值', SMART_LIGHTNING_UNITS.resistance),
    telemetryMetric('EleCharge', '电荷量', SMART_LIGHTNING_UNITS.electricCharge),
    telemetryMetric('Energy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy),
    telemetryMetric('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage)
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
    ? (definition.valueMap === SPD_ALARM_CODE_LABELS ? formatSpdAlarmCode(rawText) : resolveStatusDisplayValue(rawText, definition.valueMap)) || rawText
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
      ? (definition.valueMap === SPD_ALARM_CODE_LABELS ? formatSpdAlarmCode(rawText) : resolveStatusDisplayValue(rawText, definition.valueMap)) ?? rawText
      : rawText;
    return {
      label: definition.label,
      value: `${value}${definition.unit}`,
      // 告警码按序号解释，已知非零告警及未知正整数码突出显示；设备状态不套用二态规则。
      isAbnormal: definition.valueType === 'status' && (value === '异常' ||
        (definition.key === 'SPD_Alarm' && Number.isInteger(Number(rawText)) && Number(rawText) > 0))
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
