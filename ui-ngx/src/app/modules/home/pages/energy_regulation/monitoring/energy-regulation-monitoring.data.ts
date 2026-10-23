/** 文件职责：集中定义用能监测页面的树、真实指标与设备筛选计算。 */
import type {EnergyRegulationDeviceContext, EnergyRegulationAssetTreeNode} from '../energy-regulation-api.service';
import {EntityType} from '@shared/models/entity-type.models';

/** 共享树控件的通用结构在能源模块独立声明，业务遍历不依赖防雷数据函数。 */
export interface EnergyAssetNode {id: string; name: string; entityType: EntityType.ASSET | EntityType.DEVICE; childIds: string[];}
export interface EnergyAssetTree {roots: EnergyAssetNode[]; nodes: Map<string, EnergyAssetNode>;}

/** 监测页面独立维护周期状态，切换一个区域不影响其他区域。 */
export type EnergyMonitoringPeriod = 'day' | 'week' | 'month';
export type EnergyMonitoringCategory = 'all' | 'meter' | 'power' | 'analysis' | 'carbon';

/** 设备总览的真实遥测字段，缺少有效采样时为空。 */
export interface EnergyMonitoringDevice {
  id: string;
  name: string;
  category: Exclude<EnergyMonitoringCategory, 'all'>;
  categoryLabel: string;
  productName: string;
  assetId: string;
  status: '正常' | '告警' | '离线';
  online?: boolean;
  totalEnergyKwh: number | null;
  activePowerKw: number | null;
  voltage: number | null;
  current: number | null;
  powerFactor: number | null;
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

/** 每周期的用电与功率指标结构。 */
export interface EnergyMonitoringKpis {
  totalEnergyKwh: number | null;
  peakPowerKw: number;
  averageLoadPercent: number;
}

/** 左侧资产选择、汇总卡和设备列表共同使用的能源资产树。 */
export interface EnergyMonitoringAssetTree extends EnergyAssetTree {
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

/** 缺少额定负载和电压判定标准时保持未知，不填充演示指标。 */
export const ENERGY_MONITORING_KPIS = Object.fromEntries(['day', 'week', 'month'].map((key) => [key,
  {totalEnergyKwh: null as number | null, peakPowerKw: null as number | null, averageLoadPercent: null as number | null}])) as
  Record<EnergyMonitoringPeriod, {totalEnergyKwh: number | null; peakPowerKw: number | null; averageLoadPercent: number | null}>;
export const ENERGY_MONITORING_QUALITY = Object.fromEntries(['day', 'week', 'month'].map((key) => [key,
  {pointCount: null as number | null, abnormalCount: null as number | null, voltagePassPercent: null as number | null, frequencyDeviationHz: null as number | null}])) as
  Record<EnergyMonitoringPeriod, {pointCount: number | null; abnormalCount: number | null; voltagePassPercent: number | null; frequencyDeviationHz: number | null}>;
export const ENERGY_MONITORING_TRENDS = Object.fromEntries(['day', 'week', 'month'].map((key) => [key,
  {labels: [] as string[], values: [] as Array<number | null>}])) as Record<EnergyMonitoringPeriod, {labels: string[]; values: Array<number | null>}>;

/** 将后端按用户权限返回的嵌套真实关系树转换为共享树组件使用的索引结构。 */
export function buildEnergyMonitoringAssetTree(roots: EnergyRegulationAssetTreeNode[]): EnergyMonitoringAssetTree {
  const nodes = new Map<string, EnergyAssetNode>();
  const flatNodes: EnergyMonitoringTreeNode[] = [];
  const treeRoots: EnergyAssetNode[] = [];

  const visit = (
    source: EnergyRegulationAssetTreeNode,
    parentId: string | null,
    depth: number,
    ancestorIds: ReadonlySet<string>
  ): EnergyAssetNode | undefined => {
    if (ancestorIds.has(source.id) || nodes.has(source.id)) {
      return undefined;
    }
    const node: EnergyAssetNode = {
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

/** 将模块真实设备上下文与最新遥测映射为卡片；所有业务量使用规范键，不进行换算。 */
export function buildEnergyMonitoringDevices(tree: EnergyMonitoringAssetTree,
  contexts: readonly EnergyRegulationDeviceContext[] = [],
  latest: (id: string) => ReadonlyMap<string, {ts: number; value: string}> = () => new Map(),
  alarmed: ReadonlySet<string> = new Set()): EnergyMonitoringDevice[] {
  return contexts.map((context) => {
    const values = latest(context.deviceId);
    const number = (key: string) => {
      const raw = values.get(key)?.value;
      return raw !== undefined && String(raw).trim() !== '' && Number.isFinite(Number(raw)) ? Number(raw) : null;
    };
    const active = values.get('active')?.value;
    const online = active === undefined ? context.active === true : ['true', '1'].includes(String(active).toLowerCase());
    const timestamp = Math.max(context.lastActivityTime || 0, ...Array.from(values.values(), (sample) => sample.ts));
    return {id: context.deviceId, name: context.deviceName, category: 'meter' as const,
      categoryLabel: '电能计量', productName: context.productName || '__', assetId: context.deviceId,
      online, status: alarmed.has(context.deviceId) ? '告警' as const : online ? '正常' as const : '离线' as const,
      totalEnergyKwh: number('FwdActET'), activePowerKw: number('ActPwrTot'), voltage: number('PhAVolt'),
      current: number('PhACurr'), powerFactor: number('PwrFactT'), updatedAt: timestamp ? new Date(timestamp).toLocaleString() : '__'};
  });
}

/** 默认只展开根节点及其第一层资产，保持共享树的交互习惯。 */
export function getEnergyDefaultExpandedAssetIds(tree: EnergyMonitoringAssetTree): Set<string> {
  return new Set(tree.roots.flatMap((root) => [root.id, ...root.childIds]));
}

/** 有环或悬空节点时终止遍历，不借用其他业务模块实现。 */
export function getEnergyDescendantIds(nodes: ReadonlyMap<string, EnergyAssetNode>, id: string): string[] {
  const result = new Set<string>(); const pending = [id];
  while (pending.length) {
    const next = pending.pop()!;
    if (!result.has(next)) { result.add(next); pending.push(...(nodes.get(next)?.childIds || [])); }
  }
  return [...result];
}

/** 按当前资产节点获取其自身和全部后代节点 ID。 */
export function getEnergyMonitoringSelectedAssetIds(tree: EnergyMonitoringAssetTree, assetId: string): Set<string> {
  if (!assetId) {
    const allIds = new Set<string>();
    for (const root of tree.roots) {
      getEnergyDescendantIds(tree.nodes, root.id).forEach((id) => allIds.add(id));
    }
    return allIds;
  }
  return new Set(getEnergyDescendantIds(tree.nodes, assetId));
}

/** 汇总所选资产范围内的设备状态和告警处理数量。 */
export function buildEnergyMonitoringAssetSummary(devices: EnergyMonitoringDevice[]) {
  const total = devices.length;
  const online = devices.filter((device) => device.online ?? device.status !== '离线').length;
  const alarming = devices.filter((device) => device.status === '告警').length;
  const offline = total - online;
  return {
    total,
    normal: devices.filter((device) => device.status === '正常').length,
    online,
    alarming,
    offline,
    alarmTotal: null as number | null,
    unprocessedAlarms: null as number | null,
    dispatchedAlarms: null as number | null,
    processedAlarms: null as number | null
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
    const inScope = selectedAssetIds.has(device.assetId);
    const inCategory = category === 'all' || device.category === category;
    const matchesKeyword = !normalizedKeyword ||
      device.name.toLocaleLowerCase().includes(normalizedKeyword) ||
      device.productName.toLocaleLowerCase().includes(normalizedKeyword);
    return inScope && inCategory && matchesKeyword;
  });
}
