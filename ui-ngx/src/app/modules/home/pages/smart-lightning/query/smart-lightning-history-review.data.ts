/** 文件职责：数据查询模块的 smart-lightning-history-review.data，定义数据模型、接口结果映射与纯函数。 */
import {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {getSmartLightningRealtimeMetricDefinitions} from '../monitoring/smart-lightning-device-detail.data';

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
  getSmartLightningRealtimeMetricDefinitions(deviceProfileName).map((definition) => ({
    key: definition.key,
    label: definition.label,
    unit: definition.unit,
    valueType: definition.valueType
  }));

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

/** 回顾趋势只展示数值参数，状态/枚举值不放入单选列表。 */
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
    return definition?.valueType !== 'status' && definition?.chartable !== false && !/(status|state)$/i.test(key);
  });
};
