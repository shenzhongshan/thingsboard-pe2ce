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
package org.thingsboard.server.dao.sql.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ConverterFields;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.model.sql.ConverterEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Component
@SqlDao
public class JpaConverterDao extends JpaAbstractDao<ConverterEntity, Converter> implements ConverterDao {

    @Autowired
    private ConverterRepository converterRepository;

    @Override
    protected Class<ConverterEntity> getEntityClass() {
        return ConverterEntity.class;
    }

    @Override
    protected JpaRepository<ConverterEntity, UUID> getRepository() {
        return converterRepository;
    }

    @Override
    public Converter findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(converterRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Converter findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(converterRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<Converter> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(converterRepository.findByTenantId(
                tenantId, null, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ConverterId getExternalIdByInternal(ConverterId internalId) {
        return Optional.ofNullable(converterRepository.getExternalIdById(internalId.getId()))
                .map(ConverterId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

    /** Feeds {@code EdqsSyncService#syncTenantEntity} - see ObjectType#edqsTenantTypes. */
    @Override
    public List<ConverterFields> findNextBatch(UUID id, int batchSize) {
        return converterRepository.findNextBatch(id, Limit.of(batchSize));
    }

}
