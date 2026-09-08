import { AssetInfo } from '@shared/models/asset.models';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityRelation } from '@shared/models/relation.models';

export interface SmartLightningMonitoringAssetNode {
  id: string;
  name: string;
  entityType: EntityType.ASSET | EntityType.DEVICE;
  childIds: string[];
}

export interface SmartLightningMonitoringAssetTree {
  roots: SmartLightningMonitoringAssetNode[];
  nodes: Map<string, SmartLightningMonitoringAssetNode>;
}

export interface SmartLightningAnalysisMetric {
  label: string;
  value: string;
}

export interface SmartLightningAnalysisCard {
  id: 'spd' | 'ground' | 'current' | 'voltage';
  title: string;
  visual: SmartLightningAnalysisCard['id'];
  icon: string;
  primaryMetric: string;
  primaryMetricLabel: string;
  caption: string;
  metrics?: readonly SmartLightningAnalysisMetric[];
}

/** 顶部能力卡使用的演示数据；接入接口后只替换这组配置，不影响设备和告警统计。 */
export const SMART_LIGHTNING_ANALYSIS_CARDS: readonly SmartLightningAnalysisCard[] = [
  {
    id: 'spd',
    title: '浪涌防护系统能力评估',
    visual: 'spd',
    icon: 'bolt',
    primaryMetric: '8',
    primaryMetricLabel: '监测数量',
    caption: '当前选中资产 · SPD 防护能力',
    metrics: [
      {label: '监测数量', value: '8'},
      {label: '动作次数', value: '—'},
      {label: '劣化值', value: '—'},
      {label: '动作次数', value: '1'}
    ]
  },
  {
    id: 'ground',
    title: '接地电阻雷电流AI分析',
    visual: 'ground',
    icon: 'device_hub',
    primaryMetric: '0.35Ω',
    primaryMetricLabel: '当前接地电阻',
    caption: '地网采样稳定 · 接地状态正常'
  },
  {
    id: 'current',
    title: '雷电防护电流分析',
    visual: 'current',
    icon: 'electric_meter',
    primaryMetric: '32.8kA',
    primaryMetricLabel: '监测峰值',
    caption: '近 7 日峰值趋势 · 雷击计数 6 次'
  },
  {
    id: 'voltage',
    title: '雷电/过电压能力监测评估',
    visual: 'voltage',
    icon: 'show_chart',
    primaryMetric: '2.5kV',
    primaryMetricLabel: '峰值电压',
    caption: 'A/B/C 相电压在线 · 越限 0 次'
  }
];

/** 设备总览卡片仅使用当前用户可见设备与服务器属性 active，不伪造告警状态。 */
export interface SmartLightningMonitoringDeviceCard {
  id: string;
  name: string;
  category: string;
  status: '正常' | '报警' | '离线';
  icon: string;
  assetPath: string;
  productName: string;
  productType: string;
  lastActiveTime: string;
}

/** 设备总览卡片的业务排序：报警优先，其次离线，最后正常。 */
const smartLightningDeviceStatusOrder: Record<SmartLightningMonitoringDeviceCard['status'], number> = {
  报警: 0,
  离线: 1,
  正常: 2
};

/** 当前设备卡片需要的服务器属性。functionCategory 是业务分类，不使用设备型号替代。 */
export interface SmartLightningMonitoringDeviceAttributes {
  active?: unknown;
  functionCategory?: unknown;
  productName?: unknown;
  lastActivityTime?: unknown;
}

/** 后端 functionCategory 枚举值与数据监测页面中文标签的唯一映射。 */
const smartLightningFunctionCategoryLabels: Record<string, string> = {
  spd_monitor: 'SPD监测',
  ground_resistance: '接地电阻监测',
  lightning_current: '雷电流监测',
  arc_fault: '故障电弧监测'
};

export const getSmartLightningFunctionCategoryLabel = (value: unknown): string => {
  if (typeof value !== 'string' || !value.trim()) {
    return '未分类';
  }
  const normalizedValue = value.trim();
  return smartLightningFunctionCategoryLabels[normalizedValue] || normalizedValue;
};

