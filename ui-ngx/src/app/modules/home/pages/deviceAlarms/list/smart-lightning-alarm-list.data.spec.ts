import assert from 'node:assert/strict';
import {
  filterSmartLightningAlarmListRows,
  sortSmartLightningAlarmListRows,
  SmartLightningAlarmListRow
} from './smart-lightning-alarm-list.data';

const row = (overrides: Partial<SmartLightningAlarmListRow>): SmartLightningAlarmListRow => ({
  id: 'alarm-id',
  originatorId: 'device-id',
  projectDeviceName: '设备',
  alarmTime: 100,
  workOrderStatus: '未派发',
  alarmType: '过流',
  severity: '一般',
  severityTone: 'major',
  gatewayIdentifier: '--',
  deviceType: 'DESA',
  meterAddress: '--',
  circuitName: '--',
  timeSetting: '全时段',
  detailDescription: '--',
  ...overrides
});

const sorted = sortSmartLightningAlarmListRows([
  row({id: 'done-critical', workOrderStatus: '已处理', severity: '严重', severityTone: 'critical', alarmTime: 900}),
  row({id: 'active-minor', severity: '次要', severityTone: 'minor', alarmTime: 800}),
  row({id: 'active-critical', severity: '严重', severityTone: 'critical', alarmTime: 700}),
  row({id: 'active-critical-new', severity: '严重', severityTone: 'critical', alarmTime: 900})
]);
assert.deepEqual(sorted.map((item) => item.id), [
  'active-critical-new', 'active-critical', 'active-minor', 'done-critical'
]);

const filtered = filterSmartLightningAlarmListRows([
  row({id: 'critical', severity: '严重', severityTone: 'critical'}),
  row({id: 'normal', severity: '警告', severityTone: 'warning'}),
  row({id: 'other', alarmType: '漏电'})
], {severityTab: '紧急', alarmType: '', severity: '', workOrderStatus: '', keyword: '', startTime: '', endTime: ''});
assert.deepEqual(filtered.map((item) => item.id), ['critical']);
