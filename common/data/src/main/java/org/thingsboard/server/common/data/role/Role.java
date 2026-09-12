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
package org.thingsboard.server.common.data.role;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * CE port of PE's role. Permissions JSON shape matches PE: GENERIC roles carry a map of
 * {resource: [operations]}, GROUP roles carry a plain [operations] list. CE simplification:
 * roles are a managed data model only — CE's authority-based security is NOT rewired to
 * enforce them.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseDataWithAdditionalInfo<RoleId> implements HasName, HasTenantId,
        HasCustomerId, HasVersion, ExportableEntity<RoleId> {

    private static final long serialVersionUID = 5582010124562018986L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    /**
      * Owner of the role. A customer-scoped role may only be granted within that customer's subtree;
      * a tenant-level role is usable anywhere in the tenant. Following PE, "tenant-level" is stored
      * as the NULL_UUID sentinel rather than SQL NULL so equality lookups by (tenant, customer, name)
      * work - but every read path here also tolerates a plain NULL for rows predating the column.
      */
    @Getter @Setter
    @Schema(description = "JSON object with Customer Id. Null (or the null-UUID) means tenant-level.")
    private CustomerId customerId;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Role name", example = "Read Only")
    private String name;

    @Getter @Setter
    @Schema(description = "Role type: GENERIC or GROUP")
    private RoleType type;

    @Getter @Setter
    @Schema(description = "Role permissions: {resource: [operations]} for GENERIC, [operations] for GROUP", implementation = JsonNode.class)
    private JsonNode permissions;

    @Getter @Setter
    private RoleId externalId;

    @Getter @Setter
    private Long version;

    public Role() {
        super();
    }

    public Role(RoleId id) {
        super(id);
    }

    /** The entity that owns this role: the customer when customer-scoped, otherwise the tenant. */
    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    public void setOwnerId(EntityId entityId) {
        this.customerId = EntityType.CUSTOMER.equals(entityId.getEntityType())
                ? new CustomerId(entityId.getId()) : new CustomerId(CustomerId.NULL_UUID);
    }

    public boolean isTenantLevel() {
        return customerId == null || customerId.isNullUid();
    }

    public Role(Role role) {
        super(role);
        this.tenantId = role.getTenantId();
        this.customerId = role.getCustomerId();
        this.name = role.getName();
        this.type = role.getType();
        this.permissions = role.getPermissions();
        this.externalId = role.getExternalId();
        this.version = role.getVersion();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Role [tenantId=" + tenantId + ", name=" + name + ", type=" + type + "]";
    }

}
