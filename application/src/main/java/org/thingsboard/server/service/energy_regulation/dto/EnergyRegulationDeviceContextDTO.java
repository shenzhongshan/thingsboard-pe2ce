/** 文件职责：能源监管服务层传输当前用户可见设备所需的数据。 */
package org.thingsboard.server.service.energy_regulation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** 能源监管设备上下文 DTO，不依赖智能防雷模块的响应对象。 */
@Getter
@Builder
public class EnergyRegulationDeviceContextDTO {

    /** 设备实体 ID。 */
    private final UUID deviceId;
    /** 设备名称。 */
    private final String deviceName;
    /** ThingsBoard 设备档案名称，用于看板设备分布统计。 */
    private final String deviceProfileName;
    /** 产品大类服务器属性；能源监管设备值为 energy_regulation。 */
    private final String productCategory;
    /** 产品中文名服务器属性。 */
    private final String productName;
    /** SERVER_SCOPE功能分类，不按设备名称推断。 */
    private final String functionCategory;
    /** 设备配置中的实际传输协议。 */
    private final String deviceProtocol;
    /** 设备最近活跃时间，毫秒；缺失时为空。 */
    private final Long lastActivityTime;
    /** 在线状态：取最新 active 遥测；缺少该遥测时按离线处理。 */
    private final Boolean active;
    /** 纬度服务器属性；无有效坐标时为 null。 */
    private final Double latitude;
    /** 经度服务器属性；无有效坐标时为 null。 */
    private final Double longitude;
}
