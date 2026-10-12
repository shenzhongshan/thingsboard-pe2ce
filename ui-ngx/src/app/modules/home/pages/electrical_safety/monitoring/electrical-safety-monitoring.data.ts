/** 文件职责：集中定义电气安全监测页的展示模型、资产树样例和本地筛选汇总函数。 */
import {EntityType} from '@shared/models/entity-type.models';
import {
  buildSmartLightningMonitoringAssetTreeFromRoots,
  SmartLightningMonitoringAssetTree,
  SmartLightningMonitoringTreeResponseNode
} from '@home/pages/smart-lightning/monitoring/smart-lightning-monitoring.data';

/** 设备总览分类筛选项；other 汇总项目配置中剩余的电气安全设备类型。 */
export type ElectricalSafetyMonitoringCategoryKey =
  | 'all'
  | 'fire'
  | 'temperature'
  | 'leakage'
  | 'power-quality'
  | 'other';

/** 设备在线状态与当前告警状态分别表达连接情况和运行情况。 */
export type ElectricalSafetyConnectionState = 'online' | 'offline';
export type ElectricalSafetyConditionState = 'normal' | 'alarm';

/** 电气安全产品分类值，与项目 thingsboard.yml 中的 product-category 一致。 */
export const ELECTRICAL_SAFETY_PRODUCT_CATEGORY = 'electrical_safety';

/** 顶部监测卡片中的单个指标。 */
export interface ElectricalSafetyMonitoringMetric {
  label: string;
  value: string;
  unit?: string;
  tone?: 'healthy' | 'warning' | 'critical' | 'neutral';
}

/** 顶部三类电气安全监测卡片的展示数据。 */
export interface ElectricalSafetyMonitoringSummaryCard {
  key: 'fire' | 'leakage' | 'power-quality';
  title: string;
  icon: string;
  metrics: readonly ElectricalSafetyMonitoringMetric[];
}

/** 设备总览卡片所需的本地展示字段。 */
export interface ElectricalSafetyMonitoringDevice {
  id: string;
  name: string;
  productName: string;
  model: string;
  productCategory: string;
  functionCategory: string;
  categoryKey: Exclude<ElectricalSafetyMonitoringCategoryKey, 'all'>;
  categoryLabel: string;
  assetId: string;
  assetPath: string;
  connection: ElectricalSafetyConnectionState;
  condition: ElectricalSafetyConditionState;
  lastReportTime: string;
  todayAlarmCount: number;
  unprocessedAlarmCount: number;
  dispatchedAlarmCount: number;
  processedAlarmCount: number;
}

/** 资产范围统计，告警总数按当日报警条数汇总。 */
export interface ElectricalSafetyMonitoringScopeSummary {
  totalDevices: number;
  normalDevices: number;
  onlineDevices: number;
  alarmDevices: number;
  offlineDevices: number;
  todayAlarmCount: number;
  unprocessedAlarmCount: number;
  dispatchedAlarmCount: number;
  processedAlarmCount: number;
}

/** 设备分类按钮沿用项目产品配置中的功能分类，不添加未配置的烟雾监测分类。 */
export const ELECTRICAL_SAFETY_MONITORING_CATEGORIES: ReadonlyArray<{
  key: ElectricalSafetyMonitoringCategoryKey;
  label: string;
}> = [
  {key: 'all', label: '全部'},
  {key: 'fire', label: '火灾监测'},
  {key: 'temperature', label: '温度监测'},
  {key: 'leakage', label: '漏电监测'},
  {key: 'power-quality', label: '电力质量'},
  {key: 'other', label: '其他监测'}
];

