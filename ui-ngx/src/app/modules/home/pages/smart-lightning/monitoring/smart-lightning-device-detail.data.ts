export interface SmartLightningDeviceDetailMock {
  title: string;
  metrics: Array<{label: string; value: string}>;
  alarms: Array<{item: string; severity: string; status: string; time: string}>;
}

export type SmartLightningChartPeriod = 'day' | 'week' | 'month';

/** 详情趋势图时间轴的点数；仅改变横轴粒度，曲线值仍由页面 mock 数据提供。 */
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

/** 将少量演示点线性插值到时间轴长度，使切换日/周/月后曲线连续自然。 */
export const fitSmartLightningMockSeries = (data: readonly number[], length: number): number[] => {
  if (!data.length || length < 1) { return []; }
  if (data.length === 1 || length === 1) { return Array.from({length}, () => data[0]); }
  return Array.from({length}, (_, index) => {
    const position = index * (data.length - 1) / (length - 1);
    const lowerIndex = Math.floor(position);
    const upperIndex = Math.ceil(position);
    const ratio = position - lowerIndex;
    const value = data[lowerIndex] + (data[upperIndex] - data[lowerIndex]) * ratio;
    return Number(value.toFixed(2));
  });
};

/** 一类设备共享的实时数据卡片定义；key 是规则链规范键，sourceKeys 兼容设备原始测点。 */
export interface SmartLightningRealtimeMetricDefinition {
  key: string;
  label: string;
  unit: string;
  valueType: 'status' | 'telemetry';
  fallbackValue: string;
  /** 设备直接上报的原始键；规则链已经转换时仍优先读取 key。 */
  sourceKeys?: readonly string[];
  /** 状态/枚举值的页面显示文本，不改变后端保存的原始数值。 */
  valueMap?: Readonly<Record<string, string>>;
}

/**
 * 原始测点字典（来源：防雷设备型号与原始测点对照表）。
 * 状态类保持 0/1 数值读取，只在页面显示时转换为中文，避免破坏规则链告警条件。
 */
const ALARM_STATUS_VALUES: Readonly<Record<string, string>> = {0: '正常', 1: '报警'};
const POLARITY_VALUES: Readonly<Record<string, string>> = {0: '负极', 1: '正极'};

const statusMetric = (
  key: string,
  label: string,
  sourceKey = key,
  fallbackValue = '正常',
  valueMap: Readonly<Record<string, string>> = ALARM_STATUS_VALUES
): SmartLightningRealtimeMetricDefinition => ({
  key, label, unit: '', valueType: 'status', fallbackValue, sourceKeys: sourceKey === key ? undefined : [sourceKey], valueMap
});

const telemetryMetric = (
  key: string,
  label: string,
  unit: string,
  sourceKey = key,
  fallbackValue = '--'
): SmartLightningRealtimeMetricDefinition => ({
  key, label, unit, valueType: 'telemetry', fallbackValue, sourceKeys: sourceKey === key ? undefined : [sourceKey]
});

const phaseMetrics = (count: number, prefix: string, label: string, unit: string, sourcePrefix = prefix, start = 1): SmartLightningRealtimeMetricDefinition[] =>
  Array.from({length: count}, (_, index) => {
    const phase = start + index;
    const suffix = String.fromCharCode(65 + index);
    return telemetryMetric(`${prefix}${suffix}`, `${suffix}${label}`, unit, `${sourcePrefix}${phase}`);
  });

const phaseStatusMetrics = (count: number, keyPrefix: string, label: string, sourcePrefix: string): SmartLightningRealtimeMetricDefinition[] =>
  Array.from({length: count}, (_, index) => {
    const phase = String.fromCharCode(65 + index);
    const sourceKey = `${sourcePrefix}${index + 1}`;
    return statusMetric(`${keyPrefix}${phase}`, `${phase}${label}`, sourceKey);
  });

