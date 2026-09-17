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
package org.thingsboard.server.dao.integration;

import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

/**
 * CE port of PE's integration persistence service. Dropped vs PE: remote integrations, edge
 * assignment/templates, integration infos with stats, async lookups.
 * <p>
 * Extends {@link EntityDaoService} (as in PE) so INTEGRATION resolves through
 * EntityServiceRegistry — required by generic entity lookups such as the events API.
 */
public interface IntegrationService extends EntityDaoService {

    Integration saveIntegration(Integration integration);

    Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId);

    Optional<Integration> findIntegrationByRoutingKey(String routingKey);

    List<Integration> findAllIntegrations(TenantId tenantId);

    List<Integration> findAllEnabledIntegrations();

    List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId);

    PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink);

    void deleteIntegration(TenantId tenantId, IntegrationId integrationId);

    void deleteIntegrationsByTenantId(TenantId tenantId);


    /**
     * 把 edge 模板集成分配给一台 edge。只有模板能分配 —— 普通集成在云端有运行实例，
     * 下发给 edge 会变成两个进程同时连同一个外部端点。
     * 关系走 RelationTypeGroup.EDGE + "Contains"，与规则链一致。
     */
    Integration assignIntegrationToEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId);

    Integration unassignIntegrationFromEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId);

    PageData<Integration> findIntegrationsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

}
