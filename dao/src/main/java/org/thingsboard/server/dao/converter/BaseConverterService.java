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
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ConverterEntity;
import org.thingsboard.server.dao.sql.integration.ConverterRepository;
import org.thingsboard.server.dao.sql.integration.IntegrationRepository;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseConverterService implements ConverterService {

    private final ConverterRepository converterRepository;
    private final IntegrationRepository integrationRepository;
    private final TbTenantProfileCache tbTenantProfileCache;

    @Value("${debug.settings.default_duration:15}")
    private int defaultDebugDurationMinutes;

    @Override
    public Converter saveConverter(Converter converter) {
        log.trace("Executing saveConverter [{}]", converter);
        if (StringUtils.isEmpty(converter.getName())) {
            throw new DataValidationException("Converter name should be specified!");
        }
        if (converter.getType() == null) {
            throw new DataValidationException("Converter type should be specified!");
        }
        if (converter.getTenantId() == null) {
            throw new DataValidationException("Converter should be assigned to tenant!");
        }
        if (converter.getConfiguration() == null) {
            throw new DataValidationException("Converter configuration should be specified!");
        }
        Converter existing = findConverterByName(converter.getTenantId(), converter.getName());
        if (existing != null && (converter.getId() == null || !existing.getId().equals(converter.getId()))) {
            throw new DataValidationException("Converter with such name already exists!");
        }
        if (converter.getId() == null) {
            converter.setCreatedTime(System.currentTimeMillis());
        }
        DebugModeUtil.migrateDebugSettings(converter,
                tbTenantProfileCache.get(converter.getTenantId()).getDefaultProfileConfiguration().getMaxDebugModeDurationMinutes(),
                defaultDebugDurationMinutes, System.currentTimeMillis());
        return converterRepository.save(new ConverterEntity(converter)).toData();
    }

    @Override
    public Converter findConverterById(TenantId tenantId, ConverterId converterId) {
        return DaoUtil.getData(converterRepository.findById(converterId.getId()));
    }

    @Override
    public Converter findConverterByName(TenantId tenantId, String name) {
        ConverterEntity entity = converterRepository.findByTenantIdAndName(tenantId.getId(), name);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public PageData<Converter> findTenantConverters(TenantId tenantId, IntegrationType integrationType, PageLink pageLink) {
        return DaoUtil.toPageData(converterRepository.findByTenantId(
                tenantId.getId(), integrationType, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void deleteConverter(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing deleteConverter [{}]", converterId);
        if (!integrationRepository.findByTenantIdAndConverterId(tenantId.getId(), converterId.getId()).isEmpty()
                || !integrationRepository.findByTenantIdAndDownlinkConverterId(tenantId.getId(), converterId.getId()).isEmpty()) {
            throw new DataValidationException("The converter is referenced by an integration and cannot be deleted!");
        }
        converterRepository.deleteById(converterId.getId());
    }

    @Override
    public void deleteConvertersByTenantId(TenantId tenantId) {
        for (UUID id : converterRepository.findIdsByTenantId(tenantId.getId())) {
            converterRepository.deleteById(id);
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findConverterById(tenantId, new ConverterId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(Futures.immediateFuture(findEntity(tenantId, entityId)));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteConverter(tenantId, new ConverterId(id.getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteConvertersByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

}
