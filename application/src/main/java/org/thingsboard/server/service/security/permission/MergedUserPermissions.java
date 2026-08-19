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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CE port of PE's MergedUserPermissions: the effective, precomputed set of permissions granted to
 * a user via their entity-group memberships and the roles assigned through GroupPermission rows.
 * Two kinds of permission are tracked:
 * <ul>
 *     <li>{@code genericPermissions} — from GENERIC-type roles: tenant/customer-wide "may READ all
 *     DEVICEs" style grants, keyed by {@link Resource}.</li>
 *     <li>{@code groupPermissions} — from GROUP-type roles: "may READ entities inside this specific
 *     entity group", keyed by {@link EntityGroupId}.</li>
 * </ul>
 * CE simplification: the UI-hint precomputed maps PE also carries (readGroupPermissions etc.) are
 * omitted — they only drive optional frontend affordances, not backend enforcement.
 */
public final class MergedUserPermissions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final MergedUserPermissions ALL =
            new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.emptyMap());

    private final Map<Resource, Set<Operation>> genericPermissions;
    private final Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions;

    public MergedUserPermissions(Map<Resource, Set<Operation>> genericPermissions,
                                  Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions) {
        this.genericPermissions = genericPermissions;
        this.groupPermissions = groupPermissions;
    }

    public Map<Resource, Set<Operation>> getGenericPermissions() {
        return genericPermissions;
    }

    public Map<EntityGroupId, MergedGroupPermissionInfo> getGroupPermissions() {
        return groupPermissions;
    }

    public boolean hasGenericPermission(Resource resource, Operation operation) {
        if (checkOperation(genericPermissions.get(Resource.ALL), operation)
                || checkOperation(genericPermissions.get(resource), operation)) {
            return true;
        }
        // A grant on the ENTITY_GROUP umbrella covers every per-type group resource, so roles
        // authored before the per-type split (and the stock roles) keep working unchanged.
        return Resource.groupResources().contains(resource)
                && checkOperation(genericPermissions.get(Resource.ENTITY_GROUP), operation);
    }

    public boolean hasGroupPermissions(EntityGroupId entityGroupId, Operation operation) {
        MergedGroupPermissionInfo info = groupPermissions.get(entityGroupId);
        return info != null && checkOperation(info.getOperations(), operation);
    }

    private static boolean checkOperation(Set<Operation> operations, Operation operation) {
        return operations != null && (operations.contains(Operation.ALL) || operations.contains(operation));
    }

    /**
     * Resolves what this user may reach for one entity type and operation: either a blanket generic
     * grant, or the specific entity groups whose GROUP role covers the operation. This is the input
     * to permission-aware entity listing (PE calls the equivalent MergedGroupTypePermissionInfo).
     */
    public GroupTypePermissions getGroupPermissionsByEntityTypeAndOperation(EntityType entityType, Operation operation) {
        boolean genericRead = hasGenericPermission(Resource.of(entityType), operation);
        List<EntityGroupId> groupIds = new ArrayList<>();
        if (operation.isAllowedForGroupRole()) {
            groupPermissions.forEach((groupId, info) -> {
                if (entityType.equals(info.getEntityType()) && checkOperation(info.getOperations(), operation)) {
                    groupIds.add(groupId);
                }
            });
        }
        return new GroupTypePermissions(genericRead, List.copyOf(groupIds));
    }

    /**
     * @param hasGenericRead the user holds a generic grant for the type — everything in their owner
     *                       scope is reachable, and {@code entityGroupIds} only widens that
     * @param entityGroupIds groups of this type the user was granted through a GROUP role; these may
     *                       belong to other owners, which is the point of sharing a group
     */
    public record GroupTypePermissions(boolean hasGenericRead, List<EntityGroupId> entityGroupIds) {

        public boolean isEmpty() {
            return !hasGenericRead && entityGroupIds.isEmpty();
        }

    }

}
