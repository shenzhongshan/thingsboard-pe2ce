/** 文件职责：看板模块的 smart-lightning-summary.data，定义数据模型、接口结果映射与纯函数。 */
/** 格式化可为空的摘要指标；保留接口返回的原始数值，不在前端截断小数位。 */
export const formatSmartLightningMetric = (
  value: number | null | undefined,
  unit: string
): string => {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return '--';
  }
  return `${value} ${unit}`;
};

const padTimePart = (value: number): string => String(value).padStart(2, '0');

/** 将遥测时间戳格式化为悬浮提示中使用的本地日期时间。 */
export const formatSmartLightningTimestamp = (timestamp: number | null | undefined): string => {
  if (timestamp === null || timestamp === undefined || !Number.isFinite(timestamp)) {
    return '--';
  }
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) {
    return '--';
  }
  return `${date.getFullYear()}-${padTimePart(date.getMonth() + 1)}-${padTimePart(date.getDate())}`
    + ` ${padTimePart(date.getHours())}:${padTimePart(date.getMinutes())}:${padTimePart(date.getSeconds())}`;
};

/**
 * 按规范测点键解析悬浮信息中的相位。
 * 电压键 PhAVolt、PhBVolt、PhCVolt 从第三个字符读取相位；
 * 漏流键 LeakCurrA、LeakCurrB、LeakCurrC 则从末尾读取相位。
 */
export const getSmartLightningPhaseLabel = (
  parameterKey: string | null | undefined,
  fallback: string | null | undefined = null
): string => {
  const normalizedKey = String(parameterKey || '').trim();
  const phaseMatch = normalizedKey.match(/^(?:Ph([ABC])Volt|LeakCurr([ABC]))$/i);
  const phase = phaseMatch?.[1] || phaseMatch?.[2];
  if (phase) {
    return phase.toUpperCase() + '相';
  }
  return fallback || '--';
};

/** 组装最大值指标的完整悬浮信息，包括设备、相位/极性和发生时间。 */
export const buildSmartLightningMaximumTooltip = (
  label: string,
  value: number | null | undefined,
  unit: string,
  deviceName: string | null | undefined,
  detailName: string,
  detailValue: string | null | undefined,
  timestamp: number | null | undefined
): string => {
  const formattedValue = value === null || value === undefined || !Number.isFinite(value)
    ? '--'
    : `${value}${unit}`;
  return [
    `${label}：${formattedValue}`,
    `所属设备：${deviceName || '--'}`,
    `${detailName}：${detailValue || '--'}`,
    `发生时间：${formatSmartLightningTimestamp(timestamp)}`
  ].join('\n');
};

/**
 * 组装周期最值的完整悬浮提示，明确显示设备、相位或规范参数和发生时间。
 * 周期数据的来源已经由后端确定，前端只负责展示，不再次查询遥测。
 */
export const buildSmartLightningExtremaTooltip = (
  label: string,
  value: number | null | undefined,
  unit: string,
  deviceName: string | null | undefined,
  parameter: string | null | undefined,
  timestamp: number | null | undefined
): string => {
  const formattedValue = value === null || value === undefined || !Number.isFinite(value)
    ? '--'
    : `${value}${unit}`;
  const phase = getSmartLightningPhaseLabel(parameter);
  const parameterLine = phase === '--'
    ? `参数：${parameter || '--'}`
    : `相位：${phase}`;
  return [
    `${label}：${formattedValue}`,
    `设备：${deviceName || '--'}`,
    parameterLine,
    `时间：${formatSmartLightningTimestamp(timestamp)}`
  ].join('\n');
};

/** 返回数组中的最大有效数值；没有可用遥测时返回 null。 */
export const getMaximumFiniteValue = (values: Array<number | null | undefined>): number | null => {
  let maximum: number | null = null;
  for (const value of values) {
    if (value === null || value === undefined || !Number.isFinite(value)) {
      continue;
    }
    if (maximum === null || value > maximum) {
      maximum = value;
    }
  }
  return maximum;
};

/** 计算雷电累计计数的增量，并处理设备重启后计数器归零的情况。 */
export const calculateLightningCounterDelta = (previous: number, current: number): number => {
  if (!Number.isFinite(previous) || !Number.isFinite(current) || current < 0) {
    return 0;
  }
  return current >= previous ? current - previous : current;
};
