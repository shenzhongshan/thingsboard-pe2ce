/** 文件职责：看板模块的 smart-lightning-dashboard.data，定义数据模型、接口结果映射与纯函数。 */
import { AlarmInfo, AlarmSearchStatus, AlarmSeverity, AlarmStatus, AlarmQueryV2 } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MAX_SAFE_PAGE_SIZE, TimePageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { AlarmFilter } from '@shared/models/query/query.models';
import { EntityRelationInfo } from '@shared/models/relation.models';
import { AttributeData } from '@shared/models/telemetry/telemetry.models';

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

/** 看板告警记录表的行结构（由 AlarmInfo 映射而来）。 */
export interface SmartLightningAlarmRow {
  source: string;
  secondaryLocation: string;
  device: string;
  deviceType: string;
  deviceName: string;
  type: string;
  severity: string;
  severityTone: 'critical' | 'major' | 'minor' | 'warning' | 'indeterminate';
  status: string;
  startTs: number;
}

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

/** 格式化最新地网阻值聚合数据，缺失数据时显示占位符而不是误显示为 0。 */
export const formatSmartLightningResistance = (value: number | null | undefined): string => {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return '--';
  }
  const roundedValue = Math.round(value * 100) / 100;
  return `${roundedValue} Ω`;
};

/** 从服务器属性列表中取 active 属性原始值，供在线判断使用。 */
export const getSmartLightningActiveValue = (attributes: AttributeData[]): unknown =>
  attributes.find((attribute) => attribute.key === 'active')?.value;

const latestActiveAlarmFilter: AlarmFilter = {
  statusList: [AlarmSearchStatus.ACTIVE]
};

/** 构建看板告警面板使用的 ThingsBoard v2 原生告警查询。 */
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

/** 演示曲线系列数据（按周期组织）。 */
export interface SmartLightningSeries {
  name: string;
  color: string;
  data: number[];
}

/** 看板各图表演示数据的聚合容器。 */
export interface SmartLightningMock {
  periodLabel: string;
  axisLabels: string[];
  groundSeries: SmartLightningSeries[];
  spdSeries: SmartLightningSeries[];
  workOrderSeries: SmartLightningSeries[];
  lightningMetrics: Array<{name: string; value: number}>;
}

const periodLabels: Record<SmartLightningPeriod, string> = {
  [SmartLightningPeriod.day]: '今日',
  [SmartLightningPeriod.week]: '本周',
  [SmartLightningPeriod.month]: '本月'
};

const formatAxisDate = (date: Date): string => `${String(date.getMonth() + 1).padStart(2, '0')}-${
  String(date.getDate()).padStart(2, '0')}`;

const formatAxisTime = (date: Date): string => `${String(date.getHours()).padStart(2, '0')}:${
  String(date.getMinutes()).padStart(2, '0')}`;

const offsetAxisDate = (referenceTime: Date, days: number = 0, hours: number = 0): Date => {
  const date = new Date(referenceTime);
  date.setDate(date.getDate() - days);
  date.setHours(date.getHours() - hours);
  return date;
};

/**
 * 按图表渲染时刻生成横轴时间标签：
 * 日 = 最近 24 小时按 3 小时分 8 段；周 = 最近 7 天；
 * 月 = 最近 30 天均匀取 12 个读数点。
 */
export const buildSmartLightningAxisLabels = (
  period: SmartLightningPeriod,
  referenceTime: Date = new Date()
): string[] => {
  switch (period) {
    case SmartLightningPeriod.day:
      return Array.from({length: 8}, (_, index) => formatAxisTime(offsetAxisDate(referenceTime, 0, 21 - index * 3)));
    case SmartLightningPeriod.week:
      return Array.from({length: 7}, (_, index) => formatAxisDate(offsetAxisDate(referenceTime, 6 - index)));
    case SmartLightningPeriod.month:
      return Array.from({length: 12}, (_, index) => formatAxisDate(offsetAxisDate(
        referenceTime,
        Math.round(30 - (30 * index / 11))
      )));
  }
};

/** 按周期从记录对象中取对应值的小工具函数。 */
const byPeriod = <T>(values: Record<SmartLightningPeriod, T>, period: SmartLightningPeriod): T => values[period];

