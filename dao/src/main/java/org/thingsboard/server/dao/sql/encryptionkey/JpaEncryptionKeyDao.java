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
package org.thingsboard.server.dao.sql.encryptionkey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.encryptionkey.EncryptionKeyDao;
import org.thingsboard.server.dao.model.sql.EncryptionKeyEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaEncryptionKeyDao extends JpaAbstractDao<EncryptionKeyEntity, EncryptionKey> implements EncryptionKeyDao {

    private final EncryptionKeyRepository encryptionKeyRepository;

    @Override
    public EncryptionKey findByTenantId(TenantId tenantId) {
        return DaoUtil.getData(encryptionKeyRepository.findByTenantId(tenantId.getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        encryptionKeyRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    protected Class<EncryptionKeyEntity> getEntityClass() {
        return EncryptionKeyEntity.class;
    }

    @Override
    protected JpaRepository<EncryptionKeyEntity, UUID> getRepository() {
        return encryptionKeyRepository;
    }

}
