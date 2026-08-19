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
package org.thingsboard.server.service.entitiy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.MergedUserPermissions;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

/**
 * Backs the "entities available to the current user" endpoints (PE's {@code /api/user/devices} and
 * friends), which answer "what may I see", not "what belongs to this owner".
 * <p>
 * The result is the union of two grants:
 * <ul>
 *     <li><b>owner scope</b> — everything of that type the user's authority already lets them read
 *     (the whole tenant for a tenant admin, their own customer for a customer user), or that a
 *     GENERIC role grants them;</li>
 *     <li><b>shared groups</b> — the entity groups handed to them by GROUP roles, which may belong
 *     to other owners.</li>
 * </ul>
 * Owner scope deliberately comes from {@link AccessControlService}, not from
 * {@link MergedUserPermissions} alone: in CE, {@code MergedUserPermissions} carries only the
 * role-based overlay and is empty for a tenant admin with no roles assigned
 * ({@code DefaultUserPermissionsService} returns ALL for SYS_ADMIN only). Consulting just the
 * overlay made these endpoints return an empty page for ordinary tenant admins and hid a customer
 * user's own devices — caught by live verification, not by the unit tests.
 * <p>
 * A user with neither grant gets an empty page rather than a 403: the endpoints answer a question
 * about visibility, and "nothing is visible to you" is a valid answer.
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class UserEntitiesService {

    private final EntityGroupService entityGroupService;
    private final AccessControlService accessControlService;

    public PageData<? extends HasName> findUserEntities(SecurityUser user, EntityType entityType,
                                                        Operation operation, PageLink pageLink) throws ThingsboardException {
        boolean ownerScope = accessControlService.hasPermission(user, Resource.of(entityType), operation);

        List<EntityGroupId> sharedGroupIds = List.of();
        MergedUserPermissions permissions = user.getUserPermissions();
        if (permissions != null) {
            MergedUserPermissions.GroupTypePermissions typePermissions =
                    permissions.getGroupPermissionsByEntityTypeAndOperation(entityType, operation);
            sharedGroupIds = typePermissions.entityGroupIds();
            ownerScope = ownerScope || typePermissions.hasGenericRead();
        }
        if (!ownerScope && sharedGroupIds.isEmpty()) {
            return new PageData<>(List.of(), 0, 0, false);
        }
        // Tenant admins are scoped to the whole tenant, so they pass no owner; customer users are
        // scoped to their own customer.
        CustomerId ownerId = Authority.CUSTOMER_USER.equals(user.getAuthority()) ? user.getCustomerId() : null;
        return entityGroupService.findUserGroupEntities(user.getTenantId(), entityType, ownerId,
                sharedGroupIds, ownerScope, pageLink);
    }

}
