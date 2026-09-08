import assert from 'node:assert/strict';
import {
  buildSmartLightningQueryRows,
  filterSmartLightningQueryRows,
  getSmartLightningQueryRootAssetIds,
  getSmartLightningQueryVisibleRoots
} from './smart-lightning-data-query.data';
import {buildSmartLightningMonitoringAssetTreeFromRoots} from '../monitoring/smart-lightning-monitoring.data';

const rows = buildSmartLightningQueryRows([
  {
    id: 'device-a',
    name: 'DEA2-001',
    label: 'KSDDEA2250616001',
    deviceProfileName: 'DESA',
    protocol: 'MQTT',
    active: false
  },
  {
    id: 'device-b',
    name: 'G1A3-001',
    label: '',
    deviceProfileName: 'G1A3',
    protocol: 'HTTP',
    active: false
  },
  {
    id: 'device-c',
    name: 'M1AI-001',
    label: '',
    deviceProfileName: 'M1AI',
    protocol: 'MQTT',
    active: false
  }
], new Set(['device-a', 'device-b', 'device-c']), new Map([
  ['device-a', '项目A/配电房/DEA2-001'],
  ['device-b', '项目A/配电房/G1A3-001'],
  ['device-c', '项目A/配电房/M1AI-001']
]), new Map([
  ['device-a', {active: true, productName: '智能电涌保护器', functionCategory: 'spd_monitor', lastActivityTime: 1788310800000}],
  ['device-b', {active: false, productName: '接地电阻监测仪', functionCategory: 'ground_resistance', lastActivityTime: 1788310000000}],
  ['device-c', {productName: '雷电流监测仪', functionCategory: 'lightning_current'}]
]), new Set(['device-a']), new Map([['device-a', '紧急']]));

assert.deepEqual(rows.map((row) => row.status), ['报警', '离线', '离线']);
assert.equal(rows[0].alarmSeverity, '紧急');
assert.equal(rows[0].deviceType, 'DESA');
assert.equal(rows[1].deviceType, 'G1A3');
assert.equal(rows[0].protocol, 'MQTT');
assert.equal(rows[0].productName, '智能电涌保护器');
assert.equal(rows[0].functionCategory, 'SPD监测');
assert.equal(rows[0].assetPath, '项目A/配电房/DEA2-001');

assert.deepEqual(
  filterSmartLightningQueryRows(rows, {status: '离线'}).map((row) => row.id),
  ['device-b']
);
assert.deepEqual(
  filterSmartLightningQueryRows(rows, {functionCategory: 'SPD监测', keyword: '智能'}).map((row) => row.id),
  ['device-a']
);
assert.deepEqual(
  filterSmartLightningQueryRows(rows, {productName: '接地电阻监测仪'}).map((row) => row.id),
  ['device-b']
);

const defaultProtocolRow = buildSmartLightningQueryRows([{
  id: 'device-default',
  name: 'DEFAULT-001',
  deviceProfileName: '默认设备配置',
  protocol: 'DEFAULT'
}], new Set(['device-default']), new Map(), new Map())[0];
assert.equal(defaultProtocolRow.protocol, '默认（支持基本 MQTT、HTTP 和 CoAP 传输）');

const rootTree = buildSmartLightningMonitoringAssetTreeFromRoots([
  {id: 'root-a', name: '项目A', entityType: 'ASSET' as never, children: [
    {id: 'device-a', name: '设备A', entityType: 'DEVICE' as never, children: []}
  ]},
  {id: 'root-b', name: '项目B', entityType: 'ASSET' as never, children: [
    {id: 'device-b', name: '设备B', entityType: 'DEVICE' as never, children: []}
  ]}
]);
assert.deepEqual(getSmartLightningQueryVisibleRoots(rootTree, 'root-a').map((node) => node.id), ['root-a']);
assert.deepEqual(getSmartLightningQueryRootAssetIds(rootTree, 'root-a'), ['root-a', 'device-a']);
assert.deepEqual(getSmartLightningQueryRootAssetIds(rootTree, ''), ['root-a', 'device-a', 'root-b', 'device-b']);
