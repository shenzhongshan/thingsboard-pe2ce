/** 文件职责：看板模块的 smart-lightning-map.config，集中定义页面配置常量。 */
/**
 * 天地图矢量图层配置，与旧版智能防雷看板中已配置的图层保持一致。
 * 在地图配置纳入租户级管理之前，该页面级配置随页面源码维护。
 */
export const smartLightningTianDiTuMapConfig = {
  center: [22.5, 114.06] as [number, number],
  zoom: 12,
  token: 'ac35254b753bad84adad8f6443d5f045'
};
