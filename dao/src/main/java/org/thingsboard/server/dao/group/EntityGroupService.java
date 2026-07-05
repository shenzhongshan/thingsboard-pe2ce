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

    void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId);

    void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    PageData<? extends HasName> findGroupEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId, boolean isPublic);

}
