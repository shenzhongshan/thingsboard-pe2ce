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

import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 根据设备型号解析手动创建设备所需的产品映射。
 *
 * <p>该类不直接读取 Spring 配置，也不写数据库，因此可独立测试；设备创建流程只依赖它的解析结果。
 * 未配置的设备型号返回 {@code null}，调用方应跳过属性写入，避免把未知产品误归类。</p>
 */
@RequiredArgsConstructor
public class DeviceFunctionCategoryResolver {

    /** 配置中声明的“型号 -> 产品映射”配置。 */
    private final Map<String, DeviceProductMapping> productsByModel;

    /**
     * 按优先级查询设备型号对应的产品映射。
     *
     * <p>手动创建时优先使用 {@code device.type}；如果设备类型没有配置，
     * 再使用 Device Profile 名称兜底。这样既保留原有行为，也允许两者采用同一个型号编码。</p>
     *
     * @param deviceType        ThingsBoard 设备类型，例如 DESA、DES4
     * @param deviceProfileName Device Profile 名称
     * @return 对应的产品映射；未配置时返回 {@code null}
     */
    public DeviceProductMapping resolve(String deviceType, String deviceProfileName) {
        DeviceProductMapping mapping = find(deviceType);
        return mapping != null ? mapping : find(deviceProfileName);
    }

    /**
     * 按非空型号查询配置，兼容设备类型大小写差异。
     *
     * <p>手动创建页面可能把型号保存为小写，而配置文件通常使用大写编码；
     * 统一 trim 后进行忽略大小写匹配，避免属性初始化静默失效。</p>
     */
    private DeviceProductMapping find(String model) {
        if (model == null || model.isBlank() || productsByModel == null) {
            return null;
        }
        String normalizedModel = model.trim();
        DeviceProductMapping directMatch = productsByModel.get(normalizedModel);
        if (directMatch != null) {
            return directMatch;
        }
        for (Map.Entry<String, DeviceProductMapping> entry : productsByModel.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(normalizedModel)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
