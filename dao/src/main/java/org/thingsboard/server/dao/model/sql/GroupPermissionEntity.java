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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "group_permission")
public class GroupPermissionEntity extends BaseSqlEntity<GroupPermission> implements BaseEntity<GroupPermission> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "user_group_id")
    private UUID userGroupId;

    @Column(name = "entity_group_id")
    private UUID entityGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_group_type")
    private EntityType entityGroupType;

    @Column(name = "is_public")
    private boolean isPublic;

    public GroupPermissionEntity() {
        super();
    }

    public GroupPermissionEntity(GroupPermission groupPermission) {
        if (groupPermission.getId() != null) {
            this.setUuid(groupPermission.getId().getId());
        }
        this.setCreatedTime(groupPermission.getCreatedTime());
        if (groupPermission.getTenantId() != null) {
            this.tenantId = groupPermission.getTenantId().getId();
        }
        if (groupPermission.getRoleId() != null) {
            this.roleId = groupPermission.getRoleId().getId();
        }
        if (groupPermission.getUserGroupId() != null) {
            this.userGroupId = groupPermission.getUserGroupId().getId();
        }
        if (groupPermission.getEntityGroupId() != null) {
            this.entityGroupId = groupPermission.getEntityGroupId().getId();
        }
        this.entityGroupType = groupPermission.getEntityGroupType();
        this.isPublic = groupPermission.isPublic();
    }

    @Override
    public GroupPermission toData() {
        GroupPermission groupPermission = new GroupPermission(new GroupPermissionId(id));
        groupPermission.setCreatedTime(createdTime);
        if (tenantId != null) {
            groupPermission.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (roleId != null) {
            groupPermission.setRoleId(new RoleId(roleId));
        }
        if (userGroupId != null) {
            groupPermission.setUserGroupId(new EntityGroupId(userGroupId));
        }
        if (entityGroupId != null) {
            groupPermission.setEntityGroupId(new EntityGroupId(entityGroupId));
        }
        groupPermission.setEntityGroupType(entityGroupType);
        groupPermission.setPublic(isPublic);
        return groupPermission;
    }

}
