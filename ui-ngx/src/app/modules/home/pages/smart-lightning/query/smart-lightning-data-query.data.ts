import {
  getSmartLightningFunctionCategoryLabel,
  getSmartLightningMonitoringRootAssetIds,
  getSmartLightningMonitoringVisibleRoots
} from '../monitoring/smart-lightning-monitoring.data';

export type SmartLightningQueryStatus = '报警' | '离线' | '正常';

export interface SmartLightningQueryRow {
  id: string;
  deviceName: string;
  protocol: string;
  status: SmartLightningQueryStatus;
  alarmSeverity?: string;
  productName: string;
  deviceType: string;
  functionCategory: string;
  lastActivityTime: number | null;
  lastActivityText: string;
  assetPath: string;
}

export interface SmartLightningQueryDeviceSource {
  id: string;
  name: string;
  label?: string;
  deviceProfileName: string;
  protocol?: string;
  active?: boolean;
}

export interface SmartLightningQueryDeviceAttributes {
  active?: unknown;
  productName?: unknown;
  functionCategory?: unknown;
  lastActivityTime?: unknown;
  deviceProtocol?: unknown;
}

export interface SmartLightningQueryFilters {
  status?: '全部' | SmartLightningQueryStatus;
  productName?: string;
  functionCategory?: string;
  keyword?: string;
}

/** 查询页保留语义化别名，实际逻辑由智能防雷共享资产树数据层统一提供。 */
export const getSmartLightningQueryVisibleRoots = getSmartLightningMonitoringVisibleRoots;
export const getSmartLightningQueryRootAssetIds = getSmartLightningMonitoringRootAssetIds;

const textValue = (value: unknown): string => typeof value === 'string' && value.trim() ? value.trim() : '';

const formatProtocol = (value: unknown): string => {
  const protocol = textValue(value);
  return protocol === 'DEFAULT' ? '默认（支持基本 MQTT、HTTP 和 CoAP 传输）' : protocol || '--';
};

const timestampValue = (value: unknown): number | null => {
  const timestamp = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(timestamp) && timestamp > 0 ? timestamp : null;
};

const formatTime = (timestamp: number | null): string => {
  if (!timestamp) {
    return '--';
  }
  const date = new Date(timestamp);
  const pad = (part: number): string => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

/**
 * 把 ThingsBoard 设备实体、服务器属性和活动告警转换成数据查询表格行。
 * 未处理告警优先级最高：有告警显示“报警”，否则 active=true 显示“正常”，其余显示“离线”。
 */
export const buildSmartLightningQueryRows = (
  devices: SmartLightningQueryDeviceSource[],
  selectedDeviceIds: Set<string>,
  pathByDeviceId: Map<string, string>,
  attributesByDeviceId: Map<string, SmartLightningQueryDeviceAttributes>,
  activeAlarmDeviceIds: Set<string> = new Set(),
  alarmSeverityByDeviceId: Map<string, string> = new Map()
): SmartLightningQueryRow[] => devices
  .filter((device) => selectedDeviceIds.has(device.id))
  .map((device) => {
    const attributes = attributesByDeviceId.get(device.id) || {};
    const lastActivityTime = timestampValue(attributes.lastActivityTime);
    const activeValue = attributes.active;
    const active = activeValue === true || activeValue === 'true' ||
      (activeValue === undefined && device.active === true);
    const hasActiveAlarm = activeAlarmDeviceIds.has(device.id);
    const status: SmartLightningQueryStatus = hasActiveAlarm ? '报警' : active ? '正常' : '离线';
    return {
      id: device.id,
      deviceName: device.name,
      protocol: formatProtocol(attributes.deviceProtocol || device.protocol),
      status,
      alarmSeverity: alarmSeverityByDeviceId.get(device.id),
      productName: textValue(attributes.productName) || '未配置产品名称',
      deviceType: device.deviceProfileName || '--',
      functionCategory: getSmartLightningFunctionCategoryLabel(attributes.functionCategory),
      lastActivityTime,
      lastActivityText: formatTime(lastActivityTime),
      assetPath: pathByDeviceId.get(device.id) || device.name
    };
  })
  .sort((left, right) => left.deviceName.localeCompare(right.deviceName, 'zh-CN'));

/** 旧系统筛选栏均在已经选中的资产子树设备集合上执行。 */
export const filterSmartLightningQueryRows = (
  rows: SmartLightningQueryRow[],
  filters: SmartLightningQueryFilters
): SmartLightningQueryRow[] => {
  const keyword = (filters.keyword || '').trim().toLowerCase();
  return rows.filter((row) =>
    (!filters.status || filters.status === '全部' || row.status === filters.status) &&
    (!filters.productName || row.productName === filters.productName) &&
    (!filters.functionCategory || row.functionCategory === filters.functionCategory) &&
    (!keyword || `${row.deviceName} ${row.deviceType} ${row.productName} ${row.assetPath}`.toLowerCase().includes(keyword))
  );
};
