/** 文件职责：定义电气安全数据查询行模型，并提供本地筛选函数。 */
import {
  ElectricalSafetyMonitoringDevice,
  ELECTRICAL_SAFETY_PRODUCT_CATEGORY
} from '../monitoring/electrical-safety-monitoring.data';

/** 查询状态与防雷一致：活动告警优先，其次正常在线，最后离线。 */
export type ElectricalSafetyQueryStatus = '报警' | '离线' | '正常';

/** 表格展示字段；assetId 对应真实树中的设备叶节点 ID。 */
export interface ElectricalSafetyQueryRow {
  id: string;
  assetId: string;
  deviceName: string;
  deviceType: string;
  protocol: string;
  status: ElectricalSafetyQueryStatus;
  alarmSeverity?: string;
  productName: string;
  functionCategory: string;
  assetPath: string;
  lastActivityText: string;
}

/** 查询筛选条件与页面表单字段一一对应。 */
export interface ElectricalSafetyQueryFilters {
  status: '全部' | ElectricalSafetyQueryStatus;
  productName: string;
  functionCategory: string;
  keyword: string;
}

/** 默认协议沿用防雷查询的中文说明，表格和导出统一使用映射后的值。 */
function formatElectricalSafetyQueryProtocol(value: unknown): string {
  const protocol = typeof value === 'string' ? value.trim() : '';
  return protocol === 'DEFAULT' ? '默认（支持基本 MQTT、HTTP 和 CoAP 传输）' : protocol || '--';
}

/** 将共享监测设备模型映射为查询表格行，不重复读取或推断设备字段。 */
export function buildElectricalSafetyQueryRows(
  devices: readonly ElectricalSafetyMonitoringDevice[],
  activeAlarmDeviceIds: ReadonlySet<string> = new Set<string>(),
  alarmSeverityByDeviceId: ReadonlyMap<string, string> = new Map<string, string>()
): ElectricalSafetyQueryRow[] {
  const rows: ElectricalSafetyQueryRow[] = [];
  for (const device of devices) {
    if (device.productCategory !== ELECTRICAL_SAFETY_PRODUCT_CATEGORY) {
      continue;
    }
    rows.push({
      id: device.id,
      assetId: device.assetId,
      deviceName: device.name,
      deviceType: device.model,
      protocol: formatElectricalSafetyQueryProtocol(device.protocol),
      status: activeAlarmDeviceIds.has(device.id)
        ? '报警'
        : device.connection === 'online' ? '正常' : '离线',
      alarmSeverity: alarmSeverityByDeviceId.get(device.id),
      productName: device.productName,
      functionCategory: device.categoryLabel,
      assetPath: device.assetPath,
      lastActivityText: device.lastReportTime
    });
  }
  return rows;
}

/** 按资产范围、连接状态、产品、功能和关键词筛选设备行。 */
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
