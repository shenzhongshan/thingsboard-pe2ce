/**
 * 文件职责：首页概览页的数据模型、分类统计和展示格式化。
 * 这里只放纯函数，避免把业务请求和页面渲染逻辑混在一起。
 */

export interface HomeOverviewDevice {
  id: string;
  name: string;
  productName: string;
  profileName: string;
  /** 一级产品分类：lightning、electrical_safety、energy_regulation。 */
  productCategory?: string;
  /** 二级功能分类，例如 spd_monitor、power_quality。 */
  functionCategory?: string;
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
  key: string;
  name: string;
  value: number;
  color: string;
}

/** 首页环形图中可独立命中的单个扇区。 */
export interface HomeOverviewDonutSegmentInput {
  id: string;
  label: string;
  value: number;
  color: string;
}

/** 环形扇区的 SVG 路径、角度范围及悬浮时的径向位移。 */
export interface HomeOverviewDonutSegment extends HomeOverviewDonutSegmentInput {
  startAngle: number;
  endAngle: number;
  path: string;
  offsetX: number;
  offsetY: number;
}

/** 当前悬浮扇区的可读信息，比例按所在图表的统计总量计算。 */
export interface HomeOverviewDonutSegmentInfo {
  label: string;
  value: number;
  percentage: number;
}

export interface HomeOverviewCategoryOption {
  key: string;
  label: string;
}

export const HOME_OVERVIEW_COLORS = [
  '#2bbec5', '#8e73d1', '#4d9de0', '#ffad73', '#d96b77', '#9a82c7', '#f0cc12', '#82ad4d'
];

export const HOME_OVERVIEW_DONUT_VIEWBOX_SIZE = 200;
export const HOME_OVERVIEW_DONUT_OUTER_RADIUS = 99;
export const HOME_OVERVIEW_DISTRIBUTION_DONUT_INNER_RADIUS = 51;
export const HOME_OVERVIEW_STATUS_DONUT_INNER_RADIUS = 76;
const HOME_OVERVIEW_DONUT_SLICE_OFFSET = 6;

/** 首页使用的三个一级分类，顺序与产品分类配置保持一致。 */
export const HOME_OVERVIEW_CATEGORY_OPTIONS: HomeOverviewCategoryOption[] = [
  {key: 'lightning', label: '防雷'},
  {key: 'electrical_safety', label: '用电安全'},
  {key: 'energy_regulation', label: '能源监管'}
];

/** 二级功能分类的中文展示名称；键值仍使用后端稳定编码。 */
export const HOME_OVERVIEW_FUNCTION_LABELS: Record<string, string> = {
  spd_monitor: 'SPD监测',
  ground_resistance: '接地电阻监测',
  lightning_current: '雷电流监测',
  arc_fault: '故障电弧监测',
  electrical_fire: '电气火灾监测',
  temperature_monitor: '温度监测',
  leakage_monitor: '漏电监测',
  input_monitor: '输入状态监测',
  smart_breaker: '智能断路器',
  mains_monitor: '市电监测',
  safe_electricity: '安全用电监测',
  power_quality: '电能质量监测',
  phase_unbalance: '三相不平衡监测',
  energy_meter: '电能计量'
};

export function createEmptyHomeOverviewStats(): HomeOverviewStats {
  return {assetTotal: 0, deviceTotal: 0, onlineTotal: 0, offlineTotal: 0, alarmDeviceTotal: 0, unconfiguredTotal: 0};
}

/**
 * 将 WebSocket 的 active 原始值解析为在线状态。
 * 只接受系统约定的布尔值及 0/1 表达，未知值返回 undefined，避免把脏值误判为离线。
 */
export function parseHomeOverviewActiveValue(value: unknown): boolean | undefined {
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'number') {
    return value === 1 ? true : value === 0 ? false : undefined;
  }
  if (typeof value !== 'string') {
    return undefined;
  }

  switch (value.trim().toLowerCase()) {
    case 'true':
    case '1':
    case '1.0':
      return true;
    case 'false':
    case '0':
    case '0.0':
      return false;
    default:
      return undefined;
  }
}

/**
 * 只更新本次实时推送涉及的设备 active 字段；未变化的对象保持原引用，
 * 使首页后续分类、坐标和告警数据不会被遥测增量覆盖。
 */
export function applyHomeOverviewActiveUpdates(
  devices: HomeOverviewDevice[],
  activeByDeviceId: ReadonlyMap<string, boolean>
): HomeOverviewDevice[] {
  if (!activeByDeviceId.size) {
    return devices;
  }

  let changed = false;
  const updatedDevices = devices.map((device) => {
    const active = activeByDeviceId.get(device.id);
    if (active === undefined || active === device.active) {
      return device;
    }
    changed = true;
    return {...device, active};
  });

  return changed ? updatedDevices : devices;
}

