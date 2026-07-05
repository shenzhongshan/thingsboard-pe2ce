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
 * Custom Menu management: multiple named menus per scope (SYSTEM/TENANT), each optionally
 * assigned (ALL/CUSTOMERS/USERS) to override the effective navigation menu for the users it
 * applies to. CE port of PE's custom menu feature — see CMScope/CMAssigneeType for the
 * dropped-in-CE concepts (CUSTOMER scope, USER_GROUPS assignee type, sub-customer fallback).
 */
public interface CustomMenuService {

    CustomMenu createCustomMenu(TenantId tenantId, CustomMenuInfo info, List<EntityId> assignToList, boolean force);

    CustomMenu updateCustomMenuConfig(TenantId tenantId, CustomMenuId id, CustomMenuConfig config);

    CustomMenu updateCustomMenuName(TenantId tenantId, CustomMenuId id, String name);

    void updateAssigneeList(TenantId tenantId, CustomMenuId id, CMAssigneeType newAssigneeType, List<EntityId> newList, boolean force);

    CustomMenu findCustomMenuById(TenantId tenantId, CustomMenuId id);

    CustomMenuInfo findCustomMenuInfoById(TenantId tenantId, CustomMenuId id);

    PageData<CustomMenuInfo> findCustomMenuInfos(TenantId tenantId, CMScope scope, PageLink pageLink);

    List<EntityInfo> findCustomMenuAssigneeList(TenantId tenantId, CustomMenuId id);

    CustomMenuDeleteResult deleteCustomMenu(TenantId tenantId, CustomMenuId id, boolean force);

    void deleteByTenantId(TenantId tenantId);

    /**
     * Resolution algorithm (CE-adapted, see plan doc): current user's own override, then
     * (customer users only) their customer's override, then the tenant's own ALL-type default,
     * then the SYS_TENANT_ID-owned TENANT-scope fallback default, then built-in (empty items).
     */
    CustomMenuConfig resolveSystemAdminMenu();

    CustomMenuConfig resolveUserMenu(TenantId tenantId, CustomerId customerId, UserId userId);

}
