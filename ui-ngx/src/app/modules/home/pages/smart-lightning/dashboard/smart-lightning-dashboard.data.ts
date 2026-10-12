/** 文件职责：看板模块的 smart-lightning-dashboard.data，定义数据模型、接口结果映射与纯函数。 */
import { AlarmInfo, AlarmSearchStatus, AlarmSeverity, AlarmStatus, AlarmQueryV2 } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { AlarmFilter } from '@shared/models/query/query.models';
import { EntityRelationInfo } from '@shared/models/relation.models';
import { AttributeData } from '@shared/models/telemetry/telemetry.models';
import { SmartLightningAlarmRow } from '../../deviceAlarms/shared/smart-lightning-alarm-display';

export type { SmartLightningAlarmRow } from '../../deviceAlarms/shared/smart-lightning-alarm-display';

export enum SmartLightningPeriod {
  day = 'day',
  week = 'week',
  month = 'month'
}

export type SmartLightningChartId = 'ground' | 'spd' | 'alarm' | 'orders' | 'lightning';

export type SmartLightningChartPeriods = Record<SmartLightningChartId, SmartLightningPeriod>;

/** 看板告警记录表面板最多显示的告警条数。 */
export const SMART_LIGHTNING_ALARM_DISPLAY_LIMIT = 10;

/** 各图表周期状态的初始值（全部为“日”）。 */
export const defaultSmartLightningChartPeriods: SmartLightningChartPeriods = {
  ground: SmartLightningPeriod.day,
  spd: SmartLightningPeriod.day,
  alarm: SmartLightningPeriod.day,
  orders: SmartLightningPeriod.day,
  lightning: SmartLightningPeriod.day
};

/** 返回新状态对象，确保某张图切换周期时不会影响其他图表。 */
export const setSmartLightningChartPeriod = (
  periods: SmartLightningChartPeriods,
  chart: SmartLightningChartId,
  period: SmartLightningPeriod
): SmartLightningChartPeriods => ({...periods, [chart]: period});

/** 设备汇总统计：总数与在线/离线/未连接数量。 */
export interface SmartLightningDeviceSummary {
  total: number;
  online: number;
  offline: number;
  onlineRate: number;
}

/** 按服务器属性 active 值列表统计设备在线/离线/未连接数量。 */
export const calculateSmartLightningDeviceSummary = (activeValues: unknown[]): SmartLightningDeviceSummary => {
  const total = activeValues.length;
  const online = activeValues.filter((value) => value === true || value === 'true').length;
  return {
    total,
    online,
    offline: total - online,
    onlineRate: total ? Math.round((online / total) * 100) : 0
  };
};

/** 格式化最新地网阻值聚合数据；保留后端原始小数位，缺失数据时显示占位符。 */
export const formatSmartLightningResistance = (value: number | null | undefined): string => {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return '--';
  }
  return `${value} Ω`;
};
/** 从服务器属性列表中取 active 属性原始值，供在线判断使用。 */
export const getSmartLightningActiveValue = (attributes: AttributeData[]): unknown =>
  attributes.find((attribute) => attribute.key === 'active')?.value;
const latestActiveAlarmFilter: AlarmFilter = {
  statusList: [AlarmSearchStatus.ACTIVE]
};
/** 构建看板告警面板调用防雷模块告警接口时使用的 v2 查询条件。 */
export const createSmartLightningAlarmQuery = (): AlarmQueryV2 => new AlarmQueryV2(
  null,
  new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, {property: 'createdTime', direction: Direction.DESC}),
  latestActiveAlarmFilter
);

/**
 * 实时告警计数订阅会先推送一份初始快照。该快照不应触发第二次 REST 请求；
 * 只有计数真正发生变化时才刷新看板告警列表及其派生统计。
 */
export const shouldReloadSmartLightningAlarms = (
  previousCount: number | null,
  nextCount: number
): boolean => previousCount !== null && previousCount !== nextCount;

/**
 * 判断告警数量增加时是否需要等待一次后端事务提交再复读列表。
 * 告警创建通知可能先于告警明细落库到达浏览器；只安排一次延迟复读，
 * 不使用递归重试，避免请求链持续把页面置为加载状态。
 */
export const shouldScheduleSmartLightningAlarmSettleRefresh = (
  previousCount: number | null,
  nextCount: number
): boolean => previousCount !== null && nextCount > previousCount;

/**
 * 只允许最新一轮告警列表请求写回页面。
 *
 * WebSocket 通知可能在上一轮 HTTP 请求尚未完成时再次到达，旧请求的响应
 * 不能覆盖新请求的结果，也不能把最新请求重新置回加载态。
 */
export const isLatestSmartLightningAlarmRequest = (
  requestId: number,
  latestRequestId: number
): boolean => requestId === latestRequestId;

/** 按告警类型聚合的数量项。 */
export interface SmartLightningAlarmTypeTotal {
  type: string;
  count: number;
}

export type SmartLightningAlarmChartState = 'loading' | 'error' | 'empty' | 'ready';

/** 区分“接口不可用”与“确认没有告警”两种空状态。 */
export const getSmartLightningAlarmChartState = (
  loading: boolean,
  loadError: boolean,
  alarmTypeTotals: SmartLightningAlarmTypeTotal[]
): SmartLightningAlarmChartState => {
  if (loading) {
    return 'loading';
  }
  if (loadError) {
    return 'error';
  }
  return alarmTypeTotals.length ? 'ready' : 'empty';
};

