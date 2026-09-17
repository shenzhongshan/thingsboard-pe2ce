/** 文件职责：保存监测服务内部使用的设备业务上下文，不作为 HTTP 响应直接返回。 */
package org.thingsboard.server.service.monitoring.bo;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 设备基础信息、服务器属性和运行状态合并后的内部模型。
 *
 * <p>该类型只供监测服务的筛选、统计和遥测查询使用。接口层需要返回设备信息时，
 * 统一通过 Converter 转换为 SmartLightningDeviceContextDTO，再由 Controller 转换为 VO，
 * 避免内部业务对象成为外部契约。</p>
 */
@Getter
@Builder
public class MonitoringDeviceContextBO {

    private final UUID deviceId;
    private final String deviceName;
    private final String deviceProfileName;
    private final String productCategory;
    private final String productName;
    private final String functionCategory;
    private final String deviceProtocol;
    private final Boolean active;
    private final Long lastActivityTime;
    private final String gatewayIdentifier;
    private final String meterAddress;
    private final String circuitName;
    private final String timeSetting;
    private final Double latitude;
    private final Double longitude;
}
