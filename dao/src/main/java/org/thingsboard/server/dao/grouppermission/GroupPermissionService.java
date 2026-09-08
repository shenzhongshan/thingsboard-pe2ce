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
package org.thingsboard.server.dao.grouppermission;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.util.List;
import java.util.Optional;

public interface GroupPermissionService {

    GroupPermission saveGroupPermission(TenantId tenantId, GroupPermission groupPermission);

    GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId);

    // Enriched entries (role name/type + entity group name) for the user-group permissions UI.
    List<JsonNode> findGroupPermissionInfosByUserGroupId(TenantId tenantId, EntityGroupId userGroupId);

    // Enriched entries (role name/type + user group name/owner) for the entity-group permissions tab
    // (reverse of the above: which user groups have been granted access to this entity group).
    List<JsonNode> findGroupPermissionInfosByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    void deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId);

    /**
     * Deletes the permissions granted <em>to</em> a user group. Called when the user group itself is
     * deleted - without it the rows survive their owning group and are unreachable through the API.
     */
    void deleteGroupPermissionsByUserGroupId(TenantId tenantId, EntityGroupId userGroupId);

    /**
     * Deletes the permissions granted <em>on</em> an entity group (the reverse direction). Called
     * when the target group is deleted.
     */
    void deleteGroupPermissionsByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    List<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId);

    List<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, org.thingsboard.server.common.data.id.RoleId roleId);

    /**
     * The public (isPublic=true) group permission granted on the given entity group, if any.
     * Used by makePrivate to locate and remove it.
     */
    Optional<GroupPermission> findPublicGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

}
