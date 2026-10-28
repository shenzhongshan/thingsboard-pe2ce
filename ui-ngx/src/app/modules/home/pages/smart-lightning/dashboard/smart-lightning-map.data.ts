/** 文件职责：旧 dashboard 地图数据文件的兼容导出；实际地图模型和过滤逻辑统一位于 shared/map。 */
export {
  filterSmartLightningMapDevicesWithoutPoints
} from '../shared/map/smart-lightning-device-map.data';
export type { SmartLightningMapDeviceContext } from '../shared/map/smart-lightning-device-map.data';