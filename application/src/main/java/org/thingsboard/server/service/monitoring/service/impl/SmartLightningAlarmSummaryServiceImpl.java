package org.thingsboard.server.service.monitoring.service.impl;

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
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningAlarmSummaryService;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
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

/** 智能防雷看板告警统计实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningAlarmSummaryServiceImpl implements SmartLightningAlarmSummaryService {

    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningDeviceService deviceService;
    private final MonitoringAlarmQueryService alarmQueryService;

    @Override
    public SmartLightningAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        Set<UUID> lightningDeviceIds = deviceService.getLightningDeviceIds(user);
        if (lightningDeviceIds.isEmpty()) {
            return new SmartLightningAlarmSummaryDTO(0, Collections.emptyList());
        }

        Map<String, Integer> countsByType = new HashMap<>();
        int activeCount = countActiveAlarms(user, lightningDeviceIds, countsByType);
        List<SmartLightningAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByType.entrySet()) {
            typeTotals.add(new SmartLightningAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
        }
        typeTotals.sort(new Comparator<SmartLightningAlarmTypeTotalDTO>() {
            @Override
            public int compare(SmartLightningAlarmTypeTotalDTO left, SmartLightningAlarmTypeTotalDTO right) {
                int countOrder = Integer.compare(right.getCount(), left.getCount());
                return countOrder != 0 ? countOrder : left.getType().compareTo(right.getType());
            }
        });
        return new SmartLightningAlarmSummaryDTO(activeCount, typeTotals);
    }

    /** 只查询 ACTIVE 告警；每条告警消息计数一次，并按 alarm.type 聚合。 */
    private int countActiveAlarms(SecurityUser user, Set<UUID> deviceIds,
                                  Map<String, Integer> countsByType) throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ACTIVE),
                Collections.emptyList(), null);
        int count = 0;
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            EntityId originator = alarm.getOriginator();
            if (originator == null || originator.getEntityType() != EntityType.DEVICE
                    || !deviceIds.contains(originator.getId())) {
                continue;
            }
            String type = alarm.getType() == null || alarm.getType().isBlank()
                    ? "未分类" : alarm.getType().trim();
            countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
            count++;
        }
        return count;
    }
}
