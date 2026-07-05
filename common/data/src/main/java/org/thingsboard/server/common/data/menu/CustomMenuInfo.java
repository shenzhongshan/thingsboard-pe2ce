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
package org.thingsboard.server.common.data.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Lightweight metadata for a named custom menu (no config payload) — used for the menu
 * management list page. {@link CustomMenu} extends this with the actual menu item tree.
 */
@Schema
public class CustomMenuInfo extends BaseData<CustomMenuId> implements HasTenantId {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name of the custom menu", example = "Tenant default menu")
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Scope the menu applies to")
    private CMScope scope;

    @Schema(description = "Who this menu is assigned to")
    private CMAssigneeType assigneeType;

    public CustomMenuInfo() {
        super();
    }

    public CustomMenuInfo(CustomMenuId id) {
        super(id);
    }

    public CustomMenuInfo(CustomMenuInfo other) {
        super(other);
        this.tenantId = other.getTenantId();
        this.name = other.getName();
        this.scope = other.getScope();
        this.assigneeType = other.getAssigneeType();
    }

    @Schema(description = "The Id of the Custom Menu, auto-generated, UUID")
    @Override
    public CustomMenuId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the menu creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CMScope getScope() {
        return scope;
    }

    public void setScope(CMScope scope) {
        this.scope = scope;
    }

    public CMAssigneeType getAssigneeType() {
        return assigneeType;
    }

    public void setAssigneeType(CMAssigneeType assigneeType) {
        this.assigneeType = assigneeType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((assigneeType == null) ? 0 : assigneeType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CustomMenuInfo other = (CustomMenuInfo) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (scope != other.scope) {
            return false;
        }
        return assigneeType == other.assigneeType;
    }

    @Override
    public String toString() {
        return "CustomMenuInfo [tenantId=" + tenantId + ", name=" + name + ", scope=" + scope
                + ", assigneeType=" + assigneeType + "]";
    }
}
