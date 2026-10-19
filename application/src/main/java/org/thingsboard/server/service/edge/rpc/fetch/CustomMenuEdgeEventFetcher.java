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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.menu.CustomMenuService;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户自己的自定义菜单。
 *
 * <p>PE 用 CustomMenuFilter(tenantId, customerId, scope, assigneeType) 过滤；CE 的菜单端口
 * 没有 CUSTOMER scope（只有 SYSTEM/TENANT，见 BaseCustomMenuService 的说明），所以这里退化成
 * 按 scope 取。列表接口只返回 Info，实际配置要再按 id 查一次。
 */
@AllArgsConstructor
@Slf4j
public class CustomMenuEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomMenuService customMenuService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        PageData<CustomMenuInfo> infos = customMenuService.findCustomMenuInfos(tenantId, CMScope.TENANT, pageLink);
        List<EdgeEvent> events = new ArrayList<>();
        for (CustomMenuInfo cm : infos.getData()) {
            CustomMenu customMenu = customMenuService.findCustomMenuById(cm.getTenantId(), new CustomMenuId(cm.getUuidId()));
            if (customMenu == null) {
                continue;
            }
            events.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.CUSTOM_MENU,
                    EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(customMenu)));
        }
        return new PageData<>(events, infos.getTotalPages(), infos.getTotalElements(), infos.hasNext());
    }

}
