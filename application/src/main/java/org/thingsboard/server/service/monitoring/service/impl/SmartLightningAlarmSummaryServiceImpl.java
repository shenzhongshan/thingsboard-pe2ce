package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningAlarmSummaryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
    private final AlarmService alarmService;

    @Override
    public SmartLightningAlarmSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        Set<UUID> lightningDeviceIds = findLightningDeviceIds(user);
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

    /** 复用设备服务的权限过滤和 productCategory=lightning 过滤，不直接按租户查询设备。 */
    private Set<UUID> findLightningDeviceIds(SecurityUser user) throws ThingsboardException {
        PageData<SmartLightningDeviceContextDTO> pageData = deviceService.getLightningDeviceContexts(
                user, new PageLink(QUERY_PAGE_SIZE, 0));
        Set<UUID> deviceIds = new HashSet<>();
        for (SmartLightningDeviceContextDTO context : pageData.getData()) {
            if (context.getDeviceId() != null) {
                deviceIds.add(context.getDeviceId());
            }
        }
        return deviceIds;
    }

    /** 只查询 ACTIVE 告警；每条告警消息计数一次，并按 alarm.type 聚合。 */
    private int countActiveAlarms(SecurityUser user, Set<UUID> deviceIds,
                                  Map<String, Integer> countsByType) throws ThingsboardException {
        int count = 0;
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        while (pageLink != null) {
            AlarmQueryV2 query = new AlarmQueryV2(
                    null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ACTIVE),
                    Collections.emptyList(), null);
            PageData<AlarmInfo> pageData;
            if (user.isCustomerUser()) {
                pageData = alarmService.findCustomerAlarmsV2(user.getTenantId(), user.getCustomerId(), query);
            } else {
                pageData = alarmService.findAlarmsV2(user.getTenantId(), query);
            }
            for (AlarmInfo alarm : pageData.getData()) {
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
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
        }
        return count;
    }
}