/** 构建看板图表演示数据：曲线点数随周期变化，供遥测接入前的页面展示。 */
export const buildSmartLightningMock = (period: SmartLightningPeriod, referenceTime: Date = new Date()): SmartLightningMock => ({
  periodLabel: periodLabels[period],
  axisLabels: buildSmartLightningAxisLabels(period, referenceTime),
  groundSeries: [
    {
      name: '接地电阻', color: '#14b8a6', data: byPeriod({
        day: [9, 13, 11, 17, 15, 20, 18, 16],
        week: [14, 16, 13, 18, 17, 19, 15],
        month: [15, 13, 16, 17, 19, 16, 20, 18, 15, 17, 14, 16]
      }, period)
    },
    {
      name: '土壤湿度', color: '#8b5cf6', data: byPeriod({
        day: [18, 15, 17, 23, 22, 26, 21, 19],
        week: [19, 22, 20, 24, 27, 23, 21],
        month: [18, 20, 22, 23, 21, 25, 27, 24, 22, 20, 19, 21]
      }, period)
    },
    {
      name: '地网电流', color: '#f59e0b', data: byPeriod({
        day: [21, 26, 23, 30, 28, 34, 31, 29],
        week: [25, 29, 27, 32, 30, 35, 28],
        month: [24, 26, 30, 28, 34, 32, 37, 33, 29, 31, 27, 30]
      }, period)
    }
  ],
  spdSeries: [
    {
      name: '泄漏电流', color: '#16a34a', data: byPeriod({
        day: [27, 25, 31, 36, 33, 39, 30, 26],
        week: [31, 35, 30, 38, 34, 40, 29],
        month: [28, 32, 35, 31, 38, 36, 42, 39, 33, 30, 28, 34]
      }, period)
    },
    {
      name: '剩余电流', color: '#f0b90b', data: byPeriod({
        day: [15, 20, 18, 26, 31, 28, 19, 23],
        week: [18, 23, 21, 29, 34, 26, 22],
        month: [16, 19, 24, 22, 30, 33, 29, 25, 20, 18, 21, 24]
      }, period)
    },
    {
      name: '线电压', color: '#8b5cf6', data: byPeriod({
        day: [11, 8, 13, 18, 21, 18, 14, 17],
        week: [12, 10, 15, 20, 23, 19, 16],
        month: [10, 12, 11, 16, 21, 24, 20, 17, 14, 15, 12, 16]
      }, period)
    },
    {
      name: '接地电流', color: '#0891b2', data: byPeriod({
        day: [8, 15, 14, 20, 24, 17, 6, 13],
        week: [9, 16, 13, 22, 25, 18, 11],
        month: [8, 12, 17, 15, 23, 27, 20, 11, 8, 14, 16, 13]
      }, period)
    }
  ],
  workOrderSeries: [
    {
      name: '未处理', color: '#f4b722', data: byPeriod({
        day: [1, 2, 1, 3, 2, 1, 2, 1],
        week: [4, 5, 3, 6, 4, 3, 2],
        month: [9, 8, 11, 7, 13, 12, 8, 10, 6, 9, 7, 8]
      }, period)
    },
    {
      name: '未派发', color: '#8b5cf6', data: byPeriod({
        day: [2, 1, 1, 2, 1, 1, 0, 1],
        week: [3, 2, 2, 4, 2, 1, 2],
        month: [5, 4, 6, 5, 7, 4, 3, 5, 4, 6, 3, 4]
      }, period)
    },
    {
      name: '已处理', color: '#2563eb', data: byPeriod({
        day: [3, 2, 4, 3, 5, 4, 6, 5],
        week: [11, 9, 13, 15, 12, 10, 14],
        month: [24, 22, 28, 31, 26, 33, 30, 27, 32, 29, 35, 34]
      }, period)
    }
  ],
  lightningMetrics: byPeriod({
    day: [
      {name: '峰值电流', value: 24}, {name: '电流波形', value: 68},
      {name: '放电能量', value: 38}, {name: '电荷转移量', value: 31}
    ],
    week: [
      {name: '峰值电流', value: 46}, {name: '电流波形', value: 82},
      {name: '放电能量', value: 61}, {name: '电荷转移量', value: 49}
    ],
    month: [
      {name: '峰值电流', value: 72}, {name: '电流波形', value: 94},
      {name: '放电能量', value: 76}, {name: '电荷转移量', value: 67}
    ]
  }, period)
});