/** ThingsBoard 的 lastActivityTime 是毫秒时间戳；无有效数据时不能冒充设备创建时间。 */
export const formatSmartLightningMonitoringLastActivityTime = (value: unknown): string => {
  const timestamp = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(timestamp) || timestamp <= 0) {
    return '--';
  }
  const date = new Date(timestamp);
  const pad = (part: number) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

/** 兼容服务器属性返回的布尔值、数字值和字符串值，并回退到设备实体的 active 字段。 */
export const isSmartLightningDeviceActive = (value: unknown, fallbackValue?: unknown): boolean => {
  const candidate = value === undefined || value === null ? fallbackValue : value;
  if (candidate === true || candidate === 1) {
    return true;
  }
  return typeof candidate === 'string' && ['true', '1'].includes(candidate.trim().toLowerCase());
};

/** 仅统计选中资产子树内设备产生的告警，避免把其他项目的告警混入当前节点。 */
export interface SmartLightningMonitoringAlarmSummary {
  active: number;
  todayTotal: number;
  unhandled: number;
  assigned: number;
  handled: number;
}

/** 与 AlarmInfo 对应的最小字段集，保持统计函数可脱离 Angular 服务单独测试。 */
export interface SmartLightningMonitoringAlarmRecord {
  originator?: {id: string; entityType: EntityType | string};
  status: string;
  cleared: boolean;
  startTs: number;
  assignTs: number;
  clearTs: number;
}

/**
 * 从告警结果中提取当前资产树范围内仍处于活动状态的设备 ID。
 * 资产树只负责展示，告警状态由页面的公共数据层统一计算，避免各页面重复实现筛选条件。
 */
export const getSmartLightningActiveAlarmDeviceIds = (
  alarms: ReadonlyArray<Pick<SmartLightningMonitoringAlarmRecord, 'originator' | 'status'>>,
  visibleDeviceIds: ReadonlySet<string>
): Set<string> => {
  const activeDeviceIds = new Set<string>();
  alarms.forEach((alarm) => {
    const deviceId = alarm.originator?.id;
    if (deviceId && visibleDeviceIds.has(deviceId) &&
      alarm.originator?.entityType === EntityType.DEVICE &&
      (alarm.status === 'ACTIVE_UNACK' || alarm.status === 'ACTIVE_ACK')) {
      activeDeviceIds.add(deviceId);
    }
  });
  return activeDeviceIds;
};

/** 与后端 /api/smart-lightning/monitoring/tree 对应的轻量树节点。 */
export interface SmartLightningMonitoringTreeResponseNode {
  id: string;
  name: string;
  entityType: EntityType.ASSET | EntityType.DEVICE;
  children: SmartLightningMonitoringTreeResponseNode[];
}

/** Builds a cycle-safe hierarchy with asset branches and their device leaves. */
export const buildSmartLightningMonitoringAssetTree = (
  assets: AssetInfo[],
  devices: DeviceInfo[],
  relations: EntityRelation[]
): SmartLightningMonitoringAssetTree => {
  const nodes = new Map<string, SmartLightningMonitoringAssetNode>();
  const childToParent = new Map<string, string>();
  assets.forEach((asset) => {
    if (asset.id?.id) {
      nodes.set(asset.id.id, {id: asset.id.id, name: asset.name, entityType: EntityType.ASSET, childIds: []});
    }
  });
  devices.forEach((device) => {
    if (device.id?.id) {
      nodes.set(device.id.id, {id: device.id.id, name: device.name, entityType: EntityType.DEVICE, childIds: []});
    }
  });

  const wouldCreateCycle = (parentId: string, childId: string): boolean => {
    let ancestorId: string | undefined = parentId;
    while (ancestorId) {
      if (ancestorId === childId) {
        return true;
      }
      ancestorId = childToParent.get(ancestorId);
    }
    return false;
  };

  relations.forEach((relation) => {
    const parentId = relation.from?.id;
    const childId = relation.to?.id;
    if (relation.type !== 'Contains' || relation.from?.entityType !== EntityType.ASSET ||
      (relation.to?.entityType !== EntityType.ASSET && relation.to?.entityType !== EntityType.DEVICE) || !parentId || !childId || parentId === childId ||
      !nodes.has(parentId) || !nodes.has(childId) || childToParent.has(childId) || wouldCreateCycle(parentId, childId)) {
      return;
    }
    nodes.get(parentId)?.childIds.push(childId);
    childToParent.set(childId, parentId);
  });

  nodes.forEach((node) => node.childIds.sort((left, right) =>
    (nodes.get(left)?.name || '').localeCompare(nodes.get(right)?.name || '')
  ));
  const roots = [...nodes.values()]
    .filter((node) => !childToParent.has(node.id))
    .sort((left, right) => left.name.localeCompare(right.name));
  return {roots, nodes};
};