/** 顶部三张卡片沿用用户参考图中的指标名称、单位和示例数值。 */
export const ELECTRICAL_SAFETY_MONITORING_SUMMARY_CARDS: readonly ElectricalSafetyMonitoringSummaryCard[] = [
  {
    key: 'fire',
    title: '火灾预警监测',
    icon: 'warning_amber',
    metrics: [
      {label: '监测设备', value: '0', unit: '台', tone: 'neutral'},
      {label: '报警设备', value: '0', unit: '台', tone: 'critical'},
      {label: '平均温度', value: '25', unit: '℃', tone: 'neutral'},
      {label: '风险等级', value: '低', tone: 'healthy'}
    ]
  },
  {
    key: 'leakage',
    title: '漏电保护监测',
    icon: 'electrical_services',
    metrics: [
      {label: '保护器数', value: '0', unit: '个', tone: 'neutral'},
      {label: '动作次数', value: '0', unit: '次', tone: 'warning'},
      {label: '漏电电流', value: '0', unit: 'mA', tone: 'neutral'},
      {label: '误报率', value: '0', unit: '%', tone: 'neutral'}
    ]
  },
  {
    key: 'power-quality',
    title: '电力质量监测',
    icon: 'memory',
    metrics: [
      {label: '电压质量', value: '95', tone: 'healthy'},
      {label: '谐波含量', value: '3.5', unit: '%', tone: 'neutral'},
      {label: '功率因数', value: '0.92', tone: 'neutral'},
      {label: '质量等级', value: '优', tone: 'healthy'}
    ]
  }
];

/** 监测页只需要资产层级；静态原始树供当前 UI 页面演示根筛选和节点联动。 */
const ELECTRICAL_SAFETY_ASSET_TREE_ROOTS: SmartLightningMonitoringTreeResponseNode[] = [
  {
    id: 'electrical-project-a',
    name: '项目A',
    entityType: EntityType.ASSET,
    children: [
      {
        id: 'electrical-building-a',
        name: 'A栋',
        entityType: EntityType.ASSET,
        children: [
          {id: 'electrical-a-floor-1', name: '1楼', entityType: EntityType.ASSET, children: []},
          {id: 'electrical-a-floor-2', name: '2楼', entityType: EntityType.ASSET, children: []}
        ]
      },
      {
        id: 'electrical-building-b',
        name: 'B栋',
        entityType: EntityType.ASSET,
        children: [
          {id: 'electrical-b-floor-1', name: '1楼', entityType: EntityType.ASSET, children: []},
          {id: 'electrical-b-floor-2', name: '2楼', entityType: EntityType.ASSET, children: []}
        ]
      }
    ]
  }
];

