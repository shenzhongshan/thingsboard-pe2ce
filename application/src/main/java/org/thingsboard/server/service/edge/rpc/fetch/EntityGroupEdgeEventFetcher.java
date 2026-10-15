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
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.EntityGroupService;

/**
 * 下发分配给这台 edge 的实体分组本身（不含组内实体 —— 那是
 * {@link EntityGroupEntitiesEdgeEventFetcher} 的活）。
 *
 * <p>只取通过 {@code assignEntityGroupToEdge} 建立了 EDGE 关系的组，不是租户下的全部组。
 */
@AllArgsConstructor
@Slf4j
public class EntityGroupEdgeEventFetcher extends BasePageableEdgeEventFetcher<EntityGroup> {

    private final EntityGroupService entityGroupService;
    // 同步循环推完这批组之后，要按组类型为每个组追加"组权限"和"组实体"两个 fetcher，
    // 所以类型得能被读到（见 EdgeGrpcSession.doSync）。
    @lombok.Getter
    private final EntityType groupType;

    @Override
    PageData<EntityGroup> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return entityGroupService.findEdgeEntityGroupsByType(tenantId, edge.getId(), groupType, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, EntityGroup entityGroup) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_GROUP,
                EdgeEventActionType.ADDED, entityGroup.getId(), null);
    }

}
