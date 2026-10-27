import assert from 'node:assert/strict';
import type {AlarmInfo} from '@shared/models/alarm.models';
import {buildEnergyRegulationAlarmRecords} from './energy-regulation-dashboard.data';

// 共享组件的设备、位置、等级与状态字段来自能源接口，不借用其他模块的数据。
const alarm = {
  id: {id: 'alarm-1'}, originator: {id: 'meter-1'}, originatorName: '原始设备名',
  type: 'A相电压过高', severity: 'MAJOR', status: 'ACTIVE_UNACK', createdTime: 100, startTs: 80
} as AlarmInfo;
const paths = new Map([['meter-1', '公司/配电室/测试电表']]);
const rows = buildEnergyRegulationAlarmRecords([alarm], [{deviceId: 'meter-1', deviceName: '测试电表',
  deviceProfileName: 'E1A2', productName: 'ESA智能电表'}], paths);
assert.equal(rows[0].deviceName, '测试电表');
assert.equal(rows[0].deviceType, 'E1A2');
assert.equal(rows[0].source, '公司/配电室');
assert.equal(rows[0].secondaryLocation, '配电室');
assert.equal(rows[0].type, 'A相电压过高');
assert.equal(rows[0].severity, '重要');
assert.equal(rows[0].severityTone, 'major');
assert.equal(rows[0].status, '未确认未清除');
assert.equal(rows[0].startTs, 80);
assert.equal(rows[0].device, 'alarm-1');

// 不修改输入顺序；保留原有最近十条口径，并能展示已清除状态。
const many = Array.from({length: 12}, (_, i) => ({...alarm, id: {id: `alarm-${i}`}, createdTime: i} as AlarmInfo));
const latest = buildEnergyRegulationAlarmRecords(many, [], new Map());
assert.equal(latest.length, 10);
assert.equal(latest[0].device, 'alarm-11');
assert.equal(many[0].createdTime, 0);
const cleared = buildEnergyRegulationAlarmRecords([{...alarm, status: 'CLEARED_ACK'} as AlarmInfo], [], new Map());
assert.equal(cleared[0].status, '已确认已清除');
assert.equal(cleared[0].deviceName, '原始设备名');
assert.deepEqual(buildEnergyRegulationAlarmRecords([], [], new Map()), []);
