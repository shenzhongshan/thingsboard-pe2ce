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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.integration.IntegrationService;

/**
 * 下发分配给这台 edge 的集成。只取通过 assignIntegrationToEdge 建了 EDGE 关系的，
 * 不是租户下的全部集成 —— 后者会把每台 edge 都灌上所有外部端点的连接配置。
 *
 * <p>集成引用的上行/下行转换器由 IntegrationEdgeProcessor 在构造 downlink 时一并捎带，
 * 所以这里不需要单独的 converter fetcher。
 */
@AllArgsConstructor
@Slf4j
public class IntegrationsEdgeEventFetcher extends BasePageableEdgeEventFetcher<Integration> {

    private final IntegrationService integrationService;

    @Override
    PageData<Integration> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Integration integration) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.INTEGRATION,
                EdgeEventActionType.ADDED, integration.getId(), null);
    }

}
