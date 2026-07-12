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

import org.thingsboard.server.common.data.id.EntityGroupId;

import java.io.Serializable;
import java.util.Collections;
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
        return checkOperation(genericPermissions.get(Resource.ALL), operation)
                || checkOperation(genericPermissions.get(resource), operation);
    }

    public boolean hasGroupPermissions(EntityGroupId entityGroupId, Operation operation) {
        MergedGroupPermissionInfo info = groupPermissions.get(entityGroupId);
        return info != null && checkOperation(info.getOperations(), operation);
    }

    private static boolean checkOperation(Set<Operation> operations, Operation operation) {
        return operations != null && (operations.contains(Operation.ALL) || operations.contains(operation));
    }

}
