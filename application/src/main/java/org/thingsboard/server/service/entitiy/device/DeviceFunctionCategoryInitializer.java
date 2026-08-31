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
 * 在设备新建成功后初始化其功能分类服务器属性。
 *
 * <p>功能分类属于平台侧的产品管理信息，而非设备遥测数据，因此保存到 {@link AttributeScope#SERVER_SCOPE}。
 * 设备通过 MQTT/HTTP 上报属性时不会覆盖这个值。</p>
 *
 * <p>未在配置中登记的设备类型不会写入任何属性；这样可以安全地先创建和调试新产品，
 * 待确认分类后再在配置文件中增加映射。</p>
 */
@Service
@TbCoreComponent
public class DeviceFunctionCategoryInitializer {

    /** 统一的服务器属性键，仪表盘和实体查询应使用此键筛选功能分类。 */
    private static final String FUNCTION_CATEGORY_ATTRIBUTE = "functionCategory";
    /** 统一的服务器属性键，用于存储产品的中文展示名称。 */
    private static final String PRODUCT_NAME_ATTRIBUTE = "productName";

    private final AttributesService attributesService;
    private final DeviceProfileService deviceProfileService;
    private final DeviceFunctionCategoryResolver functionCategoryResolver;
    private final DeviceFunctionCategoryResolver productNameResolver;

    /**
     * Spring 运行时使用的构造器：将 YAML 配置转换为独立的解析器。
     * 保持解析器无 Spring 依赖，便于对映射规则进行单元测试。
     */
    @Autowired
    public DeviceFunctionCategoryInitializer(AttributesService attributesService, DeviceProfileService deviceProfileService,
                                             DeviceFunctionCategoryProperties properties) {
        this(attributesService, deviceProfileService,
                new DeviceFunctionCategoryResolver(properties.getByDeviceType()),
                new DeviceFunctionCategoryResolver(properties.getProductNameByDeviceProfile()));
    }

    /**
     * 供单元测试或其他明确需要自定义映射的调用方使用。
     */
    public DeviceFunctionCategoryInitializer(AttributesService attributesService, DeviceProfileService deviceProfileService,
                                             DeviceFunctionCategoryResolver functionCategoryResolver,
                                             DeviceFunctionCategoryResolver productNameResolver) {
        this.attributesService = attributesService;
        this.deviceProfileService = deviceProfileService;
        this.functionCategoryResolver = functionCategoryResolver;
        this.productNameResolver = productNameResolver;
    }

    /**
     * 按设备类型写入功能分类服务器属性。
     *
     * <p>属性服务采用平台既有的异步保存接口；本方法只负责提交写入请求。
     * 新建设备的调用入口保证该方法不会在普通编辑设备时执行。</p>
     *
     * @param device 已成功保存并具有设备 ID 的设备
     */
    public void initialize(Device device) {
        String functionCategory = functionCategoryResolver.resolve(device.getType());
        if (functionCategory != null) {
            saveAttribute(device, FUNCTION_CATEGORY_ATTRIBUTE, functionCategory);
        }

        if (device.getDeviceProfileId() == null) {
            return;
        }
        DeviceProfile profile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
        if (profile == null) {
            return;
        }
        String productName = productNameResolver.resolve(profile.getName());
        if (productName != null) {
            saveAttribute(device, PRODUCT_NAME_ATTRIBUTE, productName);
        }
    }

    /**
     * 使用 ThingsBoard 既有的属性服务写入平台维护的服务器属性。
     */
    private void saveAttribute(Device device, String key, String value) {
        attributesService.save(
                device.getTenantId(), device.getId(), AttributeScope.SERVER_SCOPE,
                new BaseAttributeKvEntry(new StringDataEntry(key, value), System.currentTimeMillis())
        );
    }
}
