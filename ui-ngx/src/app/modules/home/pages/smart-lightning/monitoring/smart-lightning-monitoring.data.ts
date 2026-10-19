  /** 文件职责：数据监测模块的 smart-lightning-monitoring.data，定义数据模型、接口结果映射与纯函数。 */
import { DeviceInfo } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import {
  formatSmartLightningRealtimeMetricValue,
  getSmartLightningRealtimeMetricDefinitions,
  resolveSmartLightningDeviceModel,
  SmartLightningRealtimeMetricDefinition
} from './smart-lightning-device-detail.data';

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

/** 格式化实时分析数值；null/undefined 表示没有遥测，数值小数位保持原样。 */
export const formatSmartLightningRealtimeValue = (
  value: number | null | undefined,
  unit = ''
): string => value == null || !Number.isFinite(value) ? '--' : `${value}${unit}`;

/** 设备总览卡片仅使用当前用户可见设备与 active 最新遥测，不伪造告警状态。 */
export interface SmartLightningMonitoringTelemetryValue {
  ts: number;
  value: string;
}

/** 卡片固定显示的实时遥测项；title 中保留测点键和上报时间便于悬浮查看。 */
export interface SmartLightningMonitoringRealtimeMetric {
  key: string;
  label: string;
  value: string;
  title: string;
  abnormal: boolean;
}

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
  realtimeMetrics: SmartLightningMonitoringRealtimeMetric[];
}

/** 设备总览卡片的业务排序：报警优先，其次离线，最后正常。 */
const smartLightningDeviceStatusOrder: Record<SmartLightningMonitoringDeviceCard['status'], number> = {
  报警: 0,
  离线: 1,
  正常: 2
};

/** 当前设备卡片需要的状态和业务属性。functionCategory 是业务分类，不使用设备型号替代。 */
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
  lightning_current: '雷电流监测'
};

/** 把后端功能分类枚举值映射为中文标签；未配置或未知值原样展示。 */
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

/**
 * 设备卡片只挑选职责相关的少量测点；每种型号最多显示三项。
 * 直接测点沿用详情页测点定义，复合项仅用于把同类状态合并为一个摘要。
 */
type SmartLightningCardMetricLayout =
  | {kind: 'key'; key: string}
  | {kind: 'maximum'; keys: readonly string[]; label: string}
  | {kind: 'all-status'; keys: readonly string[]; label: string};

const cardMetric = (key: string): SmartLightningCardMetricLayout => ({kind: 'key', key});
const cardMaximum = (keys: readonly string[], label: string): SmartLightningCardMetricLayout =>
  ({kind: 'maximum', keys, label});
const cardCombinedStatus = (keys: readonly string[], label: string): SmartLightningCardMetricLayout =>
  ({kind: 'all-status', keys, label});

const SMART_LIGHTNING_CARD_METRICS: Readonly<Record<string, readonly SmartLightningCardMetricLayout[]>> = {
  DES4: [cardMetric('RemoteSig'), cardMetric('McbStatus'), cardMetric('GndStatus')],
  DES9: [cardMetric('RemoteSig'), cardMetric('GndStatus'), cardMaximum(['LeakCurrA', 'LeakCurrB', 'LeakCurrC'], '最大漏流')],
  DESA: [cardMetric('RemoteSig'), cardMaximum(['LeakCurrA', 'LeakCurrB', 'LeakCurrC'], '最大漏流'), cardMetric('SPD_RUL')],
  DEM2: [cardMetric('GndStatus'), cardCombinedStatus(['Trip1', 'Trip2'], '脱扣状态'), cardMetric('LifeLeft')],
  DEM4: [cardMetric('GndStatus'), cardCombinedStatus(['Trip1', 'Trip2', 'Trip3', 'Trip4'], '脱扣状态'), cardMetric('LifeLeft')],
  DEA2: [cardMetric('GndStatus'), cardMetric('LeakCurrA'), cardMetric('LifeLeft')],
  DEA4: [cardMetric('GndStatus'), cardMaximum(['LeakCurrA', 'LeakCurrB', 'LeakCurrC'], '最大漏流'), cardMetric('LifeLeft')],
  ESM0: [cardMetric('RemoteSig'), cardMetric('GndStatus'), cardMetric('StrikeCnt')],
  ESM3: [cardMetric('RemoteSig'), cardMaximum(['LeakCurrA', 'LeakCurrB', 'LeakCurrC'], '最大漏流'), cardMetric('LifeLeft')],
  DEP4: [cardMetric('RemoteSig'), cardMetric('StrikeCnt'), cardMetric('Temp')],
  G1A3: [cardMetric('GndGridRes')],
  M1AI: [cardMetric('PeakValue'), cardMetric('SurgeCnt'), cardMetric('Polarity')],
  L2AI: [cardMetric('PeakValue'), cardMetric('SurgeCnt'), cardMetric('Polarity')],
};

