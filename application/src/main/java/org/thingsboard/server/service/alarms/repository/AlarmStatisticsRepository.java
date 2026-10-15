/**
 * 文件职责：告警统计的 JPA 聚合查询定义。
 *
 * <p>查询直接使用 ThingsBoard 已有的 alarm_info 视图，
 * 不读取 AlarmInfo 明细集合，聚合工作由数据库完成。</p>
 */
package org.thingsboard.server.service.alarms.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AlarmInfoEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AlarmStatisticsRepository extends Repository<AlarmInfoEntity, UUID> {

    /*
     * originator_type=5 是 ThingsBoard EntityType.DEVICE 的数据库编码，
     * 这里仅用于 SQL 过滤设备告警，不改变原生枚举或实体映射。
     */

    /** 设备告警类型聚合；activeOnly=true 时只统计未清除告警。 */
    @Query(value = "SELECT COALESCE(a.type, '未分类') AS dimension, "
            + "COUNT(*) AS \"alarmCount\" "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND (:startTs IS NULL OR a.start_ts >= :startTs) "
            + "AND (:endTs IS NULL OR a.start_ts < :endTs) "
            + "AND (:activeOnly = FALSE OR a.cleared = FALSE) "
            + "GROUP BY COALESCE(a.type, '未分类') "
            + "ORDER BY \"alarmCount\" DESC, dimension ASC",
            nativeQuery = true)
    List<AlarmDimensionProjection> countByType(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds,
            @Param("startTs") Long startTs,
            @Param("endTs") Long endTs,
            @Param("activeOnly") boolean activeOnly);

    /** 设备告警严重程度聚合；结果按数量降序返回。 */
    @Query(value = "SELECT COALESCE(a.severity, 'INDETERMINATE') AS dimension, "
            + "COUNT(*) AS \"alarmCount\" "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND (:startTs IS NULL OR a.start_ts >= :startTs) "
            + "AND (:endTs IS NULL OR a.start_ts < :endTs) "
            + "AND (:activeOnly = FALSE OR a.cleared = FALSE) "
            + "GROUP BY COALESCE(a.severity, 'INDETERMINATE') "
            + "ORDER BY \"alarmCount\" DESC, dimension ASC",
            nativeQuery = true)
    List<AlarmDimensionProjection> countBySeverity(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds,
            @Param("startTs") Long startTs,
            @Param("endTs") Long endTs,
            @Param("activeOnly") boolean activeOnly);

    /**
     * 按设备一次聚合周期内告警总数、已清除数和未清除且未分配数。
     * 未分配数通过原生 assignee_id 字段计算，避免把 assign_ts 误当成当前受理状态。
     */
    @Query(value = "SELECT a.originator_id AS \"deviceId\", "
            + "COUNT(*) AS \"alarmCount\", "
            + "COALESCE(SUM(CASE WHEN a.cleared = TRUE THEN 1 ELSE 0 END), 0) AS \"handledCount\", "
            + "COALESCE(SUM(CASE WHEN a.cleared = FALSE AND a.assignee_id IS NULL "
            + "THEN 1 ELSE 0 END), 0) AS \"unassignedCount\" "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND (:startTs IS NULL OR a.start_ts >= :startTs) "
            + "AND (:endTs IS NULL OR a.start_ts < :endTs) "
            + "GROUP BY a.originator_id",
            nativeQuery = true)
    List<AlarmDeviceCountProjection> countByDevice(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds,
            @Param("startTs") Long startTs,
            @Param("endTs") Long endTs);

    /** 返回指定可见设备中仍有未清除告警的设备 ID，数据库直接去重。 */
    @Query(value = "SELECT DISTINCT a.originator_id "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND a.cleared = FALSE",
            nativeQuery = true)
    List<UUID> findActiveAlarmDeviceIds(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds);

    /**
     * 一次聚合返回告警分析页的六个自然周期计数。
     *
     * <p>查询按 alarm_info.start_ts（告警触发时间）过滤，避免使用告警入库时间
     * created_time 造成跨周期统计偏差。六个 CASE 聚合由数据库完成，应用层不再
     * 为汇总数字读取和遍历告警明细。</p>
     */
    @Query(value = "SELECT "
            + "COALESCE(SUM(CASE WHEN a.start_ts >= :dayStart AND a.start_ts < :now "
            + "THEN 1 ELSE 0 END), 0) AS \"dayCurrentCount\", "
            + "COALESCE(SUM(CASE WHEN a.start_ts >= :previousDayStart AND a.start_ts < :dayStart "
            + "THEN 1 ELSE 0 END), 0) AS \"dayPreviousCount\", "
            + "COALESCE(SUM(CASE WHEN a.start_ts >= :monthStart AND a.start_ts < :now "
            + "THEN 1 ELSE 0 END), 0) AS \"monthCurrentCount\", "
            + "COALESCE(SUM(CASE WHEN a.start_ts >= :previousMonthStart AND a.start_ts < :monthStart "
            + "THEN 1 ELSE 0 END), 0) AS \"monthPreviousCount\", "
            + "COALESCE(SUM(CASE WHEN a.start_ts >= :yearStart AND a.start_ts < :now "
            + "THEN 1 ELSE 0 END), 0) AS \"yearCurrentCount\", "
            + "COALESCE(SUM(CASE WHEN a.start_ts >= :previousYearStart AND a.start_ts < :yearStart "
            + "THEN 1 ELSE 0 END), 0) AS \"yearPreviousCount\" "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND a.start_ts >= :previousYearStart "
            + "AND a.start_ts < :now",
            nativeQuery = true)
    AlarmPeriodCountProjection countPeriodSummary(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds,
            @Param("dayStart") Long dayStart,
            @Param("previousDayStart") Long previousDayStart,
            @Param("monthStart") Long monthStart,
            @Param("previousMonthStart") Long previousMonthStart,
            @Param("yearStart") Long yearStart,
            @Param("previousYearStart") Long previousYearStart,
            @Param("now") Long now);

    /**
     * 按告警触发时间分桶统计趋势数量。
     *
     * <p>bucketUnit 只允许 hour、day、month 三个内部常量，分别对应日趋势的小时、
     * 月趋势的日期和年趋势的月份。timezone 函数保证分桶使用应用传入的系统时区。</p>
     */
    @Query(value = "SELECT CAST(CASE "
            + "WHEN :bucketUnit = 'hour' THEN EXTRACT(HOUR FROM timezone(:zoneId, to_timestamp(a.start_ts / 1000.0))) "
            + "WHEN :bucketUnit = 'day' THEN EXTRACT(DAY FROM timezone(:zoneId, to_timestamp(a.start_ts / 1000.0))) - 1 "
            + "ELSE EXTRACT(MONTH FROM timezone(:zoneId, to_timestamp(a.start_ts / 1000.0))) - 1 "
            + "END AS INTEGER) AS \"bucketIndex\", "
            + "COUNT(*) AS \"alarmCount\" "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND a.start_ts >= :startTs "
            + "AND a.start_ts < :endTs "
            + "GROUP BY 1 "
            + "ORDER BY 1",
            nativeQuery = true)
    List<AlarmTrendBucketProjection> countTrendBuckets(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds,
            @Param("startTs") Long startTs,
            @Param("endTs") Long endTs,
            @Param("bucketUnit") String bucketUnit,
            @Param("zoneId") String zoneId);

    /** 返回指定周期内的状态分布、告警设备数和未清除告警设备数。 */
    @Query(value = "SELECT "
            + "COALESCE(SUM(CASE WHEN a.status = 'ACTIVE_UNACK' THEN 1 ELSE 0 END), 0) AS \"activeUnackCount\", "
            + "COALESCE(SUM(CASE WHEN a.status = 'ACTIVE_ACK' THEN 1 ELSE 0 END), 0) AS \"activeAckCount\", "
            + "COALESCE(SUM(CASE WHEN a.status = 'CLEARED_UNACK' THEN 1 ELSE 0 END), 0) AS \"clearedUnackCount\", "
            + "COALESCE(SUM(CASE WHEN a.status = 'CLEARED_ACK' THEN 1 ELSE 0 END), 0) AS \"clearedAckCount\", "
            + "COUNT(DISTINCT a.originator_id) AS \"alarmedDeviceCount\", "
            + "COUNT(DISTINCT CASE WHEN a.cleared = FALSE THEN a.originator_id END) AS \"activeAlarmDeviceCountBO\" "
            + "FROM alarm_info a "
            + "WHERE a.tenant_id = :tenantId "
            + "AND a.originator_type = 5 "
            + "AND a.originator_id IN (:deviceIds) "
            + "AND (:startTs IS NULL OR a.start_ts >= :startTs) "
            + "AND (:endTs IS NULL OR a.start_ts < :endTs)",
            nativeQuery = true)
    AlarmOverviewProjection getOverview(
            @Param("tenantId") UUID tenantId,
            @Param("deviceIds") Collection<UUID> deviceIds,
            @Param("startTs") Long startTs,
            @Param("endTs") Long endTs);
}
