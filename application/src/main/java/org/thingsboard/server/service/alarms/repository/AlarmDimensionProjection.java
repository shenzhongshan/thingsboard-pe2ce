/** 文件职责：JPA 告警维度聚合查询的投影接口。 */
package org.thingsboard.server.service.alarms.repository;

/** 告警类型或严重程度的聚合行。 */
public interface AlarmDimensionProjection {

    String getDimension();

    Long getAlarmCount();
}
