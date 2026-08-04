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
package org.thingsboard.server.dao.sql.secret;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SecretEntity;
import org.thingsboard.server.dao.secret.SecretDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaSecretDao extends JpaAbstractDao<SecretEntity, Secret> implements SecretDao {

    private final SecretRepository secretRepository;

    @Override
    public PageData<Secret> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(secretRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Secret findByName(TenantId tenantId, String name) {
        return DaoUtil.getData(secretRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        secretRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

    @Override
    protected Class<SecretEntity> getEntityClass() {
        return SecretEntity.class;
    }

    @Override
    protected JpaRepository<SecretEntity, UUID> getRepository() {
        return secretRepository;
    }

}
