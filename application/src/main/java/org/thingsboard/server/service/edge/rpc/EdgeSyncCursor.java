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
package org.thingsboard.server.service.edge.rpc;

import lombok.Getter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AiModelEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DefaultProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.service.edge.rpc.fetch.EntityGroupEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationRuleEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationTargetEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.NotificationTemplateEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OAuth2EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.ReportTemplateEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SecretEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SysAdminRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantResourcesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class EdgeSyncCursor {

    private final List<EdgeEventFetcher> fetchers = new LinkedList<>();

    @Getter
    private int currentIdx = 0;

    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
        if (fullSync) {
            fetchers.add(new TenantEdgeEventFetcher(ctx.getTenantService()));
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            // Roles before anything that references them, matching PE's ordering.
            fetchers.add(new SysAdminRolesEdgeEventFetcher(ctx.getRoleService()));
            fetchers.add(new TenantRolesEdgeEventFetcher(ctx.getRoleService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService(), TenantId.SYS_TENANT_ID));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService(), edge.getTenantId()));
            fetchers.add(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
            fetchers.add(new OAuth2EdgeEventFetcher(ctx.getDomainService()));
            fetchers.add(new SystemWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new TenantWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new AiModelEdgeEventFetcher(ctx.getAiModelService()));
        }
        Customer publicCustomer = ctx.getCustomerService().findPublicCustomer(edge.getTenantId());
        if (publicCustomer != null) {
            fetchers.add(new CustomerEdgeEventFetcher(publicCustomer.getId()));
        }
        if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
            fetchers.add(new CustomerEdgeEventFetcher(edge.getCustomerId()));
            fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
        }
        // 按分组同步，不是按租户推全量 —— 这是 PE 的语义：实体先被分配进组，组再分配给 edge，
        // 只有这样 edge 才知道每个实体属于哪个组（实体消息上的 entityGroupIdMSB/LSB）。
        // 组内实体本身由 edge 主动请求（entityGroupEntitiesRequestMsg），见
        // EntityGroupEntitiesEdgeEventFetcher。顺序对齐 PE 的 EdgeSyncCursor。
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DASHBOARD));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        // 注意：上面的 TenantAdminUsers/CustomerUsers 两个逐实体 fetcher **暂时保留**。
        // PE 不需要它们，因为 PE 在建 edge 时会调 assignTenantAdministratorsAndUsersGroupToEdge，
        // 把租户管理员/用户组挂到 edge 上，用户随后经这里的 USER 分组同步过去。
        // CE 还没有那个方法，此时若把它们删掉，新建的 edge 会一个用户都收不到 —— 没人能登录 edge。
        // 删除它们的前提是先补上 assignTenantAdministratorsAndUsersGroupToEdge。
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.USER));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.ASSET));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DEVICE));
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.ENTITY_VIEW));
        if (fullSync) {
            fetchers.add(new NotificationTemplateEdgeEventFetcher(ctx.getNotificationTemplateService()));
            fetchers.add(new NotificationTargetEdgeEventFetcher(ctx.getNotificationTargetService()));
            fetchers.add(new NotificationRuleEdgeEventFetcher(ctx.getNotificationRuleService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
            // sync device profiles twice to update software and hardware fields
            fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
            fetchers.add(new TenantResourcesEdgeEventFetcher(ctx.getResourceService()));
            fetchers.add(new SecretEdgeEventFetcher(ctx.getSecretService()));
            fetchers.add(new ReportTemplateEdgeEventFetcher(ctx.getReportTemplateService(), null));
            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                fetchers.add(new ReportTemplateEdgeEventFetcher(ctx.getReportTemplateService(), edge.getCustomerId()));
            }
            // ENTITY_GROUP, INTEGRATION and SCHEDULER_EVENT are deliberately absent. PE fetches
            // those per-edge (findEdgeEntityGroupsByType / findIntegrationsByTenantIdAndEdgeId /
            // findSchedulerEventsByTenantIdAndEdgeId) because in PE they are individually assigned
            // to an edge. CE has no such assignment, and falling back to "push every tenant one"
            // would hand every edge all integration connection configs - so these stay unsynced
            // until edge assignment exists for them.
        }
    }

    /**
     * 在遍历过程中追加 fetcher。分组是"目录"，推完一个组之后才知道要为它再取哪些数据
     * （组权限、组成员），所以游标是会变长的 —— 见 EdgeGrpcSession.appendGroupScopedFetchers。
     *
     * <p>追加到末尾，不影响 currentIdx 已经走过的部分。
     */
    public void append(EdgeEventFetcher fetcher) {
        fetchers.add(fetcher);
    }

    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    public EdgeEventFetcher getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

}
