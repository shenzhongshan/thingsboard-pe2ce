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
import org.thingsboard.server.service.query.VisibleDeviceScopeService;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 当前用户全部可见设备的未清除告警摘要实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceAlarmSummaryServiceImpl implements DeviceAlarmSummaryService {

    private final VisibleDeviceScopeService deviceService;
    private final AlarmStatisticsService alarmStatisticsService;

    /**
     * 统计当前用户可见全部设备的未清除（ACTIVE）告警总数和按类型聚合数量。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 未清除告警总数及各告警类型的数量列表（数量降序、类型名次序）
     */
    @Override
    public DeviceAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        // 统计范围来自统一权限服务，不附加智能防雷产品分类条件。
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        if (visibleDeviceIds.isEmpty()) {
            return new DeviceAlarmSummaryDTO(0, Collections.emptyList());
        }

        // 使用 JPA 聚合查询，只返回告警类型和数量，不把所有告警明细加载到 JVM。
        if (user == null || user.getTenantId() == null) {
            return new DeviceAlarmSummaryDTO(0, Collections.emptyList());
        }
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
                user, visibleDeviceIds, null, null, true);
        // 保留 long 总数累加精度；每种类型和总数在 DTO 边界转换为 int。
        List<DeviceAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
        long activeCount = 0L;
        for (AlarmDimensionCountBO row : rows) {
            int count = CountValueSupport.toIntCount(row.getCount());
            activeCount += row.getCount();
            typeTotals.add(new DeviceAlarmTypeTotalDTO(row.getDimension(), count));
        }
        return new DeviceAlarmSummaryDTO(CountValueSupport.toIntCount(activeCount), typeTotals);
    }

}
