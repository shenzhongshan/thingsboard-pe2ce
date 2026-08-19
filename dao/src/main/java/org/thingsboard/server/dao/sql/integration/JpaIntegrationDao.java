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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
public class JpaIntegrationDao extends JpaAbstractDao<IntegrationEntity, Integration> implements IntegrationDao {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Override
    protected Class<IntegrationEntity> getEntityClass() {
        return IntegrationEntity.class;
    }

    @Override
    protected JpaRepository<IntegrationEntity, UUID> getRepository() {
        return integrationRepository;
    }

    @Override
    public Integration findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(integrationRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Integration findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(integrationRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<Integration> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(integrationRepository.findByTenantId(
                tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public IntegrationId getExternalIdByInternal(IntegrationId internalId) {
        return Optional.ofNullable(integrationRepository.getExternalIdById(internalId.getId()))
                .map(IntegrationId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

}
