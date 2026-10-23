/** 文件职责：定义电气安全设备详情的实时遥测展示和时间窗口。 */
import {
  formatElectricalSafetyMetricDisplayValue,
  getElectricalSafetyRealtimeMetricDefinitions
} from './electrical-safety-monitoring.data';

/** 设备详情只保存平台返回的原始工程值和上报时间，不在前端重复换算。 */
export interface ElectricalSafetyDetailSample {
  readonly ts: number;
  readonly value: string;
}

/** 实时数据表中的一项测点；未知协议键直接显示键名，避免猜测含义和单位。 */
export interface ElectricalSafetyDetailMetric {
  key: string;
  label: string;
  value: string;
  unit: string;
  status: boolean;
}

export type ElectricalSafetyDetailPeriod = 'day' | 'week' | 'month';

/** 设备状态、位置和 ThingsBoard 系统时间键不作为业务遥测测点展示。 */
const CONTEXT_KEYS = new Set([
  'active', 'lastActivityTime', 'lastConnectTime', 'lastDisconnectTime',
  'inactivityAlarmTime', 'latitude', 'longitude', 'lat', 'lon'
]);

/** 按设备型号定义构建实时测点；已知但缺值的型号测点显示 __，其他未配置字段保留平台实际值。 */
export function buildElectricalSafetyDetailMetrics(
  keys: readonly string[],
  latest: ReadonlyMap<string, ElectricalSafetyDetailSample>,
  deviceProfileName?: string | null
): ElectricalSafetyDetailMetric[] {
  const definitions = getElectricalSafetyRealtimeMetricDefinitions(deviceProfileName);
  const definitionsByKey = new Map(definitions.map((definition) => [definition.key, definition]));
  const allKeys = new Set([...definitionsByKey.keys(), ...keys, ...latest.keys()]);
  const metrics: ElectricalSafetyDetailMetric[] = [];
  for (const key of allKeys) {
    if (CONTEXT_KEYS.has(key)) {
      continue;
    }
    const rawValue = latest.get(key)?.value;
    const definition = definitionsByKey.get(key);
    const status = definition?.valueType === 'status';
    const hasValue = rawValue !== undefined && rawValue !== null && String(rawValue).trim() !== '';
    const value = hasValue
      ? definition ? formatElectricalSafetyMetricDisplayValue(definition, rawValue) : String(rawValue)
      : definition?.fallbackValue || '--';
    metrics.push({
      key,
      label: definition?.label || key,
      value,
      unit: definition?.unit || '',
      status
    });
  }
  const configuredOrder = new Map(definitions.map((definition, index) => [definition.key, index]));
  return metrics.sort((left, right) => {
    if (left.status !== right.status) {
      return left.status ? -1 : 1;
    }
    const leftOrder = configuredOrder.get(left.key);
    const rightOrder = configuredOrder.get(right.key);
    if (leftOrder !== undefined || rightOrder !== undefined) {
      return (leftOrder ?? Number.MAX_SAFE_INTEGER) - (rightOrder ?? Number.MAX_SAFE_INTEGER);
    }
    return left.label.localeCompare(right.label, 'zh-CN');
  });
}

/** 时间范围沿用防雷详情页的本地日、近七天和近三十天口径。 */
export function getElectricalSafetyDetailWindow(period: ElectricalSafetyDetailPeriod, now = Date.now()): {
  startTs: number;
  endTs: number;
} {
  if (period === 'day') {
    const start = new Date(now);
    start.setHours(0, 0, 0, 0);
    return {startTs: start.getTime(), endTs: now};
  }
  const days = period === 'week' ? 7 : 30;
  return {startTs: now - days * 24 * 60 * 60 * 1000, endTs: now};
}
