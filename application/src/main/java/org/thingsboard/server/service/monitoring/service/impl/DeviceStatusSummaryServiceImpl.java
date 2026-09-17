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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.EntityAttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummaryDTO;
import org.thingsboard.server.service.monitoring.service.DeviceStatusSummaryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.util.DeviceStatusSummaryCalculator;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.*;

/** 顶部导航栏设备状态统计的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceStatusSummaryServiceImpl implements DeviceStatusSummaryService {

    private static final String ACTIVE_ATTRIBUTE = "active";

    private final SmartLightningDeviceService smartLightningDeviceService;
    private final AttributesService attributesService;
    private final AlarmStatisticsService alarmStatisticsService;

    /**
     * 统计当前用户可见设备中“正常”与“异常”的数量。
     *
     * <p>正常 = 在线（active 为 true/1）且没有未清除告警；
     * 异常 = 存在未清除告警的设备（按设备去重，无论在线离线）；
     * 离线且无告警的设备不计入这两项。</p>
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 正常与异常设备数量
     */
    @Override
    public DeviceStatusSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        Set<UUID> visibleDeviceIds = smartLightningDeviceService.getVisibleDeviceIds(user);
        Map<UUID, String> activeValuesByDeviceId = findActiveValues(user, visibleDeviceIds);
        Set<UUID> activeAlarmDeviceIds = alarmStatisticsService.findActiveAlarmDeviceIds(user, visibleDeviceIds);
        return DeviceStatusSummaryCalculator.calculate(visibleDeviceIds, activeValuesByDeviceId, activeAlarmDeviceIds);
    }

    /** 一次批量读取 active，避免为每台设备单独读取属性。 */
    private Map<UUID, String> findActiveValues(SecurityUser user, Set<UUID> deviceIds) {
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

}
