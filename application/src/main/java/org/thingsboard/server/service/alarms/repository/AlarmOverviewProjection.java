/** 文件职责：JPA 告警状态和设备覆盖数聚合查询的投影接口。 */
package org.thingsboard.server.service.alarms.repository;

/** 状态分布 SQL 单行投影。 */
public interface AlarmOverviewProjection {

    Long getActiveUnackCount();

    Long getActiveAckCount();

    Long getClearedUnackCount();

    Long getClearedAckCount();

    Long getAlarmedDeviceCount();

    Long getActiveAlarmDeviceCountBO();
}
