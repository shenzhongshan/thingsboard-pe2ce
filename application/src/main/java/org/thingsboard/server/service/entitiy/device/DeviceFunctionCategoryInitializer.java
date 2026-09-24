/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 在设备新建成功后初始化其产品相关服务器属性。
 *
 * <p>功能分类属于平台侧的产品管理信息，而非设备遥测数据，因此保存到
 * {@link AttributeScope#SERVER_SCOPE}。设备通过 MQTT/HTTP 上报属性时不会覆盖这个值。</p>
 *
 * <p>三个属性从同一个型号映射中读取，避免产品名称、产品大类和功能分类出现不一致。</p>
 */
@Service
@TbCoreComponent
public class DeviceFunctionCategoryInitializer {

    /** 统一的服务器属性键，仪表盘和实体查询应使用此键筛选功能分类。 */
    private static final String FUNCTION_CATEGORY_ATTRIBUTE = "functionCategory";
    /** 统一的服务器属性键，用于存储产品的中文展示名称。 */
    private static final String PRODUCT_NAME_ATTRIBUTE = "productName";
    /** 统一的服务器属性键，用于存储产品大类，例如 lightning、electric。 */
    private static final String PRODUCT_CATEGORY_ATTRIBUTE = "productCategory";

    private final AttributesService attributesService;
    private final DeviceProfileService deviceProfileService;
    private final DeviceFunctionCategoryResolver productMappingResolver;

    /**
     * Spring 运行时使用的构造器：将 YAML 中的一份产品映射转换为解析器。
     */
    @Autowired
    public DeviceFunctionCategoryInitializer(AttributesService attributesService,
                                             DeviceProfileService deviceProfileService,
                                             DeviceFunctionCategoryProperties properties) {
        this(attributesService, deviceProfileService,
                new DeviceFunctionCategoryResolver(properties.getProducts()));
    }

    /** 供单元测试或其他明确需要自定义映射的调用方使用。 */
    public DeviceFunctionCategoryInitializer(AttributesService attributesService,
                                             DeviceProfileService deviceProfileService,
                                             DeviceFunctionCategoryResolver productMappingResolver) {
        this.attributesService = attributesService;
        this.deviceProfileService = deviceProfileService;
        this.productMappingResolver = productMappingResolver;
    }

    /**
     * 按设备型号写入产品相关服务器属性。
     *
     * <p>优先使用设备类型匹配配置；设备类型未匹配时，再使用 Device Profile 名称兜底。
     * 未找到映射时不阻止设备创建，也不会写入空值属性。</p>
     *
     * @param device 已成功保存并具有设备 ID 的设备
     */
    public void initialize(Device device) {
        String profileName = findDeviceProfileName(device);
        DeviceProductMapping mapping = productMappingResolver.resolve(device.getType(), profileName);
        if (mapping == null) {
            return;
        }
        saveAttributeIfPresent(device, FUNCTION_CATEGORY_ATTRIBUTE, mapping.getFunctionCategory());
        saveAttributeIfPresent(device, PRODUCT_NAME_ATTRIBUTE, mapping.getProductName());
        saveAttributeIfPresent(device, PRODUCT_CATEGORY_ATTRIBUTE, mapping.getProductCategory());
    }

    /** 根据设备配置 ID 读取型号名称；没有配置时返回 null。 */
    private String findDeviceProfileName(Device device) {
        if (device.getDeviceProfileId() == null) {
            return null;
        }
        DeviceProfile profile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
        return profile == null ? null : profile.getName();
    }

    /** 只写入配置中实际声明的属性，避免生成空值服务器属性。 */
    private void saveAttributeIfPresent(Device device, String key, String value) {
        if (value != null && !value.isBlank()) {
            saveAttribute(device, key, value);
        }
    }

    /** 使用 ThingsBoard 既有的属性服务写入平台维护的服务器属性。 */
    private void saveAttribute(Device device, String key, String value) {
        attributesService.save(
                device.getTenantId(), device.getId(), AttributeScope.SERVER_SCOPE,
                new BaseAttributeKvEntry(new StringDataEntry(key, value), System.currentTimeMillis())
        );
    }
}
