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
import org.thingsboard.server.service.alarms.repository.AlarmDimensionProjection;
import org.thingsboard.server.service.alarms.repository.AlarmDeviceCountProjection;
import org.thingsboard.server.service.alarms.repository.AlarmOverviewProjection;
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

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmStatisticsServiceImpl implements AlarmStatisticsService {

    private final AlarmStatisticsRepository repository;

    @Override
    public List<AlarmDimensionCountBO> countByType(SecurityUser user, Set<UUID> deviceIds,
                                                    Long startTs, Long endTs, boolean activeOnly) {
        if (!hasQueryScope(user, deviceIds)) {
            return Collections.emptyList();
        }
        return mapDimensionRows(repository.countByType(
                user.getTenantId().getId(), deviceIds, startTs, endTs, activeOnly));
    }

    @Override
    public List<AlarmDimensionCountBO> countBySeverity(SecurityUser user, Set<UUID> deviceIds,
                                                         Long startTs, Long endTs, boolean activeOnly) {
        if (!hasQueryScope(user, deviceIds)) {
            return Collections.emptyList();
        }
        return mapDimensionRows(repository.countBySeverity(
                user.getTenantId().getId(), deviceIds, startTs, endTs, activeOnly));
    }

    @Override
    public List<AlarmDeviceCountBO> countByDevice(SecurityUser user, Set<UUID> deviceIds,
                                                   Long startTs, Long endTs) {
        if (!hasQueryScope(user, deviceIds)) {
            return Collections.emptyList();
        }
        List<AlarmDeviceCountProjection> rows = repository.countByDevice(
                user.getTenantId().getId(), deviceIds, startTs, endTs);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<AlarmDeviceCountBO> result = new ArrayList<>();
        for (AlarmDeviceCountProjection row : rows) {
            if (row == null || row.getDeviceId() == null) {
                continue;
            }
            result.add(new AlarmDeviceCountBO(
                    row.getDeviceId(), safeLong(row.getAlarmCount()), safeLong(row.getHandledCount())));
        }
        return result;
    }

    @Override
    public Set<UUID> findActiveAlarmDeviceIds(SecurityUser user, Set<UUID> deviceIds) {
        if (!hasQueryScope(user, deviceIds)) {
            return new HashSet<>();
        }
        return new HashSet<>(repository.findActiveAlarmDeviceIds(
                user.getTenantId().getId(), deviceIds));
    }

    @Override
    public AlarmOverviewAggregateBO getOverview(SecurityUser user, Set<UUID> deviceIds,
                                                 Long startTs, Long endTs) {
        if (!hasQueryScope(user, deviceIds)) {
            return AlarmOverviewAggregateBO.empty();
        }
        AlarmOverviewProjection projection = repository.getOverview(
                user.getTenantId().getId(), deviceIds, startTs, endTs);
        if (projection == null) {
            return AlarmOverviewAggregateBO.empty();
        }
        Map<String, Long> statusCounts = new LinkedHashMap<>();
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
        return value == null ? 0L : value;
    }
}
