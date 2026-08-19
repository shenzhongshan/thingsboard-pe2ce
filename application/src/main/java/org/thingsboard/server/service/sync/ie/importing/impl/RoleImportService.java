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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class RoleImportService extends BaseEntityImportService<RoleId, Role, EntityExportData<Role>> {

    private final RoleService roleService;

    @Override
    protected void setOwner(TenantId tenantId, Role role,
                            BaseEntityImportService<RoleId, Role, EntityExportData<Role>>.IdProvider idProvider) {
        role.setTenantId(tenantId);
    }

    @Override
    protected Role prepare(EntitiesImportCtx ctx, Role role, Role oldRole, EntityExportData<Role> exportData,
                           BaseEntityImportService<RoleId, Role, EntityExportData<Role>>.IdProvider idProvider) {
        return role;
    }

    @Override
    protected Role deepCopy(Role role) {
        return new Role(role);
    }

    @Override
    protected Role saveOrUpdate(EntitiesImportCtx ctx, Role role, EntityExportData<Role> exportData,
                                BaseEntityImportService<RoleId, Role, EntityExportData<Role>>.IdProvider idProvider,
                                CompareResult compareResult) {
        return roleService.saveRole(ctx.getTenantId(), role);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
