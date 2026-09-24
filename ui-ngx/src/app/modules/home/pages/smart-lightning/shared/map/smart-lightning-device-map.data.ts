/** 文件职责：公共设备地图的数据模型、坐标校验和候选设备过滤纯函数。 */

/** 地图点位所需的最小设备上下文，坐标来自设备 SERVER_SCOPE 服务器属性。 */
export interface SmartLightningMapDeviceContext {
  /** 设备实体 ID，用于保存属性和区分地图图层。 */
  deviceId: string;
  /** 设备实体名称，用于点位详情展示。 */
  deviceName: string;
  /** 设备服务器属性 productName，用于点位详情展示。 */
  productName?: string;
  /** 设备服务器属性 latitude；接口兼容返回数字或数字字符串。 */
  latitude?: unknown;
  /** 设备服务器属性 longitude；接口兼容返回数字或数字字符串。 */
  longitude?: unknown;
}

/** 将接口中的数字或数字字符串统一转换为有限数值。 */
export const toSmartLightningMapCoordinate = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
};

/** 判断经纬度是否落在地理坐标允许范围内。 */
export const isValidSmartLightningMapCoordinate = (latitude: unknown, longitude: unknown): boolean => {
  const normalizedLatitude = toSmartLightningMapCoordinate(latitude);
  const normalizedLongitude = toSmartLightningMapCoordinate(longitude);
  return normalizedLatitude !== null && normalizedLongitude !== null &&
    normalizedLatitude >= -90 && normalizedLatitude <= 90 &&
    normalizedLongitude >= -180 && normalizedLongitude <= 180;
};

/**
 * 过滤地图点位下拉框候选设备。
 *
 * 已有点位的设备 ID 由地图页面在完成设备上下文加载后传入，
 * 公共函数只做集合过滤，不重新请求设备或判断业务分类。
 */
export const filterSmartLightningMapDevicesWithoutPoints = <T extends SmartLightningMapDeviceContext>(
  devices: readonly T[],
  boundDeviceIds: ReadonlySet<string>
): T[] => devices.filter((device) => !boundDeviceIds.has(device.deviceId));