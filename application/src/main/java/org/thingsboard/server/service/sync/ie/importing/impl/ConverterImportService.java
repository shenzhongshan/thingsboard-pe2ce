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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class ConverterImportService extends BaseEntityImportService<ConverterId, Converter, EntityExportData<Converter>> {

    private final ConverterService converterService;

    @Override
    protected void setOwner(TenantId tenantId, Converter converter, IdProvider idProvider) {
        converter.setTenantId(tenantId);
    }

    @Override
    protected Converter prepare(EntitiesImportCtx ctx, Converter converter, Converter old,
                                EntityExportData<Converter> exportData, IdProvider idProvider) {
        return converter;
    }

    @Override
    protected Converter deepCopy(Converter converter) {
        return new Converter(converter);
    }

    @Override
    protected Converter saveOrUpdate(EntitiesImportCtx ctx, Converter converter, EntityExportData<Converter> exportData,
                                     IdProvider idProvider, CompareResult compareResult) {
        return converterService.saveConverter(converter);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

}
