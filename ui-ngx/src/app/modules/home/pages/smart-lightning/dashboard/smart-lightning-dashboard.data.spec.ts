import assert from 'node:assert/strict';
import { AlarmInfo, AlarmSeverity, AlarmStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityRelationInfo } from '@shared/models/relation.models';
import {
  buildSmartLightningMock,
  buildSmartLightningAxisLabels,
  aggregateSmartLightningActiveAlarms,
  createSmartLightningAlarmQuery,
  calculateSmartLightningDeviceSummary,
  defaultSmartLightningChartPeriods,
  formatSmartLightningAlarm,
  formatSmartLightningResistance,
  getSmartLightningParentDirectory,
  getSmartLightningProductName,
  getSmartLightningAlarmChartState,
  getSmartLightningAlarmChartMessage,
  shouldReloadSmartLightningAlarms,
  SMART_LIGHTNING_ALARM_DISPLAY_LIMIT,
  setSmartLightningChartPeriod,
  sortSmartLightningAlarms,
  SmartLightningPeriod
} from './smart-lightning-dashboard.data';

const day = buildSmartLightningMock(SmartLightningPeriod.day);
const week = buildSmartLightningMock(SmartLightningPeriod.week);
const month = buildSmartLightningMock(SmartLightningPeriod.month);

assert.equal(day.periodLabel, '今日');
assert.equal(week.periodLabel, '本周');
assert.equal(month.periodLabel, '本月');
assert.equal(day.axisLabels.length, 8);
assert.equal(week.axisLabels.length, 7);
assert.equal(month.axisLabels.length, 12);
assert.notDeepEqual(day.spdSeries[0].data, week.spdSeries[0].data);

const axisReferenceTime = new Date(2026, 8, 2, 15, 24, 0);
assert.deepEqual(buildSmartLightningAxisLabels(SmartLightningPeriod.day, axisReferenceTime), [
  '18:24', '21:24', '00:24', '03:24', '06:24', '09:24', '12:24', '15:24'
]);
assert.deepEqual(buildSmartLightningAxisLabels(SmartLightningPeriod.week, axisReferenceTime), [
  '08-27', '08-28', '08-29', '08-30', '08-31', '09-01', '09-02'
]);
assert.deepEqual(buildSmartLightningAxisLabels(SmartLightningPeriod.month, axisReferenceTime), [
  '08-03', '08-06', '08-08', '08-11', '08-14', '08-17',
  '08-19', '08-22', '08-25', '08-28', '08-30', '09-02'
]);

const independentPeriods = setSmartLightningChartPeriod(
  defaultSmartLightningChartPeriods,
  'ground',
  SmartLightningPeriod.week
);

assert.equal(independentPeriods.ground, SmartLightningPeriod.week);
assert.equal(independentPeriods.spd, SmartLightningPeriod.day);
assert.equal(independentPeriods.alarm, SmartLightningPeriod.day);

const latestAlarmQuery = createSmartLightningAlarmQuery();
assert.equal(
  latestAlarmQuery.toQuery(),
  '?pageSize=2147483647&page=0&sortProperty=createdTime&sortOrder=DESC&statusList=ACTIVE'
);

const formattedAlarm = formatSmartLightningAlarm({
  originatorName: '防雷终端 2',
  originatorLabel: '东区配电房',
  type: 'SPD 脱扣',
  severity: AlarmSeverity.CRITICAL,
  status: AlarmStatus.ACTIVE_UNACK,
  startTs: 1765550580000
} as AlarmInfo);

assert.deepEqual(formattedAlarm, {
  source: '东区配电房',
  secondaryLocation: '--',
  device: '防雷终端 2',
  deviceType: '--',
  deviceName: '--',
  type: 'SPD 脱扣',
  severity: '严重',
  severityTone: 'critical',
  status: '未确认',
  startTs: 1765550580000
});

const parentDirectory = getSmartLightningParentDirectory([
  {type: 'Contains', from: {entityType: EntityType.ENTITY_GROUP}, fromName: '第一项目组'} as EntityRelationInfo,
  {type: 'Contains', from: {entityType: EntityType.ASSET}, fromName: '东区配电房'} as EntityRelationInfo
]);
assert.equal(parentDirectory, '东区配电房');

const severityOrder = sortSmartLightningAlarms([
  {...formattedAlarm, severityTone: 'warning'},
  {...formattedAlarm, severityTone: 'minor'},
  {...formattedAlarm, severityTone: 'critical'},
  {...formattedAlarm, severityTone: 'major'},
  {...formattedAlarm, severityTone: 'indeterminate'}
]).map(alarm => alarm.severityTone);
assert.deepEqual(severityOrder, ['critical', 'major', 'minor', 'warning', 'indeterminate']);
assert.equal(SMART_LIGHTNING_ALARM_DISPLAY_LIMIT, 10);
assert.equal(getSmartLightningProductName([{key: 'productName', value: '智能防雷终端'}]), '智能防雷终端');
assert.equal(getSmartLightningProductName([]), '--');
assert.equal(formatSmartLightningResistance(0.35), '0.35 Ω');
assert.equal(formatSmartLightningResistance(12), '12 Ω');
assert.equal(formatSmartLightningResistance(null), '--');
assert.deepEqual(calculateSmartLightningDeviceSummary([true, 'true', false, 'false']), {
  total: 4,
  online: 2,
  offline: 2,
  onlineRate: 50
});

assert.deepEqual(aggregateSmartLightningActiveAlarms([
  {type: 'SPD_TEST_FAULT', status: AlarmStatus.ACTIVE_UNACK},
  {type: 'SPD_LOW_LIFE', status: AlarmStatus.ACTIVE_ACK},
  {type: 'SPD_TEST_FAULT', status: AlarmStatus.ACTIVE_ACK},
  {type: 'HISTORICAL_EVENT', status: AlarmStatus.CLEARED_ACK},
  {type: '', status: AlarmStatus.ACTIVE_UNACK}
] as AlarmInfo[]), [
  {type: 'SPD_TEST_FAULT', count: 2},
  {type: 'SPD_LOW_LIFE', count: 1},
  {type: '未分类', count: 1}
]);
assert.equal(getSmartLightningAlarmChartState(true, false, []), 'loading');
assert.equal(getSmartLightningAlarmChartState(false, true, []), 'error');
assert.equal(getSmartLightningAlarmChartState(false, false, []), 'empty');
assert.equal(getSmartLightningAlarmChartState(false, false, [{type: 'SPD脱口', count: 7}]), 'ready');
assert.equal(getSmartLightningAlarmChartMessage(true, false, []), undefined);
assert.equal(getSmartLightningAlarmChartMessage(false, true, []), '告警统计加载失败');
assert.equal(getSmartLightningAlarmChartMessage(false, false, []), '当前没有未清除告警');
assert.equal(shouldReloadSmartLightningAlarms(null, 3), false);
assert.equal(shouldReloadSmartLightningAlarms(3, 3), false);
assert.equal(shouldReloadSmartLightningAlarms(3, 4), true);
