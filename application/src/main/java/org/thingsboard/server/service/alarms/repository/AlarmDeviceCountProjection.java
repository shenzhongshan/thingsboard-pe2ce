/** 文件职责：JPA 按设备聚合告警的投影接口。 */
package org.thingsboard.server.service.alarms.repository;

import java.util.UUID;

/** 单设备告警总数、已清除数量和未派发活动告警数量的 SQL 投影。 */
public interface AlarmDeviceCountProjection {

    UUID getDeviceId();

    Long getAlarmCount();

    Long getHandledCount();

    /** 返回未清除且 assignee_id 为空的告警事件数。 */
    Long getUnassignedCount();
}
