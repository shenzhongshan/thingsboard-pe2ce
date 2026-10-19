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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;

import java.util.ArrayList;
import java.util.List;

/**
 * 下发已分配给这台 edge 的设备分组上的 OTA 配置。
 *
 * <p>只遍历已分配的 DEVICE 分组 —— OTA 配置是挂在分组上的设置行，分组没分配给 edge，
 * 它的 OTA 配置也不该下去。
 *
 * <p>事件的 entityId 是所属分组（见 EntityIdFactory 对 DEVICE_GROUP_OTA 的映射），
 * 配置内容走 body。
 */
@AllArgsConstructor
@Slf4j
public class DeviceGroupOtaPackageEdgeEventFetcher implements EdgeEventFetcher {

    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    private final EntityGroupService entityGroupService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        PageData<EntityGroup> groups = entityGroupService.findEdgeEntityGroupsByType(
                tenantId, edge.getId(), EntityType.DEVICE, pageLink);
        List<EdgeEvent> result = new ArrayList<>();
        for (EntityGroup group : groups.getData()) {
            for (DeviceGroupOtaPackage otaPackage : deviceGroupOtaPackageService
                    .findDeviceGroupOtaPackageByGroupId(tenantId, group.getId())) {
                result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_GROUP_OTA,
                        EdgeEventActionType.UPDATED, group.getId(), JacksonUtil.valueToTree(otaPackage)));
            }
        }
        return new PageData<>(result, groups.getTotalPages(), groups.getTotalElements(), groups.hasNext());
    }

}