/** 按一级 productCategory 统计设备数量。 */
export function buildCategoryDistribution(devices: HomeOverviewDevice[]): HomeOverviewDistributionItem[] {
  const totals = new Map<string, number>();
  for (const device of devices) {
    const key = normalizeCategory(device.productCategory);
    totals.set(key, (totals.get(key) || 0) + 1);
  }
  return Array.from(totals.entries())
    .sort((left, right) => right[1] - left[1])
    .map(([key, value], index) => ({
      key,
      name: categoryLabel(key),
      value,
      color: HOME_OVERVIEW_COLORS[index % HOME_OVERVIEW_COLORS.length]
    }));
}

/** 在选中的一级分类下，按二级 functionCategory 统计设备数量。 */
export function buildFunctionDistribution(devices: HomeOverviewDevice[], category?: string): HomeOverviewDistributionItem[] {
  const totals = new Map<string, number>();
  for (const device of devices) {
    if (category && normalizeCategory(device.productCategory) !== category) {
      continue;
    }
    const key = normalizeHomeOverviewFunctionCategory(device.productCategory, device.functionCategory);
    totals.set(key, (totals.get(key) || 0) + 1);
  }
  return Array.from(totals.entries())
    .sort((left, right) => right[1] - left[1])
    .map(([key, value], index) => ({
      key,
      name: HOME_OVERVIEW_FUNCTION_LABELS[key] || (key === 'unclassified' ? '未分类' : key),
      value,
      color: HOME_OVERVIEW_COLORS[index % HOME_OVERVIEW_COLORS.length]
    }));
}

/**
 * 首页在电气安全范围内合并已存在的旧二级分类，保证未批量迁移的设备也按四类统计。
 * 其他产品分类沿用其原始 functionCategory，避免影响防雷和能源监管统计。
 */
export function normalizeHomeOverviewFunctionCategory(productCategory: unknown, functionCategory: unknown): string {
  const normalizedProductCategory = normalizeCategory(productCategory);
  const normalizedFunctionCategory = typeof functionCategory === 'string'
    ? functionCategory.trim().toLowerCase()
    : '';
  if (normalizedProductCategory !== 'electrical_safety') {
    return normalizedFunctionCategory || 'unclassified';
  }

  switch (normalizedFunctionCategory) {
    case 'electrical_fire':
    case 'temperature_monitor':
    case 'leakage_monitor':
      return 'electrical_fire';
    case 'safe_electricity':
    case 'input_monitor':
    case 'smart_breaker':
    case 'mains_monitor':
    case 'neutral_ground_voltage':
      return 'safe_electricity';
    case 'power_quality':
    case 'phase_unbalance':
      return 'power_quality';
    case 'arc_fault':
      return 'arc_fault';
    default:
      return normalizedFunctionCategory || 'unclassified';
  }
}

export function normalizeCategory(value: unknown): string {
  return typeof value === 'string' && value.trim() ? value.trim().toLowerCase() : 'unclassified';
}

export function categoryLabel(value: string): string {
  return HOME_OVERVIEW_CATEGORY_OPTIONS.find((item) => item.key === value)?.label || '未分类';
}

/**
 * 根据数据占比生成独立 SVG 环形扇区，支持只突出鼠标所在的那一段。
 * 角度从正上方开始顺时针计算；输入值按非负数处理，超出总量的部分不会绘制到圆环范围外。
 */
export function buildDonutSegments(
  items: HomeOverviewDonutSegmentInput[],
  total: number,
  outerRadius: number,
  innerRadius: number
): HomeOverviewDonutSegment[] {
  if (!Number.isFinite(total) || total <= 0 || outerRadius <= innerRadius || innerRadius < 0) {
    return [];
  }

  const segments: HomeOverviewDonutSegment[] = [];
  let startAngle = 0;

  for (const item of items) {
    const value = Number.isFinite(item.value) ? Math.max(item.value, 0) : 0;
    const sweepAngle = Math.min(value * 360 / total, 360 - startAngle);
    if (sweepAngle <= 0) {
      continue;
    }

    const endAngle = startAngle + sweepAngle;
    const middleAngleRadians = ((startAngle + endAngle) / 2 - 90) * Math.PI / 180;
    segments.push({
      ...item,
      value,
      startAngle,
      endAngle,
      path: buildDonutSegmentPath(startAngle, endAngle, outerRadius, innerRadius),
      offsetX: Math.cos(middleAngleRadians) * HOME_OVERVIEW_DONUT_SLICE_OFFSET,
      offsetY: Math.sin(middleAngleRadians) * HOME_OVERVIEW_DONUT_SLICE_OFFSET
    });
    startAngle = endAngle;
  }

  return segments;
}

/** 根据扇区和图表总量生成中心提示内容；无效总量时不显示比例信息。 */
export function buildDonutSegmentInfo(
  segment: HomeOverviewDonutSegment | undefined,
  total: number
): HomeOverviewDonutSegmentInfo | null {
  if (!segment || !Number.isFinite(total) || total <= 0) {
    return null;
  }

  return {
    label: segment.label,
    value: segment.value,
    percentage: Math.min(segment.value, total) * 100 / total
  };
}