/** 将遥测记录转为卡片提示文本，同时保留原始键、原始值与时间戳。 */
const getCardMetricTooltip = (
  entries: Array<{definition: SmartLightningRealtimeMetricDefinition; sample?: SmartLightningMonitoringTelemetryValue}>
): string => entries.map(({definition, sample}) =>
  '测点：' + definition.key + '；原始值：' + (sample?.value ?? '--') +
  '；时间：' + formatSmartLightningMonitoringLastActivityTime(sample?.ts)
).join('\n');

/** 按设备型号读取最多三项职责相关的实时指标，不改变 WebSocket 中的原始值。 */
const buildSmartLightningCardRealtimeMetrics = (
  deviceProfileName: string,
  latestValues: ReadonlyMap<string, SmartLightningMonitoringTelemetryValue>
): SmartLightningMonitoringRealtimeMetric[] => {
  const model = resolveSmartLightningDeviceModel(deviceProfileName);
  const layout = model ? SMART_LIGHTNING_CARD_METRICS[model] : undefined;
  if (!layout?.length) {
    return [];
  }
  const definitions = getSmartLightningRealtimeMetricDefinitions(deviceProfileName);
  const definitionByKey = new Map(definitions.map((definition) => [definition.key, definition]));
  const result: SmartLightningMonitoringRealtimeMetric[] = [];

  layout.forEach((item) => {
    if (item.kind === 'key') {
      const definition = definitionByKey.get(item.key);
      if (!definition) {
        return;
      }
      const sample = latestValues.get(item.key);
      const value = formatSmartLightningRealtimeMetricValue(definition, sample?.value);
      result.push({
        key: item.key,
        label: definition.label,
        value,
        title: getCardMetricTooltip([{definition, sample}]),
        abnormal: definition.valueType === 'status' && value === '异常'
      });
      return;
    }

    const entries = item.keys
      .map((key) => ({definition: definitionByKey.get(key), sample: latestValues.get(key)}))
      .filter((entry): entry is {definition: SmartLightningRealtimeMetricDefinition; sample: SmartLightningMonitoringTelemetryValue} =>
        !!entry.definition && !!entry.sample);

    if (item.kind === 'maximum') {
      const maximum = entries
        .filter((entry) => Number.isFinite(Number(entry.sample.value)))
        .reduce<{definition: SmartLightningRealtimeMetricDefinition; sample: SmartLightningMonitoringTelemetryValue} | undefined>(
          (current, entry) => !current || Number(entry.sample.value) > Number(current.sample.value) ? entry : current,
          undefined
        );
      const displayEntry = maximum || entries[0];
      const definition = displayEntry?.definition || definitionByKey.get(item.keys[0]);
      if (!definition) {
        return;
      }
      result.push({
        key: displayEntry?.definition.key || definition.key,
        label: item.label,
        value: displayEntry ? formatSmartLightningRealtimeMetricValue(definition, displayEntry.sample.value) : '--',
        title: getCardMetricTooltip(displayEntry ? [displayEntry] : [{definition}]),
        abnormal: false
      });
      return;
    }

    const definitionsForGroup = item.keys
      .map((key) => definitionByKey.get(key))
      .filter((definition): definition is SmartLightningRealtimeMetricDefinition => !!definition);
    const abnormal = entries.some((entry) =>
      formatSmartLightningRealtimeMetricValue(entry.definition, entry.sample.value) === '异常');
    const complete = entries.length === item.keys.length;
    const value = abnormal ? '异常' : complete ? '正常' : entries.length ? '数据不全' : '--';
    result.push({
      key: item.keys.join(', '),
      label: item.label,
      value,
      title: getCardMetricTooltip(entries.length
        ? entries
        : definitionsForGroup.map((definition) => ({definition}))),
      abnormal
    });
  });

  return result.slice(0, 3);
};