export const smartLightningRealtimeMetricDefinitions: Record<string, SmartLightningRealtimeMetricDefinition[]> = {
  DESA: [
    statusMetric('breakerStatus', '断路器状态', 'K'),
    statusMetric('groundStatus', '接地状态', 'G'),
    telemetryMetric('voltageA', 'A相电压', 'V', 'U1', '0.00'),
    telemetryMetric('voltageB', 'B相电压', 'V', 'U2', '0.00'),
    telemetryMetric('voltageC', 'C相电压', 'V', 'U3', '0.00'),
    telemetryMetric('leakageA', 'A相漏电流', 'mA', 'I1', '0.0'),
    telemetryMetric('leakageB', 'B相漏电流', 'mA', 'I2', '0.0'),
    telemetryMetric('leakageC', 'C相漏电流', 'mA', 'I3', '0.0'),
    telemetryMetric('lifePercent', '防雷器寿命', '%', 'SPD', '100'),
    telemetryMetric('lightningCount', '雷击次数', '次', 'J', '0'),
    statusMetric('spdStatus', '防雷器状态', 'Y'),
    telemetryMetric('temperatureA', 'A相温度', '℃', 'T1', '23.4'),
    telemetryMetric('temperatureB', 'B相温度', '℃', 'T2', '23.1')
  ],
  DES4: [statusMetric('spdStatus', '防雷器状态', 'Y'), statusMetric('breakerStatus', '空开状态', 'K'), statusMetric('groundStatus', '接地状态', 'G'), telemetryMetric('lightningCount', '雷击次数', '次', 'J', '0')],
  DES9: [
    statusMetric('spdStatus', '防雷器状态', 'Y'), statusMetric('breakerStatus', '空开状态', 'K'), statusMetric('groundStatus', '接地状态', 'G'),
    telemetryMetric('lightningCount', '雷击次数', '次', 'J', '0'),
    telemetryMetric('leakageA', 'A相漏流', 'mA', 'I1'), telemetryMetric('leakageB', 'B相漏流', 'mA', 'I2'), telemetryMetric('leakageC', 'C相漏流', 'mA', 'I3'),
    telemetryMetric('temperatureA', '温度1', '℃', 'T1'), telemetryMetric('temperatureB', '温度2', '℃', 'T2')
  ],
  DEM2: [
    statusMetric('phaseVoltageStatusA', 'A相电压状态', 'VS1'), statusMetric('groundStatus', '接地状态', 'G'),
    statusMetric('tripStatus1', '脱扣1', 'SW1'), statusMetric('tripStatus2', '脱扣2', 'SW2'),
    telemetryMetric('temperatureA', '温度1', '℃', 'T1'), telemetryMetric('temperatureB', '温度2', '℃', 'T2'),
    telemetryMetric('lightningCount', '雷击次数', '次', 'J'), telemetryMetric('lifePercent', '防雷器寿命', '%', 'SPD')
  ],
  DEM4: [
    ...phaseStatusMetrics(3, 'phaseVoltageStatus', '相电压状态', 'VS'), statusMetric('groundStatus', '接地状态', 'G'),
    ...Array.from({length: 4}, (_, index) => statusMetric(`tripStatus${index + 1}`, `脱扣${index + 1}`, `SW${index + 1}`)),
    ...Array.from({length: 4}, (_, index) => telemetryMetric(`temperature${String.fromCharCode(65 + index)}`, `温度${index + 1}`, '℃', `T${index + 1}`)),
    telemetryMetric('lightningCount', '雷击次数', '次', 'J'), telemetryMetric('lifePercent', '防雷器寿命', '%', 'SPD')
  ],
  DEA2: [
    statusMetric('phaseVoltageStatusA', 'A相电压状态', 'VS1'), statusMetric('groundStatus', '接地状态', 'G'),
    statusMetric('tripStatus1', '脱扣1', 'SW1'), statusMetric('tripStatus2', '脱扣2', 'SW2'), telemetryMetric('leakageA', 'A相漏流', 'mA', 'I1'),
    telemetryMetric('temperatureA', '温度1', '℃', 'T1'), telemetryMetric('temperatureB', '温度2', '℃', 'T2'), telemetryMetric('lightningCount', '雷击次数', '次', 'J'), telemetryMetric('lifePercent', '防雷器寿命', '%', 'SPD')
  ],
  DEA4: [
    ...phaseStatusMetrics(3, 'phaseVoltageStatus', '相电压状态', 'VS'), statusMetric('groundStatus', '接地状态', 'G'),
    ...Array.from({length: 4}, (_, index) => statusMetric(`tripStatus${index + 1}`, `脱扣${index + 1}`, `SW${index + 1}`)),
    ...phaseMetrics(3, 'leakage', '相漏流', 'mA', 'I', 1),
    ...Array.from({length: 4}, (_, index) => telemetryMetric(`temperature${String.fromCharCode(65 + index)}`, `温度${index + 1}`, '℃', `T${index + 1}`)),
    telemetryMetric('lightningCount', '雷击次数', '次', 'J'), telemetryMetric('lifePercent', '防雷器寿命', '%', 'SPD')
  ],
  ESM0: [statusMetric('spdStatus', '防雷器状态', 'Y'), statusMetric('breakerStatus', '空开状态', 'K'), statusMetric('groundStatus', '接地状态', 'G'), telemetryMetric('temperatureA', 'SPD温度', '℃', 'T'), telemetryMetric('lightningCount', '雷击次数', '次', 'J')],
  ESM3: [
    statusMetric('spdStatus', '防雷器状态', 'Y'), statusMetric('breakerStatus', '空开状态', 'K'), statusMetric('groundStatus', '接地状态', 'G'),
    ...phaseMetrics(3, 'phaseVoltage', '相电压', 'V', 'U', 1), ...phaseMetrics(3, 'leakage', '相漏流', 'mA', 'I', 1),
    telemetryMetric('temperatureA', '环境温度', '℃', 'T1'), telemetryMetric('temperatureB', 'SPD温度', '℃', 'T2'), telemetryMetric('lightningCount', '雷击次数', '次', 'J'), telemetryMetric('lifePercent', '防雷器寿命', '%', 'SPD')
  ],
  G1A3: [telemetryMetric('groundResistance', '地网阻值', 'Ω', 'DR'), telemetryMetric('acquisitionCount', '采集次数', '次', 'DJ'), telemetryMetric('acquisitionInterval', '采集周期', 'min', 'MIN')],
  M1AI: [telemetryMetric('peakCurrent', '雷电流峰值', '', 'F'), telemetryMetric('surgeCount', '电涌次数', '次', 'LJ'), statusMetric('polarity', '极性', 'JX', '负极', POLARITY_VALUES)],
  L2AI: [telemetryMetric('peakCurrent', '瞬态电流峰值', '', 'F'), telemetryMetric('surgeCount', '电涌次数', '次', 'LJ'), statusMetric('polarity', '极性', 'JX', '负极', POLARITY_VALUES)],
  LDH1: [statusMetric('deviceStatus', '设备状态', 'ALM'), telemetryMetric('arcFaultCount', '故障电弧次数', '次', 'AJ')],
  FAP1: [statusMetric('deviceStatus', '设备状态', 'ALM')]
};

