/** 文件职责：定义电气安全数据查询页的行模型、产品分类范围和本地筛选函数。 */
import {
  ELECTRICAL_SAFETY_MONITORING_DEVICES,
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY
} from '../monitoring/electrical-safety-monitoring.data';

/** 表格按告警优先、离线其次的顺序展示设备运行状态。 */
export type ElectricalSafetyQueryStatus = '报警' | '离线' | '正常';

/** 数据查询表格展示字段；资产编号用于将查询结果限制在左侧树选中范围。 */
export interface ElectricalSafetyQueryRow {
  id: string;
  assetId: string;
  deviceName: string;
  deviceType: string;
  protocol: string;
  status: ElectricalSafetyQueryStatus;
  productName: string;
  functionCategory: string;
  assetPath: string;
  lastActivityText: string;
}

/** 查询筛选条件与防雷数据查询页保持一致。 */
export interface ElectricalSafetyQueryFilters {
  status: '全部' | ElectricalSafetyQueryStatus;
  productName: string;
  functionCategory: string;
  keyword: string;
}

/**
 * 复用电气安全监测页面的设备展示数据，保证同一设备在模块页面中的名称和状态一致。
 * 协议字段暂无对应的页面数据，按通用查询页约定显示“--”，不推断设备协议。
 */
export const ELECTRICAL_SAFETY_QUERY_ROWS: readonly ElectricalSafetyQueryRow[] =
  ELECTRICAL_SAFETY_MONITORING_DEVICES
    .filter((device) => device.productCategory === ELECTRICAL_SAFETY_PRODUCT_CATEGORY)
    .map((device) => ({
      id: device.id,
      assetId: device.assetId,
      deviceName: device.name,
      deviceType: device.model,
      protocol: '--',
      status: device.condition === 'alarm'
        ? '报警'
        : device.connection === 'offline' ? '离线' : '正常',
      productName: device.productName,
      functionCategory: device.categoryLabel,
      assetPath: device.assetPath,
      lastActivityText: device.lastReportTime
    }));

/**
 * 在资产树选中范围、状态、产品、功能类型和关键词范围内筛选查询行。
 * 产品分类在数据源入口已限制为 electrical_safety，不与其他模块设备混用。
 */
export function filterElectricalSafetyQueryRows(
  rows: readonly ElectricalSafetyQueryRow[],
  selectedAssetIds: ReadonlySet<string>,
  filters: ElectricalSafetyQueryFilters
): ElectricalSafetyQueryRow[] {
  const keyword = filters.keyword.trim().toLocaleLowerCase();
  return rows
    .filter((row) => selectedAssetIds.has(row.assetId))
    .filter((row) => filters.status === '全部' || row.status === filters.status)
    .filter((row) => !filters.productName || row.productName === filters.productName)
    .filter((row) => !filters.functionCategory || row.functionCategory === filters.functionCategory)
    .filter((row) => !keyword ||
      `${row.deviceName} ${row.deviceType} ${row.productName} ${row.assetPath}`
        .toLocaleLowerCase()
        .includes(keyword))
    .sort((left, right) => left.deviceName.localeCompare(right.deviceName, 'zh-CN'));
}
