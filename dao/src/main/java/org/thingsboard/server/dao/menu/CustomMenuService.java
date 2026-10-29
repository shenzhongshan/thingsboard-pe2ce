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

import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

/**
 * Custom Menu management: multiple named menus per scope (SYSTEM/TENANT/CUSTOMER), each
 * optionally assigned (ALL/CUSTOMERS/USERS/USER_GROUPS) to override the effective navigation
 * menu for the users it applies to. A menu is owned by a (tenantId, customerId) pair; the
 * install seeds one ALL-assignee default per scope at the system level, which every tenant and
 * customer inherits until it configures its own.
 */
public interface CustomMenuService {

    /** userGroupNames on {@code info} is only read when {@code info.getAssigneeType() == USER_GROUPS}. */
    CustomMenu createCustomMenu(TenantId tenantId, CustomerId customerId, CustomMenuInfo info,
                                List<EntityId> assignToList, boolean force);

    CustomMenu updateCustomMenuConfig(TenantId tenantId, CustomMenuId id, CustomMenuConfig config);

    CustomMenu updateCustomMenuName(TenantId tenantId, CustomMenuId id, String name);

    /** newUserGroupNames is only read when {@code newAssigneeType == USER_GROUPS}; pass null otherwise. */
    void updateAssigneeList(TenantId tenantId, CustomMenuId id, CMAssigneeType newAssigneeType,
                            List<EntityId> newList, String[] newUserGroupNames, boolean force);

    CustomMenu findCustomMenuById(TenantId tenantId, CustomMenuId id);

    CustomMenuInfo findCustomMenuInfoById(TenantId tenantId, CustomMenuId id);

    /** Menus owned by the given (tenant, customer) pair — what the current user may manage. */
    PageData<CustomMenuInfo> findCustomMenuInfos(TenantId tenantId, CustomerId customerId, CMScope scope, PageLink pageLink);

    List<EntityInfo> findCustomMenuAssigneeList(TenantId tenantId, CustomMenuId id);

    CustomMenuDeleteResult deleteCustomMenu(TenantId tenantId, CustomMenuId id, boolean force);

    void deleteByTenantId(TenantId tenantId);

    /** Seeds the per-scope system default menus if they are missing. Idempotent. */
    void createDefaultCustomMenus();

    /** SYS_ADMIN: the system-level SYSTEM-scope default menu. */
    CustomMenuConfig resolveSystemAdminMenu();

    /**
     * Tenant user: the user's own assigned menu, then the tenant's TENANT-scope default, then the
     * system-level TENANT-scope default.
     */
    CustomMenuConfig resolveTenantUserMenu(TenantId tenantId, UserId userId);

    /**
     * Customer user: the user's own assigned menu, then the customer hierarchy (the customer's
     * CUSTOMER-scope default, its directly assigned menu, then the same walk up its parents),
     * then the tenant's CUSTOMER-scope default, then the system-level CUSTOMER-scope default.
     */
    CustomMenuConfig resolveCustomerUserMenu(TenantId tenantId, CustomerId customerId, UserId userId);

}
