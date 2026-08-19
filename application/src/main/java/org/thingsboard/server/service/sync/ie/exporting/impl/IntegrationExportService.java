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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

/**
 * An integration points at an uplink converter and optionally a downlink converter, so both are
 * remapped to external ids. The connection configuration blob is left as-is.
 */
@Service
@TbCoreComponent
public class IntegrationExportService extends BaseEntityExportService<IntegrationId, Integration, EntityExportData<Integration>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Integration integration, EntityExportData<Integration> exportData) {
        integration.setDefaultConverterId(getExternalIdOrElseInternal(ctx, integration.getDefaultConverterId()));
        integration.setDownlinkConverterId(getExternalIdOrElseInternal(ctx, integration.getDownlinkConverterId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.INTEGRATION);
    }

}
