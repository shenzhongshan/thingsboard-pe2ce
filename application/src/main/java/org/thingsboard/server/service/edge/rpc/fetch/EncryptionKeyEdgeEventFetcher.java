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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

/**
 * 下发租户的加密密钥。edge 本地要用同一把密钥解开云端加密过的凭据（Secret、集成配置里的
 * 密码等）；密钥不同则 edge 上所有密文都解不开。
 *
 * <p>形状与实体类 fetcher 不同：这是"设置类"负载，只产出一条事件且 entityId 为 null，
 * 真正的内容由 processor 现查。
 */
@AllArgsConstructor
@Slf4j
public class EncryptionKeyEdgeEventFetcher implements EdgeEventFetcher {

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = List.of(EdgeUtils.constructEdgeEvent(
                tenantId, edge.getId(), EdgeEventType.ENCRYPTION_KEY, EdgeEventActionType.ADDED, null, null));
        return new PageData<>(result, 1, result.size(), false);
    }

}
