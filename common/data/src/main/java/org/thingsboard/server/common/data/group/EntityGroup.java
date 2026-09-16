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
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * CE port of PE's entity group. Simplifications: owner is TENANT or CUSTOMER only (PE also
 * allows other owner types), group types limited to USER/CUSTOMER/DEVICE/ASSET/ENTITY_VIEW/
 * DASHBOARD, no edge sync. The "All" group is a real persisted group whose membership is
 * maintained via ordinary Contains/FROM_ENTITY_GROUP relations, matching PE.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class EntityGroup extends BaseData<EntityGroupId> implements HasName, HasTenantId,
        HasVersion, ExportableEntity<EntityGroupId> {

    private static final long serialVersionUID = 2807349040519543363L;

    // 与 PE 4.3.1.1 一致的 7 种可分组实体。新增一项时务必同步检查所有按 group type 分派的
    // switch —— BaseEntityGroupService 里是 default -> throw，只加类型不加分支会在运行时
    // 才炸而编译完全通过。EntityGroupTypeDispatchTest 是这条约束的守卫。
    public static final EntityType[] groupTypes = new EntityType[]{
            EntityType.USER, EntityType.CUSTOMER, EntityType.DEVICE, EntityType.ASSET,
            EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE
    };
    public static final String GROUP_ALL_NAME = "All";

    @Getter @Setter
    @Schema(description = "Type of the entities in the group", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityType type;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Entity group name", example = "Temperature sensors")
    private String name;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id (the group owner)", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @Schema(description = "JSON object with the owner's entity id — TENANT for a tenant-owned group, CUSTOMER for a customer-owned group")
    private EntityId ownerId;

    @Getter @Setter
    @NoXss
    @Schema(description = "Additional parameters of the entity group (description, isPublic)", implementation = JsonNode.class)
    private JsonNode additionalInfo;

    @Getter @Setter
    @Schema(description = "Reserved: group entity-table view configuration (columns, settings, actions)", implementation = JsonNode.class)
    private JsonNode configuration;

    @Getter @Setter
    private EntityGroupId externalId;

    @Getter @Setter
    private Long version;

    public EntityGroup() {
        super();
    }

    public EntityGroup(EntityGroupId id) {
        super(id);
    }

    public EntityGroup(EntityGroup entityGroup) {
        super(entityGroup);
        this.type = entityGroup.getType();
        this.name = entityGroup.getName();
        this.tenantId = entityGroup.getTenantId();
        this.ownerId = entityGroup.getOwnerId();
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
        this.externalId = entityGroup.getExternalId();
        this.version = entityGroup.getVersion();
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isGroupAll() {
        return GROUP_ALL_NAME.equals(name);
    }

    @JsonIgnore
    public boolean isPublic() {
        return additionalInfo != null && additionalInfo.has("isPublic") && additionalInfo.get("isPublic").asBoolean();
    }

    @Override
    public String toString() {
        return "EntityGroup [type=" + type + ", name=" + name + ", tenantId=" + tenantId + "]";
    }

}
