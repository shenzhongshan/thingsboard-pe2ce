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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * CE port of PE's group permission: links a USER entity group to a role, and — for GROUP-type
 * roles — to the target entity group the role applies to. Managed data model only (not enforced
 * by CE's security layer).
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class GroupPermission extends BaseData<GroupPermissionId> implements HasTenantId {

    private static final long serialVersionUID = 5582010124562018986L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @Schema(description = "Id of the USER entity group the permission is granted to")
    private EntityGroupId userGroupId;

    @Getter @Setter
    @Schema(description = "Id of the granted role")
    private RoleId roleId;

    @Getter @Setter
    @Schema(description = "Target entity group id (required for GROUP-type roles)")
    private EntityGroupId entityGroupId;

    @Getter @Setter
    @Schema(description = "Target entity group type (required for GROUP-type roles)")
    private EntityType entityGroupType;

    @Getter @Setter
    @Schema(description = "Indicates a permission auto-created for a public group", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isPublic;

    public GroupPermission() {
        super();
    }

    public GroupPermission(GroupPermissionId id) {
        super(id);
    }

    public GroupPermission(GroupPermission groupPermission) {
        super(groupPermission);
        this.tenantId = groupPermission.getTenantId();
        this.userGroupId = groupPermission.getUserGroupId();
        this.roleId = groupPermission.getRoleId();
        this.entityGroupId = groupPermission.getEntityGroupId();
        this.entityGroupType = groupPermission.getEntityGroupType();
        this.isPublic = groupPermission.isPublic();
    }

    @Override
    public String toString() {
        return "GroupPermission [tenantId=" + tenantId + ", userGroupId=" + userGroupId
                + ", roleId=" + roleId + ", entityGroupId=" + entityGroupId + "]";
    }

}
