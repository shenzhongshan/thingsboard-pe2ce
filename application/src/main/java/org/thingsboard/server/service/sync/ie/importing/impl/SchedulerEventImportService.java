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
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SchedulerEventImportService extends BaseEntityImportService<SchedulerEventId, SchedulerEvent, EntityExportData<SchedulerEvent>> {

    private final SchedulerEventService schedulerEventService;

    @Override
    protected void setOwner(TenantId tenantId, SchedulerEvent schedulerEvent, IdProvider idProvider) {
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setCustomerId(idProvider.getInternalId(schedulerEvent.getCustomerId()));
    }

    @Override
    protected SchedulerEvent prepare(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent, SchedulerEvent old,
                                     EntityExportData<SchedulerEvent> exportData, IdProvider idProvider) {
        // The originator may legitimately point outside the exported set, so do not fail the import
        // when it cannot be resolved — keep whatever id came in.
        schedulerEvent.setOriginatorId(idProvider.getInternalId(schedulerEvent.getOriginatorId(), false));
        return schedulerEvent;
    }

    @Override
    protected SchedulerEvent deepCopy(SchedulerEvent schedulerEvent) {
        return new SchedulerEvent(schedulerEvent);
    }

    @Override
    protected SchedulerEvent saveOrUpdate(EntitiesImportCtx ctx, SchedulerEvent schedulerEvent,
                                          EntityExportData<SchedulerEvent> exportData, IdProvider idProvider,
                                          CompareResult compareResult) {
        return schedulerEventService.saveSchedulerEvent(schedulerEvent);
    }

    @Override
    protected void cleanupForComparison(SchedulerEvent e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
