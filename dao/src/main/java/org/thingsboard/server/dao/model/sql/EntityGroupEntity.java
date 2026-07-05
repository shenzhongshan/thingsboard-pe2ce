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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "entity_group")
public class EntityGroupEntity extends BaseSqlEntity<EntityGroup> implements BaseEntity<EntityGroup> {

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private EntityType type;

    @Column(name = "name")
    private String name;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    @Convert(converter = JsonConverter.class)
    @Column(name = "configuration")
    private JsonNode configuration;

    public EntityGroupEntity() {
        super();
    }

    public EntityGroupEntity(EntityGroup entityGroup) {
        if (entityGroup.getId() != null) {
            this.setUuid(entityGroup.getId().getId());
        }
        this.setCreatedTime(entityGroup.getCreatedTime());
        this.type = entityGroup.getType();
        this.name = entityGroup.getName();
        if (entityGroup.getTenantId() != null) {
            this.tenantId = entityGroup.getTenantId().getId();
        }
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
    }

    @Override
    public EntityGroup toData() {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(id));
        entityGroup.setCreatedTime(createdTime);
        entityGroup.setType(type);
        entityGroup.setName(name);
        if (tenantId != null) {
            entityGroup.setTenantId(TenantId.fromUUID(tenantId));
        }
        entityGroup.setAdditionalInfo(additionalInfo);
        entityGroup.setConfiguration(configuration);
        return entityGroup;
    }

}
