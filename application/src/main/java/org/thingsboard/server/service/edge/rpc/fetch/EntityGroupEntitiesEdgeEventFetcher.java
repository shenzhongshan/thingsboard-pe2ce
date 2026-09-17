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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.EntityGroupService;

/**
 * 下发某个已分配分组内的实体。
 *
 * <p>这是全量同步侧 {@code edge_event.entity_group_id} 的供给方 —— 每条事件都带上所属分组，
 * 值一路传到实体消息的 entityGroupIdMSB/LSB。少了它，edge 收到实体却不知道该放进哪个组。
 */
@AllArgsConstructor
@Slf4j
public class EntityGroupEntitiesEdgeEventFetcher extends BasePageableEdgeEventFetcher<EntityId> {

    private final EntityGroupService entityGroupService;
    private final EntityType groupType;
    private final EntityGroupId entityGroupId;

    @Override
    PageData<EntityId> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return entityGroupService.findEntityIds(tenantId, groupType, entityGroupId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, EntityId entityId) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(),
                EdgeUtils.getEdgeEventTypeByEntityType(groupType),
                EdgeEventActionType.ADDED, entityId, null, entityGroupId);
    }

}
