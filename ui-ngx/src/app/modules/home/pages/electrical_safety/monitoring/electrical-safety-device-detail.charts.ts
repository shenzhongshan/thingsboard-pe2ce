/** 文件职责：按电气安全设备型号定义趋势图分组，并转换历史遥测点。 */
import type {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import type {ElectricalSafetyDeviceModel} from './electrical-safety-monitoring.data';
import {
  getElectricalSafetyRealtimeMetricDefinitions,
  resolveElectricalSafetyDeviceModel
} from './electrical-safety-monitoring.data';
import {getElectricalSafetyDetailWindow} from './electrical-safety-device-detail.data';
import type {ElectricalSafetyDetailPeriod} from './electrical-safety-device-detail.data';

/** 单张趋势图中的规范测点曲线，键名必须与 ThingsBoard 时序键完全一致。 */
export interface ElectricalSafetyDetailSeriesDefinition {
  key: string;
  label: string;
  unit: string;
}

/** 按型号和业务测量类型配置的趋势图；priority 越小越优先展示。 */
export interface ElectricalSafetyDetailChartDefinition {
  id: string;
  title: string;
  priority: number;
  unit: string;
  series: readonly ElectricalSafetyDetailSeriesDefinition[];
}

/** 一条历史曲线及其毫秒时间戳数据点。 */
export interface ElectricalSafetyDetailChartSeries {
  definition: ElectricalSafetyDetailSeriesDefinition;
  points: Array<[number, number]>;
}

/** 用于声明一条曲线，名称和单位从该型号的测点映射读取。 */
const series = (
  key: string,
  label: string,
  unit: string
): ElectricalSafetyDetailSeriesDefinition => ({key, label, unit});

/** 从型号测点定义生成曲线，避免另行维护重复的中文名称和单位。 */
function modelSeries(
  model: ElectricalSafetyDeviceModel,
  keys: readonly string[]
): ElectricalSafetyDetailSeriesDefinition[] {
  const definitions = new Map(getElectricalSafetyRealtimeMetricDefinitions(model).map((item) => [item.key, item]));
  const result: ElectricalSafetyDetailSeriesDefinition[] = [];
  for (const key of keys) {
    const definition = definitions.get(key);
    if (definition) {
      result.push(series(definition.key, definition.label, definition.unit === '无' ? '' : definition.unit));
    }
  }
  return result;
}

/** 创建趋势图卡；同一卡片内曲线使用同一物理量单位。 */
function chart(
  id: string,
  title: string,
  priority: number,
  chartSeries: readonly ElectricalSafetyDetailSeriesDefinition[]
): ElectricalSafetyDetailChartDefinition {
  return {id, title, priority, unit: chartSeries[0]?.unit || '', series: chartSeries};
}

/** 根据 SFA 测点表定义每个业务量的曲线分组与展示优先级。 */
function sfaChart(
  id: string,
  title: string,
  priority: number,
  keys: readonly string[]
): ElectricalSafetyDetailChartDefinition {
  return chart(id, title, priority, modelSeries('SFA', keys));
}

/**
 * 各型号趋势图职责独立配置，与防雷按型号维护图表定义的方式一致。
 * SFA 和 E1P1 使用已确认的型号测点表；其他型号没有可靠键表，因此不猜测曲线。
 */
export const ELECTRICAL_SAFETY_CHART_DEFINITIONS: Readonly<
  Record<ElectricalSafetyDeviceModel, readonly ElectricalSafetyDetailChartDefinition[]>
> = {
  LDH1: [],
  FAP1: [],
  ESF1: [],
  E1T2: [],
  E2C3: [],
  E1I2: [],
  CBP4: [],
  CBP3: [],
  CBP2: [],
  CBP1: [],
  LDA1: [],
  E1B2: [],
  E1P1: [
    chart('zero-ground-voltage', '零地电压', 1, modelSeries('E1P1', ['UG']))
  ],
  E1E2: [],
  SFE: [],
  SFA: [
    sfaChart('phase-voltage', '相电压', 1, ['PhAVolt', 'PhBVolt', 'PhCVolt']),
    sfaChart('line-voltage', '线电压', 2, ['LineVoltAB', 'LineVoltBC', 'LineVoltCA']),
    sfaChart('phase-current', '相电流', 3, ['PhACurr', 'PhBCurr', 'PhCCurr']),
    sfaChart('active-power', '有功功率', 4, ['ActPwrA', 'ActPwrB', 'ActPwrC', 'ActPwrTot']),
    sfaChart('frequency', '频率', 5, ['Freq']),
    sfaChart('vector-current', '三相电流矢量和', 6, ['CurrVecSum']),
    sfaChart('residual-current', '剩余电流', 7, ['ResCurr']),
    sfaChart('reactive-power', '无功功率', 8, ['RctPwrA', 'RctPwrB', 'RctPwrC', 'RctPwrTot']),
    sfaChart('apparent-power', '视在功率', 9, ['AppPwrA', 'AppPwrB', 'AppPwrC', 'AppPwrTot']),
    sfaChart('power-factor', '功率因数', 10, ['PwrFactA', 'PwrFactB', 'PwrFactC', 'PwrFactT']),
    sfaChart('forward-active-energy', '正向有功电能', 11, ['FwdActEA', 'FwdActEB', 'FwdActEC', 'FwdActET']),
    sfaChart('reverse-reactive-energy', '反向无功电能', 12, ['RevRctEA', 'RevRctEB', 'RevRctEC', 'RevRctET']),
    sfaChart('forward-reactive-energy', '正向无功电能', 13, ['FwdRctEA', 'FwdRctEB', 'FwdRctEC', 'FwdRctET'])
  ]
};

/** 返回型号对应的完整趋势职责表，未知型号不借用其他型号的曲线。 */
export function getElectricalSafetyChartDefinitions(
  deviceProfileName?: string | null
): ElectricalSafetyDetailChartDefinition[] {
  const model = resolveElectricalSafetyDeviceModel(deviceProfileName);
  return model
    ? ELECTRICAL_SAFETY_CHART_DEFINITIONS[model].map((definition) => ({
      ...definition,
      series: [...definition.series]
    }))
    : [];
}

/** 只保留设备实际拥有的时序键，并按优先级限制首屏图卡数量。 */
export function filterElectricalSafetyChartDefinitions(
  definitions: readonly ElectricalSafetyDetailChartDefinition[],
  availableKeys: readonly string[],
  maxCharts = 4
): ElectricalSafetyDetailChartDefinition[] {
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
}

/** 汇总指定图表所需的时序键，去重后供单次历史接口请求使用。 */
export function buildElectricalSafetyChartReadKeys(
  definitions: readonly ElectricalSafetyDetailChartDefinition[]
): string[] {
  const keys = new Set<string>();
  for (const definition of definitions) {
    for (const item of definition.series) {
      keys.add(item.key);
    }
  }
  return [...keys];
}

/** 把 ThingsBoard 历史时序转换为排序后的数值点，忽略空值及非法时间/数值。 */
export function buildElectricalSafetyChartSeriesData(
  definition: ElectricalSafetyDetailChartDefinition,
  timeseries: TimeseriesData
): ElectricalSafetyDetailChartSeries[] {
  const result: ElectricalSafetyDetailChartSeries[] = [];
  for (const item of definition.series) {
    const samples = Array.isArray(timeseries[item.key]) ? timeseries[item.key] : [];
    const points = samples
      .filter((sample) => sample.value !== null && sample.value !== undefined && String(sample.value).trim() !== '')
      .map((sample) => [Number(sample.ts), Number(sample.value)] as [number, number])
      .filter(([timestamp, value]) => Number.isFinite(timestamp) && Number.isFinite(value))
      .sort((left, right) => left[0] - right[0]);
    if (points.length) {
      result.push({definition: item, points});
    }
  }
  return result;
}

/** 按防雷趋势图的真实数据留白规则计算纵轴边界。 */
export function buildElectricalSafetyYAxisBounds(
  seriesData: readonly ElectricalSafetyDetailChartSeries[]
): {min: number; max: number} | undefined {
  const values: number[] = [];
  for (const item of seriesData) {
    for (const point of item.points) {
      if (Number.isFinite(point[1])) {
        values.push(point[1]);
      }
    }
  }
  if (!values.length) {
    return undefined;
  }
  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const range = rawMax - rawMin;
  const padding = range > 0 ? range * 0.1 : Math.max(Math.abs(rawMax) * 0.1, 1);
  return {
    min: rawMin > 0 ? Math.max(Number.EPSILON, rawMin - padding) : rawMin - padding,
    max: rawMax < 0 ? Math.min(-Number.EPSILON, rawMax + padding) : rawMax + padding
  };
}

/** 统一转发详情页周期窗口，防止日/周/月的时间口径出现两套实现。 */
export function getElectricalSafetyChartWindow(
  period: ElectricalSafetyDetailPeriod,
  now = Date.now()
): {startTs: number; endTs: number} {
  return getElectricalSafetyDetailWindow(period, now);
}
