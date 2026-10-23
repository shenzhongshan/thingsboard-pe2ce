/** 文件职责：能源监管设备上下文 HTTP 响应对象。 */
package org.thingsboard.server.controller.energy_regulation.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 仅包含能源监管看板展示与地图后续接入需要的设备字段。 */
@Getter
@Builder
public class EnergyRegulationDeviceContextVO {

    /** 设备实体 ID。 */
    private final UUID deviceId;
    /** 设备名称。 */
    private final String deviceName;
    /** ThingsBoard 设备档案名称。 */
    private final String deviceProfileName;
    /** 产品大类服务器属性。 */
    private final String productCategory;
    /** 产品中文名服务器属性。 */
    private final String productName;
    /** SERVER_SCOPE功能分类，不按设备名称推断。 */
    private final String functionCategory;
    /** 设备配置中的实际传输协议。 */
    private final String deviceProtocol;
    /** 设备最近活跃时间，毫秒；缺失时为空。 */
    private final Long lastActivityTime;
    /** 在线状态：最新 active 遥测为真时在线。 */
    private final Boolean active;
    /** 有效纬度；缺失或无效时为 null。 */
    private final Double latitude;
    /** 有效经度；缺失或无效时为 null。 */
    private final Double longitude;
}
