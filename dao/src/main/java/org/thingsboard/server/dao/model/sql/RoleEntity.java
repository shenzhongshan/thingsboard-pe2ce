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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "role")
public class RoleEntity extends BaseSqlEntity<Role> implements BaseEntity<Role> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private RoleType type;

    @Convert(converter = JsonConverter.class)
    @Column(name = "permissions")
    private JsonNode permissions;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public RoleEntity() {
        super();
    }

    public RoleEntity(Role role) {
        if (role.getId() != null) {
            this.setUuid(role.getId().getId());
        }
        this.setCreatedTime(role.getCreatedTime());
        if (role.getTenantId() != null) {
            this.tenantId = role.getTenantId().getId();
        }
        this.name = role.getName();
        this.type = role.getType();
        this.permissions = role.getPermissions();
        this.additionalInfo = role.getAdditionalInfo();
    }

    @Override
    public Role toData() {
        Role role = new Role(new RoleId(id));
        role.setCreatedTime(createdTime);
        if (tenantId != null) {
            role.setTenantId(TenantId.fromUUID(tenantId));
        }
        role.setName(name);
        role.setType(type);
        role.setPermissions(permissions);
        role.setAdditionalInfo(additionalInfo);
        return role;
    }

}
