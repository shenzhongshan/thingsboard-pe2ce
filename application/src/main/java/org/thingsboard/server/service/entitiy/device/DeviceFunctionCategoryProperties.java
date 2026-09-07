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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 产品型号与功能分类的配置绑定对象。
 *
 * <p>配置来源为 {@code thingsboard.yml} 中的 {@code tb.product-category}：
 * {@code by-device-type} 保存细分功能分类，{@code product-name-by-device-profile} 保存中文产品名，
 * {@code product-family-by-device-profile} 保存产品大类。键使用稳定的型号或 Device Profile 名称编码。</p>
 *
 * <p>新增产品时只需补充配置映射，无需修改设备创建流程代码。</p>
 */
@Component
@ConfigurationProperties(prefix = "tb.product-category")
@Getter
@Setter
public class DeviceFunctionCategoryProperties {

    /***
     * 以设备类型为键的功能分类映射。
     * 使用 {@link LinkedHashMap} 保持配置文件中的声明顺序，便于日志、调试和后续管理界面展示。
     */
    private Map<String, String> byDeviceType = new LinkedHashMap<>();

    /***
     * 以设备配置文件名称为键的中文产品名映射。
     * 不使用设备配置文件 UUID，避免配置导入到其他环境后 UUID 改变而导致映射失效。
     */
    private Map<String, String> productNameByDeviceProfile = new LinkedHashMap<>();

    /**
     * 以设备配置文件名称为键的产品大类映射，例如 lightning、electric。
     * 产品大类用于一级业务分类，与 functionCategory 的具体功能分类保持分离。
     */
    private Map<String, String> productFamilyByDeviceProfile = new LinkedHashMap<>();
}
