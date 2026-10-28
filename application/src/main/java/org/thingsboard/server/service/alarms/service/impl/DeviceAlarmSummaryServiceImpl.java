package org.thingsboard.server.service.alarms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmSummaryService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.query.VisibleDeviceContextService;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 当前用户可见防雷设备的未清除告警摘要实现，统计条数与告警类型分布。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceAlarmSummaryServiceImpl implements DeviceAlarmSummaryService {

    /** 智能防雷设备 SERVER_SCOPE.productCategory 的约定值。 */
    private static final String LIGHTNING_PRODUCT_CATEGORY = "lightning";

    /** 按当前用户 READ 权限及设备服务器属性分类返回可见设备上下文。 */
    private final VisibleDeviceContextService deviceContextService;
    /** 在数据库侧按设备和告警类型聚合，避免将告警明细加载到应用层。 */
    private final AlarmStatisticsService alarmStatisticsService;

    /**
     * 统计当前用户可见防雷设备的未清除（ACTIVE）告警总数和按类型聚合数量。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 未清除告警总数及各告警类型的数量列表（数量降序、类型名次序）
     */
    @Override
    public DeviceAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        // 当前端点属于智能防雷模块，统计范围按用户权限和 productCategory=lightning 双重限定。
        // 汇总仅统计防雷模块设备；HashSet 用于设备 ID 去重并作为后续聚合查询范围。
        Set<UUID> visibleDeviceIds = new HashSet<>();
        for (VisibleDeviceContextBO context : deviceContextService
                .findVisibleDeviceContexts(user, LIGHTNING_PRODUCT_CATEGORY)) {
            if (context != null && context.getDeviceId() != null) {
                visibleDeviceIds.add(context.getDeviceId());
            }
        }
        // 无可见防雷设备时返回稳定的零值结构，不执行告警聚合查询。
        if (visibleDeviceIds.isEmpty()) {
            return new DeviceAlarmSummaryDTO(0, Collections.emptyList());
        }

        // 使用 JPA 聚合查询，只返回告警类型和数量，不把所有告警明细加载到 JVM。
        if (user == null || user.getTenantId() == null) {
            return new DeviceAlarmSummaryDTO(0, Collections.emptyList());
        }
        // 只统计 ACTIVE（未清除）告警；from/to 为空表示不额外限制告警发生时间。
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
                user, visibleDeviceIds, null, null, true);
        // 保留 long 总数累加精度；每种类型和总数在 DTO 边界转换为 int。
        List<DeviceAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
        long activeCount = 0L;
        // 将数据库按类型聚合结果逐项映射为 DTO，并累加未清除告警总数。
        for (AlarmDimensionCountBO row : rows) {
            int count = CountValueSupport.toIntCount(row.getCount());
            activeCount += row.getCount();
            typeTotals.add(new DeviceAlarmTypeTotalDTO(row.getDimension(), count));
        }
        return new DeviceAlarmSummaryDTO(CountValueSupport.toIntCount(activeCount), typeTotals);
    }

}