/**
 * 将专用接口的嵌套响应转为组件使用的索引树。
 * 后端已完成权限过滤；前端仍保留环路保护，以免异常历史关系导致页面递归卡死。
 */
export const buildSmartLightningMonitoringAssetTreeFromRoots = (
  responseRoots: SmartLightningMonitoringTreeResponseNode[]
): SmartLightningMonitoringAssetTree => {
  const nodes = new Map<string, SmartLightningMonitoringAssetNode>();
  const visit = (source: SmartLightningMonitoringTreeResponseNode, ancestorIds: Set<string>): void => {
    if (ancestorIds.has(source.id) || nodes.has(source.id)) {
      return;
    }
    const target: SmartLightningMonitoringAssetNode = {
      id: source.id,
      name: source.name,
      entityType: source.entityType,
      childIds: []
    };
    nodes.set(target.id, target);
    const nextAncestorIds = new Set(ancestorIds);
    nextAncestorIds.add(target.id);
    (source.children || []).forEach((child) => {
      visit(child, nextAncestorIds);
      if (nodes.has(child.id) && !target.childIds.includes(child.id)) {
        target.childIds.push(child.id);
      }
    });
  };
  responseRoots.forEach((root) => visit(root, new Set<string>()));
  const roots = responseRoots
    .map((root) => nodes.get(root.id))
    .filter((root): root is SmartLightningMonitoringAssetNode => !!root);
  return {roots, nodes};
};

/** 返回每个资产/设备从根节点开始的完整层级地址，用于设备总览卡片展示。 */
export const getSmartLightningMonitoringAssetPaths = (
  tree: SmartLightningMonitoringAssetTree
): Map<string, string> => {
  const paths = new Map<string, string>();
  const visit = (node: SmartLightningMonitoringAssetNode, ancestors: string[]): void => {
    if (paths.has(node.id)) {
      return;
    }
    const name = node.name.trim() || node.id;
    const path = [...ancestors, name];
    paths.set(node.id, path.join('/'));
    node.childIds.forEach((childId) => {
      const child = tree.nodes.get(childId);
      if (child) {
        visit(child, path);
      }
    });
  };
  tree.roots.forEach((root) => visit(root, []));
  return paths;
};

/** Returns the selected asset plus every known descendant, once each. */
export const getSmartLightningMonitoringSelectedAssetIds = (
  nodes: Map<string, SmartLightningMonitoringAssetNode>,
  selectedId: string
): string[] => {
  const selectedIds: string[] = [];
  const visited = new Set<string>();
  const visit = (id: string) => {
    if (visited.has(id) || !nodes.has(id)) {
      return;
    }
    visited.add(id);
    selectedIds.push(id);
    nodes.get(id)?.childIds.forEach(visit);
  };
  visit(selectedId);
  return selectedIds;
};

/** 返回根资产下拉框当前应展示的根节点；空值表示不限定根资产。 */
export const getSmartLightningMonitoringVisibleRoots = (
  tree: SmartLightningMonitoringAssetTree,
  rootAssetId: string
): SmartLightningMonitoringAssetNode[] => {
  if (!rootAssetId) {
    return tree.roots;
  }
  const selectedRoot = tree.roots.find((root) => root.id === rootAssetId);
  return selectedRoot ? [selectedRoot] : tree.roots;
};

/** 将根资产选择转换为根节点及全部下属节点集合，用于统计和列表筛选。 */
export const getSmartLightningMonitoringRootAssetIds = (
  tree: SmartLightningMonitoringAssetTree,
  rootAssetId: string
): string[] => {
  const ids: string[] = [];
  const visited = new Set<string>();
  const roots = getSmartLightningMonitoringVisibleRoots(tree, rootAssetId);
  roots.forEach((root) => {
    getSmartLightningMonitoringSelectedAssetIds(tree.nodes, root.id).forEach((id) => {
      if (!visited.has(id)) {
        visited.add(id);
        ids.push(id);
      }
    });
  });
  return ids;
};

