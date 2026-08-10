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
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityGroupImportService extends BaseEntityImportService<EntityGroupId, EntityGroup, EntityExportData<EntityGroup>> {

    private final EntityGroupService entityGroupService;

    @Override
    protected void setOwner(TenantId tenantId, EntityGroup entityGroup, IdProvider idProvider) {
        entityGroup.setTenantId(tenantId);
        entityGroup.setOwnerId(idProvider.getInternalId(entityGroup.getOwnerId()));
    }

    @Override
    protected EntityGroup prepare(EntitiesImportCtx ctx, EntityGroup entityGroup, EntityGroup old, EntityExportData<EntityGroup> exportData, IdProvider idProvider) {
        return entityGroup;
    }

    @Override
    protected EntityGroup saveOrUpdate(EntitiesImportCtx ctx, EntityGroup entityGroup, EntityExportData<EntityGroup> exportData, IdProvider idProvider, CompareResult compareResult) {
        return entityGroupService.saveEntityGroup(ctx.getTenantId(), entityGroup);
    }

    @Override
    protected EntityGroup deepCopy(EntityGroup entityGroup) {
        return new EntityGroup(entityGroup);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

}
