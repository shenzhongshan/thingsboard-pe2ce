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
import org.thingsboard.server.service.edge.rpc.fetch.CustomerRolesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.PublicCustomerUserGroupEdgeEventFetcher;
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
            fetchers.add(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), publicCustomer.getId()));
            // 公共仪表板靠这个组授权匿名访问；edge 上没有它，公共分享在本地就失效了。
            fetchers.add(new PublicCustomerUserGroupEdgeEventFetcher(ctx.getEntityGroupService(), publicCustomer.getId()));
        }
        if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
            fetchers.add(new CustomerEdgeEventFetcher(edge.getCustomerId()));
            fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
            // 客户自有角色：组权限会引用它们，缺了 edge 本地解析权限时会找不到角色。
            fetchers.add(new CustomerRolesEdgeEventFetcher(ctx.getRoleService(), edge.getCustomerId()));
        }
        // 按分组同步，不是按租户推全量 —— 这是 PE 的语义：实体先被分配进组，组再分配给 edge，
        // 只有这样 edge 才知道每个实体属于哪个组（实体消息上的 entityGroupIdMSB/LSB）。
        // 组本身在这里推；每个组的权限与成员由 EdgeGrpcSession.appendGroupScopedFetchers
        // 在同步过程中动态追加。顺序对齐 PE 的 EdgeSyncCursor —— profiles 夹在分组之间是
        // 有依据的，设备/资产必须在其 profile 之后到达。
        fetchers.add(new EntityGroupEdgeEventFetcher(ctx.getEntityGroupService(), EntityType.DASHBOARD));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        // 注意：上面的 TenantAdminUsers/CustomerUsers 两个逐实体 fetcher **暂时保留**。
        // PE 不需要它们：PE 建 edge 时调 assignTenantAdministratorsAndUsersGroupToEdge 把用户组
        // 挂到 edge 上，用户随后经下面的 USER 分组同步过去。CE 现在也有这个方法了，但它只在
        // 新建 edge 时调用 —— **存量 edge 没有这层关系**，此刻删掉这两个 fetcher 会让它们
        // 立刻失去全部用户，没人能登录 edge。
        // 删除前提：给存量 edge 补一次分组关系（backfill），或在每次同步时幂等地确保它存在。
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
