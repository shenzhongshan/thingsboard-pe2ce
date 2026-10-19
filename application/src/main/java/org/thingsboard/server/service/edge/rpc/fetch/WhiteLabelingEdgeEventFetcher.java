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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.ArrayList;
import java.util.List;

/**
 * 白标。事件的 body 里放的不是白标内容本身，而是**归属实体的 id**（系统/租户/客户），
 * 由 WhiteLabelingEdgeProcessor 在下发时按 id 去查当前值 —— 这样同步期间的改动也能带上。
 *
 * <p>三层依次下发：系统 → 租户 → （edge 归客户时）客户，且沿父客户链一路向上补。
 * edge 侧的解析是"就近覆盖"，缺了上层就没有回退的底，所以每层都要推。
 */
@AllArgsConstructor
@Slf4j
public class WhiteLabelingEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomerService customerService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();
        for (EdgeEventType wlType : List.of(EdgeEventType.WHITE_LABELING, EdgeEventType.LOGIN_WHITE_LABELING, EdgeEventType.MAIL_TEMPLATES)) {
            result.addAll(getWhiteLabelingEdgeEvents(tenantId, edge, wlType));
        }
        return new PageData<>(result, 1, result.size(), false);
    }

    private List<EdgeEvent> getWhiteLabelingEdgeEvents(TenantId tenantId, Edge edge, EdgeEventType eventType) {
        EntityId ownerId = edge.getOwnerId();
        List<EdgeEvent> result = new ArrayList<>();
        result.add(constructWlEvent(tenantId, edge.getId(), eventType, TenantId.SYS_TENANT_ID));
        result.add(constructWlEvent(tenantId, edge.getId(), eventType, tenantId));
        if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
            CustomerId customerId = new CustomerId(ownerId.getId());
            result.add(constructWlEvent(tenantId, edge.getId(), eventType, customerId));
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer != null && customer.isSubCustomer()) {
                addParentCustomerEvents(tenantId, edge.getId(), customer.getParentCustomerId(), eventType, result);
            }
        }
        return result;
    }

    private void addParentCustomerEvents(TenantId tenantId, EdgeId edgeId, CustomerId customerId, EdgeEventType eventType, List<EdgeEvent> events) {
        events.add(constructWlEvent(tenantId, edgeId, eventType, customerId));
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer != null && customer.isSubCustomer()) {
            addParentCustomerEvents(tenantId, edgeId, customer.getParentCustomerId(), eventType, events);
        }
    }

    private EdgeEvent constructWlEvent(TenantId tenantId, EdgeId edgeId, EdgeEventType eventType, EntityId ownerId) {
        return EdgeUtils.constructEdgeEvent(tenantId, edgeId, eventType, EdgeEventActionType.UPDATED,
                null, JacksonUtil.valueToTree(ownerId));
    }

}
