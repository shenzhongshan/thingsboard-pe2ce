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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.kv.EntityAttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.monitoring.DeviceStatusSummary;
import org.thingsboard.server.service.monitoring.SmartLightningMonitoringTreeBuilder;
import org.thingsboard.server.service.monitoring.DeviceStatusSummaryCalculator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 智能防雷数据监测页专用接口。
 *
 * <p>浏览器只发起一次请求。控制器先取得当前用户可见的资产和设备，随后在服务端读取
 * Contains 关系并组装树，避免前端按层级和每个资产反复访问通用关系接口。</p>
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/smart-lightning/monitoring")
@RequiredArgsConstructor
public class SmartLightningMonitoringController extends BaseController {

    /** 单次读取上限；监测树不是分页列表，页面需要完整层级。 */
    private static final int TREE_ENTITY_PAGE_SIZE = 10_000;
    /** 服务器属性名和值：只有明确标记为 lightning 的设备进入智能防雷列表。 */
    private static final String PRODUCT_CATEGORY_ATTRIBUTE = "productCategory";
    private static final String LIGHTNING_PRODUCT_CATEGORY = "lightning";
    private static final String ACTIVE_ATTRIBUTE = "active";
    private static final List<String> DEVICE_STATUS_ATTRIBUTES = List.of(PRODUCT_CATEGORY_ATTRIBUTE, ACTIVE_ATTRIBUTE);

    private final UserEntitiesService userEntitiesService;
    private final RelationService relationService;
    private final SmartLightningMonitoringTreeBuilder treeBuilder;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SmartLightningMonitoringTreeBuilder.TreeResponse getMonitoringTree() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleAssets = findVisibleEntities(user, EntityType.ASSET);
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleDevices = findVisibleEntities(user, EntityType.DEVICE);
        List<EntityRelation> relations = new ArrayList<>();

