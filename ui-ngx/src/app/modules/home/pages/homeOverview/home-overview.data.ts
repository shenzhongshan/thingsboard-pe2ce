/**
 * 文件职责：首页概览页的数据模型、状态计算和展示格式化。
 * 这里只放纯函数，避免把业务请求和页面渲染逻辑混在一起。
 */

export interface HomeOverviewDevice {
  id: string;
  name: string;
  productName: string;
  profileName: string;
  active: boolean;
  latitude?: number;
  longitude?: number;
  assetPath: string;
  alarmNames: string[];
}

export interface HomeOverviewStats {
  assetTotal: number;
  deviceTotal: number;
  onlineTotal: number;
  offlineTotal: number;
  alarmDeviceTotal: number;
  unconfiguredTotal: number;
}

export interface HomeOverviewDistributionItem {
  name: string;
  value: number;
  color: string;
}

export const HOME_OVERVIEW_COLORS = [
  '#2bbec5', '#8e73d1', '#4d9de0', '#ffad73', '#d96b77', '#9a82c7', '#f0cc12', '#82ad4d'
];

export function createEmptyHomeOverviewStats(): HomeOverviewStats {
  return {assetTotal: 0, deviceTotal: 0, onlineTotal: 0, offlineTotal: 0, alarmDeviceTotal: 0, unconfiguredTotal: 0};
}

export function buildDeviceDistribution(devices: HomeOverviewDevice[]): HomeOverviewDistributionItem[] {
  const totals = new Map<string, number>();
  for (const device of devices) {
    // 数量统计按产品中文名聚合，不按设备配置编码或型号聚合。
    const name = device.productName || '未配置产品名称';
    totals.set(name, (totals.get(name) || 0) + 1);
  }
  return Array.from(totals.entries())
    .sort((left, right) => right[1] - left[1])
    .map(([name, value], index) => ({name, value, color: HOME_OVERVIEW_COLORS[index % HOME_OVERVIEW_COLORS.length]}));
}

export function buildDonutStyle(value: number, total: number, color: string): Record<string, string> {
  const percent = total > 0 ? Math.round(value * 100 / total) : 0;
  return {background: `conic-gradient(${color} ${percent}%, #e6edf3 ${percent}% 100%)`};
}

export function parseNumber(value: unknown): number | undefined {
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export function formatCoordinate(value: number | undefined): string {
  return value === undefined ? '--' : value.toFixed(6);
}