/** 根据指针位置返回扇区；已突出扇区会额外覆盖动画过渡区域，避免外缘悬浮闪断。 */
export function findDonutSegmentAtPoint(
  x: number,
  y: number,
  segments: HomeOverviewDonutSegment[],
  innerRadius: number,
  outerRadius: number,
  activeSegmentId?: string
): string | null {
  if (!Number.isFinite(x) || !Number.isFinite(y)) {
    return null;
  }

  const stationaryHit = findDonutSegmentAtBasePoint(x, y, segments, innerRadius, outerRadius);
  if (stationaryHit) {
    return stationaryHit;
  }

  const activeSegment = segments.find((segment) => segment.id === activeSegmentId);
  if (!activeSegment) {
    return null;
  }

  // CSS 正在把扇区从原位移动到突出位置时，反向检查几个过渡位置，保持指针命中稳定。
  for (const progress of [1, 0.75, 0.5, 0.25]) {
    const candidate = findDonutSegmentAtBasePoint(
      x - activeSegment.offsetX * progress,
      y - activeSegment.offsetY * progress,
      segments,
      innerRadius,
      outerRadius
    );
    if (candidate === activeSegment.id) {
      return candidate;
    }
  }

  return null;
}

/** 按未变形的圆环几何计算命中扇区，供悬浮检测复用。 */
function findDonutSegmentAtBasePoint(
  x: number,
  y: number,
  segments: HomeOverviewDonutSegment[],
  innerRadius: number,
  outerRadius: number
): string | null {
  const center = HOME_OVERVIEW_DONUT_VIEWBOX_SIZE / 2;
  const offsetX = x - center;
  const offsetY = y - center;
  const radius = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
  if (radius < innerRadius || radius > outerRadius) {
    return null;
  }

  const angle = (Math.atan2(offsetY, offsetX) * 180 / Math.PI + 90 + 360) % 360;
  return segments.find((segment) => angle >= segment.startAngle && angle < segment.endAngle)?.id || null;
}

/** 将一个角度范围转换为环形扇区路径；整圆用两段半圆弧避免 SVG 无法绘制 360 度单弧。 */
function buildDonutSegmentPath(startAngle: number, endAngle: number, outerRadius: number, innerRadius: number): string {
  const sweepAngle = endAngle - startAngle;
  if (sweepAngle >= 359.999) {
    const outerTop = formatSvgNumber(100 - outerRadius);
    const outerBottom = formatSvgNumber(100 + outerRadius);
    const innerTop = formatSvgNumber(100 - innerRadius);
    const innerBottom = formatSvgNumber(100 + innerRadius);
    return `M 100 ${outerTop} A ${outerRadius} ${outerRadius} 0 0 1 100 ${outerBottom} ` +
      `A ${outerRadius} ${outerRadius} 0 0 1 100 ${outerTop} L 100 ${innerTop} ` +
      `A ${innerRadius} ${innerRadius} 0 0 0 100 ${innerBottom} ` +
      `A ${innerRadius} ${innerRadius} 0 0 0 100 ${innerTop} Z`;
  }

  const outerStart = pointOnCircle(startAngle, outerRadius);
  const outerEnd = pointOnCircle(endAngle, outerRadius);
  const innerEnd = pointOnCircle(endAngle, innerRadius);
  const innerStart = pointOnCircle(startAngle, innerRadius);
  const largeArc = sweepAngle > 180 ? 1 : 0;

  return `M ${outerStart.x} ${outerStart.y} A ${outerRadius} ${outerRadius} 0 ${largeArc} 1 ${outerEnd.x} ${outerEnd.y} ` +
    `L ${innerEnd.x} ${innerEnd.y} A ${innerRadius} ${innerRadius} 0 ${largeArc} 0 ${innerStart.x} ${innerStart.y} Z`;
}

/** 计算从正上方开始顺时针指定角度的圆周坐标。 */
function pointOnCircle(angle: number, radius: number): {x: string; y: string} {
  const radians = (angle - 90) * Math.PI / 180;
  return {
    x: formatSvgNumber(100 + radius * Math.cos(radians)),
    y: formatSvgNumber(100 + radius * Math.sin(radians))
  };
}

/** 统一 SVG 坐标精度，避免输出过长的小数。 */
function formatSvgNumber(value: number): string {
  return Number(value.toFixed(3)).toString();
}

export function parseNumber(value: unknown): number | undefined {
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

/** 将接口用 null 表示的缺失坐标转换为地图模型使用的 undefined。 */
export function normalizeCoordinate(value: number | null | undefined): number | undefined {
  return value === null || value === undefined ? undefined : value;
}

/** 设备必须同时具备纬度和经度才算已配置坐标；仅缺一项也计入“未配置”。 */
export function countDevicesWithoutCoordinates(
  devices: Array<Pick<HomeOverviewDevice, 'latitude' | 'longitude'>>
): number {
  return devices.filter((device) => device.latitude === undefined || device.longitude === undefined).length;
}

export function formatCoordinate(value: number | undefined): string {
  return value === undefined ? '--' : `${value}`;
}
