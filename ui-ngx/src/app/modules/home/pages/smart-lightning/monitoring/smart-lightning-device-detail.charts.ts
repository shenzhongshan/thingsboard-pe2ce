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
  const suffix = key.startsWith('Ph') || key.startsWith('LeakCurr') ? String.fromCharCode(65 + index) : key.startsWith('Temp') ? String(index + 1) : key === 'AmbTemp' ? '环境' : key === 'SpdTemp' ? 'SPD' : '';
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
const life = (): SmartLightningChartSeriesDefinition[] =>
  [series('LifeLeft', '防雷器寿命', SMART_LIGHTNING_UNITS.percentage)];
const lightningCount = (): SmartLightningChartSeriesDefinition[] =>
  [series('StrikeCnt', '雷击次数', SMART_LIGHTNING_UNITS.count)];
/**
 * 按型号定义趋势图优先级。此处只定义“该产品负责监测什么”，不读取接口、不生成演示数据。
 * 规则：先按产品职责排序，再由可用规范遥测键过滤，最终最多显示四个趋势图。
 */
export const SMART_LIGHTNING_CHART_DEFINITIONS: Readonly<Record<string, readonly SmartLightningChartDefinition[]>> = {
  DESA: [
    chart('voltage', '相电压', 1, voltage()),
    chart('leakage', '漏电流', 2, leakage()),
    chart('temperature', '温度', 3, temperature(2)),
    chart('life', '寿命', 4, life()),
    chart('StrikeCnt', '雷击次数', 5, lightningCount())
  ],
  DES4: [chart('StrikeCnt', '雷击次数', 1, lightningCount())],
  DES9: [
    chart('leakage', '漏电流', 1, leakage()),
    chart('temperature', '温度', 2, temperature(2)),
    chart('StrikeCnt', '雷击次数', 3, lightningCount())
  ],
  DEM2: [
    chart('temperature', '温度', 1, temperature(2)),
    chart('life', '寿命', 2, life()),
    chart('StrikeCnt', '雷击次数', 3, lightningCount())
  ],
  DEM4: [
    chart('temperature', '温度', 1, temperature(4)),
    chart('life', '寿命', 2, life()),
    chart('StrikeCnt', '雷击次数', 3, lightningCount())
  ],
  DEA2: [
    chart('leakage', '漏电流', 1, leakage(1)),
    chart('temperature', '温度', 2, temperature(2)),
    chart('life', '寿命', 3, life()),
    chart('StrikeCnt', '雷击次数', 4, lightningCount())
  ],
  DEA4: [
    chart('leakage', '漏电流', 1, leakage()),
    chart('temperature', '温度', 2, temperature(4)),
    chart('life', '寿命', 3, life()),
    chart('StrikeCnt', '雷击次数', 4, lightningCount())
  ],
  ESM0: [
    chart('temperature', '温度', 1, [series('SpdTemp', 'SPD温度', SMART_LIGHTNING_UNITS.temperature)]),
    chart('StrikeCnt', '雷击次数', 2, lightningCount())
  ],
  ESM3: [
    chart('voltage', '相电压', 1, voltage()),
    chart('leakage', '漏电流', 2, leakage()),
    chart('temperature', '温度', 3, temperature(2, ['AmbTemp', 'SpdTemp'])),
    chart('life', '寿命', 4, life()),
    chart('StrikeCnt', '雷击次数', 5, lightningCount())
  ],
  G1A3: [
    chart('GndGridRes', '地网阻值', 1, [series('GndGridRes', '地网阻值', SMART_LIGHTNING_UNITS.resistance)]),
    chart('SampleCnt', '采集次数', 2, [series('SampleCnt', '采集次数', SMART_LIGHTNING_UNITS.count)]),
    chart('SamplePrd', '采集周期', 3, [series('SamplePrd', '采集周期', SMART_LIGHTNING_UNITS.interval)])
  ],
  M1AI: [
    chart('peakCurrent', '雷电流峰值', 1, [series('PeakValue', '雷电流峰值', SMART_LIGHTNING_UNITS.lightningCurrent)]),
    chart('SurgeCnt', '电涌次数', 2, [series('SurgeCnt', '电涌次数', SMART_LIGHTNING_UNITS.count)]),
    chart('StrikeDur', '持续时间', 3, [series('StrikeDur', '持续时间', SMART_LIGHTNING_UNITS.duration)]),
    chart('Charge', '电荷量', 4, [series('Charge', '电荷量', SMART_LIGHTNING_UNITS.charge)]),
    chart('PeakVolt', '峰值电压', 5, [series('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage)]),
    chart('SpecEnergy', '单位能量', 6, [series('SpecEnergy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy)]),
    chart('WaveImp', '波阻抗', 7, [series('WaveImp', '波阻抗', SMART_LIGHTNING_UNITS.resistance)])
  ],
  L2AI: [
    chart('peakCurrent', '瞬态电流峰值', 1, [series('PeakValue', '瞬态电流峰值', SMART_LIGHTNING_UNITS.lightningCurrent)]),
    chart('SurgeCnt', '电涌次数', 2, [series('SurgeCnt', '电涌次数', SMART_LIGHTNING_UNITS.count)]),
    chart('StrikeDur', '持续时间', 3, [series('StrikeDur', '持续时间', SMART_LIGHTNING_UNITS.duration)]),
    chart('Charge', '电荷量', 4, [series('Charge', '电荷量', SMART_LIGHTNING_UNITS.charge)]),
    chart('PeakVolt', '峰值电压', 5, [series('PeakVolt', '峰值电压', SMART_LIGHTNING_UNITS.highVoltage)]),
    chart('SpecEnergy', '单位能量', 6, [series('SpecEnergy', '单位能量', SMART_LIGHTNING_UNITS.specificEnergy)]),
    chart('WaveImp', '波阻抗', 7, [series('WaveImp', '波阻抗', SMART_LIGHTNING_UNITS.resistance)])
  ],
  DEP4: [
    chart('temperature', '温度', 1, [series('Temp1', '温度1', SMART_LIGHTNING_UNITS.temperature)]),
    chart('StrikeCnt', '雷击次数', 2, lightningCount())
  ],
  LDH1: [],
  FAP1: []
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
