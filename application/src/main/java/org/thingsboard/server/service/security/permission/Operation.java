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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Operation {

    ALL(true),
    CREATE(true, true, true),
    READ(true),
    WRITE(true, false, true),
    DELETE(true, true, true),
    ASSIGN_TO_CUSTOMER,
    UNASSIGN_FROM_CUSTOMER,
    RPC_CALL(true),
    READ_CREDENTIALS(true),
    WRITE_CREDENTIALS(true),
    READ_ATTRIBUTES(true),
    WRITE_ATTRIBUTES(true, false, true),
    READ_TELEMETRY(true),
    WRITE_TELEMETRY(true, false, true),
    CLAIM_DEVICES,
    ASSIGN_TO_TENANT,
    READ_CALCULATED_FIELD(true),
    WRITE_CALCULATED_FIELD(true),
    // PE-parity group-role operations (added for RBAC enforcement port).
    ADD_TO_GROUP,
    REMOVE_FROM_GROUP,
    CHANGE_OWNER,
    SHARE_GROUP(true);

    public static final Set<Operation> defaultEntityOperations = new HashSet<>(Arrays.asList(
            ALL, READ, WRITE, CREATE, DELETE, READ_ATTRIBUTES, WRITE_ATTRIBUTES, READ_TELEMETRY, WRITE_TELEMETRY, CHANGE_OWNER));
    public static final Set<Operation> defaultEntityGroupOperations = new HashSet<>(Arrays.asList(
            ALL, READ, WRITE, CREATE, DELETE, READ_ATTRIBUTES, WRITE_ATTRIBUTES, READ_TELEMETRY, WRITE_TELEMETRY,
            ADD_TO_GROUP, REMOVE_FROM_GROUP, SHARE_GROUP));
    public static final Set<Operation> crudOperations = new HashSet<>(Arrays.asList(ALL, READ, WRITE, CREATE, DELETE));

    private final boolean allowedForGroupRole;
    private final boolean allowedForGroupOwnerOnly;
    private final boolean groupOperationAllowedForGroupOwnerOnly;

    Operation() {
        this(false, false, false);
    }

    Operation(boolean allowedForGroupRole) {
        this(allowedForGroupRole, false, false);
    }

    Operation(boolean allowedForGroupRole, boolean allowedForGroupOwnerOnly) {
        this(allowedForGroupRole, allowedForGroupOwnerOnly, false);
    }

    Operation(boolean allowedForGroupRole, boolean allowedForGroupOwnerOnly, boolean groupOperationAllowedForGroupOwnerOnly) {
        this.allowedForGroupRole = allowedForGroupRole;
        this.allowedForGroupOwnerOnly = allowedForGroupOwnerOnly;
        this.groupOperationAllowedForGroupOwnerOnly = groupOperationAllowedForGroupOwnerOnly;
    }

    public boolean isAllowedForGroupRole() {
        return allowedForGroupRole;
    }

    public boolean isAllowedForGroupOwnerOnly() {
        return allowedForGroupOwnerOnly;
    }

    public boolean isGroupOperationAllowedForGroupOwnerOnly() {
        return groupOperationAllowedForGroupOwnerOnly;
    }

}
