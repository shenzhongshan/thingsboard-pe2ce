/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.kv.EntityAttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummary;
import org.thingsboard.server.service.monitoring.service.DeviceStatusSummaryService;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.util.DeviceStatusSummaryCalculator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 顶部导航栏设备状态统计的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceStatusSummaryServiceImpl implements DeviceStatusSummaryService {

    private static final int QUERY_PAGE_SIZE = 10_000;
    private static final String ACTIVE_ATTRIBUTE = "active";

    private final UserEntitiesService userEntitiesService;
    private final AttributesService attributesService;
    private final MonitoringAlarmQueryService alarmQueryService;

    @Override
    public DeviceStatusSummary getSummary(SecurityUser user) throws ThingsboardException {
        List<UUID> visibleDeviceIds = findVisibleDeviceIds(user);
        Map<UUID, String> activeValuesByDeviceId = findActiveValues(user, visibleDeviceIds);
        List<UUID> activeAlarmDeviceIds = findActiveAlarmDeviceIds(user);
        return DeviceStatusSummaryCalculator.calculate(visibleDeviceIds, activeValuesByDeviceId, activeAlarmDeviceIds);
    }

    private List<UUID> findVisibleDeviceIds(SecurityUser user) throws ThingsboardException {
        PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                user, EntityType.DEVICE, Operation.READ, new PageLink(QUERY_PAGE_SIZE, 0));
        List<UUID> deviceIds = new ArrayList<>();
        for (HasName entity : pageData.getData()) {
            if (entity instanceof HasId<?> identifiedEntity && identifiedEntity.getId() != null) {
                deviceIds.add(identifiedEntity.getId().getId());
            }
        }
        return deviceIds;
    }

    /** 一次批量读取 active，避免为每台设备单独读取属性。 */
    private Map<UUID, String> findActiveValues(SecurityUser user, List<UUID> deviceIds) {
        List<EntityId> entityIds = new ArrayList<>();
        for (UUID deviceId : deviceIds) {
            entityIds.add(new DeviceId(deviceId));
        }
        List<EntityAttributeKvEntry> attributes = attributesService.findLatestByEntityIdsAndScopeWithEntityId(
                user.getTenantId(), entityIds, AttributeScope.SERVER_SCOPE, List.of(ACTIVE_ATTRIBUTE));
        Map<UUID, String> activeValuesByDeviceId = new HashMap<>();
        for (EntityAttributeKvEntry entry : attributes) {
            if (entry.entityId() != null && entry.attribute() != null && ACTIVE_ATTRIBUTE.equals(entry.attribute().getKey())) {
                activeValuesByDeviceId.put(entry.entityId(), entry.attribute().getValueAsString());
            }
        }
        return activeValuesByDeviceId;
    }

    /** 逐页读取未清除告警，避免固定上限造成异常设备统计遗漏。 */
    private List<UUID> findActiveAlarmDeviceIds(SecurityUser user) throws ThingsboardException {
        List<UUID> deviceIds = new ArrayList<>();
        TimePageLink pageLink = new TimePageLink(QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ACTIVE), Collections.emptyList(), null);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            EntityId originator = alarm.getOriginator();
            if (originator != null && originator.getEntityType() == EntityType.DEVICE) {
                deviceIds.add(originator.getId());
            }
        }
        return deviceIds;
    }
}
