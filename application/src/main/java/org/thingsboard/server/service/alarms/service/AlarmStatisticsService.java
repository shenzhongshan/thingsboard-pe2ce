/** 文件职责：告警 JPA 聚合查询的业务适配接口。 */
package org.thingsboard.server.service.alarms.service;

import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmDeviceCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.bo.AlarmPeriodCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmTrendBucketBO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 将权限范围和 JPA 聚合结果转换为告警服务内部结果。 */
public interface AlarmStatisticsService {

    List<AlarmDimensionCountBO> countByType(SecurityUser user, Set<UUID> deviceIds,
                                             Long startTs, Long endTs, boolean activeOnly);

    List<AlarmDimensionCountBO> countBySeverity(SecurityUser user, Set<UUID> deviceIds,
                                                  Long startTs, Long endTs, boolean activeOnly);

    /** 按设备返回周期告警总数和已处理数量，供末级资产统计使用。 */
    List<AlarmDeviceCountBO> countByDevice(SecurityUser user, Set<UUID> deviceIds,
                                            Long startTs, Long endTs);

    /** 查询当前权限范围内仍有未清除告警的设备 ID，结果已由数据库去重。 */
    Set<UUID> findActiveAlarmDeviceIds(SecurityUser user, Set<UUID> deviceIds);

    /** 按告警触发时间一次返回日、月、年当前及上一周期的数量。 */
    AlarmPeriodCountBO countPeriodSummary(SecurityUser user, Set<UUID> deviceIds,
                                          Long dayStart, Long previousDayStart,
                                          Long monthStart, Long previousMonthStart,
                                          Long yearStart, Long previousYearStart, Long now);

    /** 按告警触发时间分桶返回趋势数量，bucketUnit 为 hour、day 或 month。 */
    List<AlarmTrendBucketBO> countTrendBuckets(SecurityUser user, Set<UUID> deviceIds,
                                               Long startTs, Long endTs,
                                               String bucketUnit, String zoneId);
    AlarmOverviewAggregateBO getOverview(SecurityUser user, Set<UUID> deviceIds,
                                           Long startTs, Long endTs);
}
