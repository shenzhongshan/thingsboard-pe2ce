/** 文件职责：告警列表模块的 smart-lightning-alarm-list.data，定义数据模型、接口结果映射与纯函数。 */
import {
  getSmartLightningAlarmSeverityLabel,
  getSmartLightningAlarmSeverityTone,
  getSmartLightningAlarmStatusLabel
} from '../shared/smart-lightning-alarm-display';
import type {SmartLightningAlarmSeverityTone} from '../shared/smart-lightning-alarm-display';

export type SmartLightningAlarmListSeverityTone = SmartLightningAlarmSeverityTone;
export type SmartLightningAlarmListWorkOrderStatus = '未派发' | '已派发';
export type SmartLightningAlarmListAlarmStatus = '未确认未清除' | '已确认未清除' | '未确认已清除' | '已确认已清除';
export type SmartLightningAlarmListSeverityTab = '全部' | '严重' | '重要' | '次要' | '警告' | '不确定';

export interface SmartLightningAlarmListRow {
  id: string;
  originatorId: string;
  projectDeviceName: string;
  alarmTime: number;
  alarmStatus: SmartLightningAlarmListAlarmStatus;
  workOrderStatus: SmartLightningAlarmListWorkOrderStatus;
  alarmType: string;
  severity: string;
  severityTone: SmartLightningAlarmListSeverityTone;
  deviceType: string;
  productName: string;
  meterAddress: string;
  timeSetting: string;
  detailDescription: string;
  /** 原生告警状态，用于决定确认、清除和派发操作是否可用。 */
  status: string;
  cleared: boolean;
}

export interface SmartLightningAlarmListFilters {
  severityTab: SmartLightningAlarmListSeverityTab;
  alarmType: string;
  severity: string;
  workOrderStatus: string;
  productName: string;
  keyword: string;
  startTime: string;
  endTime: string;
}

export interface SmartLightningAlarmListContext {
  deviceType?: string;
  productName?: string;
  meterAddress?: string;
  timeSetting?: string;
}

/** 与 AlarmInfo 对齐的最小结构，避免纯数据转换器依赖 Angular HTTP 模块。 */
export interface SmartLightningAlarmLike {
  id?: {id: string};
  originator?: {id: string; entityType?: string};
  originatorDisplayName?: string;
  originatorName?: string;
  startTs: number;
  createdTime?: number;
  type?: string;
  severity: string;
  status: string;
  cleared: boolean;
  assignTs: number;
  details?: unknown;
}

const severityRank: Record<SmartLightningAlarmListSeverityTone, number> = {
  critical: 0,
  major: 1,
  minor: 2,
  warning: 3,
  indeterminate: 4
};

/** 同一告警状态、同一严重程度下，未派发优先于已派发。 */
const workOrderStatusRank: Record<SmartLightningAlarmListWorkOrderStatus, number> = {
  '未派发': 0,
  '已派发': 1
};

/** ThingsBoard 原生告警状态在页面中的中文展示与排序顺序。 */
const alarmStatusRank: Record<SmartLightningAlarmListAlarmStatus, number> = {
  '未确认未清除': 0,
  '已确认未清除': 1,
  '未确认已清除': 2,
  '已确认已清除': 3
};

const valueAsText = (value: unknown, fallback = '--'): string => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return fallback;
  }
  return String(value);
};

const detailValue = (details: unknown, keys: string[]): string => {
  if (!details || typeof details !== 'object') {
    return '--';
  }
  const source = details as Record<string, unknown>;
  for (const key of keys) {
    if (source[key] !== undefined && source[key] !== null && String(source[key]).trim() !== '') {
      return String(source[key]);
    }
  }
  return '--';
};

const detailText = (details: unknown): string => {
  if (details === undefined || details === null) {
    return '--';
  }
  if (typeof details === 'string') {
    return details;
  }
  if (typeof details === 'object') {
    const source = details as Record<string, unknown>;
    const message = detailValue(source, ['message', 'description', 'detail', 'alarmMessage']);
    if (message !== '--') {
      return message;
    }
    try {
      return JSON.stringify(details);
    } catch {
      return '--';
    }
  }
  return String(details);
};

/** 工单列仅反映是否已经派发，不再混入告警确认/清除状态。 */
const workOrderStatus = (alarm: Pick<SmartLightningAlarmLike, 'assignTs'>): SmartLightningAlarmListWorkOrderStatus =>
  alarm.assignTs > 0 ? '已派发' : '未派发';

