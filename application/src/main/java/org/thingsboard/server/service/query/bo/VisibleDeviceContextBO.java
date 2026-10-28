/** 文件职责：承载共享设备查询服务返回的设备基础信息、属性和运行状态。 */
package org.thingsboard.server.service.query.bo;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 通用设备查询上下文，仅供服务层跨业务传递，不作为 HTTP 响应直接返回。
 * 在线状态取最新 active 遥测；缺少该遥测时按离线处理。
 */
@Getter
@Builder
public class VisibleDeviceContextBO {

    /** 平台设备 ID，用于后续批量遥测和资产关系查询。 */
    private final UUID deviceId;
    /** 设备显示名称。 */
    private final String deviceName;
    /** 设备配置文件名称。 */
    private final String deviceProfileName;
    /** SERVER_SCOPE 中的产品大类，用于业务模块分类。 */
    private final String productCategory;
    /** SERVER_SCOPE 中的 productName 展示值。 */
    private final String productName;
    /** SERVER_SCOPE 中的功能分类。 */
    private final String functionCategory;
    /** device_data 中 transportConfiguration.type 的协议标识。 */
    private final String deviceProtocol;
    /** 最新 active 遥测转换后的在线状态；缺失时按离线处理。 */
    private final Boolean active;
    /** SERVER_SCOPE 的 lastActivityTime 属性解析值；缺失或非法时为空。 */
    private final Long lastActivityTime;
    /** 网关标识，优先读取 gatewayIdentifier，缺失时兼容 gatewayId。 */
    private final String gatewayIdentifier;
    /** SERVER_SCOPE 的 meterAddress 属性值。 */
    private final String meterAddress;
    /** SERVER_SCOPE 的 circuitName 属性值。 */
    private final String circuitName;
    /** SERVER_SCOPE 的 timeSetting 属性值。 */
    private final String timeSetting;
    /** SERVER_SCOPE 中的纬度坐标；未配置时为空。 */
    private final Double latitude;
    /** SERVER_SCOPE 中的经度坐标；未配置时为空。 */
    private final Double longitude;
}
