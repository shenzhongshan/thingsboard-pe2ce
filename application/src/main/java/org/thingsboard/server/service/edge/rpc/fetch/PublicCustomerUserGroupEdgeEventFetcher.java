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
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.ArrayList;
import java.util.List;

/**
 * 公共客户的 "Public Users" 用户组。公共仪表板靠它授权匿名访问 —— edge 上没有这个组，
 * 公共分享在 edge 本地就失效了。
 *
 * <p>只产出一条事件，不分页。
 */
@AllArgsConstructor
@Slf4j
public class PublicCustomerUserGroupEdgeEventFetcher implements EdgeEventFetcher {

    private final EntityGroupService entityGroupService;
    private final CustomerId publicCustomerId;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();
        EntityGroup publicUserGroup = entityGroupService.findOrCreatePublicUsersGroup(tenantId, publicCustomerId);
        if (publicUserGroup != null) {
            result.add(EdgeUtils.constructEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_GROUP,
                    EdgeEventActionType.ADDED, publicUserGroup.getId(), null));
        }
        return new PageData<>(result, 1, result.size(), false);
    }

}
