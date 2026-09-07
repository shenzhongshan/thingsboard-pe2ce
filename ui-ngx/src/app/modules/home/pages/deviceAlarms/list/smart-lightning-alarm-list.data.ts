export type SmartLightningAlarmListSeverityTone = 'critical' | 'major' | 'minor' | 'warning' | 'indeterminate';
export type SmartLightningAlarmListWorkOrderStatus = '未派发' | '已派发' | '已处理';
export type SmartLightningAlarmListSeverityTab = '全部' | '紧急' | '一般' | '正常';

export interface SmartLightningAlarmListRow {
  id: string;
  originatorId: string;
  projectDeviceName: string;
  alarmTime: number;
  workOrderStatus: SmartLightningAlarmListWorkOrderStatus;
  alarmType: string;
  severity: string;
  severityTone: SmartLightningAlarmListSeverityTone;
  gatewayIdentifier: string;
  deviceType: string;
  meterAddress: string;
  circuitName: string;
  timeSetting: string;
  detailDescription: string;
}

export interface SmartLightningAlarmListFilters {
  severityTab: SmartLightningAlarmListSeverityTab;
  alarmType: string;
  severity: string;
  workOrderStatus: string;
  keyword: string;
  startTime: string;
  endTime: string;
}

export interface SmartLightningAlarmListContext {
  deviceType?: string;
  productName?: string;
  meterAddress?: string;
  circuitName?: string;
  gatewayIdentifier?: string;
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

const severityLabels: Record<SmartLightningAlarmListSeverityTone, string> = {
  critical: '严重',
  major: '重要',
  minor: '次要',
  warning: '警告',
  indeterminate: '不确定'
};

const severityRank: Record<SmartLightningAlarmListSeverityTone, number> = {
  critical: 0,
  major: 1,
  minor: 2,
  warning: 3,
  indeterminate: 4
};

const alarmSeverityTone = (severity: string): SmartLightningAlarmListSeverityTone => {
  switch (severity) {
    case 'CRITICAL': return 'critical';
    case 'MAJOR': return 'major';
    case 'MINOR': return 'minor';
    case 'WARNING': return 'warning';
    default: return 'indeterminate';
  }
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

const workOrderStatus = (alarm: Pick<SmartLightningAlarmLike, 'status' | 'cleared' | 'assignTs'>): SmartLightningAlarmListWorkOrderStatus => {
  if (alarm.cleared || alarm.status === 'CLEARED_ACK' || alarm.status === 'CLEARED_UNACK') {
    return '已处理';
  }
  return alarm.assignTs > 0 ? '已派发' : '未派发';
};

/** 将原生 AlarmInfo 和设备上下文转换为原型表格使用的行结构。 */
export const mapSmartLightningAlarmListRow = (
  alarm: SmartLightningAlarmLike,
  context: SmartLightningAlarmListContext = {}
): SmartLightningAlarmListRow => {
  const tone = alarmSeverityTone(alarm.severity);
  return {
    id: alarm.id?.id || `${alarm.originator?.id || 'unknown'}-${alarm.startTs}-${alarm.type || 'alarm'}`,
    originatorId: alarm.originator?.id || '',
    projectDeviceName: valueAsText(alarm.originatorDisplayName || alarm.originatorName, '未命名来源'),
    alarmTime: Number(alarm.startTs) || Number(alarm.createdTime) || 0,
    workOrderStatus: workOrderStatus(alarm),
    alarmType: valueAsText(alarm.type, '未分类'),
    severity: severityLabels[tone],
    severityTone: tone,
    gatewayIdentifier: valueAsText(context.gatewayIdentifier || detailValue(alarm.details, ['gatewayIdentifier', 'gatewayId', 'gateway'])),
    deviceType: valueAsText(context.deviceType),
    meterAddress: valueAsText(context.meterAddress || detailValue(alarm.details, ['meterAddress', 'instrumentAddress', 'address'])),
    circuitName: valueAsText(context.circuitName || detailValue(alarm.details, ['circuitName', 'loopName', 'circuit'])),
    timeSetting: valueAsText(context.timeSetting || detailValue(alarm.details, ['timeSetting', 'period']), '全时段'),
    detailDescription: detailText(alarm.details)
  };
};

/** 活动/未处理告警优先，活动告警按严重程度，已处理告警按发生时间倒序。 */
export const sortSmartLightningAlarmListRows = (rows: SmartLightningAlarmListRow[]): SmartLightningAlarmListRow[] =>
  [...rows].sort((left, right) => {
    const leftHandled = left.workOrderStatus === '已处理';
    const rightHandled = right.workOrderStatus === '已处理';
    if (leftHandled !== rightHandled) {
      return leftHandled ? 1 : -1;
    }
    if (!leftHandled) {
      const severityDifference = severityRank[left.severityTone] - severityRank[right.severityTone];
      if (severityDifference) {
        return severityDifference;
      }
    }
    return right.alarmTime - left.alarmTime;
  });

const matchesSeverityTab = (row: SmartLightningAlarmListRow, tab: SmartLightningAlarmListSeverityTab): boolean => {
  if (tab === '全部') return true;
  if (tab === '紧急') return row.severityTone === 'critical';
  if (tab === '一般') return row.severityTone === 'major' || row.severityTone === 'minor';
  return row.severityTone === 'warning' || row.severityTone === 'indeterminate';
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
    row.alarmTime >= startTime && row.alarmTime <= endTime &&
    (!keyword || `${row.projectDeviceName} ${row.alarmType} ${row.gatewayIdentifier} ${row.deviceType} ${row.meterAddress} ${row.circuitName} ${row.detailDescription}`.toLowerCase().includes(keyword))
  );
};

export const formatSmartLightningAlarmListTime = (timestamp: number): string => {
  if (!Number.isFinite(timestamp) || timestamp <= 0) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).format(new Date(timestamp)).replace(/\//g, '-');
};
