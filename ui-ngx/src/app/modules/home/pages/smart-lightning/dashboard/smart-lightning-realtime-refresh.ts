/**
 * 文件职责：根据 WebSocket 返回的设备 ID，计算智能防雷看板需要刷新的监测模块。
 *
 * 未知设备或设备上下文尚未加载完成时返回全部模块，保证新增设备和首帧竞争条件下
 * 不会漏掉数据；已知设备只刷新其 functionCategory 对应模块。
 */

export type SmartLightningSummaryTarget = 'ground' | 'spd' | 'lightning';

export interface SmartLightningRefreshDevice {
  readonly deviceId: string;
  readonly functionCategory?: string;
}

const ALL_TARGETS: readonly SmartLightningSummaryTarget[] = ['ground', 'spd', 'lightning'];

/** 将实时变化设备映射为稳定、有序且去重后的监测模块列表。 */
export function getSmartLightningChangedSummaryTargets(
  devices: readonly SmartLightningRefreshDevice[],
  changedDeviceIds: ReadonlySet<string>
): SmartLightningSummaryTarget[] {
  if (!devices.length || !changedDeviceIds.size) {
    return [...ALL_TARGETS];
  }

  const devicesById = new Map<string, SmartLightningRefreshDevice>();
  for (const device of devices) {
    devicesById.set(device.deviceId, device);
  }

  const targets = new Set<SmartLightningSummaryTarget>();
  for (const deviceId of changedDeviceIds) {
    const device = devicesById.get(deviceId);
    if (!device) {
      return [...ALL_TARGETS];
    }
    const target = targetFromFunctionCategory(device.functionCategory);
    if (!target) {
      // 老数据缺少分类属性时无法安全判断所属模块，回退为全量刷新避免漏更新。
      return [...ALL_TARGETS];
    }
    targets.add(target);
  }

  return ALL_TARGETS.filter((target) => targets.has(target));
}

function targetFromFunctionCategory(functionCategory?: string): SmartLightningSummaryTarget | undefined {
  switch (functionCategory?.trim().toLowerCase()) {
    case 'ground_resistance':
      return 'ground';
    case 'spd_monitor':
      return 'spd';
    case 'lightning_current':
      return 'lightning';
    default:
      return undefined;
  }
}
