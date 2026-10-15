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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;

/**
 * 下发某个已分配分组上的权限授予，让 edge 本地也能做 RBAC 判定。
 *
 * <p>方向按组类型分：USER 组问的是"这个用户组被授了什么角色"，其它类型问的是
 * "谁被授权访问这个组"。两者查的是 group_permission 的不同列，混了会同步错数据。
 */
@AllArgsConstructor
@Slf4j
public class EntityGroupPermissionsEdgeEventFetcher extends BasePageableEdgeEventFetcher<GroupPermission> {

    private final GroupPermissionService groupPermissionService;
    private final EntityType groupType;
    private final EntityGroupId entityGroupId;

    @Override
    PageData<GroupPermission> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return groupPermissionService.findGroupPermissionsByTenantIdAndGroupId(
                edge.getTenantId(), entityGroupId, EntityType.USER.equals(groupType), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, GroupPermission groupPermission) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.GROUP_PERMISSION,
                EdgeEventActionType.ADDED, groupPermission.getId(), null);
    }

}
