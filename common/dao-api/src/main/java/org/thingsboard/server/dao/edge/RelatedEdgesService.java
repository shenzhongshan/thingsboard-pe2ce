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
package org.thingsboard.server.dao.edge;

import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public interface RelatedEdgesService {

    PageData<EdgeId> findEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    /**
     * 直连分配 ∪ 经分组分配 的带缓存版本，用于 DEVICE/ASSET/ENTITY_VIEW/DASHBOARD/USER。
     *
     * <p>与 {@link #findEdgeIdsByEntityId} 共用同一个 cache key（tenantId + entityId）：
     * 一个实体类型只会走其中一个方法，不存在两种语义同时缓存的情况。若分成两个 key，
     * 失效时就得记得清两处，漏一处就是陈旧结果。
     */
    PageData<EdgeId> findEdgeIdsByEntityIdIncludingGroups(TenantId tenantId, EntityId entityId, PageLink pageLink);

    void publishRelatedEdgeIdsEvictEvent(TenantId tenantId, EntityId entityId);

    /**
     * 整租户清空 related-edges 缓存。用于"受影响的键集合未知"的场景 ——
     * 例如一个实体分组被分配/取消分配给 edge，或分组本身被删除。
     */
    void publishEdgeIdsEvictEventByTenantId(TenantId tenantId);

}
