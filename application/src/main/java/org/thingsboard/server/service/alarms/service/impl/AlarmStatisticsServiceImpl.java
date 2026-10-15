/**
 * 文件职责：告警统计 JPA 聚合结果适配实现。
 *
 * <p>这里不再读取告警明细，也不承担设备权限查询；权限范围由调用方先解析为
 * 当前用户可见的设备 ID 集合，本类只负责调用 Repository 并转换投影。</p>
 */
package org.thingsboard.server.service.alarms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmDeviceCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.bo.AlarmPeriodCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmTrendBucketBO;
import org.thingsboard.server.service.alarms.repository.AlarmDimensionProjection;
import org.thingsboard.server.service.alarms.repository.AlarmDeviceCountProjection;
import org.thingsboard.server.service.alarms.repository.AlarmOverviewProjection;
import org.thingsboard.server.service.alarms.repository.AlarmPeriodCountProjection;
import org.thingsboard.server.service.alarms.repository.AlarmTrendBucketProjection;
import org.thingsboard.server.service.alarms.repository.AlarmStatisticsRepository;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/** 告警统计仓储适配实现：校验设备范围、执行数据库聚合并映射内部结果。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmStatisticsServiceImpl implements AlarmStatisticsService {

    private final AlarmStatisticsRepository repository;

    @Override
    public List<AlarmDimensionCountBO> countByType(SecurityUser user, Set<UUID> deviceIds,
                                                    Long startTs, Long endTs, boolean activeOnly) {
        // 空权限范围直接返回空列表，避免把空集合传给数据库查询。
        if (!hasQueryScope(user, deviceIds)) {
            return Collections.emptyList();
        }
        // 由仓储按告警类型聚合，再统一映射为空值安全的内部 BO。
        return mapDimensionRows(repository.countByType(
                user.getTenantId().getId(), deviceIds, startTs, endTs, activeOnly));
    }

    @Override
    public List<AlarmDimensionCountBO> countBySeverity(SecurityUser user, Set<UUID> deviceIds,
                                                         Long startTs, Long endTs, boolean activeOnly) {
        // 先验证调用方给出的可见设备范围，再执行严重程度聚合。
        if (!hasQueryScope(user, deviceIds)) {
            return Collections.emptyList();
        }
        return mapDimensionRows(repository.countBySeverity(
                user.getTenantId().getId(), deviceIds, startTs, endTs, activeOnly));
    }

    @Override
    public List<AlarmDeviceCountBO> countByDevice(SecurityUser user, Set<UUID> deviceIds,
                                                   Long startTs, Long endTs) {
        // 空查询范围和空投影都按空列表处理，不进入逐行转换。
        if (!hasQueryScope(user, deviceIds)) {
            return Collections.emptyList();
        }
        List<AlarmDeviceCountProjection> rows = repository.countByDevice(
                user.getTenantId().getId(), deviceIds, startTs, endTs);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<AlarmDeviceCountBO> result = new ArrayList<>();
        // 仅接收有设备 ID 的投影，并把 SQL null 计数统一归零。
        for (AlarmDeviceCountProjection row : rows) {
            if (row == null || row.getDeviceId() == null) {
                continue;
            }
            result.add(new AlarmDeviceCountBO(
                    row.getDeviceId(), safeLong(row.getAlarmCount()), safeLong(row.getHandledCount()),
                    safeLong(row.getUnassignedCount())));
        }
        return result;
    }

    @Override
    public Set<UUID> findActiveAlarmDeviceIds(SecurityUser user, Set<UUID> deviceIds) {
        // 空设备范围返回独立空集合；非空时用仓储查询活动告警设备 ID。
        if (!hasQueryScope(user, deviceIds)) {
            return new HashSet<>();
        }
        return new HashSet<>(repository.findActiveAlarmDeviceIds(
                user.getTenantId().getId(), deviceIds));
    }

    @Override
    public AlarmPeriodCountBO countPeriodSummary(SecurityUser user, Set<UUID> deviceIds,
                                                  Long dayStart, Long previousDayStart,
                                                  Long monthStart, Long previousMonthStart,
                                                  Long yearStart, Long previousYearStart, Long now) {
        // 权限范围或聚合投影为空时返回全零结果，调用方无需处理 null。
        if (!hasQueryScope(user, deviceIds)) {
            return AlarmPeriodCountBO.empty();
        }
        AlarmPeriodCountProjection projection = repository.countPeriodSummary(
                user.getTenantId().getId(), deviceIds, dayStart, previousDayStart,
                monthStart, previousMonthStart, yearStart, previousYearStart, now);
        if (projection == null) {
            return AlarmPeriodCountBO.empty();
        }
        return new AlarmPeriodCountBO(
                safeLong(projection.getDayCurrentCount()),
                safeLong(projection.getDayPreviousCount()),
                safeLong(projection.getMonthCurrentCount()),
                safeLong(projection.getMonthPreviousCount()),
                safeLong(projection.getYearCurrentCount()),
                safeLong(projection.getYearPreviousCount()));
    }
    @Override
    public List<AlarmTrendBucketBO> countTrendBuckets(SecurityUser user, Set<UUID> deviceIds,
                                                       Long startTs, Long endTs,
                                                       String bucketUnit, String zoneId) {
        // 同时校验查询范围和分桶单位，阻止非法单位进入动态 SQL 分支。
        if (!hasQueryScope(user, deviceIds) || !Set.of("hour", "day", "month").contains(bucketUnit)) {
            return Collections.emptyList();
        }
        List<AlarmTrendBucketProjection> rows = repository.countTrendBuckets(
                user.getTenantId().getId(), deviceIds, startTs, endTs, bucketUnit, zoneId);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<AlarmTrendBucketBO> result = new ArrayList<>();
        // 忽略缺少桶序号的投影，并把空计数转成 0 后交给趋势组装层。
        for (AlarmTrendBucketProjection row : rows) {
            if (row == null || row.getBucketIndex() == null) {
                continue;
            }
            result.add(new AlarmTrendBucketBO(
                    row.getBucketIndex(), safeLong(row.getAlarmCount())));
        }
        return result;
    }
    @Override
    public AlarmOverviewAggregateBO getOverview(SecurityUser user, Set<UUID> deviceIds,
                                                 Long startTs, Long endTs) {
        // 先验证设备范围，再由仓储一次聚合状态分布及设备覆盖数。
        if (!hasQueryScope(user, deviceIds)) {
            return AlarmOverviewAggregateBO.empty();
        }
        AlarmOverviewProjection projection = repository.getOverview(
                user.getTenantId().getId(), deviceIds, startTs, endTs);
        if (projection == null) {
            return AlarmOverviewAggregateBO.empty();
        }
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        // 保留平台四种告警状态的固定键名，供上层生成完整状态分布。
        statusCounts.put("ACTIVE_UNACK", safeLong(projection.getActiveUnackCount()));
        statusCounts.put("ACTIVE_ACK", safeLong(projection.getActiveAckCount()));
        statusCounts.put("CLEARED_UNACK", safeLong(projection.getClearedUnackCount()));
        statusCounts.put("CLEARED_ACK", safeLong(projection.getClearedAckCount()));
        return new AlarmOverviewAggregateBO(
                statusCounts,
                safeLong(projection.getAlarmedDeviceCount()),
                safeLong(projection.getActiveAlarmDeviceCountBO()));
    }

    /** 统一验证权限范围，防止空设备集合生成无意义的全表查询。 */
    private boolean hasQueryScope(SecurityUser user, Set<UUID> deviceIds) {
        return user != null && user.getTenantId() != null
                && deviceIds != null && !deviceIds.isEmpty();
    }

    /** 将 JPA 投影转换成稳定的内部结果，空投影字段按 0 处理。 */
    private List<AlarmDimensionCountBO> mapDimensionRows(List<AlarmDimensionProjection> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        // 将空维度标记为“未分类”，并统一处理空计数。
        List<AlarmDimensionCountBO> result = new ArrayList<>();
        for (AlarmDimensionProjection row : rows) {
            if (row == null) {
                continue;
            }
            String dimension = row.getDimension();
            if (dimension == null || dimension.isBlank()) {
                dimension = "未分类";
            }
            result.add(new AlarmDimensionCountBO(dimension, safeLong(row.getAlarmCount())));
        }
        return result;
    }

    private long safeLong(Long value) {
        // 数据库聚合字段允许为空，告警计数中将 null 按 0 处理。
        return value == null ? 0L : value;
    }
}
