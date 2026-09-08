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
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.EntityAttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 智能防雷数据监测页设备列表的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningDeviceServiceImpl implements SmartLightningDeviceService {

    private static final int QUERY_PAGE_SIZE = 10_000;
    private static final String PRODUCT_CATEGORY_ATTRIBUTE = "productCategory";
    private static final String LIGHTNING_PRODUCT_CATEGORY = "lightning";
    private static final String ACTIVE_ATTRIBUTE = "active";
    private static final List<String> DEVICE_ATTRIBUTES = List.of(PRODUCT_CATEGORY_ATTRIBUTE, ACTIVE_ATTRIBUTE);
    private static final List<String> DEVICE_CONTEXT_ATTRIBUTES = List.of(
            PRODUCT_CATEGORY_ATTRIBUTE,
            "productName",
            "functionCategory",
            "deviceProtocol",
            ACTIVE_ATTRIBUTE,
            "lastActivityTime",
            "gatewayIdentifier",
            "gatewayId",
            "meterAddress",
            "circuitName",
            "timeSetting"
    );

    private final UserEntitiesService userEntitiesService;
    private final AttributesService attributesService;

    @Override
    public PageData<DeviceInfo> getLightningDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException {
        List<DeviceInfo> visibleDevices = findVisibleDevices(user, pageLink.getTextSearch());
        Map<UUID, Map<String, String>> attributesByDevice = findDeviceAttributes(user, visibleDevices, DEVICE_ATTRIBUTES);
        List<DeviceInfo> lightningDevices = filterLightningDevices(visibleDevices, attributesByDevice);
        return createPageData(lightningDevices, pageLink);
    }

    @Override
    public PageData<SmartLightningDeviceContextDTO> getLightningDeviceContexts(
            SecurityUser user, PageLink pageLink) throws ThingsboardException {
        List<DeviceInfo> visibleDevices = findVisibleDevices(user, pageLink.getTextSearch());
        Map<UUID, Map<String, String>> attributesByDevice = findDeviceAttributes(
                user, visibleDevices, DEVICE_CONTEXT_ATTRIBUTES);
        List<SmartLightningDeviceContextDTO> contexts = new ArrayList<>();
        for (DeviceInfo device : visibleDevices) {
            Map<String, String> attributes = attributesByDevice.get(device.getId().getId());
            if (!isLightningDevice(attributes)) {
                continue;
            }
            contexts.add(toDeviceContext(device, attributes));
        }
        return createContextPageData(contexts, pageLink);
    }

    /** 平台授权服务同时覆盖租户、客户和通过实体组分享得到的设备权限。 */
    private List<DeviceInfo> findVisibleDevices(SecurityUser user, String textSearch) throws ThingsboardException {
        PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                user, EntityType.DEVICE, Operation.READ, new PageLink(QUERY_PAGE_SIZE, 0, textSearch));
        List<DeviceInfo> devices = new ArrayList<>();
        for (HasName entity : pageData.getData()) {
            if (entity instanceof DeviceInfo deviceInfo) {
                devices.add(deviceInfo);
            }
        }
        return devices;
    }

    /** 一次批量读取类别和在线属性，避免按设备逐条访问属性服务。 */
    private Map<UUID, Map<String, String>> findDeviceAttributes(
            SecurityUser user, List<DeviceInfo> devices, List<String> attributeKeys) {
        if (devices.isEmpty() || attributeKeys.isEmpty()) {
            return new HashMap<>();
        }
        List<EntityId> deviceIds = new ArrayList<>();
        for (DeviceInfo device : devices) {
            deviceIds.add(device.getId());
        }
        List<EntityAttributeKvEntry> attributes = attributesService.findLatestByEntityIdsAndScopeWithEntityId(
                user.getTenantId(), deviceIds, AttributeScope.SERVER_SCOPE, attributeKeys);
        Map<UUID, Map<String, String>> attributesByDevice = new HashMap<>();
        for (EntityAttributeKvEntry entry : attributes) {
            if (entry.entityId() == null || entry.attribute() == null) {
                continue;
            }
            Map<String, String> deviceAttributes = attributesByDevice.get(entry.entityId());
            if (deviceAttributes == null) {
                deviceAttributes = new HashMap<>();
                attributesByDevice.put(entry.entityId(), deviceAttributes);
            }
            deviceAttributes.put(entry.attribute().getKey(), entry.attribute().getValueAsString());
        }
        return attributesByDevice;
    }

    private SmartLightningDeviceContextDTO toDeviceContext(
            DeviceInfo device, Map<String, String> attributes) {
        SmartLightningDeviceContextDTO context = new SmartLightningDeviceContextDTO();
        context.setDeviceId(device.getId().getId());
        context.setDeviceName(device.getName());
        context.setDeviceProfileName(device.getDeviceProfileName() == null
                ? device.getType() : device.getDeviceProfileName());
        context.setProductCategory(attributes.get(PRODUCT_CATEGORY_ATTRIBUTE));
        context.setProductName(attributes.get("productName"));
        context.setFunctionCategory(attributes.get("functionCategory"));
        context.setDeviceProtocol(attributes.get("deviceProtocol"));
        context.setActive(attributes.containsKey(ACTIVE_ATTRIBUTE)
                ? isActive(attributes.get(ACTIVE_ATTRIBUTE)) : device.isActive());
        context.setLastActivityTime(parseTimestamp(attributes.get("lastActivityTime")));
        String gatewayIdentifier = attributes.get("gatewayIdentifier");
        if (gatewayIdentifier == null || gatewayIdentifier.isBlank()) {
            gatewayIdentifier = attributes.get("gatewayId");
        }
        context.setGatewayIdentifier(gatewayIdentifier);
        context.setMeterAddress(attributes.get("meterAddress"));
        context.setCircuitName(attributes.get("circuitName"));
        context.setTimeSetting(attributes.get("timeSetting"));
        return context;
    }

    private Long parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<DeviceInfo> filterLightningDevices(List<DeviceInfo> visibleDevices,
                                                      Map<UUID, Map<String, String>> attributesByDevice) {
        List<DeviceInfo> lightningDevices = new ArrayList<>();
        for (DeviceInfo device : visibleDevices) {
            Map<String, String> deviceAttributes = attributesByDevice.get(device.getId().getId());
            if (!isLightningDevice(deviceAttributes)) {
                continue;
            }
            device.setActive(isActive(deviceAttributes.get(ACTIVE_ATTRIBUTE)));
            lightningDevices.add(device);
        }
        return lightningDevices;
    }

    private boolean isLightningDevice(Map<String, String> attributes) {
        if (attributes == null) {
            return false;
        }
        String category = attributes.get(PRODUCT_CATEGORY_ATTRIBUTE);
        return category != null && LIGHTNING_PRODUCT_CATEGORY.equalsIgnoreCase(category.trim());
    }

    /** productCategory 过滤完成后再分页，totalElements 才是防雷设备总数。 */
    private PageData<DeviceInfo> createPageData(List<DeviceInfo> devices, PageLink pageLink) {
        int total = devices.size();
        int from = Math.min(pageLink.getPage() * pageLink.getPageSize(), total);
        int to = Math.min(from + pageLink.getPageSize(), total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageLink.getPageSize());
        return new PageData<>(devices.subList(from, to), totalPages, total, to < total);
    }

    private PageData<SmartLightningDeviceContextDTO> createContextPageData(List<SmartLightningDeviceContextDTO> contexts, PageLink pageLink) {
        int total = contexts.size();
        int from = Math.min(pageLink.getPage() * pageLink.getPageSize(), total);
        int to = Math.min(from + pageLink.getPageSize(), total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageLink.getPageSize());
        return new PageData<>(contexts.subList(from, to), totalPages, total, to < total);
    }

    private boolean isActive(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