/** 设备列表数据依照 thingsboard.yml 中 electrical_safety 的型号和功能分类构造。 */
export const ELECTRICAL_SAFETY_MONITORING_DEVICES: readonly ElectricalSafetyMonitoringDevice[] = [
  {
    id: 'electrical-device-esf-01', name: '电气火灾监测器-01', productName: 'ESF电气火灾监测模组',
    model: 'ESF1', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'electrical_fire', categoryKey: 'fire', categoryLabel: '火灾监测',
    assetId: 'electrical-a-floor-1', assetPath: '项目A / A栋 / 1楼', connection: 'online', condition: 'alarm',
    lastReportTime: '10:42:18', todayAlarmCount: 2, unprocessedAlarmCount: 1, dispatchedAlarmCount: 0, processedAlarmCount: 1
  },
  {
    id: 'electrical-device-est-02', name: '温度监测器-02', productName: 'EST温度监测模组（有线测温）',
    model: 'E1T2', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'temperature_monitor', categoryKey: 'temperature', categoryLabel: '温度监测',
    assetId: 'electrical-a-floor-1', assetPath: '项目A / A栋 / 1楼', connection: 'online', condition: 'normal',
    lastReportTime: '10:41:53', todayAlarmCount: 0, unprocessedAlarmCount: 0, dispatchedAlarmCount: 0, processedAlarmCount: 0
  },
  {
    id: 'electrical-device-esc-03', name: '漏电监测器-03', productName: 'ESC漏电监测模组',
    model: 'E2C3', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'leakage_monitor', categoryKey: 'leakage', categoryLabel: '漏电监测',
    assetId: 'electrical-a-floor-2', assetPath: '项目A / A栋 / 2楼', connection: 'online', condition: 'alarm',
    lastReportTime: '10:40:26', todayAlarmCount: 1, unprocessedAlarmCount: 0, dispatchedAlarmCount: 1, processedAlarmCount: 0
  },
  {
    id: 'electrical-device-ese-04', name: '电能质量监测器-04', productName: 'ESE电能质量监测器',
    model: 'E1E2', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'power_quality', categoryKey: 'power-quality', categoryLabel: '电力质量',
    assetId: 'electrical-a-floor-2', assetPath: '项目A / A栋 / 2楼', connection: 'online', condition: 'normal',
    lastReportTime: '10:39:11', todayAlarmCount: 1, unprocessedAlarmCount: 0, dispatchedAlarmCount: 0, processedAlarmCount: 1
  },
  {
    id: 'electrical-device-fap-05', name: '故障电弧探测器-05', productName: 'FAP故障电弧探测器（一体式）',
    model: 'FAP1', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'arc_fault', categoryKey: 'other', categoryLabel: '故障电弧',
    assetId: 'electrical-b-floor-1', assetPath: '项目A / B栋 / 1楼', connection: 'online', condition: 'alarm',
    lastReportTime: '10:38:44', todayAlarmCount: 1, unprocessedAlarmCount: 0, dispatchedAlarmCount: 1, processedAlarmCount: 0
  },
  {
    id: 'electrical-device-fecb-06', name: '智能断路器-06', productName: 'FECB智能断路器（4P）',
    model: 'CBP4', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'smart_breaker', categoryKey: 'other', categoryLabel: '智能断路器',
    assetId: 'electrical-b-floor-1', assetPath: '项目A / B栋 / 1楼', connection: 'offline', condition: 'normal',
    lastReportTime: '09:58:02', todayAlarmCount: 0, unprocessedAlarmCount: 0, dispatchedAlarmCount: 0, processedAlarmCount: 0
  },
  {
    id: 'electrical-device-fd-07', name: '市电监测模块-07', productName: 'FD市电监测模块',
    model: 'LDA1', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'mains_monitor', categoryKey: 'other', categoryLabel: '市电监测',
    assetId: 'electrical-b-floor-2', assetPath: '项目A / B栋 / 2楼', connection: 'online', condition: 'normal',
    lastReportTime: '10:37:09', todayAlarmCount: 0, unprocessedAlarmCount: 0, dispatchedAlarmCount: 0, processedAlarmCount: 0
  },
  {
    id: 'electrical-device-esb-08', name: '三相不平衡监测器-08', productName: 'ESB三相不平衡监测器',
    model: 'E1B2', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'phase_unbalance', categoryKey: 'other', categoryLabel: '三相不平衡',
    assetId: 'electrical-b-floor-2', assetPath: '项目A / B栋 / 2楼', connection: 'online', condition: 'normal',
    lastReportTime: '10:35:46', todayAlarmCount: 0, unprocessedAlarmCount: 0, dispatchedAlarmCount: 0, processedAlarmCount: 0
  },
  {
    id: 'electrical-device-esp-09', name: '零地电压监测器-09', productName: 'ESP零地电压监测器',
    model: 'E1P1', productCategory: ELECTRICAL_SAFETY_PRODUCT_CATEGORY,
    functionCategory: 'neutral_ground_voltage', categoryKey: 'other', categoryLabel: '零地电压',
    assetId: 'electrical-b-floor-2', assetPath: '项目A / B栋 / 2楼', connection: 'offline', condition: 'normal',
    lastReportTime: '09:51:34', todayAlarmCount: 0, unprocessedAlarmCount: 0, dispatchedAlarmCount: 0, processedAlarmCount: 0
  }
];

