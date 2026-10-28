/** 文件职责：集中定义用能监测页面的树、指标、设备样例及筛选计算。 */
import type {EnergyRegulationAssetTreeNode} from '../energy-regulation-api.service';
import {
  getSmartLightningMonitoringSelectedAssetIds,
  SmartLightningMonitoringAssetNode,
  SmartLightningMonitoringAssetTree
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';
import {EntityType} from '@shared/models/entity-type.models';

/** 监测页面独立维护周期状态，切换一个区域不影响其他区域。 */
export type EnergyMonitoringPeriod = 'day' | 'week' | 'month';
export type EnergyMonitoringCategory = 'all' | 'meter' | 'power' | 'analysis' | 'carbon';

/** 设备总览使用的演示设备字段，仅用于前端展示和筛选。 */
export interface EnergyMonitoringDevice {
  id: string;
  name: string;
  category: Exclude<EnergyMonitoringCategory, 'all'>;
  categoryLabel: string;
  productName: string;
  assetId: string;
  status: '正常' | '告警' | '离线';
  totalEnergyKwh: number;
  activePowerKw: number;
  voltage: number;
  current: number;
  powerFactor: number;
  updatedAt: string;
}

/** 单条平铺资产树节点包含父级和层级，供共享防雷资产树组件渲染。 */
export interface EnergyMonitoringTreeNode {
  id: string;
  name: string;
  parentId: string | null;
  depth: number;
  entityType: EntityType.ASSET | EntityType.DEVICE;
}

/** 按能源页面所需单位维护分时用电曲线。 */
export interface EnergyMonitoringTrend {
  labels: string[];
  values: number[];
}

/** 每周期的总用电、峰值负荷和平均负载样例。 */
export interface EnergyMonitoringKpis {
  totalEnergyKwh: number;
  peakPowerKw: number;
  averageLoadPercent: number;
}

/** 左侧资产选择、汇总卡和设备列表共同使用的能源资产树。 */
export interface EnergyMonitoringAssetTree extends SmartLightningMonitoringAssetTree {
  flatNodes: EnergyMonitoringTreeNode[];
}

/** 三个周期使用统一的日历标签，标题区各自保存所选周期。 */
export const ENERGY_MONITORING_PERIODS: ReadonlyArray<{key: EnergyMonitoringPeriod; label: string}> = [
  {key: 'day', label: '日'},
  {key: 'week', label: '周'},
  {key: 'month', label: '月'}
];

/** 设备分类筛选标签与页面展示名称的映射。 */
export const ENERGY_MONITORING_CATEGORIES: ReadonlyArray<{key: EnergyMonitoringCategory; label: string}> = [
  {key: 'all', label: '全部'},
  {key: 'meter', label: '电能计量'},
  {key: 'power', label: '功率监测'},
  {key: 'analysis', label: '能耗分析'},
  {key: 'carbon', label: '碳排放计算'}
];

/** 监测指标样例；每个周期独立读取，不与趋势周期联动。 */
export const ENERGY_MONITORING_KPIS: Record<EnergyMonitoringPeriod, EnergyMonitoringKpis> = {
  day: {totalEnergyKwh: 16619, peakPowerKw: 861, averageLoadPercent: 89},
  week: {totalEnergyKwh: 112384, peakPowerKw: 912, averageLoadPercent: 83},
  month: {totalEnergyKwh: 486271, peakPowerKw: 948, averageLoadPercent: 81}
};

/** 电能质量样例按周期展示监测点数、异常次数、合格率和频率偏差。 */
export const ENERGY_MONITORING_QUALITY: Record<EnergyMonitoringPeriod, {
  pointCount: number; abnormalCount: number; voltagePassPercent: number; frequencyDeviationHz: number;
}> = {
  day: {pointCount: 25, abnormalCount: 2, voltagePassPercent: 98, frequencyDeviationHz: 0.19},
  week: {pointCount: 25, abnormalCount: 7, voltagePassPercent: 97, frequencyDeviationHz: 0.23},
  month: {pointCount: 25, abnormalCount: 18, voltagePassPercent: 96, frequencyDeviationHz: 0.27}
};

/** 能耗趋势图使用的日、周、月样例序列。 */
export const ENERGY_MONITORING_TRENDS: Record<EnergyMonitoringPeriod, EnergyMonitoringTrend> = {
  day: {
    labels: Array.from({length: 24}, (_, hour) => `${String(hour).padStart(2, '0')}:00`),
    values: [142, 139, 118, 91, 82, 96, 126, 137, 135, 132, 54, 121, 48, 128, 137, 133, 97, 58, 116, 134, 140, 127, 103, 84]
  },
  week: {
    labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    values: [15820, 16619, 16247, 17180, 16934, 14320, 13264]
  },
  month: {
    labels: ['第1周', '第2周', '第3周', '第4周'],
    values: [108340, 116284, 123920, 137727]
  }
};

/** 将后端按用户权限返回的嵌套真实关系树转换为共享树组件使用的索引结构。 */
export function buildEnergyMonitoringAssetTree(roots: EnergyRegulationAssetTreeNode[]): EnergyMonitoringAssetTree {
  const nodes = new Map<string, SmartLightningMonitoringAssetNode>();
  const flatNodes: EnergyMonitoringTreeNode[] = [];
  const treeRoots: SmartLightningMonitoringAssetNode[] = [];

  const visit = (
    source: EnergyRegulationAssetTreeNode,
    parentId: string | null,
    depth: number,
    ancestorIds: ReadonlySet<string>
  ): SmartLightningMonitoringAssetNode | undefined => {
    if (ancestorIds.has(source.id) || nodes.has(source.id)) {
      return undefined;
    }
    const node: SmartLightningMonitoringAssetNode = {
      id: source.id,
      name: source.name,
      entityType: source.entityType === EntityType.DEVICE ? EntityType.DEVICE : EntityType.ASSET,
      childIds: []
    };
    nodes.set(node.id, node);
    flatNodes.push({id: node.id, name: node.name, parentId, depth, entityType: node.entityType});
    const nextAncestors = new Set(ancestorIds);
    nextAncestors.add(node.id);
    for (const child of source.children || []) {
      const childNode = visit(child, node.id, depth + 1, nextAncestors);
      if (childNode) {
        node.childIds.push(childNode.id);
      }
    }
    return node;
  };

  for (const root of roots || []) {
    const rootNode = visit(root, null, 0, new Set<string>());
    if (rootNode) {
      treeRoots.push(rootNode);
    }
  }

  return {roots: treeRoots, nodes, flatNodes};
}

/** 生成供筛选器、设备卡和资产统计共用的能源设备列表。 */
export function buildEnergyMonitoringDevices(tree: EnergyMonitoringAssetTree): EnergyMonitoringDevice[] {
  const leafAssets = tree.flatNodes.filter((node) => node.entityType === EntityType.ASSET &&
    !tree.nodes.get(node.id)?.childIds.some((childId) => tree.nodes.get(childId)?.entityType === EntityType.ASSET));
  if (leafAssets.length === 0) {
    return [];
  }
  const templates = [
    {name: '1号低压进线表', category: 'meter', categoryLabel: '电能计量', productName: 'ESA智能电表', status: '正常', totalEnergyKwh: 16619, activePowerKw: 861, voltage: 231.4, current: 82.6, powerFactor: 0.96},
    {name: '2号低压进线表', category: 'meter', categoryLabel: '电能计量', productName: 'ESA智能电表', status: '正常', totalEnergyKwh: 14328, activePowerKw: 724, voltage: 229.8, current: 76.3, powerFactor: 0.94},
    {name: '空调主机回路', category: 'power', categoryLabel: '功率监测', productName: 'ESE电能质量监测器', status: '告警', totalEnergyKwh: 8264, activePowerKw: 418, voltage: 225.2, current: 58.1, powerFactor: 0.89},
    {name: '动力设备回路', category: 'power', categoryLabel: '功率监测', productName: 'ESB三相不平衡监测器', status: '正常', totalEnergyKwh: 7126, activePowerKw: 356, voltage: 231.1, current: 49.4, powerFactor: 0.93},
    {name: '照明用能分析', category: 'analysis', categoryLabel: '能耗分析', productName: 'E1E2电能质量检测器', status: '正常', totalEnergyKwh: 4632, activePowerKw: 215, voltage: 233.6, current: 31.2, powerFactor: 0.98},
    {name: '生产线用能分析', category: 'analysis', categoryLabel: '能耗分析', productName: 'E1E2电能质量检测器', status: '离线', totalEnergyKwh: 9531, activePowerKw: 0, voltage: 0, current: 0, powerFactor: 0},
    {name: '园区碳排核算点', category: 'carbon', categoryLabel: '碳排放计算', productName: '能源分析终端', status: '正常', totalEnergyKwh: 5842, activePowerKw: 282, voltage: 230.2, current: 41.8, powerFactor: 0.95},
    {name: '配电房环境监测', category: 'carbon', categoryLabel: '碳排放计算', productName: '能源分析终端', status: '正常', totalEnergyKwh: 3891, activePowerKw: 174, voltage: 228.7, current: 27.4, powerFactor: 0.92}
  ] as const;

  return templates.map((template, index) => {
    const asset = leafAssets[index % leafAssets.length];
    return {
      ...template,
      id: `energy-device-${index + 1}`,
      name: template.name,
      category: template.category,
      assetId: asset.id,
      updatedAt: `2026-10-10 ${String(14 + index % 8).padStart(2, '0')}:${String(8 + index * 7 % 52).padStart(2, '0')}:20`
    };
  });
}

/** 按当前资产节点获取其自身和全部后代节点 ID。 */
export function getEnergyMonitoringSelectedAssetIds(tree: EnergyMonitoringAssetTree, assetId: string): Set<string> {
  if (!assetId) {
    const allIds = new Set<string>();
    for (const root of tree.roots) {
      getSmartLightningMonitoringSelectedAssetIds(tree.nodes, root.id).forEach((id) => allIds.add(id));
    }
    return allIds;
  }
  return new Set(getSmartLightningMonitoringSelectedAssetIds(tree.nodes, assetId));
}

/** 汇总所选资产范围内的设备状态和告警处理数量。 */
export function buildEnergyMonitoringAssetSummary(devices: EnergyMonitoringDevice[]) {
  const total = devices.length;
  const online = devices.filter((device) => device.status !== '离线').length;
  const alarming = devices.filter((device) => device.status === '告警').length;
  const offline = total - online;
  return {
    total,
    normal: devices.filter((device) => device.status === '正常').length,
    online,
    alarming,
    offline,
    alarmTotal: alarming + 1,
    unprocessedAlarms: alarming,
    dispatchedAlarms: Math.max(0, alarming - 1),
    processedAlarms: 1
  };
}

/** 仅展示命中资产子树、分类和设备名称的设备。 */
export function filterEnergyMonitoringDevices(
  devices: EnergyMonitoringDevice[],
  selectedAssetIds: ReadonlySet<string>,
  category: EnergyMonitoringCategory,
  keyword: string
): EnergyMonitoringDevice[] {
  const normalizedKeyword = keyword.trim().toLocaleLowerCase();
  return devices.filter((device) => {
    const inScope = selectedAssetIds.size === 0 || selectedAssetIds.has(device.assetId);
    const inCategory = category === 'all' || device.category === category;
    const matchesKeyword = !normalizedKeyword ||
      device.name.toLocaleLowerCase().includes(normalizedKeyword) ||
      device.productName.toLocaleLowerCase().includes(normalizedKeyword);
    return inScope && inCategory && matchesKeyword;
  });
}
