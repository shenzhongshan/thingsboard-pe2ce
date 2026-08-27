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

import lombok.RequiredArgsConstructor;

import java.util.Map;
@RequiredArgsConstructor

/**
 * 根据设备类型解析功能分类。
 *
 * <p>该类不直接读取 Spring 配置，也不写数据库，因此可独立测试；设备创建流程只依赖它的解析结果。
 * 未配置的设备类型返回 {@code null}，调用方应跳过属性写入，避免把未知产品误归类。</p>
 */
public class DeviceFunctionCategoryResolver {

    /** 配置中声明的“设备类型 -> 功能分类”映射。 */
    private final Map<String, String> categoriesByDeviceType;

    /**
     * 查询指定设备类型对应的功能分类。
     *
     * @param deviceType ThingsBoard 设备类型，例如 DESA、DES4
     * @return 对应的功能分类编码；未配置时返回 {@code null}
     */
    public String resolve(String deviceType) {
        return categoriesByDeviceType.get(deviceType);
    }
}
