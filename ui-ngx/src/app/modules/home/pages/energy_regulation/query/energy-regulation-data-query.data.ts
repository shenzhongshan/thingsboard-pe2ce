/** 文件职责：集中定义用能数据查询页面的模拟设备模型与筛选/导出纯函数。 */
import {EntityType} from '@shared/models/entity-type.models';
import type {EnergyMonitoringAssetTree} from '../monitoring/energy-regulation-monitoring.data';

/** 查询页设备状态三态：在线（active）/ 离线（有活跃时间但未在线）/ 未连接（从未上报）。 */
export type EnergyQueryStatus = '在线' | '离线' | '未连接';

/** 查询页演示设备（模拟数据，真实接口接入后由后端上下文替换）。 */
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

/** 演示设备模板：按 thingsboard.yml 能源监管产品（SFE/ESE/ESA）组织。 */
export const ENERGY_QUERY_DEVICE_TEMPLATES: ReadonlyArray<Omit<EnergyQueryDevice, 'id' | 'assetId'>> = [
  {
    name: 'SFA电能质量监测器1', serialNo: 'KSDSFA7260901001', deviceType: 'SFE', protocol: 'MQTT',
    status: '在线', productName: 'SFE电能质量监测器', functionCategory: '电能质量', lastActivityTime: Date.now() - 3 * 60_000,
    updatedAt: '2026-10-10 17:12:20'
  },
  {
    name: 'SFA电能质量监测器2', serialNo: 'KSDSFA7251101001', deviceType: 'SFE', protocol: 'MQTT',
    status: '在线', productName: 'SFE电能质量监测器', functionCategory: '电能质量', lastActivityTime: Date.now() - 8 * 60_000,
    updatedAt: '2026-10-10 17:05:41'
  },
  {
    name: 'SFA电能质量监测器3', serialNo: 'KSDSFA2260928009', deviceType: 'SFE', protocol: 'MQTT',
    status: '未连接', productName: 'SFE电能质量监测器', functionCategory: '电能质量', lastActivityTime: null,
    updatedAt: '2026-10-08 09:31:15'
  },
  {
    name: '1号低压进线表', serialNo: 'KSDESA7260901002', deviceType: 'E1A2', protocol: 'MQTT',
    status: '在线', productName: 'ESA智能电表', functionCategory: '电能计量', lastActivityTime: Date.now() - 60_000,
    updatedAt: '2026-10-10 17:15:08'
  },
  {
    name: '2号低压进线表', serialNo: 'KSDESA7260901003', deviceType: 'E1A2', protocol: 'MQTT',
    status: '离线', productName: 'ESA智能电表', functionCategory: '电能计量', lastActivityTime: Date.now() - 26 * 3600_000,
    updatedAt: '2026-10-09 15:40:33'
  },
  {
    name: '空调主机回路监测器', serialNo: 'KSDESE7260901004', deviceType: 'E1E2', protocol: 'MQTT',
    status: '在线', productName: 'ESE电能质量监测器', functionCategory: '电能质量', lastActivityTime: Date.now() - 5 * 60_000,
    updatedAt: '2026-10-10 17:10:52'
  },
  {
    name: '动力设备回路监测器', serialNo: 'KSDESE7260901005', deviceType: 'E1E2', protocol: 'MQTT',
    status: '离线', productName: 'ESE电能质量监测器', functionCategory: '电能质量', lastActivityTime: Date.now() - 51 * 3600_000,
    updatedAt: '2026-10-08 20:16:47'
  }
];

/** 把演示设备按叶子资产均匀挂到真实资产树上（与用能监测页同一套生成约定）。 */
export function buildEnergyQueryDevices(tree: EnergyMonitoringAssetTree): EnergyQueryDevice[] {
  const leafAssets = tree.flatNodes.filter((node) => node.entityType === EntityType.ASSET &&
    !tree.nodes.get(node.id)?.childIds.some((childId) => tree.nodes.get(childId)?.entityType === EntityType.ASSET));
  if (leafAssets.length === 0) {
    return [];
  }
  return ENERGY_QUERY_DEVICE_TEMPLATES.map((template, index) => ({
    ...template,
    id: `energy-query-device-${index + 1}`,
    assetId: leafAssets[index % leafAssets.length].id
  }));
}

/** 设备模式行：由演示设备直接映射。 */
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

/** 应用全部筛选条件；返回顺序保持模板顺序（编号递增）。 */
export function filterEnergyQueryDevices(devices: EnergyQueryDevice[], filters: EnergyQueryFilters): EnergyQueryDevice[] {
  return devices.filter((device) => {
    if (filters.assetIds.size > 0 && !filters.assetIds.has(device.assetId)) {
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
