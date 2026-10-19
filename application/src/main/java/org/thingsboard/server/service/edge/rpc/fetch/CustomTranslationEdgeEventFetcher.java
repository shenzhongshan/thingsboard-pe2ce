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

import com.fasterxml.jackson.databind.JsonNode;
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.translation.CustomTranslationService;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义翻译。按 locale 逐条下发，层级同白标：系统 → 租户 → 客户（含父客户链）。
 *
 * <p>取的是"本层自己的"覆盖（CE 的 getCustomizedLocales / getCustomTranslation），不是合并结果 ——
 * 合并要在 edge 侧按同样的层级顺序做，推合并值会让 edge 无法区分某个 key 来自哪一层。
 */
@AllArgsConstructor
@Slf4j
public class CustomTranslationEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomerService customerService;
    private final CustomTranslationService customTranslationService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        List<EdgeEvent> result = new ArrayList<>();
        EdgeId edgeId = edge.getId();
        processTenantLocales(TenantId.SYS_TENANT_ID, edgeId, result);
        processTenantLocales(tenantId, edgeId, result);
        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
            processCustomerLocales(tenantId, new CustomerId(edge.getOwnerId().getId()), edgeId, result);
        }
        return new PageData<>(result, 1, result.size(), false);
    }

    private void processTenantLocales(TenantId tenantId, EdgeId edgeId, List<EdgeEvent> result) {
        for (String locale : customTranslationService.getCustomizedLocales(tenantId, null)) {
            addLocaleEvent(tenantId, null, locale, edgeId, result);
        }
    }

    private void processCustomerLocales(TenantId tenantId, CustomerId customerId, EdgeId edgeId, List<EdgeEvent> result) {
        for (String locale : customTranslationService.getCustomizedLocales(tenantId, customerId)) {
            addLocaleEvent(tenantId, customerId, locale, edgeId, result);
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer == null || !customer.isSubCustomer()) {
                continue;
            }
            addParentCustomerEvents(tenantId, customer.getParentCustomerId(), locale, edgeId, result);
        }
    }

    private void addParentCustomerEvents(TenantId tenantId, CustomerId customerId, String locale, EdgeId edgeId, List<EdgeEvent> result) {
        addLocaleEvent(tenantId, customerId, locale, edgeId, result);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer != null && customer.isSubCustomer()) {
            addParentCustomerEvents(tenantId, customer.getParentCustomerId(), locale, edgeId, result);
        }
    }

    private void addLocaleEvent(TenantId tenantId, CustomerId customerId, String locale, EdgeId edgeId, List<EdgeEvent> events) {
        CustomTranslation own = customTranslationService.getCustomTranslation(tenantId, customerId, locale);
        JsonNode value = own != null ? own.getValue() : null;
        CustomTranslation customTranslation = new CustomTranslation();
        customTranslation.setTenantId(tenantId);
        customTranslation.setCustomerId(customerId);
        customTranslation.setLocaleCode(locale);
        customTranslation.setValue(value);
        events.add(EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.CUSTOM_TRANSLATION,
                EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(customTranslation)));
    }

}