/**
 * 由设备实体信息和 active 服务器属性生成节点设备卡片。
 * selectedIds 由资产树递归计算得出，因此选中资产会自然包含其全部下属设备。
 */
export const buildSmartLightningMonitoringDeviceCards = (
  devices: DeviceInfo[],
  selectedIds: Set<string>,
  attributesByDeviceId: Map<string, SmartLightningMonitoringDeviceAttributes>,
  activeAlarmDeviceIds: Set<string> = new Set(),
  assetPathByDeviceId: ReadonlyMap<string, string> = new Map()
): SmartLightningMonitoringDeviceCard[] => devices
  .filter((device) => !!device.id?.id && selectedIds.has(device.id.id))
  .map((device) => {
    const id = device.id.id;
    const deviceProfileName = device.deviceProfileName || device.type || '未配置';
    const attributes = attributesByDeviceId.get(id);
    const active = attributes?.active;
    const functionCategory = getSmartLightningFunctionCategoryLabel(attributes?.functionCategory);
    const isOnline = isSmartLightningDeviceActive(active, device.active);
    const hasActiveAlarm = activeAlarmDeviceIds.has(id);
    const status: SmartLightningMonitoringDeviceCard['status'] = hasActiveAlarm
      ? '报警'
      : isOnline ? '正常' : '离线';
    return {
      id,
      name: device.name || '未命名设备',
      category: functionCategory,
      status,
      icon: 'electric_meter',
      assetPath: assetPathByDeviceId.get(id) || '--',
      productName: typeof attributes?.productName === 'string' && attributes.productName.trim()
        ? attributes.productName.trim()
        : '--',
      productType: deviceProfileName,
      lastActiveTime: formatSmartLightningMonitoringLastActivityTime(attributes?.lastActivityTime)
    };
  })
  .sort((left, right) => {
    const statusOrder = smartLightningDeviceStatusOrder[left.status] - smartLightningDeviceStatusOrder[right.status];
    return statusOrder || left.name.localeCompare(right.name);
  });

/**
 * 通过 ThingsBoard 原生告警字段形成设备节点统计。
 * 未处理 = ACTIVE_UNACK；已派发 = 当前活动且已有 assignTs；已处理 = 当天已清除。
 */
export const calculateSmartLightningMonitoringAlarmSummary = (
  alarms: SmartLightningMonitoringAlarmRecord[],
  selectedDeviceIds: Set<string>,
  startOfToday: number
): SmartLightningMonitoringAlarmSummary => {
  const nodeAlarms = alarms.filter((alarm) =>
    alarm.originator?.entityType === EntityType.DEVICE && selectedDeviceIds.has(alarm.originator.id)
  );
  const activeAlarms = nodeAlarms.filter((alarm) =>
    alarm.status === 'ACTIVE_UNACK' || alarm.status === 'ACTIVE_ACK'
  );
  return {
    active: activeAlarms.length,
    todayTotal: nodeAlarms.filter((alarm) => alarm.startTs >= startOfToday).length,
    unhandled: activeAlarms.filter((alarm) => alarm.status === 'ACTIVE_UNACK').length,
    assigned: activeAlarms.filter((alarm) => alarm.assignTs > 0).length,
    handled: nodeAlarms.filter((alarm) => alarm.cleared && alarm.clearTs >= startOfToday).length
  };
};

/** 前端分页统一钳制页码，筛选后页数减少时不会落入空白页。 */
export const getSmartLightningMonitoringPage = <T>(items: T[], requestedPage: number, pageSize: number) => {
  const safePageSize = Math.max(1, pageSize);
  const totalPages = Math.max(1, Math.ceil(items.length / safePageSize));
  const currentPage = Math.min(Math.max(1, requestedPage), totalPages);
  const start = (currentPage - 1) * safePageSize;
  return {
    items: items.slice(start, start + safePageSize),
    currentPage,
    totalPages
  };
};
