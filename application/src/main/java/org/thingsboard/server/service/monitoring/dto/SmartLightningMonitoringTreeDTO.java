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
package org.thingsboard.server.service.monitoring.dto;

import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/** 智能防雷资产树服务的传输对象。 */
public record SmartLightningMonitoringTreeDTO(List<TreeNode> roots) {

    /** 树节点传输结构，设备只作为叶子节点返回。 */
    public record TreeNode(UUID id, String name, EntityType entityType, List<TreeNode> children) {
    }
}
