/** 文件职责：数据查询模块的 smart-lightning-history-review.data，定义数据模型、接口结果映射与纯函数。 */
import {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {getSmartLightningRealtimeMetricDefinitions} from '../monitoring/smart-lightning-device-detail.data';
import {buildSmartLightningWaveformSeriesData, getSmartLightningChartDefinitions,
  SmartLightningChartDefinition, SmartLightningChartSeriesData} from '../monitoring/smart-lightning-device-detail.charts';
import {SMART_LIGHTNING_WAVEFORM_KEY} from '../shared/telemetry/smart-lightning-measurements';

/** 一条 WF 遥测即一次完整波形事件；时间戳表示平台保存的上报时间。 */
export interface SmartLightningWaveformHistoryRecord {
  timestamp: number;
  seriesData: SmartLightningChartSeriesData[];
  sampleCount: number;
}

/** 分页结果保留排他结束时间游标，异常波形仍在列表中显示而不静默丢弃。 */
export interface SmartLightningWaveformHistoryPage {
  records: SmartLightningWaveformHistoryRecord[];
  hasMore: boolean;
  nextEndTs?: number;
}

/** 按上报时间从新到旧整理一页事件；多读取一条判断是否还存在更早记录。 */
export const buildSmartLightningWaveformHistoryPage = (
  timeseries: TimeseriesData,
  definition: SmartLightningChartDefinition,
  pageSize: number
): SmartLightningWaveformHistoryPage => {
  const samples = [...(timeseries[SMART_LIGHTNING_WAVEFORM_KEY] || [])]
    .filter((sample) => Number.isFinite(Number(sample.ts)))
    .sort((left, right) => Number(right.ts) - Number(left.ts));
  const records = samples.slice(0, pageSize).map((sample) => {
    const timestamp = Number(sample.ts);
    const seriesData = buildSmartLightningWaveformSeriesData(definition, sample.value, timestamp);
    return {timestamp, seriesData, sampleCount: seriesData[0]?.points.length || 0};
  });
  return {
    records,
    hasMore: samples.length > pageSize,
    nextEndTs: records.length ? records[records.length - 1].timestamp : undefined
  };
};

export type SmartLightningHistoryPoint = [number, number];

export interface SmartLightningHistoryYAxisRange {
  min: number;
  max: number;
}

/** 历史回顾弹窗可选用业务模块提供的键名、单位和状态定义。 */
export interface SmartLightningHistoryMetricDefinition {
  readonly key: string;
  readonly label: string;
  readonly unit?: string;
  readonly valueType?: 'status' | 'telemetry';
  readonly chartable?: boolean;
}

/** 将防雷的共享定义投影为回顾弹窗统一结构，供电气安全覆盖型号配置。 */
const resolveHistoryMetricDefinitions = (
  deviceProfileName: string,
  metricDefinitions?: readonly SmartLightningHistoryMetricDefinition[]
): readonly SmartLightningHistoryMetricDefinition[] => metricDefinitions ??
  [...getSmartLightningRealtimeMetricDefinitions(deviceProfileName).map((definition) => ({
    key: definition.key,
    label: definition.label,
    unit: definition.unit,
    valueType: definition.valueType
  })), ...getSmartLightningChartDefinitions(deviceProfileName)
    .filter((definition) => definition.kind === 'waveform')
    .map((definition) => ({
      key: SMART_LIGHTNING_WAVEFORM_KEY,
      label: definition.title,
      unit: definition.series[0]?.unit,
      valueType: 'telemetry' as const
    }))];

/** 折线图只接收可解析为有限数值的数据，并按时间从旧到新排序。 */
export const buildSmartLightningHistoryPoints = (
  timeseries: TimeseriesData,
  key: string
): SmartLightningHistoryPoint[] => (timeseries[key] || [])
  .map((item) => [item.ts, Number(item.value)] as SmartLightningHistoryPoint)
  .filter((item) => Number.isFinite(item[0]) && Number.isFinite(item[1]))
  .sort((left, right) => left[0] - right[0]);

/**
 * 根据当前折线数据计算纵轴范围：不强制从零开始，并在数据上下各预留 15% 空间，
 * 让小幅变化也能在回顾图中清晰呈现；没有数据时返回安全兜底范围。
 */
export const getSmartLightningHistoryYAxisRange = (
  points: ReadonlyArray<SmartLightningHistoryPoint>
): SmartLightningHistoryYAxisRange => {
  if (!points.length) {
    return {min: 0, max: 1};
  }

  let dataMin = points[0][1];
  let dataMax = points[0][1];
  for (const point of points) {
    dataMin = Math.min(dataMin, point[1]);
    dataMax = Math.max(dataMax, point[1]);
  }

  const dataRange = dataMax - dataMin;
  const padding = dataRange > 0
    ? dataRange * 0.15
    : Math.max(Math.abs(dataMin) * 0.1, 1);
  return {
    min: dataMin - padding,
    max: dataMax + padding
  };
};

/** 已配置的设备参数显示中文业务名，同时保留原始遥测 key 便于排障。 */
export const getSmartLightningTelemetryParameterLabel = (
  deviceProfileName: string,
  key: string,
  metricDefinitions?: readonly SmartLightningHistoryMetricDefinition[]
): string => {
  const definitions = resolveHistoryMetricDefinitions(deviceProfileName, metricDefinitions);
  const definition = definitions.find((item) => item.key === key);
  return definition ? `${definition.label}（${key}）` : key;
};

/** 返回指定遥测参数的单位；未配置的参数返回空字符串。 */
export const getSmartLightningTelemetryParameterUnit = (
  deviceProfileName: string,
  key: string,
  metricDefinitions?: readonly SmartLightningHistoryMetricDefinition[]
): string => {
  const definitions = resolveHistoryMetricDefinitions(deviceProfileName, metricDefinitions);
  return definitions.find((item) => item.key === key)?.unit || '';
};

/** 防雷仅展示当前型号已配置中文语义的数值参数；外部模块传入定义时保持既有过滤行为。 */
export const filterSmartLightningTelemetryParameters = (
  deviceProfileName: string,
  keys: string[],
  metricDefinitions?: readonly SmartLightningHistoryMetricDefinition[]
): string[] => {
  const definitions = resolveHistoryMetricDefinitions(deviceProfileName, metricDefinitions);
  return keys.filter((key) => {
    // 在线状态和活跃时间不是历史指标，不放入设备数据查询参数列表。
    if (['active', 'inactivityAlarmTime', 'lastActivityTime'].includes(key)) {
      return false;
    }
    const definition = definitions.find((item) => item.key === key);
    // WF 是整包波形字符串，只允许已配置波形的防雷型号进入专用查询，不能当普通数值绘制。
    if (key === SMART_LIGHTNING_WAVEFORM_KEY) {
      return metricDefinitions === undefined && !!definition;
    }
    // 防雷型号测点清单已完整：旧键、未知键不能以纯英文形式混入历史回顾。
    // 电气安全等调用方显式传入定义时，保留其原有筛选契约。
    if (metricDefinitions === undefined && !definition) {
      return false;
    }
    return definition?.valueType !== 'status' && definition?.chartable !== false && !/(status|state)$/i.test(key);
  });
};
