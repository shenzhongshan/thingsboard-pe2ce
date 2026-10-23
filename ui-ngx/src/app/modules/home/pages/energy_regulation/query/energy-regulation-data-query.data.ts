/** 文件职责：集中定义用能数据查询页面的真实设备模型与筛选/导出纯函数。 */
import type {EnergyRegulationDeviceContext} from '../energy-regulation-api.service';
import type {EnergyMonitoringAssetTree} from '../monitoring/energy-regulation-monitoring.data';

/** 查询页设备状态三态：在线（active）/ 离线（有活跃时间但未在线）/ 未连接（从未上报）。 */
export type EnergyQueryStatus = '在线' | '离线' | '未连接';

/** 查询页设备，由能源监管设备上下文和共享最新遥测映射。 */
export interface EnergyQueryDevice {
  id: string;
  name: string;
  serialNo: string;
  deviceType: string;
  protocol: string;
  status: EnergyQueryStatus;
  productName: string;
  functionCategory: string;
  assetId: string;
  lastActivityTime: number | null;
  updatedAt: string;
}

/** 设备模式表格行。 */
export interface EnergyQueryDeviceRow {
  id: string;
  deviceName: string;
  serialNo: string;
  deviceType: string;
  protocol: string;
  status: EnergyQueryStatus;
  productName: string;
}

/** 查询条件；时段按设备最近活跃时间过滤。 */
export interface EnergyQueryFilters {
  status: '全部' | EnergyQueryStatus;
  deviceType: string;
  functionCategory: string;
  keyword: string;
  startTime: string;
  endTime: string;
  assetIds: ReadonlySet<string>;
}

/** 保留原导出契约但不再提供演示设备，真实数据通过上下文传入。 */
export const ENERGY_QUERY_DEVICE_TEMPLATES: ReadonlyArray<Omit<EnergyQueryDevice, 'id' | 'assetId'>> = [];

/** 当前用户可见能源设备和平台实体ID一一对应，不生成虚拟设备或挂载关系。 */
export function buildEnergyQueryDevices(tree: EnergyMonitoringAssetTree, contexts: readonly EnergyRegulationDeviceContext[] = [],
  latest: (id: string) => ReadonlyMap<string, {ts: number; value: string}> = () => new Map()): EnergyQueryDevice[] {
  return contexts.map((context) => {
    const values = latest(context.deviceId); const active = values.get('active')?.value;
    const online = active === undefined ? context.active === true : ['true','1'].includes(String(active).toLowerCase());
    const lastActivityTime = Math.max(context.lastActivityTime || 0, ...Array.from(values.values(), (sample) => sample.ts)) || null;
    return {id: context.deviceId, name: context.deviceName, serialNo: context.deviceName,
      deviceType: context.deviceProfileName || '__', protocol: context.deviceProtocol || '__',
      status: online ? '在线' as const : lastActivityTime ? '离线' as const : '未连接' as const,
      productName: context.productName || '__', functionCategory: context.functionCategory || '__',
      assetId: context.deviceId, lastActivityTime, updatedAt: lastActivityTime ? new Date(lastActivityTime).toLocaleString() : '__'};
  });
}

/** 设备模式行：由真实设备直接映射。 */
export function buildEnergyQueryDeviceRows(devices: EnergyQueryDevice[]): EnergyQueryDeviceRow[] {
  return devices.map((device) => ({
    id: device.id,
    deviceName: device.name,
    serialNo: device.serialNo,
    deviceType: device.deviceType,
    protocol: device.protocol,
    status: device.status,
    productName: device.productName
  }));
}

/** 关键字同时匹配设备名称、设备编号、设备类型与产品名称。 */
function matchesKeyword(device: EnergyQueryDevice, keyword: string): boolean {
  const normalized = keyword.trim().toLocaleLowerCase();
  if (!normalized) {
    return true;
  }
  return [device.name, device.serialNo, device.deviceType, device.productName]
    .some((value) => value.toLocaleLowerCase().includes(normalized));
}

/** 将 yyyy-MM-dd 转为本地时区当天零点，可按自然日偏移以构造右开边界。 */
function getLocalDateStart(date: string, dayOffset = 0): number {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day + dayOffset).getTime();
}

/** 时段过滤按设备最近活跃时间；结束日期使用次日零点作为右开边界。 */
function matchesTimeRange(device: EnergyQueryDevice, startTime: string, endTime: string): boolean {
  if (!startTime && !endTime) {
    return true;
  }
  if (device.lastActivityTime == null) {
    return false;
  }
  const start = startTime ? getLocalDateStart(startTime) : -Infinity;
  const endExclusive = endTime ? getLocalDateStart(endTime, 1) : Infinity;
  return device.lastActivityTime >= start && device.lastActivityTime < endExclusive;
}

/** 应用全部筛选条件；保留模块设备上下文的返回顺序。 */
export function filterEnergyQueryDevices(devices: EnergyQueryDevice[], filters: EnergyQueryFilters): EnergyQueryDevice[] {
  return devices.filter((device) => {
    if (!filters.assetIds.has(device.assetId)) {
      return false;
    }
    if (filters.status !== '全部' && device.status !== filters.status) {
      return false;
    }
    if (filters.deviceType && device.deviceType !== filters.deviceType) {
      return false;
    }
    if (filters.functionCategory && device.functionCategory !== filters.functionCategory) {
      return false;
    }
    return matchesKeyword(device, filters.keyword) && matchesTimeRange(device, filters.startTime, filters.endTime);
  });
}

/** 筛选下拉的可选项：设备类型与功能分类各自去重。 */
export function getEnergyQueryDeviceTypes(devices: EnergyQueryDevice[]): string[] {
  return [...new Set(devices.map((device) => device.deviceType))].sort();
}

export function getEnergyQueryFunctionCategories(devices: EnergyQueryDevice[]): string[] {
  return [...new Set(devices.map((device) => device.functionCategory))].sort();
}

/** 通用 CSV 导出：首行 BOM 头保证 Excel 打开中文不乱码（与防雷查询页同一约定）。 */
export function buildEnergyQueryCsv(lines: string[][]): Blob {
  const escape = (value: string): string => `"${value.replace(/"/g, '""')}"`;
  const content = lines.map((line) => line.map(escape).join(',')).join('\r\n');
  return new Blob([`﻿${content}`], {type: 'text/csv;charset=utf-8'});
}

/** 设备模式导出行。 */
export function buildEnergyQueryDeviceCsv(devices: EnergyQueryDevice[]): string[][] {
  return [
    ['序号', '设备名称', '设备编号', '设备类型', '设备协议', '状态', '产品名称', '功能分类'],
    ...devices.map((device, index) => [
      String(index + 1), device.name, device.serialNo, device.deviceType,
      device.protocol, device.status, device.productName, device.functionCategory
    ])
  ];
}
