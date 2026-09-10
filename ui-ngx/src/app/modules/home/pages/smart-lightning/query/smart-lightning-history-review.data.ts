/** 文件职责：数据查询模块的 smart-lightning-history-review.data，定义数据模型、接口结果映射与纯函数。 */
import {TimeseriesData} from '@shared/models/telemetry/telemetry.models';
import {getSmartLightningRealtimeMetricDefinitions} from '../monitoring/smart-lightning-device-detail.data';

export type SmartLightningHistoryPoint = [number, number];

/** 折线图只接收可解析为有限数值的数据，并按时间从旧到新排序。 */
export const buildSmartLightningHistoryPoints = (
  timeseries: TimeseriesData,
  key: string
): SmartLightningHistoryPoint[] => (timeseries[key] || [])
  .map((item) => [item.ts, Number(item.value)] as SmartLightningHistoryPoint)
  .filter((item) => Number.isFinite(item[0]) && Number.isFinite(item[1]))
  .sort((left, right) => left[0] - right[0]);

/** 已配置的设备参数显示中文业务名，同时保留原始遥测 key 便于排障。 */
export const getSmartLightningTelemetryParameterLabel = (deviceProfileName: string, key: string): string => {
  const definition = getSmartLightningRealtimeMetricDefinitions(deviceProfileName)
    .find((item) => item.key === key);
  return definition ? `${definition.label}（${key}）` : key;
};

/** 返回指定遥测参数的单位；未配置的参数返回空字符串。 */
export const getSmartLightningTelemetryParameterUnit = (deviceProfileName: string, key: string): string =>
  getSmartLightningRealtimeMetricDefinitions(deviceProfileName).find((item) => item.key === key)?.unit || '';

/** 回顾趋势只展示数值参数，状态/枚举值不放入单选列表。 */
export const filterSmartLightningTelemetryParameters = (deviceProfileName: string, keys: string[]): string[] => {
  const definitions = getSmartLightningRealtimeMetricDefinitions(deviceProfileName);
  return keys.filter((key) => {
    const definition = definitions.find((item) => item.key === key);
    return definition?.valueType !== 'status' && !/(status|state)$/i.test(key);
  });
};
