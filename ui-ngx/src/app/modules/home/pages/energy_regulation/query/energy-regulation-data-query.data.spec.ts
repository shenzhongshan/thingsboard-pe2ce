import assert from 'node:assert/strict';
import {
  buildEnergyQueryDeviceCsv,
  buildEnergyQueryDeviceRows,
  buildEnergyQueryDevices,
  filterEnergyQueryDevices,
  getEnergyQueryDeviceTypes,
  getEnergyQueryFunctionCategories,
  ENERGY_QUERY_DEVICE_TEMPLATES,
  EnergyQueryDevice
} from './energy-regulation-data-query.data';
import {EntityType} from '@shared/models/entity-type.models';

// 构造一棵两层资产树：根资产 → 两个叶子资产。
const tree = {
  roots: [{id: 'root-1', name: '园区A', entityType: EntityType.ASSET, childIds: ['floor-1', 'floor-2']}],
  nodes: new Map([
    ['root-1', {id: 'root-1', name: '园区A', entityType: EntityType.ASSET, childIds: ['floor-1', 'floor-2']}],
    ['floor-1', {id: 'floor-1', name: '1号楼', entityType: EntityType.ASSET, childIds: []}],
    ['floor-2', {id: 'floor-2', name: '2号楼', entityType: EntityType.ASSET, childIds: []}]
  ]),
  flatNodes: [
    {id: 'root-1', name: '园区A', parentId: null, depth: 0, entityType: EntityType.ASSET},
    {id: 'floor-1', name: '1号楼', parentId: 'root-1', depth: 1, entityType: EntityType.ASSET},
    {id: 'floor-2', name: '2号楼', parentId: 'root-1', depth: 1, entityType: EntityType.ASSET}
  ]
} as any;

const devices = buildEnergyQueryDevices(tree);

// 演示设备必须按模板生成，并按叶子资产轮换挂载。
assert.equal(devices.length, ENERGY_QUERY_DEVICE_TEMPLATES.length);
assert.equal(devices[0].assetId, 'floor-1');
assert.equal(devices[1].assetId, 'floor-2');
assert.equal(devices[2].assetId, 'floor-1');

// 设备模式行字段与设备一一对应。
const rows = buildEnergyQueryDeviceRows(devices);
assert.equal(rows.length, devices.length);
assert.equal(rows[0].serialNo, 'KSDSFA7260901001');
assert.equal(rows[0].protocol, 'MQTT');
assert.equal(rows[0].status, '在线');

const all = (overrides: Partial<Parameters<typeof filterEnergyQueryDevices>[1]> = {}) => ({
  status: '全部', deviceType: '', functionCategory: '', keyword: '',
  startTime: '', endTime: '', assetIds: new Set<string>(), ...overrides
} as const);

// 状态三态筛选。
assert.equal(filterEnergyQueryDevices(devices, all({status: '在线'})).length, 4);
assert.equal(filterEnergyQueryDevices(devices, all({status: '离线'})).length, 2);
assert.equal(filterEnergyQueryDevices(devices, all({status: '未连接'})).length, 1);

// 资产范围筛选（floor-1 挂了 4 台）。
assert.equal(filterEnergyQueryDevices(devices, all({assetIds: new Set(['floor-1'])})).length, 4);

// 设备类型与功能分类下拉各自去重排序。
assert.deepEqual(getEnergyQueryDeviceTypes(devices), ['E1A2', 'E1E2', 'SFE']);
assert.deepEqual(getEnergyQueryFunctionCategories(devices), ['电能质量', '电能计量']);

// 关键字命中设备编号。
const bySerial = filterEnergyQueryDevices(devices, all({keyword: 'KSDSFA7260901001'}));
assert.equal(bySerial.length, 1);
assert.equal(bySerial[0].name, 'SFA电能质量监测器1');

// 未连接设备在设定了时段时被过滤掉。
const withRange = filterEnergyQueryDevices(devices, all({startTime: '2026-10-09', endTime: '2026-10-10'}));
assert.ok(withRange.every((device: EnergyQueryDevice) => device.status !== '未连接'));

// CSV 导出带表头与全部行，序号从 1 递增。
const csvLines = buildEnergyQueryDeviceCsv(devices);
assert.equal(csvLines.length, devices.length + 1);
assert.equal(csvLines[1][0], '1');
assert.equal(csvLines[csvLines.length - 1][0], String(devices.length));
