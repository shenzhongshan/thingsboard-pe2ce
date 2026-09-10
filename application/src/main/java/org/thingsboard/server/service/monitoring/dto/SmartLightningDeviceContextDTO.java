package org.thingsboard.server.service.monitoring.dto;

import lombok.Data;

import java.util.UUID;

/** 设备基础信息与服务器属性合并后的批量查询结果。 */
@Data
public class SmartLightningDeviceContextDTO {

    /** 设备实体 ID。 */
    private UUID deviceId;
    /** 设备名称（实体名）。 */
    private String deviceName;
    /** 设备配置（型号）名称，如 DESA、G1A3；未配置时回退为设备 type。 */
    private String deviceProfileName;
    /** 产品大类服务器属性：lightning=防雷、electric=电气安全。 */
    private String productCategory;
    /** 产品中文名服务器属性，由 DeviceFunctionCategoryInitializer 创建设备时写入。 */
    private String productName;
    /** 功能细分服务器属性：spd_monitor/ground_resistance/lightning_current/arc_fault。 */
    private String functionCategory;
    /** 传输协议类型，来自 device_data.transportConfiguration.type（如 DEFAULT、MQTT）。 */
    private String deviceProtocol;
    /** 在线状态：优先取 SERVER_SCOPE 的 active（true/1），无该属性时回退设备实体的 active。 */
    private Boolean active;
    /** 最近活跃时间（毫秒时间戳）服务器属性；无有效值时为 null。 */
    private Long lastActivityTime;
    /** 网关识别号服务器属性；为空时回退读取 gatewayId。 */
    private String gatewayIdentifier;
    /** 仪表地址服务器属性。 */
    private String meterAddress;
    /** 回路名称服务器属性。 */
    private String circuitName;
    /** 时段设置服务器属性。 */
    private String timeSetting;
    /** 地图纬度，来自设备 SERVER_SCOPE 的 latitude 服务器属性。 */
    private Double latitude;
    /** 地图经度，来自设备 SERVER_SCOPE 的 longitude 服务器属性。 */
    private Double longitude;
}