export const getSmartLightningRealtimeMetricDefinitions = (deviceProfileName?: string): SmartLightningRealtimeMetricDefinition[] =>
  deviceProfileName ? smartLightningRealtimeMetricDefinitions[deviceProfileName] || [] : [];

/** 已有最新遥测时优先显示其原始值；先查规则链规范键，再查设备原始键。 */
export const buildSmartLightningRealtimeMetrics = (
  deviceProfileName: string | undefined,
  latestValues: Map<string, unknown>,
  defaultMetrics: Array<{label: string; value: string}>
): Array<{label: string; value: string}> => {
  const definitions = getSmartLightningRealtimeMetricDefinitions(deviceProfileName);
  if (!definitions.length) {
    return defaultMetrics;
  }
  return definitions.map((definition) => {
    const keys = [definition.key, ...(definition.sourceKeys || [])];
    const rawValue = keys.map((key) => latestValues.get(key)).find((candidate) => candidate !== undefined && candidate !== null && String(candidate).trim() !== '');
    const rawText = rawValue === undefined ? definition.fallbackValue : String(rawValue);
    const value = definition.valueMap?.[rawText.trim()] || rawText;
    return {label: definition.label, value: `${value}${definition.unit}`};
  });
};

/** 真实设备遥测接入前的固定页面数据；资产树仍由现有接口提供。 */
export const buildSmartLightningDeviceDetailMock = (deviceId?: string): SmartLightningDeviceDetailMock => ({
  title: deviceId ? `设备监测 · ${deviceId.slice(0, 8)}` : '设备监测',
  metrics: [
    {label: '防雷器状态', value: '正常'}, {label: '温度1', value: '17.3℃'},
    {label: '温度2', value: '23.4℃'}, {label: '温度3', value: '18.1℃'},
    {label: 'A相电压', value: '186.8V'}, {label: 'B相电压', value: '186.8V'},
    {label: 'C相电压', value: '186.8V'}, {label: 'A相电流', value: '6.2A'},
    {label: 'B相电流', value: '5.9A'}, {label: 'C相电流', value: '6.4A'},
    {label: '剩余电流', value: '12mA'}, {label: '接地状态', value: '正常'}
  ],
  alarms: [
    {item: '相电流', severity: '紧急', status: '未处理', time: '2026/09/02 10:23'},
    {item: '温度1', severity: '紧急', status: '已处理', time: '2026/09/02 09:45'},
    {item: '温度2', severity: '隐患', status: '已处理', time: '2026/09/01 18:10'},
    {item: '剩余电流', severity: '隐患', status: '未处理', time: '2026/09/01 16:30'}
  ]
});