/** 页面设备数据入口按产品分类过滤，避免其他产品混入统计、列表或资产树范围。 */
export const ELECTRICAL_SAFETY_MONITORING_DEVICES_BY_PRODUCT_CATEGORY =
  ELECTRICAL_SAFETY_MONITORING_DEVICES.filter(
    (device) => device.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY
  );

/** 仅保留挂载了目标产品分类设备的资产及其祖先，作为共享资产树的数据范围。 */
const filterElectricalSafetyAssetTreeRoots = (
  nodes: readonly SmartLightningMonitoringTreeResponseNode[],
  deviceAssetIds: ReadonlySet<string>
): SmartLightningMonitoringTreeResponseNode[] => {
  const filteredNodes: SmartLightningMonitoringTreeResponseNode[] = [];
  nodes.forEach((node) => {
    const children = filterElectricalSafetyAssetTreeRoots(node.children || [], deviceAssetIds);
    if (deviceAssetIds.has(node.id) || children.length > 0) {
      filteredNodes.push({...node, children});
    }
  });
  return filteredNodes;
};

/** 共享资产树只接收目标产品分类设备所在的资产路径。 */
const electricalSafetyDeviceAssetIds = new Set(
  ELECTRICAL_SAFETY_MONITORING_DEVICES_BY_PRODUCT_CATEGORY.map((device) => device.assetId)
);
export const ELECTRICAL_SAFETY_MONITORING_ASSET_TREE: SmartLightningMonitoringAssetTree =
  buildSmartLightningMonitoringAssetTreeFromRoots(
    filterElectricalSafetyAssetTreeRoots(ELECTRICAL_SAFETY_ASSET_TREE_ROOTS, electricalSafetyDeviceAssetIds)
  );

/** 汇总当前资产子树中的设备连接状态与当日报警处理数量。 */
export function buildElectricalSafetyMonitoringScopeSummary(
  devices: readonly ElectricalSafetyMonitoringDevice[]
): ElectricalSafetyMonitoringScopeSummary {
  const filteredDevices = devices.filter(
    (device) => device.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY
  );
  return {
    totalDevices: filteredDevices.length,
    normalDevices: filteredDevices.filter((device) => device.condition === 'normal').length,
    onlineDevices: filteredDevices.filter((device) => device.connection === 'online').length,
    alarmDevices: filteredDevices.filter((device) => device.condition === 'alarm').length,
    offlineDevices: filteredDevices.filter((device) => device.connection === 'offline').length,
    todayAlarmCount: filteredDevices.reduce((total, device) => total + device.todayAlarmCount, 0),
    unprocessedAlarmCount: filteredDevices.reduce((total, device) => total + device.unprocessedAlarmCount, 0),
    dispatchedAlarmCount: filteredDevices.reduce((total, device) => total + device.dispatchedAlarmCount, 0),
    processedAlarmCount: filteredDevices.reduce((total, device) => total + device.processedAlarmCount, 0)
  };
}

/** 按所选资产、功能分类和设备关键字筛选本地展示数据。 */
export function filterElectricalSafetyMonitoringDevices(
  devices: readonly ElectricalSafetyMonitoringDevice[],
  selectedAssetIds: ReadonlySet<string>,
  category: ElectricalSafetyMonitoringCategoryKey,
  search: string
): ElectricalSafetyMonitoringDevice[] {
  const normalizedSearch = search.trim().toLocaleLowerCase();
  return devices.filter((device) => {
    const matchesProduct = device.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY;
    const matchesAsset = selectedAssetIds.has(device.assetId);
    const matchesCategory = category === 'all' || device.categoryKey === category;
    const searchableText = `${device.name} ${device.productName} ${device.model} ${device.assetPath}`;
    const matchesSearch = !normalizedSearch || searchableText.toLocaleLowerCase().includes(normalizedSearch);
    return matchesProduct && matchesAsset && matchesCategory && matchesSearch;
  });
}
