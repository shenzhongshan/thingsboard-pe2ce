import assert from 'node:assert/strict';
import { AssetInfo } from '@shared/models/asset.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityRelation } from '@shared/models/relation.models';
import {
  buildSmartLightningMonitoringAssetTree,
  buildSmartLightningMonitoringAssetTreeFromRoots,
  buildSmartLightningMonitoringDeviceCards,
  calculateSmartLightningMonitoringAlarmSummary,
  getSmartLightningFunctionCategoryLabel,
  getSmartLightningMonitoringPage,
  getSmartLightningMonitoringSelectedAssetIds,
  SMART_LIGHTNING_ANALYSIS_CARDS
} from './smart-lightning-monitoring.data';

assert.deepEqual(SMART_LIGHTNING_ANALYSIS_CARDS.map((card) => card.title), [
  '浪涌防护系统能力评估',
  '接地电阻雷电流AI分析',
  '雷电防护电流分析',
  '雷电/过电压能力监测评估'
]);
assert.equal(getSmartLightningFunctionCategoryLabel('arc_fault'), '故障电弧监测');
assert.deepEqual(SMART_LIGHTNING_ANALYSIS_CARDS.map((card) => card.id), [
  'spd', 'ground', 'current', 'voltage'
]);
assert.deepEqual(SMART_LIGHTNING_ANALYSIS_CARDS[0].metrics, [
  {label: '监测数量', value: '8'},
  {label: '动作次数', value: '—'},
  {label: '劣化值', value: '—'},
  {label: '动作次数', value: '1'}
]);
assert.equal(SMART_LIGHTNING_ANALYSIS_CARDS[1].primaryMetric, '0.35Ω');
assert.equal(SMART_LIGHTNING_ANALYSIS_CARDS[2].primaryMetric, '32.8kA');
assert.equal(SMART_LIGHTNING_ANALYSIS_CARDS[3].primaryMetric, '2.5kV');

const assets = [
  {id: {id: 'project', entityType: EntityType.ASSET}, name: '智慧园区', type: '项目'},
  {id: {id: 'building', entityType: EntityType.ASSET}, name: '1号楼', type: '建筑'},
  {id: {id: 'cabinet', entityType: EntityType.ASSET}, name: '配电柜 A', type: '配电柜'},
  {id: {id: 'standalone', entityType: EntityType.ASSET}, name: '独立机房', type: '机房'}
] as AssetInfo[];

const relations = [
  {from: assets[0].id, to: assets[1].id, type: 'Contains'},
  {from: assets[1].id, to: assets[2].id, type: 'Contains'},
  {from: assets[2].id, to: {id: 'device-a', entityType: EntityType.DEVICE}, type: 'Contains'}
] as EntityRelation[];

const devices = [
  {id: {id: 'device-a', entityType: EntityType.DEVICE}, name: '防雷终端 A-01', type: '防雷终端'}
] as DeviceInfo[];

const tree = buildSmartLightningMonitoringAssetTree(assets, devices, relations);
assert.deepEqual(tree.roots.map((node) => node.id), ['standalone', 'project']);
assert.deepEqual(tree.nodes.get('project')?.childIds, ['building']);
assert.deepEqual(tree.nodes.get('building')?.childIds, ['cabinet']);
assert.deepEqual(tree.nodes.get('cabinet')?.childIds, ['device-a']);
assert.equal(tree.nodes.get('device-a')?.entityType, EntityType.DEVICE);
assert.deepEqual(getSmartLightningMonitoringSelectedAssetIds(tree.nodes, 'project'), ['project', 'building', 'cabinet', 'device-a']);
assert.deepEqual(getSmartLightningMonitoringSelectedAssetIds(tree.nodes, 'standalone'), ['standalone']);

