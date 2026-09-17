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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.EntityAttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.UserEntitiesService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/** 智能防雷数据监测页设备列表的业务实现。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningDeviceServiceImpl implements SmartLightningDeviceService {

    private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;
    private static final String PRODUCT_CATEGORY_ATTRIBUTE = "productCategory";
    private static final String LIGHTNING_PRODUCT_CATEGORY = "lightning";
    private static final String ACTIVE_ATTRIBUTE = "active";
    private static final List<String> DEVICE_ATTRIBUTES = List.of(PRODUCT_CATEGORY_ATTRIBUTE, ACTIVE_ATTRIBUTE);
    /**
     * 设备上下文接口一次批量读取的服务器属性。
     * latitude/longitude 与其他业务属性共用同一次批量查询，不会为每台设备单独访问数据库。
     */
    private static final List<String> DEVICE_CONTEXT_ATTRIBUTES = List.of(
            PRODUCT_CATEGORY_ATTRIBUTE,
            "productName",
            "functionCategory",
            ACTIVE_ATTRIBUTE,
            "lastActivityTime",
            "gatewayIdentifier",
            "gatewayId",
            "meterAddress",
            "circuitName",
            "timeSetting",
            "latitude",
            "longitude"
    );

    private final UserEntitiesService userEntitiesService;
    private final AttributesService attributesService;
    private final DeviceService deviceService;

    /**
     * 返回当前用户可见且 productCategory=lightning 的设备列表（分页）。
     *
     * <p>流程：按权限取可见设备 → 批量读取分类/在线属性 → 服务端过滤 → 内存分页，
     * 保证 totalElements 是防雷设备数量而非全部设备数量。</p>
     */
    @Override
    public PageData<DeviceInfo> getLightningDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException {
        List<DeviceInfo> visibleDevices = findVisibleDevices(user, pageLink.getTextSearch());
        Map<UUID, Map<String, String>> attributesByDevice = findDeviceAttributes(user, visibleDevices, DEVICE_ATTRIBUTES);
        List<DeviceInfo> lightningDevices = filterLightningDevices(visibleDevices, attributesByDevice);
        return createPageData(lightningDevices, pageLink);
    }

    /**
     * 批量返回当前用户可见防雷设备及其服务器属性上下文（分页）。
     *
     * <p>相比 {@link #getLightningDevices} 额外带出产品名、功能分类、传输协议、
     * 仪表地址以及地图经纬度等业务字段，供告警列表、地图、SPD/雷电流监测等模块一次取全上下文，
     * 避免调用方再按设备逐个查询属性。</p>
     */
    @Override
    public PageData<MonitoringDeviceContextBO> getLightningDeviceContexts(
            SecurityUser user, PageLink pageLink) throws ThingsboardException {
        List<DeviceInfo> visibleDevices = findVisibleDevices(user, pageLink.getTextSearch());
        List<MonitoringDeviceContextBO> contexts = buildLightningDeviceContexts(user, visibleDevices);
        return createPageData(contexts, pageLink);
    }

    /**
     * 一次返回当前用户可见的全部防雷设备上下文，供后端汇总接口使用。
     * 权限过滤、productCategory 判断和服务器属性批量读取均复用本服务既有实现。
     */
    @Override
    public List<MonitoringDeviceContextBO> getAllLightningDeviceContexts(SecurityUser user)
            throws ThingsboardException {
        return buildLightningDeviceContexts(user, findVisibleDevices(user, null));
    }

    /** 将已完成权限过滤的设备批量转换为防雷设备上下文。 */
    private List<MonitoringDeviceContextBO> buildLightningDeviceContexts(
            SecurityUser user, List<DeviceInfo> visibleDevices) {
        Map<UUID, Map<String, String>> attributesByDevice = findDeviceAttributes(
                user, visibleDevices, DEVICE_CONTEXT_ATTRIBUTES);
        Map<UUID, Device> devicesById = findDevicesWithConfiguration(visibleDevices);
        List<MonitoringDeviceContextBO> contexts = new ArrayList<>();
        for (DeviceInfo device : visibleDevices) {
            Map<String, String> attributes = attributesByDevice.get(device.getId().getId());
            if (!isLightningDevice(attributes)) {
                continue;
            }
            contexts.add(toDeviceContext(device, devicesById.get(device.getId().getId()), attributes));
        }
        return contexts;
    }

    /** 返回当前用户有 READ 权限的全部设备 ID，不按产品分类过滤（告警分析页“全体设备”口径）。 */
    @Override
    public Set<UUID> getVisibleDeviceIds(SecurityUser user) throws ThingsboardException {
        Set<UUID> deviceIds = new HashSet<>();
        for (DeviceInfo device : findVisibleDevices(user, null)) {
            if (device.getId() != null && device.getId().getId() != null) {
                deviceIds.add(device.getId().getId());
            }
        }
        return deviceIds;
    }

    /** 平台授权服务同时覆盖租户、客户和通过实体组分享得到的设备权限。 */
    private List<DeviceInfo> findVisibleDevices(SecurityUser user, String textSearch) throws ThingsboardException {
        List<DeviceInfo> devices = new ArrayList<>();
        PageLink pageLink = new PageLink(QUERY_PAGE_SIZE, 0, textSearch);
        // 租户管理员的设备范围就是当前租户本身。
        // 直接使用设备服务的租户查询，避免通用实体组权限查询受分页或权限合并影响而只返回部分设备。
        if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
            while (pageLink != null) {
                PageData<DeviceInfo> pageData = deviceService.findDeviceInfosByFilter(
                        DeviceInfoFilter.builder().tenantId(user.getTenantId()).build(), pageLink);
                for (DeviceInfo deviceInfo : pageData.getData()) {
                    if (textSearch == null || deviceInfo.getName().toLowerCase().contains(textSearch.toLowerCase())) {
                        devices.add(deviceInfo);
                    }
                }
                pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
            }
            return devices;
        }

        // 不能假设授权服务一定会按请求的 10000 条返回数据。
        // 平台可能受全局分页上限、数据库方言或权限查询实现影响，实际只返回较小的一页。
        // 如果只读取第一页，就会出现数据库有 14 台而接口只返回 10 台的情况。
        while (pageLink != null) {
            PageData<? extends HasName> pageData = userEntitiesService.findUserEntities(
                    user, EntityType.DEVICE, Operation.READ, pageLink);
            for (HasName entity : pageData.getData()) {
                if (entity instanceof DeviceInfo deviceInfo) {
                    devices.add(deviceInfo);
                }
            }
            // PageData 只提供是否有下一页，下一页链接由当前 PageLink 生成。
            pageLink = pageData.hasNext() ? pageLink.nextPageLink() : null;
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

    /**
     * 将设备实体、批量属性和设备配置合并为服务内部上下文。
     *
     * @param device 已通过权限过滤的设备概要信息
     * @param deviceWithConfiguration 从 device 表补充的完整设备配置，可为 null
     * @param attributes 该设备的服务器属性键值表
     * @return 合并后的设备上下文
     */
    private MonitoringDeviceContextBO toDeviceContext(
            DeviceInfo device, Device deviceWithConfiguration, Map<String, String> attributes) {
        String deviceProtocol = null;
        // 对应 device.device_data.transportConfiguration.type，直接读取设备配置。
        DeviceData deviceData = deviceWithConfiguration != null
                ? deviceWithConfiguration.getDeviceData() : device.getDeviceData();
        if (deviceData != null && deviceData.getTransportConfiguration() != null
                && deviceData.getTransportConfiguration().getType() != null) {
            deviceProtocol = deviceData.getTransportConfiguration().getType().name();
        }
        String gatewayIdentifier = attributes.get("gatewayIdentifier");
        if (gatewayIdentifier == null || gatewayIdentifier.isBlank()) {
            gatewayIdentifier = attributes.get("gatewayId");
        }
        return MonitoringDeviceContextBO.builder()
                .deviceId(device.getId().getId())
                .deviceName(device.getName())
                .deviceProfileName(device.getDeviceProfileName() == null
                        ? device.getType() : device.getDeviceProfileName())
                .productCategory(attributes.get(PRODUCT_CATEGORY_ATTRIBUTE))
                .productName(attributes.get("productName"))
                .functionCategory(attributes.get("functionCategory"))
                .deviceProtocol(deviceProtocol)
                // Device 只包含配置，不包含 active；回退值必须来自权限查询返回的 DeviceInfo。
                .active(resolveActive(attributes, device.isActive()))
                .lastActivityTime(parseTimestamp(attributes.get("lastActivityTime")))
                .gatewayIdentifier(gatewayIdentifier)
                .meterAddress(attributes.get("meterAddress"))
                .circuitName(attributes.get("circuitName"))
                .timeSetting(attributes.get("timeSetting"))
                // 经纬度与设备 ID 一起保留，接口 DTO 映射后供前端恢复设备与地图点位关系。
                .latitude(parseCoordinate(attributes.get("latitude"), -90, 90))
                .longitude(parseCoordinate(attributes.get("longitude"), -180, 180))
                .build();
    }

    /**
     * 解析地图坐标服务器属性。
     * 无值、非数字或超出地理坐标范围时返回 null，防止前端把无效点位
     * 渲染到地图边界之外。
     */
    private Double parseCoordinate(String value, double min, double max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double coordinate = Double.parseDouble(value);
            return Double.isFinite(coordinate) && coordinate >= min && coordinate <= max ? coordinate : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 device 表批量取回 device_data，避免依赖 device_info_view 是否投影了 JSONB 列。
     * visibleDevices 已经过平台权限过滤，因此这里只按这些可见设备 ID补充配置数据。
     */
    private Map<UUID, Device> findDevicesWithConfiguration(List<DeviceInfo> visibleDevices) {
        if (visibleDevices.isEmpty()) {
            return new HashMap<>();
        }
        List<DeviceId> deviceIds = new ArrayList<>();
        for (DeviceInfo device : visibleDevices) {
            if (device.getId() != null && device.getId().getId() != null) {
                deviceIds.add(device.getId());
            }
        }
        Map<UUID, Device> devicesById = new HashMap<>();
        for (Device device : deviceService.findDevicesByIds(deviceIds)) {
            if (device.getId() != null && device.getId().getId() != null) {
                devicesById.put(device.getId().getId(), device);
            }
        }
        return devicesById;
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
            device.setActive(resolveActive(deviceAttributes, device.isActive()));
            lightningDevices.add(device);
        }
        return lightningDevices;
    }

    /**
     * 统一设备在线状态口径：服务器属性存在时优先使用属性，属性缺失时回退到设备实体状态。
     */
    private boolean resolveActive(Map<String, String> attributes, boolean fallbackActive) {
        if (attributes != null && attributes.containsKey(ACTIVE_ATTRIBUTE)) {
            return MonitoringValueParser.isActive(attributes.get(ACTIVE_ATTRIBUTE));
        }
        return fallbackActive;
    }

    private boolean isLightningDevice(Map<String, String> attributes) {
        if (attributes == null) {
            return false;
        }
        String category = attributes.get(PRODUCT_CATEGORY_ATTRIBUTE);
        return category != null && LIGHTNING_PRODUCT_CATEGORY.equalsIgnoreCase(category.trim());
    }

    /** productCategory 过滤完成后再分页，totalElements 才是防雷设备总数。 */
    private <T> PageData<T> createPageData(List<T> data, PageLink pageLink) {
        int total = data.size();
        int from = Math.min(pageLink.getPage() * pageLink.getPageSize(), total);
        int to = Math.min(from + pageLink.getPageSize(), total);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageLink.getPageSize());
        return new PageData<>(data.subList(from, to), totalPages, total, to < total);
    }

}
