/** 文件职责：设备详情趋势图的产品职责映射、遥测键过滤与历史点转换。 */
import {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {SMART_LIGHTNING_UNITS} from '../shared/telemetry/smart-lightning-measurements';
import {resolveSmartLightningDeviceModel, SmartLightningChartPeriod} from './smart-lightning-device-detail.data';

/** 单个趋势图中的一条业务测点曲线，只使用协议转换器输出的规范键。 */
export interface SmartLightningChartSeriesDefinition {
  key: string;
  label: string;
  unit: string;
}

/** 一个趋势图卡片；priority 越小越重要，最多保留四个图卡。 */
export interface SmartLightningChartDefinition {
  id: string;
  title: string;
  priority: number;
  series: readonly SmartLightningChartSeriesDefinition[];
}

/** 转换后的 ECharts 数据点，时间戳使用毫秒。 */
export interface SmartLightningChartSeriesData {
  definition: SmartLightningChartSeriesDefinition;
  points: Array<[number, number]>;
}

/**
 * 根据当前曲线数据计算纵轴范围。
 *
 * <p>趋势图不再默认把纵轴下限固定为 0，而是围绕真实数据增加少量留白，
 * 让温度、电压、漏流等变化较小的曲线也能清楚显示。没有有效数据时返回
 * undefined，由 ECharts 使用默认范围。</p>
 */
export const buildSmartLightningYAxisBounds = (
  seriesData: readonly SmartLightningChartSeriesData[]
): {min: number; max: number} | undefined => {
  const values = seriesData
    .flatMap((item) => item.points.map((point) => point[1]))
    .filter((value) => Number.isFinite(value));
  if (!values.length) {
    return undefined;
  }

  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const range = rawMax - rawMin;
  const padding = range > 0 ? range * 0.1 : Math.max(Math.abs(rawMax) * 0.1, 1);

  // 正值数据的下限保持为正数，避免重新退回到 0；包含 0 或负值时保留真实数据范围。
  const min = rawMin > 0 ? Math.max(Number.EPSILON, rawMin - padding) : rawMin - padding;
  const max = rawMax < 0 ? Math.min(-Number.EPSILON, rawMax + padding) : rawMax + padding;
  return {min, max};
};

const series = (
  key: string,
  label: string,
  unit: string
): SmartLightningChartSeriesDefinition => ({key, label, unit});

const phaseSeries = (
  keys: readonly string[],
  label: string,
  unit: string
): SmartLightningChartSeriesDefinition[] => keys.map((key, index) => {
  const suffix = key.startsWith('Ph') || key.startsWith('LeakCurr') ? String.fromCharCode(65 + index) : key.startsWith('Temp') ? String(index + 1) : '';
  return series(key, `${suffix}${label}`, unit);
});

const chart = (
  id: string,
  title: string,
  priority: number,
  chartSeries: readonly SmartLightningChartSeriesDefinition[]
): SmartLightningChartDefinition => ({id, title, priority, series: chartSeries});

const voltage = (count = 3): SmartLightningChartSeriesDefinition[] =>
  phaseSeries(['PhAVolt', 'PhBVolt', 'PhCVolt'].slice(0, count), '相电压', SMART_LIGHTNING_UNITS.voltage);
const leakage = (count = 3): SmartLightningChartSeriesDefinition[] =>
  phaseSeries(['LeakCurrA', 'LeakCurrB', 'LeakCurrC'].slice(0, count), '相漏电流', SMART_LIGHTNING_UNITS.leakageCurrent);
const temperature = (count: number, keys: readonly string[] = ['Temp1', 'Temp2', 'Temp3', 'Temp4']): SmartLightningChartSeriesDefinition[] =>
  phaseSeries(keys.slice(0, count), '温度', SMART_LIGHTNING_UNITS.temperature);
const lightningCount = (): SmartLightningChartSeriesDefinition[] =>
  [series('StrikeCnt', '雷击次数', SMART_LIGHTNING_UNITS.count)];
/** 新版SPD寿命与诊断曲线按单位独立分组；只引用已入库原值，不作倍率换算。 */
const spdLife = (): SmartLightningChartSeriesDefinition[] =>
  [series('SPD_RUL', '剩余寿命', SMART_LIGHTNING_UNITS.percentage)];
/** DES9／DESA／DEM4具有三相健康度，DEM2仅有A相；平均漏流仅为DES9／DESA配置。 */
const spdDiagnosticCharts = (priority: number, threePhase: boolean, hasAverageLeakage = threePhase): SmartLightningChartDefinition[] => {
  const definitions = [
    chart('spdHealth', '分相健康度', priority,
      (threePhase ? ['A', 'B', 'C'] : ['A']).map((phase) =>
        series(`SPD_RUL${phase}`, `${phase}相健康度`, SMART_LIGHTNING_UNITS.percentage))),
    chart('spdLifeDays', '剩余寿命天数', priority + 1, [series('SPD_RULDay', '剩余寿命天数', '天')]),
    chart('spdTemperatureRise', '温升指标', priority + 2,
      [series('SPD_DeltaT', '温升指标', SMART_LIGHTNING_UNITS.temperature)])
  ];
  if (hasAverageLeakage) {
    // 平均值由设备直接上报，不在前端根据瞬时漏流重新计算。
    definitions.push(
      chart('spdLeakage1h', '1小时平均漏流', priority + 3, ['A', 'B', 'C'].map((phase) =>
        series(`SPD_I1h${phase}`, `${phase}相1小时平均漏流`, SMART_LIGHTNING_UNITS.leakageCurrent))),
      chart('spdLeakage24h', '24小时平均漏流', priority + 4, ['A', 'B', 'C'].map((phase) =>
        series(`SPD_I24h${phase}`, `${phase}相24小时平均漏流`, SMART_LIGHTNING_UNITS.leakageCurrent)))
    );
  }
  return definitions;
};
/**
 * 按型号定义趋势图优先级。此处只定义“该产品负责监测什么”，不读取接口、不生成演示数据。
 * 规则：先按产品职责排序，再由可用规范遥测键过滤，最终最多显示四个趋势图。
 */
export const SMART_LIGHTNING_CHART_DEFINITIONS: Readonly<Record<string, readonly SmartLightningChartDefinition[]>> = {
  DESA: [
    chart('voltage', '相电压', 1, voltage()),
    chart('leakage', '漏电流', 2, leakage()),
    chart('temperature', '温度', 3, temperature(2)),
    chart('life', '剩余寿命', 4, spdLife()),
    chart('StrikeCnt', '雷击次数', 5, lightningCount()),
    ...spdDiagnosticCharts(6, true)
  ],
  DES4: [chart('StrikeCnt', '雷击次数', 1, lightningCount())],
  DES9: [
    chart('leakage', '漏电流', 1, leakage()),
    chart('temperature', '温度', 2, temperature(2)),
    chart('StrikeCnt', '雷击次数', 3, lightningCount()),
    chart('life', '剩余寿命', 4, spdLife()),
    ...spdDiagnosticCharts(5, true)
  ],
  DEM2: [
    chart('temperature', '温度', 1, temperature(2)),
    chart('life', '剩余寿命', 2, spdLife()),
    chart('StrikeCnt', '雷击次数', 3, lightningCount()),
    ...spdDiagnosticCharts(4, false)
  ],
  DEM4: [
    chart('temperature', '温度', 1, temperature(4)),
    chart('life', '剩余寿命', 2, spdLife()),
    chart('StrikeCnt', '雷击次数', 3, lightningCount()),
    ...spdDiagnosticCharts(4, true, false)
  ],
  DEA2: [
    chart('leakage', '漏电流', 1, leakage(1)),
    chart('temperature', '温度', 2, temperature(2)),
    chart('life', '剩余寿命', 3, spdLife()),
    chart('StrikeCnt', '雷击次数', 4, lightningCount()),
    ...spdDiagnosticCharts(5, false),
    // DEA2仅有A相平均漏流，使用实际报文中的无L键；直接画上报值，不重新聚合或换算。
    chart('spdLeakage1h', '1小时平均漏流', 8,
      [series('SPD_I1hA', 'A相1小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent)]),
    chart('spdLeakage24h', '24小时平均漏流', 9,
      [series('SPD_I24hA', 'A相24小时平均漏流', SMART_LIGHTNING_UNITS.leakageCurrent)])
  ],
  DEA4: [
    chart('leakage', '漏电流', 1, leakage()),
    chart('temperature', '温度', 2, temperature(4)),
    chart('life', '剩余寿命', 3, spdLife()),
    chart('StrikeCnt', '雷击次数', 4, lightningCount()),
    ...spdDiagnosticCharts(5, true, false),
    // 按用户确认使用不带L的三相平均漏流键，不重新计算平均值或执行换算。
    chart('spdLeakage1h', '1小时平均漏流', 8, ['A', 'B', 'C'].map((phase) =>
      series(`SPD_I1h${phase}`, `${phase}相1小时平均漏流`, SMART_LIGHTNING_UNITS.leakageCurrent))),
    chart('spdLeakage24h', '24小时平均漏流', 9, ['A', 'B', 'C'].map((phase) =>
      series(`SPD_I24h${phase}`, `${phase}相24小时平均漏流`, SMART_LIGHTNING_UNITS.leakageCurrent)))
  ],
  ESM0: [
    chart('temperature', '温度', 1, [series('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature)]),
    chart('StrikeCnt', '雷击次数', 2, lightningCount())
  ],
  ESM3: [
    chart('voltage', '相电压', 1, voltage()),
    chart('leakage', '漏电流', 2, leakage()),
    // 温度图例与实时数据保持一致，按Temp1／Temp2显示温度1／温度2。
    chart('temperature', '温度', 3, [
      series('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature),
      series('Temp2', '温度2', SMART_LIGHTNING_UNITS.temperature)
    ]),
    chart('life', '剩余寿命', 4, spdLife()),
    chart('StrikeCnt', '雷击次数', 5, lightningCount()),
    ...spdDiagnosticCharts(6, true)
  ],
  G1A3: [
    chart('GndGridRes', '地网阻值', 1, [series('GndGridRes', '地网阻值', SMART_LIGHTNING_UNITS.resistance)]),
    chart('GndVolt', '地网电压', 2, [series('GndVolt', '地网电压', SMART_LIGHTNING_UNITS.voltage)]),
    chart('SampleCnt', '采集次数', 3, [series('SampleCnt', '采集次数', SMART_LIGHTNING_UNITS.count)]),
    chart('SamplePrd', '采集周期', 4, [series('SamplePrd', '采集周期', SMART_LIGHTNING_UNITS.interval)])
    // ResDecimal仅在实时信息中展示，不作为连续物理量绘图，也不参与阻值换算。
  ],
  M1AI: [
    chart('peakCurrent', '雷电峰值', 1, [series('PeakCur', '雷电峰值', SMART_LIGHTNING_UNITS.lightningCurrent)]),
    chart('StrikeCnt', '雷击次数', 2, [series('StrikeCnt', '雷击次数', SMART_LIGHTNING_UNITS.count)]),
    chart('KeepTime', '持续时间', 3, [series('KeepTime', '持续时间', SMART_LIGHTNING_UNITS.duration)]),
    chart('EleCharge', '电荷量', 4, [series('EleCharge', '电荷量', SMART_LIGHTNING_UNITS.electricCharge)]),
    chart('PeakVolt', '峰值电压', 5, [series('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage)]),
    chart('Energy', '单位能量', 6, [series('Energy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy)]),
    chart('CharImpe', '波阻抗值', 7, [series('CharImpe', '波阻抗值', SMART_LIGHTNING_UNITS.resistance)])
    // 极性为枚举，不绘制成连续物理量曲线；实际报文没有年月日字段。
  ],
  L2AI: [
    chart('peakCurrent', '雷电峰值', 1, [series('PeakCur', '雷电峰值', SMART_LIGHTNING_UNITS.lightningCurrent)]),
    chart('SurgeCnt', '电涌次数', 2, [series('SurgeCnt', '电涌次数', SMART_LIGHTNING_UNITS.count)]),
    chart('KeepTime', '持续时间', 3, [series('KeepTime', '持续时间', SMART_LIGHTNING_UNITS.duration)]),
    chart('EleCharge', '电荷量', 4, [series('EleCharge', '电荷量', SMART_LIGHTNING_UNITS.electricCharge)]),
    chart('PeakVolt', '峰值电压', 5, [series('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage)]),
    chart('Energy', '单位能量', 6, [series('Energy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy)]),
    chart('CharImpe', '波阻抗值', 7, [series('CharImpe', '波阻抗值', SMART_LIGHTNING_UNITS.resistance)])
  ],
  DEP4: [
    chart('temperature', '温度', 1, [series('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature)]),
    chart('StrikeCnt', '雷击次数', 2, lightningCount())
  ]
};

/** 返回指定型号的原始趋势图定义；未知型号不显示曲线，避免用错产品指标。 */
export const getSmartLightningChartDefinitions = (deviceProfileName?: string): SmartLightningChartDefinition[] =>
  (resolveSmartLightningDeviceModel(deviceProfileName)
    ? SMART_LIGHTNING_CHART_DEFINITIONS[resolveSmartLightningDeviceModel(deviceProfileName)!] || []
    : []).map((definition) => ({
    ...definition,
    series: [...definition.series]
  }));

/** 根据设备真实存在的遥测键过滤曲线，并严格限制最多四个图卡。 */
export const filterSmartLightningChartDefinitions = (
  definitions: readonly SmartLightningChartDefinition[],
  availableKeys: readonly string[],
  maxCharts = 4
): SmartLightningChartDefinition[] => {
  const available = new Set(availableKeys);
  return definitions
    .slice()
    .sort((left, right) => left.priority - right.priority)
    .map((definition) => ({
      ...definition,
      series: definition.series.filter((item) => available.has(item.key))
    }))
    .filter((definition) => definition.series.length > 0)
    .slice(0, maxCharts);
};

/** 汇总曲线所需的规范键，供一次 keys 查询和一次历史数据查询使用。 */
export const buildSmartLightningChartReadKeys = (definitions: readonly SmartLightningChartDefinition[]): string[] => {
  const keys = new Set<string>();
  definitions.forEach((definition) => definition.series.forEach((item) => keys.add(item.key)));
  return [...keys];
};
/** 将 ThingsBoard 历史遥测转换成排序后的数值点；非法值直接忽略。 */
export const buildSmartLightningChartSeriesData = (
  definition: SmartLightningChartDefinition,
  timeseries: TimeseriesData
): SmartLightningChartSeriesData[] => definition.series.map((item) => {
  const points = Array.isArray(timeseries[item.key]) ? timeseries[item.key]
    .map((point) => [Number(point.ts), Number(point.value)] as [number, number])
    .filter(([timestamp, value]) => Number.isFinite(timestamp) && Number.isFinite(value))
    .sort((left, right) => left[0] - right[0]) : [];
  return {definition: item, points};
}).filter((item) => item.points.length > 0);
/**
 * 根据所选日/周/月返回查询窗口；每张图单独调用，不同步其它图表。
 * 日周期从当天本地时间 00:00 开始，避免“最近 24 小时”把前一天的数据带进来。
 */
export const getSmartLightningChartWindow = (period: SmartLightningChartPeriod, now = Date.now()): {startTs: number; endTs: number} => {
  if (period === 'day') {
    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);
    return {startTs: startOfToday.getTime(), endTs: now};
  }
  const duration = period === 'week' ? 7 * 24 * 60 * 60 * 1000 : 30 * 24 * 60 * 60 * 1000;
  return {startTs: now - duration, endTs: now};
};
