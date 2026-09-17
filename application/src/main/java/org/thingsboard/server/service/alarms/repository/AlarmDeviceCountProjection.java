/** 文件职责：JPA 按设备聚合告警的投影接口。 */
package org.thingsboard.server.service.alarms.repository;

import java.util.UUID;

/** 单设备告警总数与已处理数量的 SQL 投影。 */
public interface AlarmDeviceCountProjection {

    UUID getDeviceId();

    Long getAlarmCount();

    Long getHandledCount();
}
