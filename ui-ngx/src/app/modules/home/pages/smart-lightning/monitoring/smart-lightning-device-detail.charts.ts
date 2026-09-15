/** 文件职责：设备详情趋势图的产品职责映射、遥测键过滤与历史点转换。 */
import {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {SMART_LIGHTNING_UNITS} from '../shared/telemetry/smart-lightning-measurements';
import {SmartLightningChartPeriod} from './smart-lightning-device-detail.data';

/** 单个趋势图中的一条业务测点曲线。sourceKeys 用于兼容规则链规范键和设备原始键。 */
export interface SmartLightningChartSeriesDefinition {
  key: string;
  label: string;
  unit: string;
  sourceKeys?: readonly string[];
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
  unit: string,
  sourceKeys: readonly string[] = []
): SmartLightningChartSeriesDefinition => ({key, label, unit, sourceKeys});

const phaseSeries = (
  keyPrefix: string,
  label: string,
  unit: string,
  sourcePrefix: string,
  count: number
): SmartLightningChartSeriesDefinition[] => Array.from({length: count}, (_, index) => {
  const suffix = String.fromCharCode(65 + index);
  return series(`${keyPrefix}${suffix}`, `${suffix}${label}`, unit, [`${sourcePrefix}${index + 1}`]);
});

const chart = (
  id: string,
  title: string,
  priority: number,
  chartSeries: readonly SmartLightningChartSeriesDefinition[]
): SmartLightningChartDefinition => ({id, title, priority, series: chartSeries});

const voltage = (count = 3): SmartLightningChartSeriesDefinition[] =>
  phaseSeries('voltage', '相电压', SMART_LIGHTNING_UNITS.voltage, 'U', count);
const leakage = (count = 3): SmartLightningChartSeriesDefinition[] =>
  phaseSeries('leakage', '相漏电流', SMART_LIGHTNING_UNITS.leakageCurrent, 'I', count);
const temperature = (count: number, sourcePrefix = 'T'): SmartLightningChartSeriesDefinition[] =>
  phaseSeries('temperature', '温度', SMART_LIGHTNING_UNITS.temperature, sourcePrefix, count);
const life = (): SmartLightningChartSeriesDefinition[] =>
  [series('lifePercent', '防雷器寿命', SMART_LIGHTNING_UNITS.percentage, ['SPD'])];
const lightningCount = (): SmartLightningChartSeriesDefinition[] =>
  [series('lightningCount', '雷击次数', SMART_LIGHTNING_UNITS.count, ['J'])];

/**
 * 按型号定义趋势图优先级。此处只定义“该产品负责监测什么”，不读取接口、不生成演示数据。
 * 规则：先按产品职责排序，再由可用遥测键过滤，最终最多显示四个趋势图。
 */
export const SMART_LIGHTNING_CHART_DEFINITIONS: Readonly<Record<string, readonly SmartLightningChartDefinition[]>> = {
  DESA: [
    chart('voltage', '相电压', 1, voltage()),
    chart('leakage', '漏电流', 2, leakage()),
    chart('temperature', '温度', 3, temperature(2)),
    chart('life', '寿命', 4, life()),
    chart('lightningCount', '雷击次数', 5, lightningCount())
  ],
  DES4: [chart('lightningCount', '雷击次数', 1, lightningCount())],
  DES9: [
    chart('leakage', '漏电流', 1, leakage()),
    chart('temperature', '温度', 2, temperature(2)),
    chart('lightningCount', '雷击次数', 3, lightningCount())
  ],
  DEM2: [
    chart('temperature', '温度', 1, temperature(2)),
    chart('life', '寿命', 2, life()),
    chart('lightningCount', '雷击次数', 3, lightningCount())
  ],
  DEM4: [
    chart('temperature', '温度', 1, temperature(4)),
    chart('life', '寿命', 2, life()),
    chart('lightningCount', '雷击次数', 3, lightningCount())
  ],
  DEA2: [
    chart('leakage', '漏电流', 1, leakage(1)),
    chart('temperature', '温度', 2, temperature(2)),
    chart('life', '寿命', 3, life()),
    chart('lightningCount', '雷击次数', 4, lightningCount())
  ],
  DEA4: [
    chart('leakage', '漏电流', 1, leakage()),
    chart('temperature', '温度', 2, temperature(4)),
    chart('life', '寿命', 3, life()),
    chart('lightningCount', '雷击次数', 4, lightningCount())
  ],
  ESM0: [
    chart('temperature', '温度', 1, [series('temperatureA', '温度', SMART_LIGHTNING_UNITS.temperature, ['T'])]),
    chart('lightningCount', '雷击次数', 2, lightningCount())
  ],
  ESM3: [
    chart('voltage', '相电压', 1, voltage()),
    chart('leakage', '漏电流', 2, leakage()),
    chart('temperature', '温度', 3, temperature(2)),
    chart('life', '寿命', 4, life()),
    chart('lightningCount', '雷击次数', 5, lightningCount())
  ],
  G1A3: [
    chart('groundResistance', '地网阻值', 1, [series('groundResistance', '地网阻值', SMART_LIGHTNING_UNITS.resistance, ['DR'])]),
    chart('acquisitionCount', '采集次数', 2, [series('acquisitionCount', '采集次数', SMART_LIGHTNING_UNITS.count, ['DJ'])])
  ],
  M1AI: [
    chart('peakCurrent', '雷电流峰值', 1, [series('peakCurrent', '雷电流峰值', SMART_LIGHTNING_UNITS.lightningCurrent, ['surgePeak', 'F'])]),
    chart('surgeCount', '雷击次数', 2, [series('surgeCount', '雷击次数', SMART_LIGHTNING_UNITS.count, ['surgeCount', 'LJ'])])
  ],
  L2AI: [
    chart('peakCurrent', '瞬态电流峰值', 1, [series('peakCurrent', '瞬态电流峰值', SMART_LIGHTNING_UNITS.lightningCurrent, ['surgePeak', 'F'])]),
    chart('surgeCount', '雷击次数', 2, [series('surgeCount', '雷击次数', SMART_LIGHTNING_UNITS.count, ['surgeCount', 'LJ'])])
  ],
  LDH1: [chart('arcFaultCount', '故障电弧次数', 1, [series('arcFaultCount', '故障电弧次数', SMART_LIGHTNING_UNITS.count, ['AJ'])])],
  FAP1: []
};

/** 返回指定型号的原始趋势图定义；未知型号不显示曲线，避免用错产品指标。 */
export const getSmartLightningChartDefinitions = (deviceProfileName?: string): SmartLightningChartDefinition[] =>
  (deviceProfileName ? SMART_LIGHTNING_CHART_DEFINITIONS[deviceProfileName] || [] : []).map((definition) => ({
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
      series: definition.series.filter((item) => available.has(item.key) || (item.sourceKeys || []).some((key) => available.has(key)))
    }))
    .filter((definition) => definition.series.length > 0)
    .slice(0, maxCharts);
};