        // 只从可见资产向外查 Contains；树构建器会再次过滤不可见的关系另一端。
        for (SmartLightningMonitoringTreeBuilder.VisibleEntity asset : visibleAssets) {
            relations.addAll(relationService.findByFrom(user.getTenantId(), new AssetId(asset.id()), RelationTypeGroup.COMMON));
        }
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleEntities = new ArrayList<>(visibleAssets);
        visibleEntities.addAll(visibleDevices);
        return treeBuilder.build(visibleEntities, relations);
    }

    /**
     * 顶部导航栏的全局设备状态统计。
     *
     * <p>此接口不按 productCategory 过滤，范围是当前登录用户拥有 READ 权限的全部设备。
     * 在线状态只读取设备 SERVER_SCOPE 中的 active 属性（true/1），不使用 DeviceInfo.active；
     * 未清除告警按设备去重后计入异常。</p>
     */
    @GetMapping("/device-status-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceStatusSummary getDeviceStatusSummary() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        List<UUID> visibleDeviceIds = new ArrayList<>();
        for (HasName entity : findVisibleEntities(user, EntityType.DEVICE, null)) {
            if (entity instanceof HasId<?> identifiedEntity && identifiedEntity.getId() != null) {
                visibleDeviceIds.add(identifiedEntity.getId().getId());
            }
        }

        List<EntityId> entityIds = new ArrayList<>();
        for (UUID deviceId : visibleDeviceIds) {
            entityIds.add(new org.thingsboard.server.common.data.id.DeviceId(deviceId));
        }
        List<EntityAttributeKvEntry> attributes = attributesService.findLatestByEntityIdsAndScopeWithEntityId(
                user.getTenantId(), entityIds, AttributeScope.SERVER_SCOPE, List.of(ACTIVE_ATTRIBUTE));
        Map<UUID, String> activeValuesByDeviceId = new HashMap<>();
        for (EntityAttributeKvEntry entry : attributes) {
            if (entry.entityId() != null && entry.attribute() != null && ACTIVE_ATTRIBUTE.equals(entry.attribute().getKey())) {
                activeValuesByDeviceId.put(entry.entityId(), entry.attribute().getValueAsString());
            }
        }

        List<UUID> activeAlarmDeviceIds = new ArrayList<>();
        for (AlarmInfo alarm : findAllActiveAlarms(user)) {
            EntityId originator = alarm.getOriginator();
            if (originator != null && originator.getEntityType() == EntityType.DEVICE) {
                activeAlarmDeviceIds.add(originator.getId());
            }
        }
        return DeviceStatusSummaryCalculator.calculate(visibleDeviceIds, activeValuesByDeviceId, activeAlarmDeviceIds);
    }

    /**
     * 返回当前用户有读取权限且 productCategory=lightning 的设备列表。
     *
     * <p>设备列表本身来自 UserEntitiesService，保留租户、客户和实体组授权边界；
     * productCategory 位于设备 SERVER_SCOPE 属性中，因此先批量读取属性再在服务端过滤。
     * 过滤后重新分页，避免把其他产品计入 totalElements。</p>
     */
    @GetMapping("/devices")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<DeviceInfo> getLightningDevices(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch
    ) throws ThingsboardException {
        PageLink requestedPage = createPageLink(pageSize, page, textSearch, null, null);
        SecurityUser user = getCurrentUser();

        // 读取完整的当前用户可见设备集合，再按服务器属性过滤，保证分页总数准确。
        // 设备接口与资产树共用同一个当前用户可见实体查询方法，避免两套授权/查询逻辑。
        List<DeviceInfo> visibleDevices = new ArrayList<>();
        for (HasName entity : findVisibleEntities(user, EntityType.DEVICE, requestedPage.getTextSearch())) {
            if (entity instanceof DeviceInfo deviceInfo) {
                visibleDevices.add(deviceInfo);
            }
        }

        List<EntityId> deviceIds = new ArrayList<>();
        for (DeviceInfo device : visibleDevices) {
            deviceIds.add(device.getId());
        }

        // 一次读取所有设备的 productCategory 和 active，避免每台设备单独访问属性服务。
        List<EntityAttributeKvEntry> statusAttributes = attributesService.findLatestByEntityIdsAndScopeWithEntityId(
                user.getTenantId(), deviceIds, AttributeScope.SERVER_SCOPE, DEVICE_STATUS_ATTRIBUTES);
        Map<UUID, Map<String, String>> attributesByDevice = groupAttributesByDevice(statusAttributes);

        List<DeviceInfo> lightningDevices = new ArrayList<>();
        for (DeviceInfo device : visibleDevices) {
            Map<String, String> deviceAttributes = attributesByDevice.get(device.getId().getId());
            if (deviceAttributes == null) {
                continue;
            }
            String categoryValue = deviceAttributes.get(PRODUCT_CATEGORY_ATTRIBUTE);
            if (categoryValue == null || !LIGHTNING_PRODUCT_CATEGORY.equalsIgnoreCase(categoryValue.trim())) {
                continue;
            }
            device.setActive(isActive(deviceAttributes.get(ACTIVE_ATTRIBUTE)));
            lightningDevices.add(device);
        }

        int total = lightningDevices.size();
        int from = Math.min(requestedPage.getPage() * requestedPage.getPageSize(), total);
        int to = Math.min(from + requestedPage.getPageSize(), total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / requestedPage.getPageSize());
        return new PageData<>(lightningDevices.subList(from, to), totalPages, total, to < total);
    }

    /** 将一次批量查询返回的属性按设备 ID 分组，便于普通循环读取。 */
    private Map<UUID, Map<String, String>> groupAttributesByDevice(List<EntityAttributeKvEntry> statusAttributes) {
        Map<UUID, Map<String, String>> attributesByDevice = new HashMap<>();
        for (EntityAttributeKvEntry entry : statusAttributes) {
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

    /** 逐页读取未清除告警，避免固定 10,000 条时漏统计异常设备。 */
    private List<AlarmInfo> findAllActiveAlarms(SecurityUser user) {
        List<AlarmInfo> alarms = new ArrayList<>();
        TimePageLink pageLink = new TimePageLink(TREE_ENTITY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        while (pageLink != null) {
            AlarmQueryV2 query = new AlarmQueryV2(
                    null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ACTIVE), Collections.emptyList(), null);
            PageData<AlarmInfo> pageData;
            if (user.isCustomerUser()) {
                pageData = alarmService.findCustomerAlarmsV2(user.getTenantId(), user.getCustomerId(), query);
            } else {
                pageData = alarmService.findAlarmsV2(user.getTenantId(), query);
            }
            alarms.addAll(pageData.getData());
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
        }
        return alarms;
    }

    /** active 属性支持布尔字符串和 0/1 两种常见格式。 */
    private boolean isActive(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 复用平台的“当前用户可见实体”服务，而不是按租户直查。
     * 这样客户用户和通过实体组分享获得权限的用户，都只会看到被授权的数据。
     */
    private List<SmartLightningMonitoringTreeBuilder.VisibleEntity> findVisibleEntities(SecurityUser user, EntityType entityType)
            throws ThingsboardException {
        List<SmartLightningMonitoringTreeBuilder.VisibleEntity> visibleEntities = new ArrayList<>();
        for (HasName entity : findVisibleEntities(user, entityType, null)) {
            if (entity instanceof HasId<?> identifiedEntity && identifiedEntity.getId() != null) {
                visibleEntities.add(new SmartLightningMonitoringTreeBuilder.VisibleEntity(
                        identifiedEntity.getId().getId(), entity.getName(), entityType));
            }
        }
        return visibleEntities;
    }

    /**
     * 查询当前用户可见实体的统一入口。设备列表和资产树都必须经过这里，
     * 这样租户、客户及实体组授权边界保持一致。
     */
    private List<? extends HasName> findVisibleEntities(SecurityUser user, EntityType entityType, String textSearch)
            throws ThingsboardException {
        PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                user, entityType, Operation.READ,
                new PageLink(TREE_ENTITY_PAGE_SIZE, 0, textSearch));
        return pageData.getData();
    }
}