const cycleSafeTree = buildSmartLightningMonitoringAssetTree(assets, devices, [
  ...relations,
  {from: assets[2].id, to: assets[0].id, type: 'Contains'} as EntityRelation
]);
assert.deepEqual(cycleSafeTree.roots.map((node) => node.id), ['standalone', 'project']);

const apiTree = buildSmartLightningMonitoringAssetTreeFromRoots([
  {
    id: 'project', name: '智慧园区', entityType: EntityType.ASSET, children: [
      {
        id: 'device-a', name: '防雷终端 A-01', entityType: EntityType.DEVICE, children: []
      }
    ]
  }
]);
assert.deepEqual(apiTree.roots.map((node) => node.id), ['project']);
assert.deepEqual(apiTree.nodes.get('project')?.childIds, ['device-a']);
assert.deepEqual(getSmartLightningMonitoringSelectedAssetIds(apiTree.nodes, 'project'), ['project', 'device-a']);

const deviceCards = buildSmartLightningMonitoringDeviceCards(
  [
    {
      id: {id: 'device-a', entityType: EntityType.DEVICE},
      name: '防雷终端 A-01',
      type: '防雷终端',
      deviceProfileName: 'DESA',
      active: false
    },
    {
      id: {id: 'device-b', entityType: EntityType.DEVICE},
      name: '接地监测柜 B-03',
      type: '接地监测柜',
      deviceProfileName: '接地监测配置',
      active: false
    }
  ] as DeviceInfo[],
  new Set(['device-a']),
  new Map([
    ['device-a', {
      active: true,
      functionCategory: 'spd_monitor',
      productName: 'FS防雷监测模块(全要素)',
      lastActivityTime: new Date(2026, 8, 2, 16, 18, 0).getTime()
    }],
    ['device-b', {active: false, functionCategory: 'ground_resistance'}]
  ]),
  new Set(['device-a'])
);
assert.deepEqual(deviceCards, [{
  id: 'device-a',
  name: '防雷终端 A-01',
  category: 'SPD监测',
  status: '报警',
  icon: 'electric_meter',
  productName: 'FS防雷监测模块(全要素)',
  productType: 'DESA',
  lastActiveTime: '2026-09-02 16:18:00'
}]);

const startOfToday = new Date(2026, 8, 2).getTime();
const alarmSummary = calculateSmartLightningMonitoringAlarmSummary([
  {
    originator: {id: 'device-a', entityType: EntityType.DEVICE},
    status: 'ACTIVE_UNACK',
    cleared: false,
    startTs: startOfToday + 1_000,
    assignTs: 0,
    clearTs: 0
  },
  {
    originator: {id: 'device-a', entityType: EntityType.DEVICE},
    status: 'ACTIVE_ACK',
    cleared: false,
    startTs: startOfToday + 2_000,
    assignTs: startOfToday + 3_000,
    clearTs: 0
  },
  {
    originator: {id: 'device-b', entityType: EntityType.DEVICE},
    status: 'CLEARED_ACK',
    cleared: true,
    startTs: startOfToday - 86_400_000,
    assignTs: startOfToday - 1_000,
    clearTs: startOfToday + 4_000
  },
  {
    originator: {id: 'tree-external', entityType: EntityType.DEVICE},
    status: 'ACTIVE_UNACK',
    cleared: false,
    startTs: startOfToday + 1_000,
    assignTs: 0,
    clearTs: 0
  }
], new Set(['device-a', 'device-b']), startOfToday);
assert.deepEqual(alarmSummary, {
  active: 2,
  todayTotal: 2,
  unhandled: 1,
  assigned: 1,
  handled: 1
});

assert.deepEqual(getSmartLightningMonitoringPage(['a', 'b', 'c'], 2, 2), {
  items: ['c'],
  currentPage: 2,
  totalPages: 2
});
assert.deepEqual(getSmartLightningMonitoringPage(['a', 'b', 'c'], 9, 2), {
  items: ['c'],
  currentPage: 2,
  totalPages: 2
});
