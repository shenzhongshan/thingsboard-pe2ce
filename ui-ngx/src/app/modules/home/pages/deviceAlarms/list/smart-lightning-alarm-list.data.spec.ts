/** 文件职责：告警列表模块的 smart-lightning-alarm-list.data.spec，验证数据模型、映射与纯函数行为。 */
/** 告警列表数据层单测：行排序、筛选与仪表地址取值优先级（node assert 轻量运行）。 */
import assert from 'node:assert/strict';
import {
  filterSmartLightningAlarmListRows,
  mapSmartLightningAlarmListRow,
  sortSmartLightningAlarmListRows,
  SmartLightningAlarmListRow
} from './smart-lightning-alarm-list.data';

const row = (overrides: Partial<SmartLightningAlarmListRow>): SmartLightningAlarmListRow => ({
  id: 'alarm-id',
  originatorId: 'device-id',
  projectDeviceName: '设备',
  alarmTime: 100,
  alarmStatus: '未确认未清除',
  workOrderStatus: '未派发',
  alarmType: '过流',
  severity: '重要',
  severityTone: 'major',
  deviceType: 'DESA',
  meterAddress: '--',
  timeSetting: '全时段',
  detailDescription: '--',
  status: 'ACTIVE_UNACK',
  cleared: false,
  ...overrides
});

const sorted = sortSmartLightningAlarmListRows([
  row({id: 'cleared-ack-critical', alarmStatus: '已确认已清除', severity: '严重', severityTone: 'critical', alarmTime: 1100}),
  row({id: 'cleared-unack-critical', alarmStatus: '未确认已清除', severity: '严重', severityTone: 'critical', alarmTime: 1000}),
  row({id: 'active-ack-critical', alarmStatus: '已确认未清除', severity: '严重', severityTone: 'critical', alarmTime: 900}),
  row({id: 'active-unack-minor', severity: '次要', severityTone: 'minor', alarmTime: 800}),
  row({id: 'active-unack-critical-assigned', workOrderStatus: '已派发', severity: '严重', severityTone: 'critical', alarmTime: 950}),
  row({id: 'active-unack-critical', severity: '严重', severityTone: 'critical', alarmTime: 700})
]);
assert.deepEqual(sorted.map((item) => item.id), [
  'active-unack-critical', 'active-unack-critical-assigned', 'active-unack-minor',
  'active-ack-critical', 'cleared-unack-critical', 'cleared-ack-critical'
]);

const filtered = filterSmartLightningAlarmListRows([
  row({id: 'critical', severity: '严重', severityTone: 'critical'}),
  row({id: 'normal', severity: '警告', severityTone: 'warning'}),
  row({id: 'other', alarmType: '漏电'})
], {severityTab: '严重', alarmType: '', severity: '', workOrderStatus: '', keyword: '', startTime: '', endTime: ''});
assert.deepEqual(filtered.map((item) => item.id), ['critical']);

// 仪表地址列优先显示资产树绝对地址，无路径时回退服务器属性与告警详情。
const baseAlarm = {
  id: {id: 'alarm-1'},
  originator: {id: 'device-1'},
  originatorDisplayName: 'DESA-001',
  startTs: 100,
  type: '过流',
  severity: 'MAJOR',
  status: 'ACTIVE_UNACK',
  cleared: false,
  assignTs: 0
};
assert.equal(
  mapSmartLightningAlarmListRow(baseAlarm, {deviceType: 'DESA'}, '项目A/配电房/DESA-001').meterAddress,
  '项目A/配电房/DESA-001'
);
assert.equal(
  mapSmartLightningAlarmListRow(baseAlarm, {deviceType: 'DESA', meterAddress: 'WH-001'}).meterAddress,
  'WH-001'
);
assert.equal(
  mapSmartLightningAlarmListRow(
    {...baseAlarm, details: {meterAddress: 'DETAIL-9'}},
    {deviceType: 'DESA'}
  ).meterAddress,
  'DETAIL-9'
);
