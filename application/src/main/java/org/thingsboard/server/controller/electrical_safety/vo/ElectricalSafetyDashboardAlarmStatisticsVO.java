/** 文件职责：定义电气安全看板活动告警和周期告警统计的 HTTP 响应结构。 */
package org.thingsboard.server.controller.electrical_safety.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/** 电气安全看板告警统计接口响应，不包含其他业务模块的内部对象。 */
@Getter
@Builder
public class ElectricalSafetyDashboardAlarmStatisticsVO {

    /** 当前仍有未清除告警的电气安全设备 ID。 */
    private final List<UUID> activeAlarmDeviceIds;
    /** 当前所选周期内按设备功能分类汇总的告警次数。 */
    private final List<ElectricalSafetyFunctionAlarmCountVO> functionCategoryCounts;
}
