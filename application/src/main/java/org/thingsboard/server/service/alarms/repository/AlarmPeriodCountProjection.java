/** 文件职责：承载告警自然周期聚合查询的只读投影。 */
package org.thingsboard.server.service.alarms.repository;

/**
 * 数据库一次返回今日/昨日、本月/上月、今年/去年的告警数量。
 * 使用接口投影而不是实体对象，避免为了六个计数值加载告警明细。
 */
public interface AlarmPeriodCountProjection {

    Long getDayCurrentCount();

    Long getDayPreviousCount();

    Long getMonthCurrentCount();

    Long getMonthPreviousCount();

    Long getYearCurrentCount();

    Long getYearPreviousCount();
}