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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 产品型号与功能分类的配置绑定对象。
 *
 * <p>配置来源为 {@code thingsboard.yml} 中的 {@code tb.product-category.products}。
 * 每个型号只维护一份 {@link DeviceProductMapping}，同时保存功能分类、中文产品名和产品大类。</p>
 *
 * <p>新增产品时只需补充配置映射，无需修改设备创建流程代码。</p>
 */
@Component
@ConfigurationProperties(prefix = "tb.product-category")
@Getter
@Setter
public class DeviceFunctionCategoryProperties {

    /**
     * 以稳定型号编码为键的产品映射。
     * 使用 {@link LinkedHashMap} 保持配置文件中的声明顺序，便于日志、调试和管理界面展示。
     */
    private Map<String, DeviceProductMapping> products = new LinkedHashMap<>();
}
