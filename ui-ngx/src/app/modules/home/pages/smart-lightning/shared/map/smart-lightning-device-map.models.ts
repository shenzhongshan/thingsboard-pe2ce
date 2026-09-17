/**
 * 文件职责：地图组件接收的通用设备模型。
 * 智能防雷看板和平台首页都转换为该结构，因此地图只负责绘制，不参与设备分类业务。
 */
export interface SmartLightningMapDevice {
  id: string;
  name: string;
  productName: string;
  profileName: string;
  /** 产品大类，用于页面级筛选；地图组件本身不规定业务分类。 */
  productCategory?: string;
  active: boolean;
  latitude?: number;
  longitude?: number;
  assetPath?: string;
  alarmNames?: string[];
}

export interface SmartLightningMapDraftPoint {
  latitude: number;
  longitude: number;
}
