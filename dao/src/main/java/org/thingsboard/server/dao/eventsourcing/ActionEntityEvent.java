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
package org.thingsboard.server.dao.eventsourcing;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
public class ActionEntityEvent<T> {
    private final TenantId tenantId;
    private final T entity;
    private final EntityId entityId;
    private final EdgeId edgeId;
    private final String body;
    private final ActionType actionType;
    // 动作发生所在的实体分组。PE 的 edge 同步以分组为单位，这个值最终变成
    // edge_event.entity_group_id，再变成实体消息上的 entityGroupIdMSB/LSB。
    // 生产方（分配实体到分组的流程）在后续阶段接入，在那之前恒为 null。
    private final EntityGroup entityGroup;
    /**
     * 显式指定 edge 事件类型，覆盖"由 entityId 的 EntityType 反推"的默认行为。
     *
     * <p>白标/自定义菜单/自定义翻译这类 owner-scoped 设置必须用它：它们没有自己的实体 id，
     * entityId 填的是所有者（租户或客户），反推只会得到 TENANT/CUSTOMER，于是事件被当成
     * 租户更新下发，真正的设置内容永远到不了 edge。对齐 PE 的同名字段。
     */
    private final EdgeEventType edgeEventType;
}
