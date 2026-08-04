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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SecretInfoEntity;
import org.thingsboard.server.dao.secret.SecretInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaSecretInfoDao extends JpaAbstractDao<SecretInfoEntity, SecretInfo> implements SecretInfoDao {

    private final SecretInfoRepository secretInfoRepository;

    @Override
    public PageData<SecretInfo> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(secretInfoRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public SecretInfo findByName(TenantId tenantId, String name) {
        return DaoUtil.getData(secretInfoRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public List<String> findAllNamesByTenantId(TenantId tenantId) {
        return secretInfoRepository.findAllNamesByTenantId(tenantId.getId());
    }

    @Override
    protected Class<SecretInfoEntity> getEntityClass() {
        return SecretInfoEntity.class;
    }

    @Override
    protected JpaRepository<SecretInfoEntity, UUID> getRepository() {
        return secretInfoRepository;
    }

}