/** 兼容 active 遥测返回的布尔值、数字值和字符串值，并回退到设备实体状态。 */
export const isSmartLightningDeviceActive = (value: unknown, fallbackValue?: unknown): boolean => {
  const candidate = value === undefined || value === null ? fallbackValue : value;
  if (candidate === true || candidate === 1) {
    return true;
  }
  return typeof candidate === 'string' && ['true', '1'].includes(candidate.trim().toLowerCase());
};
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

/**
 * 返回存在未清除告警设备的资产及其全部上级资产。
 * 此函数只计算资产节点标记；设备自身的标记由共享树组件直接使用告警设备 ID 判断。
 */
export const getSmartLightningAlarmAssetIds = (
  tree: SmartLightningMonitoringAssetTree,
  alarmDeviceIds: ReadonlySet<string>
): Set<string> => {
  const alarmAssetIds = new Set<string>();
  const visiting = new Set<string>();
  const hasAlarmDescendant = (node: SmartLightningMonitoringAssetNode): boolean => {
    if (visiting.has(node.id)) {
      return false;
    }
    visiting.add(node.id);
    let containsAlarm = false;
    // 必须遍历全部子分支：Array.some() 会在第一个告警分支返回 true 后提前停止，
    // 导致同级后续资产（例如测试电柜-010）没有被计算告警标记。
    for (const childId of node.childIds) {
      const child = tree.nodes.get(childId);
      if (!child) {
        continue;
      }
      const childContainsAlarm = child.entityType === EntityType.DEVICE
        ? alarmDeviceIds.has(child.id)
        : hasAlarmDescendant(child);
      if (childContainsAlarm) {
        containsAlarm = true;
      }
    }
    if (node.entityType === EntityType.ASSET && containsAlarm) {
      alarmAssetIds.add(node.id);
    }
    visiting.delete(node.id);
    return containsAlarm;
  };
  tree.roots.forEach((root) => hasAlarmDescendant(root));
  return alarmAssetIds;
};

/** 默认只展开前两层资产，设备和更深层级保持收起。 */
export const getSmartLightningDefaultExpandedAssetIds = (
  tree: SmartLightningMonitoringAssetTree,
  visibleLevels = 2
): Set<string> => {
  const expandedIds = new Set<string>();
  const visit = (node: SmartLightningMonitoringAssetNode, level: number): void => {
    if (node.entityType !== EntityType.ASSET) {
      return;
    }
    if (level < visibleLevels && node.childIds.some((childId) => tree.nodes.get(childId)?.entityType === EntityType.ASSET)) {
      expandedIds.add(node.id);
    }
    node.childIds.forEach((childId) => {
      const child = tree.nodes.get(childId);
      if (child) {
        visit(child, level + 1);
      }
    });
  };
  tree.roots.forEach((root) => visit(root, 1));
  return expandedIds;
};

/** 与后端 /api/smart-lightning/monitoring/tree 对应的轻量树节点。 */
export interface SmartLightningMonitoringTreeResponseNode {
  id: string;
  name: string;
  entityType: EntityType.ASSET | EntityType.DEVICE;
  children: SmartLightningMonitoringTreeResponseNode[];
}
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

/** 返回选中节点及其全部已知后代节点，每个节点只出现一次。 */
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
 * 由设备实体信息和 active 最新遥测生成节点设备卡片。
 * selectedIds 由资产树递归计算得出，因此选中资产会自然包含其全部下属设备。
 */
export const buildSmartLightningMonitoringDeviceCards = (
  devices: DeviceInfo[],
  selectedIds: Set<string>,
  attributesByDeviceId: Map<string, SmartLightningMonitoringDeviceAttributes>,
  activeAlarmDeviceIds: Set<string> = new Set(),
  assetPathByDeviceId: ReadonlyMap<string, string> = new Map(),
  telemetryByDeviceId: ReadonlyMap<string, ReadonlyMap<string, SmartLightningMonitoringTelemetryValue>> = new Map()
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
      lastActiveTime: formatSmartLightningMonitoringLastActivityTime(attributes?.lastActivityTime),
      realtimeMetrics: buildSmartLightningCardRealtimeMetrics(
        deviceProfileName,
        telemetryByDeviceId.get(id) || new Map()
      )
    };
  })
  .sort((left, right) => {
    const statusOrder = smartLightningDeviceStatusOrder[left.status] - smartLightningDeviceStatusOrder[right.status];
    return statusOrder || left.name.localeCompare(right.name);
  });
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
