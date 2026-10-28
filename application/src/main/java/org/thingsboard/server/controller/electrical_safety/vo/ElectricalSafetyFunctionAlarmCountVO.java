/** 文件职责：定义电气安全看板功能分类告警统计的 HTTP 响应结构。 */
package org.thingsboard.server.controller.electrical_safety.vo;

import lombok.Builder;
import lombok.Getter;

/** 面向前端的功能分类告警计数，不暴露数据库投影或聚合 BO。 */
@Getter
@Builder
public class ElectricalSafetyFunctionAlarmCountVO {

    /** 设备服务器属性中的规范功能分类值。 */
    private final String functionCategory;
    /** 统计时间范围内触发的告警事件条数。 */
    private final long alarmCount;
    /** 周期内尚未清除的告警事件数。 */
    private final long pendingCount;
    /** 周期内尚未清除且没有受理人的告警事件数。 */
    private final long unassignedCount;
    /** 周期内已清除的告警事件数。 */
    private final long processedCount;
}
