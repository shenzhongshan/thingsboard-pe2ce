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

/** 地图使用的有效经纬度；遥测和服务器属性必须作为完整坐标对处理。 */
export interface SmartLightningResolvedMapCoordinates {
  latitude: number;
  longitude: number;
}

/** 将接口中的数字或数字字符串统一转换为有限数值。 */
export const toSmartLightningMapCoordinate = (value: unknown): number | null => {
  if (value === null || value === undefined || (typeof value === 'string' && value.trim() === '')) {
    return null;
  }
  const numberValue = typeof value === 'number' ? value : Number(String(value).trim());
  return Number.isFinite(numberValue) ? numberValue : null;
};

/**
 * 解析设备地图坐标：先使用完整且有效的 GPS 遥测 Longitude/Latitude，
 * 遥测坐标缺失或无效时再回退到完整有效的 SERVER_SCOPE 经纬度属性。
 * 不允许把遥测中的单轴坐标与服务器属性中的另一轴拼接。
 */
export const resolveSmartLightningMapCoordinates = (
  serverLatitude: unknown,
  serverLongitude: unknown,
  telemetryLatitude?: unknown,
  telemetryLongitude?: unknown
): SmartLightningResolvedMapCoordinates | null => {
  if (isValidSmartLightningMapCoordinate(telemetryLatitude, telemetryLongitude)) {
    return {
      latitude: toSmartLightningMapCoordinate(telemetryLatitude) as number,
      longitude: toSmartLightningMapCoordinate(telemetryLongitude) as number
    };
  }
  if (isValidSmartLightningMapCoordinate(serverLatitude, serverLongitude)) {
    return {
      latitude: toSmartLightningMapCoordinate(serverLatitude) as number,
      longitude: toSmartLightningMapCoordinate(serverLongitude) as number
    };
  }
  return null;
};

/** 坐标详情最多保留五位小数，并按经纬度正负标注 N/S/E/W。 */
export const formatSmartLightningMapCoordinate = (
  value: unknown,
  axis: 'latitude' | 'longitude'
): string => {
  const coordinate = toSmartLightningMapCoordinate(value);
  if (coordinate === null) {
    return '--';
  }
  const direction = axis === 'latitude'
    ? (coordinate < 0 ? 'S' : 'N')
    : (coordinate < 0 ? 'W' : 'E');
  const roundedCoordinate = Number(Math.abs(coordinate).toFixed(5));
  return `${roundedCoordinate}°${direction}`;
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
