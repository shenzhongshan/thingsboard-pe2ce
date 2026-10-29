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
package org.thingsboard.server.dao.menu;

import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;

public interface CustomMenuDao {

    CustomMenu save(TenantId tenantId, CustomMenu customMenu);

    CustomMenu findById(TenantId tenantId, CustomMenuId id);

    CustomMenuInfo findInfoById(TenantId tenantId, CustomMenuId id);

    /** Menus owned by the given (tenant, customer) pair; {@code scope} is an optional filter. */
    PageData<CustomMenuInfo> findInfosByOwner(TenantId tenantId, CustomerId customerId, CMScope scope, PageLink pageLink);

    /** The owner's ALL-assignee menu for the scope — i.e. the default menu of that scope. */
    Optional<CustomMenu> findDefaultByOwnerAndScope(TenantId tenantId, CustomerId customerId, CMScope scope);

    /** Every USER_GROUPS-assignee menu owned by (tenant, customer) at the given scope, oldest first. */
    List<CustomMenu> findUserGroupMenusByOwnerAndScope(TenantId tenantId, CustomerId customerId, CMScope scope);

    void removeById(TenantId tenantId, CustomMenuId id);

    void removeByTenantId(TenantId tenantId);

}
