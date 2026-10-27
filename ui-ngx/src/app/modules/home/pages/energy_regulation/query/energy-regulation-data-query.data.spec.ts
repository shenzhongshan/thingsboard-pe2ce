import assert from 'node:assert/strict';
import {buildEnergyQueryDeviceCsv, buildEnergyQueryDeviceRows, buildEnergyQueryDevices, filterEnergyQueryDevices} from './energy-regulation-data-query.data';

// 使用真实接口结构作为输入，回归资产范围、活动告警优先级和即时筛选，不生成页面演示设备。
const tree = {roots: [], flatNodes: [], nodes: new Map([
  ['root', {id: 'root', name: '公司', entityType: 'ASSET', childIds: ['a', 'b']}],
  ['a', {id: 'a', name: '电表A', entityType: 'DEVICE', childIds: []}],
  ['b', {id: 'b', name: '电表B', entityType: 'DEVICE', childIds: []}]
])} as any;
const contexts = [
  {deviceId: 'a', deviceName: '电表A', deviceProfileName: 'E1A2', productName: 'ESA智能电表', active: false, functionCategory: 'energy_meter'},
  {deviceId: 'b', deviceName: '电表B', deviceProfileName: 'E1A2', productName: 'ESA智能电表', active: false, functionCategory: 'energy_meter'},
  {deviceId: 'c', deviceName: '电表C', deviceProfileName: 'E1A2', productName: 'ESA智能电表', active: true, functionCategory: 'energy_meter'}
];
const devices = buildEnergyQueryDevices(tree, contexts, () => new Map(), new Map([['a', '严重']]));
assert.equal(devices[0].status, '报警');
assert.equal(devices[1].status, '离线');
assert.equal(devices[2].status, '正常');
assert.equal(devices[0].assetPath, '公司/电表A');
assert.equal(devices[0].functionCategory, '电能计量');
assert.equal(buildEnergyQueryDevices(tree).length, 0);
const filters = {status: '全部' as const, productName: '', functionCategory: '', keyword: '', assetIds: new Set(['a', 'b'])};
assert.equal(filterEnergyQueryDevices(devices, filters).length, 2);
assert.equal(filterEnergyQueryDevices(devices, {...filters, status: '报警'}).length, 1);
assert.equal(filterEnergyQueryDevices(devices, {...filters, keyword: 'E1A2'}).length, 2);
assert.equal(filterEnergyQueryDevices(devices, {...filters, productName: '其他产品'}).length, 0);
assert.equal(filterEnergyQueryDevices(devices, {...filters, functionCategory: '电能计量'}).length, 2);
assert.equal(filterEnergyQueryDevices(devices, {...filters, assetIds: new Set()}).length, 0);
const rows = buildEnergyQueryDeviceRows(devices);
assert.equal(rows[0].alarmSeverity, '严重');
assert.equal(rows[0].assetPath, '公司/电表A');
assert.equal(buildEnergyQueryDeviceCsv(devices).length, 4);
assert.equal(buildEnergyQueryDevices(tree, [contexts[1]], () => new Map([['active', {ts: 10, value: 'true'}]]))[0].status, '正常');
console.log('16项能源查询数据回归断言通过');
