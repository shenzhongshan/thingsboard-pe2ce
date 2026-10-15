/** 文件职责：承载电气安全模块对当前用户可见设备的基础信息。 */
package org.thingsboard.server.service.electrical_safety.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 电气安全服务层设备上下文，不依赖防雷或能源监管模块的数据对象。 */
@Getter
@Builder
public class ElectricalSafetyDeviceContextDTO {

    /** ThingsBoard 设备实体 ID，用于和电气安全资产树中的设备叶节点关联。 */
    private final UUID deviceId;
    /** 设备实体名称，按平台设备名称原样返回。 */
    private final String deviceName;
    /** ThingsBoard 设备配置名称，供页面展示设备型号。 */
    private final String deviceProfileName;
    /** SERVER_SCOPE 中的产品大类；本模块只返回 electrical_safety。 */
    private final String productCategory;
    /** SERVER_SCOPE 中配置的产品中文名称。 */
    private final String productName;
    /** SERVER_SCOPE 中配置的功能分类，不根据设备名称推断。 */
    private final String functionCategory;
    /** 设备传输配置中的协议标识。 */
    private final String deviceProtocol;
    /** 最新 active 遥测状态；仅 true 表示在线，缺失时按离线返回 false。 */
    private final Boolean active;
    /** 最近活跃时间，单位为 Unix 毫秒时间戳；无有效值时为空。 */
    private final Long lastActivityTime;
    /** SERVER_SCOPE 中配置的纬度；无有效坐标时为空，地图不应绘制该设备。 */
    private final Double latitude;
    /** SERVER_SCOPE 中配置的经度；无有效坐标时为空，地图不应绘制该设备。 */
    private final Double longitude;
}