/** 汇总曲线所需的规范键与原始键，供一次 keys 查询和一次历史数据查询使用。 */
export const buildSmartLightningChartReadKeys = (definitions: readonly SmartLightningChartDefinition[]): string[] => {
  const keys = new Set<string>();
  definitions.forEach((definition) => definition.series.forEach((item) => {
    keys.add(item.key);
    (item.sourceKeys || []).forEach((key) => keys.add(key));
  }));
  return [...keys];
};

/** 将 ThingsBoard 历史遥测转换成排序后的数值点；非法值直接忽略。 */
export const buildSmartLightningChartSeriesData = (
  definition: SmartLightningChartDefinition,
  timeseries: TimeseriesData
): SmartLightningChartSeriesData[] => definition.series.map((item) => {
  const keys = [item.key, ...(item.sourceKeys || [])];
  const sourceKey = keys.find((key) => Array.isArray(timeseries[key]) && timeseries[key].length > 0);
  const points = sourceKey ? timeseries[sourceKey]
    .map((point) => [Number(point.ts), Number(point.value)] as [number, number])
    .filter(([timestamp, value]) => Number.isFinite(timestamp) && Number.isFinite(value))
    .sort((left, right) => left[0] - right[0]) : [];
  return {definition: item, points};
}).filter((item) => item.points.length > 0);

/** 根据所选日/周/月返回查询窗口；每张图单独调用，不同步其它图表。 */
export const getSmartLightningChartWindow = (period: SmartLightningChartPeriod, now = Date.now()): {startTs: number; endTs: number} => {
  const duration = period === 'day' ? 24 * 60 * 60 * 1000 : period === 'week' ? 7 * 24 * 60 * 60 * 1000 : 30 * 24 * 60 * 60 * 1000;
  return {startTs: now - duration, endTs: now};
};
