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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.role.RoleService;

/**
 * 客户自有的角色。edge 归属某个客户时，除了租户级角色，还要把该客户（及其祖先客户）
 * 的角色一并下发，否则 edge 本地解析组权限时会找不到被引用的角色。
 */
@Slf4j
public class CustomerRolesEdgeEventFetcher extends BaseRolesEdgeEventFetcher {

    private final CustomerId customerId;

    public CustomerRolesEdgeEventFetcher(RoleService roleService, CustomerId customerId) {
        super(roleService);
        this.customerId = customerId;
    }

    @Override
    PageData<Role> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return roleService.findRolesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
    }

}
