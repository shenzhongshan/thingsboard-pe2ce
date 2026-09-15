package org.thingsboard.server.service.alarms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmSummaryService;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 当前用户全部可见设备的未清除告警摘要实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceAlarmSummaryServiceImpl implements DeviceAlarmSummaryService {

    /** 单次设备或告警查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningDeviceService deviceService;
    private final DeviceAlarmQueryService alarmQueryService;

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

        Map<String, Integer> countsByType = new HashMap<>();
        int activeCount = countActiveAlarms(user, visibleDeviceIds, countsByType);
        // 聚合结果转为 DTO 列表：数量多的类型排前面，数量相同时按类型名排序。
        List<DeviceAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByType.entrySet()) {
            typeTotals.add(new DeviceAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
        }
        typeTotals.sort(new Comparator<DeviceAlarmTypeTotalDTO>() {
            @Override
            public int compare(DeviceAlarmTypeTotalDTO left, DeviceAlarmTypeTotalDTO right) {
                int countOrder = Integer.compare(right.getCount(), left.getCount());
                return countOrder != 0 ? countOrder : left.getType().compareTo(right.getType());
            }
        });
        return new DeviceAlarmSummaryDTO(activeCount, typeTotals);
    }

    /**
     * 只查询 ACTIVE 告警；每条告警消息计数一次，并按 alarm.type 聚合。
     *
     * <p>类型为空的告警归入“未分类”，避免前端收到空类型无法展示。</p>
     */
    private int countActiveAlarms(SecurityUser user, Set<UUID> deviceIds,
                                  Map<String, Integer> countsByType) throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ACTIVE),
                Collections.emptyList(), null);
        int count = 0;
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            // 跳过非设备来源和当前用户不可见设备的告警。
            EntityId originator = alarm.getOriginator();
            if (originator == null || originator.getEntityType() != EntityType.DEVICE
                    || !deviceIds.contains(originator.getId())) {
                continue;
            }
            // 空类型统一记为“未分类”，保证聚合键始终有展示文本。
            String type = alarm.getType() == null || alarm.getType().isBlank()
                    ? "未分类" : alarm.getType().trim();
            countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
            count++;
        }
        return count;
    }
}
