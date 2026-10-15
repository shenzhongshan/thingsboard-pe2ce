/** 文件职责：保存数据库按设备聚合后的告警统计结果。 */
package org.thingsboard.server.service.alarms.bo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** 服务内部使用的单设备告警周期计数，不作为 HTTP DTO 返回。 */
@Getter
@RequiredArgsConstructor
public class AlarmDeviceCountBO {

    private final UUID deviceId;
    /** 所选周期内触发的告警事件总数。 */
    private final long alarmCount;
    /** 所选周期内已经清除的告警事件数。 */
    private final long handledCount;
    /** 所选周期内仍未清除且尚未分配受理人的告警事件数。 */
    private final long unassignedCount;
}
