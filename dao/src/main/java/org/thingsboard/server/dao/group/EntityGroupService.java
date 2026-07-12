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
package org.thingsboard.server.dao.group;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

/**
 * CE port of PE's entity group service. Groups are tenant-owned; membership is stored as
 * relations (FROM group TO entity, type "Contains", group FROM_ENTITY_GROUP). The "All" group
 * is virtual: it is auto-seeded per tenant per type and its member listing is the plain tenant
 * entity list.
 */
public interface EntityGroupService {

    String GROUP_RELATION_TYPE = "Contains";

    EntityGroup saveEntityGroup(TenantId tenantId, EntityGroup entityGroup);

    EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityType groupType, PageLink pageLink);

    /** ownerId = null → tenant-owned groups; otherwise the customer's groups of that type. */
    PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, CustomerId ownerId, EntityType groupType, PageLink pageLink);

    void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId);

    void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    PageData<? extends HasName> findGroupEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId, boolean isPublic);

    /** Ensures + returns the "All" group id for the given owner+type (tenant-owned when ownerId is null). */
    EntityGroupId findOrCreateAllGroupId(TenantId tenantId, CustomerId ownerId, EntityType groupType);

    /**
     * All entity-group ids the given entity belongs to: its tenant-level (and, if customerId is
     * provided, its customer-level) implicit "All" group, plus any explicit group memberships
     * (relations). Used by permission enforcement to resolve GROUP-role scoping.
     */
    List<EntityGroupId> findEntityGroupsForEntity(TenantId tenantId, EntityId entityId, CustomerId customerId);

    /** Member user ids of a USER-type entity group (including the virtual "All" group). Used for permission-cache eviction. */
    List<org.thingsboard.server.common.data.id.UserId> findGroupUserIds(TenantId tenantId, EntityGroupId entityGroupId);

    /** Sync lookup by owner+type+name (ownerId null = tenant-owned). Returns null if not found. */
    EntityGroup findEntityGroupByTypeAndName(TenantId tenantId, CustomerId ownerId, EntityType groupType, String name);

    void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    /** Raw member entity ids of a group (including the virtual "All" group). */
    List<EntityId> findAllEntityIds(TenantId tenantId, EntityGroupId entityGroupId);

}
