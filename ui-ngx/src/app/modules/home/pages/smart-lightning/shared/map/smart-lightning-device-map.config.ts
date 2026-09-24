/** 文件职责：公共设备地图的天地图图层、初始中心和缩放级别配置。 */

/**
 * 天地图矢量和注记图层使用的租户地图配置。
 * 业务页面只负责筛选设备，地图组件统一复用这份底图配置。
 */
export const smartLightningTianDiTuMapConfig = {
  center: [22.5, 114.06] as [number, number],
  zoom: 12,
  token: 'ac35254b753bad84adad8f6443d5f045'
};