/** 加载失败时保持静默，避免图表面板闪现短暂的空状态提示。 */
export const getSmartLightningAlarmChartMessage = (
  loading: boolean,
  loadError: boolean,
  alarmTypeTotals: SmartLightningAlarmTypeTotal[]
): string | undefined => {
  switch (getSmartLightningAlarmChartState(loading, loadError, alarmTypeTotals)) {
    case 'error':
      return '告警统计加载失败';
    case 'empty':
      return '当前没有未清除告警';
    default:
      return undefined;
  }
};
/** 只统计当前未清除的告警，并按 ThingsBoard 告警类型分组。 */
export const aggregateSmartLightningActiveAlarms = (alarms: AlarmInfo[]): SmartLightningAlarmTypeTotal[] => {
  const counts = new Map<string, number>();
  alarms
    .filter((alarm) => alarm.status === AlarmStatus.ACTIVE_UNACK || alarm.status === AlarmStatus.ACTIVE_ACK)
    .forEach((alarm) => {
      const type = alarm.type?.trim() || '未分类';
      counts.set(type, (counts.get(type) || 0) + 1);
    });
  return [...counts.entries()]
    .map(([type, count]) => ({type, count}))
    .sort((left, right) => {
      const countDifference = right.count - left.count;
      if (countDifference) {
        return countDifference;
      }
      if (left.type === '未分类') {
        return 1;
      }
      if (right.type === '未分类') {
        return -1;
      }
      return left.type.localeCompare(right.type);
    });
};
const alarmSeverityLabels: Record<AlarmSeverity, SmartLightningAlarmRow['severity']> = {
  [AlarmSeverity.CRITICAL]: '严重',
  [AlarmSeverity.MAJOR]: '重要',
  [AlarmSeverity.MINOR]: '次要',
  [AlarmSeverity.WARNING]: '警告',
  [AlarmSeverity.INDETERMINATE]: '不确定'
};

const alarmSeverityTones: Record<AlarmSeverity, SmartLightningAlarmRow['severityTone']> = {
  [AlarmSeverity.CRITICAL]: 'critical',
  [AlarmSeverity.MAJOR]: 'major',
  [AlarmSeverity.MINOR]: 'minor',
  [AlarmSeverity.WARNING]: 'warning',
  [AlarmSeverity.INDETERMINATE]: 'indeterminate'
};

const alarmStatusLabels: Record<AlarmStatus, string> = {
  [AlarmStatus.ACTIVE_UNACK]: '未确认',
  [AlarmStatus.ACTIVE_ACK]: '已确认',
  [AlarmStatus.CLEARED_UNACK]: '已清除未确认',
  [AlarmStatus.CLEARED_ACK]: '已清除'
};

/** 只映射 AlarmInfo 保证存在的字段；接口缺失设备信息时不伪造元数据。 */
export interface SmartLightningAlarmContext {
  parentDirectory?: string;
  deviceProfileName?: string;
  productName?: string;
}

/** 把 AlarmInfo 格式化为告警记录表行：中文化等级/状态并补充上下文字段。 */
export const formatSmartLightningAlarm = (
  alarm: AlarmInfo,
  context: SmartLightningAlarmContext = {}
): SmartLightningAlarmRow => ({
  source: alarm.originatorLabel || alarm.originatorName || '未命名来源',
  secondaryLocation: context.parentDirectory || '--',
  device: alarm.originatorDisplayName || alarm.originatorName || '未命名设备',
  deviceType: context.deviceProfileName || '--',
  deviceName: context.productName || '--',
  type: alarm.type || '未分类',
  severity: alarmSeverityLabels[alarm.severity] || '不确定',
  severityTone: alarmSeverityTones[alarm.severity] || 'indeterminate',
  status: alarmStatusLabels[alarm.status] || '未知',
  startTs: alarm.startTs
});

const severityRank: Record<SmartLightningAlarmRow['severityTone'], number> = {
  critical: 0,
  major: 1,
  minor: 2,
  warning: 3,
  indeterminate: 4
};

/** 告警行比较器：严重程度高的在前，同等级按时间从新到旧。 */
export const compareSmartLightningAlarms = (left: SmartLightningAlarmRow, right: SmartLightningAlarmRow): number =>
  severityRank[left.severityTone] - severityRank[right.severityTone] || right.startTs - left.startTs;
/** 让监控墙聚焦于严重程度最高的活动告警。 */
export const sortSmartLightningAlarms = (alarms: SmartLightningAlarmRow[]): SmartLightningAlarmRow[] =>
  [...alarms].sort(compareSmartLightningAlarms);
/** 选取标准父级目录关系：资产关系优先于实体分组关系。 */
export const getSmartLightningParentDirectory = (relations: EntityRelationInfo[]): string => {
  const contains = relations.filter(relation => relation.type === 'Contains');
  return contains.find(relation => relation.from.entityType === EntityType.ASSET)?.fromName ||
    contains.find(relation => relation.from.entityType === EntityType.ENTITY_GROUP)?.fromName || '--';
};

/** 从服务器属性列表中取产品名称，缺失时显示占位符。 */
export const getSmartLightningProductName = (attributes: AttributeData[]): string =>
  attributes.find((attribute) => attribute.key === 'productName')?.value || '--';
