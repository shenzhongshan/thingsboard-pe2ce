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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.CUSTOM_MENU_TABLE_NAME)
public final class CustomMenuEntity extends BaseSqlEntity<CustomMenu> implements BaseEntity<CustomMenu> {

    @Column(name = ModelConstants.CUSTOM_MENU_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.CUSTOM_MENU_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.CUSTOM_MENU_SCOPE_PROPERTY)
    private CMScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.CUSTOM_MENU_ASSIGNEE_TYPE_PROPERTY)
    private CMAssigneeType assigneeType;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CUSTOM_MENU_CONFIG_PROPERTY)
    private JsonNode config;

    public CustomMenuEntity() {
        super();
    }

    public CustomMenuEntity(CustomMenu customMenu) {
        if (customMenu.getId() != null) {
            this.setUuid(customMenu.getId().getId());
        }
        this.setCreatedTime(customMenu.getCreatedTime());
        this.tenantId = customMenu.getTenantId().getId();
        this.name = customMenu.getName();
        this.scope = customMenu.getScope();
        this.assigneeType = customMenu.getAssigneeType();
        if (customMenu.getConfig() != null) {
            this.config = JacksonUtil.valueToTree(customMenu.getConfig());
        }
    }

    @Override
    public CustomMenu toData() {
        CustomMenu customMenu = new CustomMenu(new CustomMenuId(id));
        customMenu.setCreatedTime(createdTime);
        customMenu.setTenantId(TenantId.fromUUID(tenantId));
        customMenu.setName(name);
        customMenu.setScope(scope);
        customMenu.setAssigneeType(assigneeType);
        if (config != null) {
            customMenu.setConfig(JacksonUtil.treeToValue(config, CustomMenuConfig.class));
        }
        return customMenu;
    }

    public CustomMenuInfo toInfo() {
        CustomMenuInfo info = new CustomMenuInfo(new CustomMenuId(id));
        info.setCreatedTime(createdTime);
        info.setTenantId(TenantId.fromUUID(tenantId));
        info.setName(name);
        info.setScope(scope);
        info.setAssigneeType(assigneeType);
        return info;
    }

}
