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
package org.thingsboard.server.dao.sql.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.RoleFields;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Component
@SqlDao
public class JpaRoleDao extends JpaAbstractDao<RoleEntity, Role> implements RoleDao {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected Class<RoleEntity> getEntityClass() {
        return RoleEntity.class;
    }

    @Override
    protected JpaRepository<RoleEntity, UUID> getRepository() {
        return roleRepository;
    }

    @Override
    public Role findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(roleRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Role findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(roleRepository.findByTenantIdAndName(tenantId, name).orElse(null));
    }

    @Override
    public PageData<Role> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(roleRepository.findByTenantId(
                tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RoleId getExternalIdByInternal(RoleId internalId) {
        return Optional.ofNullable(roleRepository.getExternalIdById(internalId.getId()))
                .map(RoleId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

    /** Feeds {@code EdqsSyncService#syncTenantEntity} - see ObjectType#edqsTenantTypes. */
    @Override
    public List<RoleFields> findNextBatch(UUID id, int batchSize) {
        return roleRepository.findNextBatch(id, Limit.of(batchSize));
    }

}
