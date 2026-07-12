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
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * Computes and caches the effective {@link MergedUserPermissions} for a user, and exposes
 * invalidation hooks called whenever a role, group-permission, or user-group membership changes.
 */
public interface UserPermissionsService {

    MergedUserPermissions getMergedPermissions(SecurityUser user, boolean isPublic);

    void onRoleUpdated(Role role);

    void onGroupPermissionUpdated(GroupPermission groupPermission);

    void onGroupPermissionDeleted(GroupPermission groupPermission);

    void onUserGroupMembershipChanged(TenantId tenantId, EntityGroupId userGroupId);

    void onUserUpdatedOrRemoved(User user);

}
