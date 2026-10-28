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

/**
 * 单个产品型号的手动创建设备映射。
 *
 * <p>该对象只承载配置，不负责读取设备、写入属性或执行业务逻辑。
 * 三个字段由同一个型号映射统一维护，避免产品名称、产品大类和功能分类分散在多张配置表中。</p>
 */
@Getter
@Setter
public class DeviceProductMapping {

    /** 设备在平台中的功能分类，例如 spd_monitor、ground_resistance。 */
    private String functionCategory;

    /** 产品中文展示名称。 */
    private String productName;

    /** 产品大类，例如 lightning。 */
    private String productCategory;
}
