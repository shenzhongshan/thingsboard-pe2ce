/** 文件职责：承载电气安全设备按功能分类聚合后的周期告警次数。 */
package org.thingsboard.server.service.electrical_safety.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 电气安全服务层的分类告警和处置状态统计，不依赖其他业务模块 DTO。 */
@Getter
@RequiredArgsConstructor
public class ElectricalSafetyFunctionAlarmCountDTO {

    /** 设备 SERVER_SCOPE 中的规范 functionCategory 值。 */
    private final String functionCategory;
    /** 在统计时间范围内触发的告警条数，按告警事件计数而非设备数。 */
    private final long alarmCount;
    /** 周期内尚未清除的告警事件数，表示待处理告警。 */
    private final long pendingCount;
    /** 周期内尚未清除且没有受理人的告警事件数；这是待处理数量的子集。 */
    private final long unassignedCount;
    /** 周期内已清除的告警事件数；不等同于外部维修工单的完成记录。 */
    private final long processedCount;
}
