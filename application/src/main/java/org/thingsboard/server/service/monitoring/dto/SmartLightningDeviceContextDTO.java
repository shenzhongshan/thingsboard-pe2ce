package org.thingsboard.server.service.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 设备基础信息与服务器属性合并后的服务层传输对象。
 *
 * <p>监测服务内部计算使用 MonitoringDeviceContextBO，二者由
 * MonitoringDeviceDTOConverter 集中转换；Controller 再将 DTO 转换为 VO。</p>
 */
@Getter
@Builder
public class SmartLightningDeviceContextDTO {

    /** 设备实体 ID。 */
    private final UUID deviceId;
    /** 设备名称（实体名）。 */
    private final String deviceName;
    /** 设备配置（型号）名称，如 DESA、G1A3；未配置时回退为设备 type。 */
    private final String deviceProfileName;
    /** 产品大类服务器属性：lightning=防雷、electric=电气安全。 */
    private final String productCategory;
    /** 产品中文名服务器属性，由 DeviceFunctionCategoryInitializer 创建设备时写入。 */
    private final String productName;
    /** 功能细分服务器属性：spd_monitor/ground_resistance/lightning_current/arc_fault。 */
    private final String functionCategory;
    /** 传输协议类型，来自 device_data.transportConfiguration.type（如 DEFAULT、MQTT）。 */
    private final String deviceProtocol;
    /** 在线状态：取最新 active 遥测（true/1）；缺少该遥测时按离线处理。 */
    private final Boolean active;
    /** 最近活跃时间（毫秒时间戳）服务器属性；无有效值时为 null。 */
    private final Long lastActivityTime;
    /** 网关识别号服务器属性；为空时回退读取 gatewayId。 */
    private final String gatewayIdentifier;
    /** 仪表地址服务器属性。 */
    private final String meterAddress;
    /** 回路名称服务器属性。 */
    private final String circuitName;
    /** 时段设置服务器属性。 */
    private final String timeSetting;
    /** 地图纬度，来自设备 SERVER_SCOPE 的 latitude 服务器属性。 */
    private final Double latitude;
    /** 地图经度，来自设备 SERVER_SCOPE 的 longitude 服务器属性。 */
    private final Double longitude;
}
