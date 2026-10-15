/** 文件职责：承载电气安全看板所需的活动告警设备和周期告警统计。 */
package org.thingsboard.server.service.electrical_safety.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/** 电气安全看板告警统计的服务层结果，不包含页面样式或其他模块对象。 */
@Getter
@RequiredArgsConstructor
public class ElectricalSafetyDashboardAlarmStatisticsDTO {

    /** 当前仍有未清除告警的设备 ID；集合范围已经按当前用户权限和产品分类收敛。 */
    private final List<UUID> activeAlarmDeviceIds;
    /** 统计周期内按规范功能分类聚合的告警事件数量。 */
    private final List<ElectricalSafetyFunctionAlarmCountDTO> functionCategoryCounts;
}
