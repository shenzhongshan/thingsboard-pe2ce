package org.thingsboard.server.service.monitoring.dto;

import lombok.Data;

import java.util.UUID;

/** 设备基础信息与服务器属性合并后的批量查询结果。 */
@Data
public class SmartLightningDeviceContextDTO {

    private UUID deviceId;
    private String deviceName;
    private String deviceProfileName;
    private String productCategory;
    private String productName;
    private String functionCategory;
    private String deviceProtocol;
    private Boolean active;
    private Long lastActivityTime;
    private String gatewayIdentifier;
    private String meterAddress;
    private String circuitName;
    private String timeSetting;
}
