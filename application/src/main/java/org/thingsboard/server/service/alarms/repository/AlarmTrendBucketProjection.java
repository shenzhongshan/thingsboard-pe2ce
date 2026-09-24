/** 文件职责：承载告警趋势分桶查询的只读投影。 */
package org.thingsboard.server.service.alarms.repository;

/** 数据库返回一个趋势桶的下标及该桶内告警数量。 */
public interface AlarmTrendBucketProjection {

    Integer getBucketIndex();

    Long getAlarmCount();
}