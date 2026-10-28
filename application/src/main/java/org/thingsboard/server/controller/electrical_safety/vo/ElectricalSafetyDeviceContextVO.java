/** 文件职责：定义电气安全设备上下文接口的 HTTP 响应字段。 */
package org.thingsboard.server.controller.electrical_safety.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 电气安全页面所需的设备基础资料，不包含告警统计或设备测点值。 */
@Getter
@Builder
public class ElectricalSafetyDeviceContextVO {

    /** ThingsBoard 设备实体 ID。 */
    private final UUID deviceId;
    /** 设备实体名称。 */
    private final String deviceName;
    /** ThingsBoard 设备配置名称，作为设备型号展示来源。 */
    private final String deviceProfileName;
    /** SERVER_SCOPE 产品分类；该接口只返回 electrical_safety。 */
    private final String productCategory;
    /** SERVER_SCOPE 产品名称。 */
    private final String productName;
    /** SERVER_SCOPE 功能分类。 */
    private final String functionCategory;
    /** 设备传输配置中的协议标识。 */
    private final String deviceProtocol;
    /** 最新 active 遥测状态；true 在线，false 离线。 */
    private final Boolean active;
    /** 最近活跃时间，Unix 毫秒时间戳；缺失时为 null。 */
    private final Long lastActivityTime;
    /** SERVER_SCOPE 中配置的纬度；缺失时为 null。 */
    private final Double latitude;
    /** SERVER_SCOPE 中配置的经度；缺失时为 null。 */
    private final Double longitude;
}