/** 将原生 AlarmInfo 和设备上下文转换为原型表格使用的行结构。 */
export const mapSmartLightningAlarmListRow = (
  alarm: SmartLightningAlarmLike,
  context: SmartLightningAlarmListContext = {},
  assetPath = ''
): SmartLightningAlarmListRow => {
  const tone = getSmartLightningAlarmSeverityTone(alarm.severity);
  return {
    id: alarm.id?.id || `${alarm.originator?.id || 'unknown'}-${alarm.startTs}-${alarm.type || 'alarm'}`,
    originatorId: alarm.originator?.id || '',
    projectDeviceName: valueAsText(alarm.originatorDisplayName || alarm.originatorName, '未命名来源'),
    alarmTime: Number(alarm.startTs) || Number(alarm.createdTime) || 0,
    alarmStatus: getSmartLightningAlarmStatusLabel(alarm.status) as SmartLightningAlarmListAlarmStatus,
    workOrderStatus: workOrderStatus(alarm),
    alarmType: valueAsText(alarm.type, '未分类'),
    severity: getSmartLightningAlarmSeverityLabel(tone),
    severityTone: tone,
    deviceType: valueAsText(context.deviceType),
    productName: valueAsText(context.productName),
    // 仪表地址列展示设备的资产树绝对地址；设备不在树内时回退服务器属性或告警详情。
    meterAddress: valueAsText(assetPath.trim() || context.meterAddress || detailValue(alarm.details, ['meterAddress', 'instrumentAddress', 'address'])),
    timeSetting: valueAsText(context.timeSetting || detailValue(alarm.details, ['timeSetting', 'period']), '全时段'),
    detailDescription: detailText(alarm.details),
    status: alarm.status,
    cleared: Boolean(alarm.cleared)
  };
};

/**
 * 列表排序：先按原生告警状态（未确认未清除 → 已确认未清除 → 未确认已清除 → 已确认已清除）；
 * 每个状态组内按严重程度由高到低，再按派发状态和告警发生时间排序。
 */
export const sortSmartLightningAlarmListRows = (rows: SmartLightningAlarmListRow[]): SmartLightningAlarmListRow[] =>
  [...rows].sort((left, right) => {
    const alarmStatusDifference = alarmStatusRank[left.alarmStatus] - alarmStatusRank[right.alarmStatus];
    if (alarmStatusDifference) {
      return alarmStatusDifference;
    }
    const severityDifference = severityRank[left.severityTone] - severityRank[right.severityTone];
    if (severityDifference) {
      return severityDifference;
    }
    const workOrderDifference = workOrderStatusRank[left.workOrderStatus] - workOrderStatusRank[right.workOrderStatus];
    if (workOrderDifference) {
      return workOrderDifference;
    }
    return right.alarmTime - left.alarmTime;
  });

const matchesSeverityTab = (row: SmartLightningAlarmListRow, tab: SmartLightningAlarmListSeverityTab): boolean => {
  if (tab === '全部') return true;
  return row.severity === tab;
};

/** 前端筛选仅作用于已经按资产树授权范围取得的告警。 */
export const filterSmartLightningAlarmListRows = (
  rows: SmartLightningAlarmListRow[],
  filters: SmartLightningAlarmListFilters
): SmartLightningAlarmListRow[] => {
  const keyword = filters.keyword.trim().toLowerCase();
  const startTime = filters.startTime ? new Date(filters.startTime).getTime() : 0;
  const endTime = filters.endTime ? new Date(filters.endTime).getTime() : Number.POSITIVE_INFINITY;
  return rows.filter((row) =>
    matchesSeverityTab(row, filters.severityTab) &&
    (!filters.alarmType || row.alarmType === filters.alarmType) &&
    (!filters.severity || row.severity === filters.severity) &&
    (!filters.workOrderStatus || row.workOrderStatus === filters.workOrderStatus) &&
    (!filters.productName || row.productName === filters.productName) &&
    row.alarmTime >= startTime && row.alarmTime <= endTime &&
    (!keyword || `${row.projectDeviceName} ${row.alarmType} ${row.deviceType} ${row.meterAddress} ${row.detailDescription}`.toLowerCase().includes(keyword))
  );
};

export const formatSmartLightningAlarmListTime = (timestamp: number): string => {
  if (!Number.isFinite(timestamp) || timestamp <= 0) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).format(new Date(timestamp)).replace(/\//g, '-');
};
