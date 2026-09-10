/** 文件职责：看板模块的 smart-lightning-map.data，定义数据模型、接口结果映射与纯函数。 */
/** 地图点位所需的最小设备上下文，坐标来自设备 SERVER_SCOPE 服务器属性。 */
export interface SmartLightningMapDeviceContext {
  /** 设备实体 ID，用于保存属性和区分地图图层。 */
  deviceId: string;
  /** 设备实体名称，用于点位弹窗展示。 */
  deviceName: string;
  /** 产品名称来自设备服务器属性 productName，用于点位弹窗展示。 */
  productName?: string;
  /** 设备服务器属性 latitude；接口兼容返回数字或数字字符串。 */
  latitude?: unknown;
  /** 设备服务器属性 longitude；接口兼容返回数字或数字字符串。 */
  longitude?: unknown;
}

/** 已通过合法性校验、可直接交给 Leaflet 渲染的设备点位。 */
export interface SmartLightningMapPoint {
  /** 设备实体 ID，也是持久化图层集合的唯一键。 */
  deviceId: string;
  /** 点位弹窗显示的设备名称。 */
  deviceName: string;
  /** 点位弹窗显示的产品名称。 */
  productName?: string;
  /** 合法范围为 -90 至 90。 */
  latitude: number;
  /** 合法范围为 -180 至 180。 */
  longitude: number;
}

/**
 * 将批量设备上下文转换成可渲染的地图点位。
 *
 * 只有同时存在有效纬度和经度的设备才会显示，避免一个坐标缺失时把点
 * 渲染到地图默认位置。接口返回数字或数字字符串都兼容，超出经纬度
 * 合法范围的数据直接丢弃。
 */
export const buildSmartLightningMapPoints = (
  devices: readonly SmartLightningMapDeviceContext[]
): SmartLightningMapPoint[] => {
  const points: SmartLightningMapPoint[] = [];
  for (const device of devices) {
    const latitude = toFiniteNumber(device.latitude);
    const longitude = toFiniteNumber(device.longitude);
    if (latitude === null || longitude === null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      continue;
    }
    const point: SmartLightningMapPoint = {
      deviceId: device.deviceId,
      deviceName: device.deviceName,
      latitude,
      longitude
    };
    // 没有 productName 时不写入 undefined 字段，保持旧数据结构兼容。
    if (device.productName?.trim()) {
      point.productName = device.productName.trim();
    }
    points.push(point);
  }
  return points;
};

/**
 * 过滤地图点位下拉框候选设备。
 *
 * <p>绑定判断以已经绘制到地图上的点位 ID 为准，而不是再次根据设备上下文
 * 的经纬度字段推断。这样可以避免接口返回坐标但地图尚未绘制时，把全部设备
 * 错误地过滤掉。</p>
 */
export const filterSmartLightningMapDevicesWithoutPoints = <T extends SmartLightningMapDeviceContext>(
  devices: readonly T[],
  boundDeviceIds: ReadonlySet<string>
): T[] => devices.filter((device) => !boundDeviceIds.has(device.deviceId));

/** 将接口中的数字或数字字符串统一转成有限数值，无效输入返回 null。 */
const toFiniteNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
};
