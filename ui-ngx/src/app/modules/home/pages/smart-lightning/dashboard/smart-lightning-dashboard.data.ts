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

export const SMART_LIGHTNING_ALARM_DISPLAY_LIMIT = 10;

export const defaultSmartLightningChartPeriods: SmartLightningChartPeriods = {
  ground: SmartLightningPeriod.day,
  spd: SmartLightningPeriod.day,
  alarm: SmartLightningPeriod.day,
  orders: SmartLightningPeriod.day,
  lightning: SmartLightningPeriod.day
};

/** Returns a new state so selecting one chart's period never alters any other chart. */
export const setSmartLightningChartPeriod = (
  periods: SmartLightningChartPeriods,
  chart: SmartLightningChartId,
  period: SmartLightningPeriod
): SmartLightningChartPeriods => ({...periods, [chart]: period});

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

export interface SmartLightningDeviceSummary {
  total: number;
  online: number;
  offline: number;
  onlineRate: number;
}

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

export const getSmartLightningActiveValue = (attributes: AttributeData[]): unknown =>
  attributes.find((attribute) => attribute.key === 'active')?.value;

const latestActiveAlarmFilter: AlarmFilter = {
  statusList: [AlarmSearchStatus.ACTIVE]
};

/** Builds the existing ThingsBoard v2 alarm query used by the dashboard record panel. */
export const createSmartLightningAlarmQuery = (): AlarmQueryV2 => new AlarmQueryV2(
  null,
  new TimePageLink(MAX_SAFE_PAGE_SIZE, 0, null, {property: 'createdTime', direction: Direction.DESC}),
  latestActiveAlarmFilter
);

/**
 * The realtime alarm-count subscription emits an initial snapshot. That snapshot
 * must not trigger a second REST request; only a later count change should
 * refresh the dashboard list and its derived statistics.
 */
export const shouldReloadSmartLightningAlarms = (
  previousCount: number | null,
  nextCount: number
): boolean => previousCount !== null && previousCount !== nextCount;

export interface SmartLightningAlarmTypeTotal {
  type: string;
  count: number;
}

export type SmartLightningAlarmChartState = 'loading' | 'error' | 'empty' | 'ready';

/** Keeps an unavailable response distinct from a verified empty alarm set. */
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

/** Loading is intentionally silent so the chart panel does not show a transient empty-state message. */
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

/** Counts only currently un-cleared alarms, grouped by their ThingsBoard alarm type. */
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

/** Maps only fields guaranteed by AlarmInfo; no device metadata is fabricated when the API omits it. */
export interface SmartLightningAlarmContext {
  parentDirectory?: string;
  deviceProfileName?: string;
  productName?: string;
}

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

export const compareSmartLightningAlarms = (left: SmartLightningAlarmRow, right: SmartLightningAlarmRow): number =>
  severityRank[left.severityTone] - severityRank[right.severityTone] || right.startTs - left.startTs;

/** Keeps the monitoring wall focused on the highest-severity active alarms. */
export const sortSmartLightningAlarms = (alarms: SmartLightningAlarmRow[]): SmartLightningAlarmRow[] =>
  [...alarms].sort(compareSmartLightningAlarms);

/** Chooses the standard parent directory relation: assets take precedence over entity groups. */
export const getSmartLightningParentDirectory = (relations: EntityRelationInfo[]): string => {
  const contains = relations.filter(relation => relation.type === 'Contains');
  return contains.find(relation => relation.from.entityType === EntityType.ASSET)?.fromName ||
    contains.find(relation => relation.from.entityType === EntityType.ENTITY_GROUP)?.fromName || '--';
};

export const getSmartLightningProductName = (attributes: AttributeData[]): string =>
  attributes.find((attribute) => attribute.key === 'productName')?.value || '--';

export interface SmartLightningSeries {
  name: string;
  color: string;
  data: number[];
}

export interface SmartLightningAlarm {
  area: string;
  location: string;
  model: string;
  device: string;
  deviceCode: string;
  status: '一般' | '紧急' | '正常';
  event: string;
  occurredAt: string;
}

export interface SmartLightningMock {
  periodLabel: string;
  axisLabels: string[];
  groundSeries: SmartLightningSeries[];
  spdSeries: SmartLightningSeries[];
  alarmBars: number[];
  workOrderSeries: SmartLightningSeries[];
  lightningMetrics: Array<{name: string; value: number}>;
  alarms: SmartLightningAlarm[];
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
 * Builds each chart x-axis relative to the instant it is rendered:
 * day = eight three-hour buckets in the latest 24 hours; week = latest seven days;
 * month = twelve evenly distributed readings across the latest 30 days.
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

const byPeriod = <T>(values: Record<SmartLightningPeriod, T>, period: SmartLightningPeriod): T => values[period];

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
  alarmBars: byPeriod({
    day: [9, 7, 10, 6, 8],
    week: [36, 28, 41, 25, 33],
    month: [114, 90, 127, 82, 108]
  }, period),
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
  }, period),
  alarms: [
    {area: '防雷监测器', location: '4AL 配电箱', model: 'HST/ZJC-R', device: '全要素', deviceCode: '3车道', status: '一般', event: '接地异常', occurredAt: '2025/12/12 17:23'},
    {area: '防雷监测器', location: '4AL 配电箱', model: 'HST/ZJC-R', device: '九要素', deviceCode: '1车道', status: '紧急', event: 'SPD 脱扣', occurredAt: '2025/12/12 17:23'},
    {area: '防雷监测器', location: '4AL 配电箱', model: 'HST/ZJC-R', device: '防雷终端 2', deviceCode: 'A 区', status: '正常', event: '状态恢复', occurredAt: '2025/12/12 16:58'},
    {area: '防雷监测器', location: '东区配电房', model: 'HST/ZJC-R', device: '防雷终端 3', deviceCode: 'B 区', status: '一般', event: '寿命预警', occurredAt: '2025/12/12 16:40'}
  ]
});
