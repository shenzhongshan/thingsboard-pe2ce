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
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

/**
 * The template configuration itself is opaque JSON and may embed dashboard/entity references; those
 * are not remapped, matching how this codebase treats other opaque configuration blobs.
 */
@Service
@TbCoreComponent
public class ReportTemplateExportService extends BaseEntityExportService<ReportTemplateId, ReportTemplate, EntityExportData<ReportTemplate>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, ReportTemplate reportTemplate, EntityExportData<ReportTemplate> exportData) {
        reportTemplate.setCustomerId(getExternalIdOrElseInternal(ctx, reportTemplate.getCustomerId()));
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.REPORT_TEMPLATE);
    }

}
