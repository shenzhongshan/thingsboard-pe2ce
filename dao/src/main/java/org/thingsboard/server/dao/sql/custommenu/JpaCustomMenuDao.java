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
package org.thingsboard.server.dao.sql.custommenu;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.menu.CustomMenuDao;
import org.thingsboard.server.dao.model.sql.CustomMenuEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaCustomMenuDao implements CustomMenuDao {

    private final CustomMenuRepository customMenuRepository;

    @Override
    public CustomMenu save(TenantId tenantId, CustomMenu customMenu) {
        if (customMenu.getId() == null) {
            customMenu.setCreatedTime(System.currentTimeMillis());
        }
        return customMenuRepository.save(new CustomMenuEntity(customMenu)).toData();
    }

    @Override
    public CustomMenu findById(TenantId tenantId, CustomMenuId id) {
        return customMenuRepository.findById(id.getId()).map(CustomMenuEntity::toData).orElse(null);
    }

    @Override
    public CustomMenuInfo findInfoById(TenantId tenantId, CustomMenuId id) {
        return customMenuRepository.findById(id.getId()).map(CustomMenuEntity::toInfo).orElse(null);
    }

    @Override
    public PageData<CustomMenuInfo> findInfosByOwner(TenantId tenantId, CustomerId customerId, CMScope scope, PageLink pageLink) {
        UUID customerUuid = toCustomerUuid(customerId);
        Page<CustomMenuEntity> page = scope == null
                ? customMenuRepository.findByTenantIdAndCustomerId(tenantId.getId(), customerUuid, DaoUtil.toPageable(pageLink))
                : customMenuRepository.findByTenantIdAndCustomerIdAndScope(tenantId.getId(), customerUuid, scope, DaoUtil.toPageable(pageLink));
        List<CustomMenuInfo> infos = page.getContent().stream().map(CustomMenuEntity::toInfo).toList();
        return new PageData<>(infos, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @Override
    public Optional<CustomMenu> findDefaultByOwnerAndScope(TenantId tenantId, CustomerId customerId, CMScope scope) {
        return customMenuRepository.findByTenantIdAndCustomerIdAndScopeAndAssigneeType(
                        tenantId.getId(), toCustomerUuid(customerId), scope, CMAssigneeType.ALL)
                .map(CustomMenuEntity::toData);
    }

    @Override
    public List<CustomMenu> findUserGroupMenusByOwnerAndScope(TenantId tenantId, CustomerId customerId, CMScope scope) {
        return customMenuRepository.findByTenantIdAndCustomerIdAndScopeAndAssigneeTypeOrderByCreatedTimeAsc(
                        tenantId.getId(), toCustomerUuid(customerId), scope, CMAssigneeType.USER_GROUPS)
                .stream().map(CustomMenuEntity::toData).toList();
    }

    @Override
    public void removeById(TenantId tenantId, CustomMenuId id) {
        customMenuRepository.deleteById(id.getId());
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        customMenuRepository.deleteByTenantId(tenantId.getId());
    }

    // The column is NOT NULL with the NULL_UUID sentinel as default, so a missing/absent
    // customer must be translated to the sentinel rather than to SQL NULL.
    private static UUID toCustomerUuid(CustomerId customerId) {
        return customerId == null ? CustomerId.NULL_UUID : customerId.getId();
    }

}
