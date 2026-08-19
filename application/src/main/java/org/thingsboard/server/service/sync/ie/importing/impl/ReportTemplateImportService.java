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
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ReportTemplateImportService extends BaseEntityImportService<ReportTemplateId, ReportTemplate, EntityExportData<ReportTemplate>> {

    private final ReportTemplateService reportTemplateService;

    @Override
    protected void setOwner(TenantId tenantId, ReportTemplate reportTemplate, IdProvider idProvider) {
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setCustomerId(idProvider.getInternalId(reportTemplate.getCustomerId()));
    }

    @Override
    protected ReportTemplate prepare(EntitiesImportCtx ctx, ReportTemplate reportTemplate, ReportTemplate old,
                                     EntityExportData<ReportTemplate> exportData, IdProvider idProvider) {
        return reportTemplate;
    }

    @Override
    protected ReportTemplate deepCopy(ReportTemplate reportTemplate) {
        return new ReportTemplate(reportTemplate);
    }

    @Override
    protected ReportTemplate saveOrUpdate(EntitiesImportCtx ctx, ReportTemplate reportTemplate,
                                          EntityExportData<ReportTemplate> exportData, IdProvider idProvider,
                                          CompareResult compareResult) {
        return reportTemplateService.saveReportTemplate(ctx.getTenantId(), reportTemplate);
    }

    @Override
    protected void cleanupForComparison(ReportTemplate e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

}
