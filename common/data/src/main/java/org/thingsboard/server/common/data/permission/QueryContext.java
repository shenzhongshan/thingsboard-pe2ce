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
package org.thingsboard.server.common.data.permission;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QueryContext {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final CustomerId customerId;
    @Getter
    private final EntityType entityType;
    @Getter
    private final boolean ignorePermissionCheck;

    /**
     * Only set for the STATE_ENTITY_OWNER filter, where the queried entity type is not known from the
     * filter alone: it is whatever the resolved owner of the state entity turns out to be (TENANT or
     * CUSTOMER). {@code entityType} above is that owner's type; this is the owner id itself.
     */
    @Getter
    private final EntityId ownerId;

    /**
     * What the querying user may see: which entity types their own owner scope covers, and which
     * entity groups a GROUP role grants them. UNRESTRICTED for internal callers, which run at tenant
     * scope and are not acting for a user.
     */
    @Getter
    private final QueryPermissions queryPermissions;

    @Getter
    private final Map<UUID, UUID> relatedParentIdMap = new HashMap<>();

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType) {
        this(tenantId, customerId, entityType, false);
    }

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, ignorePermissionCheck, null);
    }

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, boolean ignorePermissionCheck, EntityId ownerId) {
        this(tenantId, customerId, entityType, ignorePermissionCheck, ownerId, QueryPermissions.UNRESTRICTED);
    }

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, boolean ignorePermissionCheck,
                        EntityId ownerId, QueryPermissions queryPermissions) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.entityType = entityType;
        this.ignorePermissionCheck = ignorePermissionCheck;
        this.ownerId = ownerId;
        this.queryPermissions = queryPermissions == null ? QueryPermissions.UNRESTRICTED : queryPermissions;
    }

    public boolean isTenantUser() {
        return customerId == null || customerId.isNullUid();
    }
}