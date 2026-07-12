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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "change owner",
        configClazz = TbChangeOwnerNodeConfiguration.class,
        version = 1,
        nodeDescription = "Changes owner of the originator entity to the selected owner by type Tenant or Customer.",
        nodeDetails = "If <b>Tenant</b> is selected, rule node changes the owner of the originator to the tenant.<br>" +
                "If <b>Customer</b> is selected, rule node finds target owner by owner name pattern and then changes the owner of the originator entity.<br>" +
                "If the target owner does not exist and the 'Create new owner if not exists' toggle is enabled, the rule node will create a new owner.<br>" +
                "If both 'Create new owner if not exists' and 'Create new owner as sub-customer of current owner' are enabled, the rule node creates a new owner as a sub-customer of the current owner.<br><br>" +
                "Output connections: <code>Success</code> - if an entity already belongs to this owner or entity owner is successfully changed, otherwise - <code>Failure</code>.",
        icon = "assignment_ind"
)
public class TbChangeOwnerNode implements TbNode {

    private static final Set<EntityType> SUPPORTED_OWNER_TYPES = EnumSet.of(EntityType.TENANT, EntityType.CUSTOMER);
    private static final String SUPPORTED_OWNER_TYPES_STR = SUPPORTED_OWNER_TYPES.stream().map(Enum::name).collect(Collectors.joining(", "));

    private TbChangeOwnerNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbChangeOwnerNodeConfiguration.class);
        EntityType ownerType = config.getOwnerType();
        if (ownerType == null) {
            throw new TbNodeException("Owner type should be specified!", true);
        }
        if (!SUPPORTED_OWNER_TYPES.contains(ownerType)) {
            throw new TbNodeException("Unsupported owner type '" + ownerType + "'! Only " + SUPPORTED_OWNER_TYPES_STR + " types are allowed.", true);
        }
        if (EntityType.CUSTOMER.equals(ownerType) && StringUtils.isBlank(config.getOwnerNamePattern())) {
            throw new TbNodeException("Owner name should be specified!", true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        EntityId originator = msg.getOriginator();
        ListenableFuture<Void> resultFuture;
        if (EntityType.TENANT.equals(config.getOwnerType())) {
            resultFuture = changeOwnerAsync(ctx, originator, ctx.getTenantId());
        } else {
            String ownerName = TbNodeUtils.processPattern(config.getOwnerNamePattern(), msg);
            resultFuture = Futures.transformAsync(findOrCreateCustomerAsync(ctx, originator, ownerName),
                    customerId -> changeOwnerAsync(ctx, originator, customerId),
                    MoreExecutors.directExecutor());
        }
        withCallback(resultFuture,
                v -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<CustomerId> findOrCreateCustomerAsync(TbContext ctx, EntityId originator, String ownerName) {
        CustomerService customerService = ctx.getCustomerService();
        var tenantId = ctx.getTenantId();
        return Futures.transform(customerService.findCustomerByTenantIdAndTitleAsync(tenantId, ownerName), customerOpt -> {
            if (customerOpt.isPresent()) {
                return customerOpt.get().getId();
            }
            if (!config.isCreateOwnerIfNotExists()) {
                throw new NoSuchElementException("Customer with title '" + ownerName + "' doesn't exist!");
            }
            try {
                Customer newCustomer = new Customer();
                newCustomer.setTitle(ownerName);
                newCustomer.setTenantId(tenantId);
                if (config.isCreateOwnerOnOriginatorLevel()) {
                    EntityId currentOwnerId = ctx.getOwner(originator);
                    if (currentOwnerId != null && EntityType.CUSTOMER.equals(currentOwnerId.getEntityType())) {
                        newCustomer.setParentCustomerId(new CustomerId(currentOwnerId.getId()));
                    }
                }
                Customer saved = customerService.saveCustomer(newCustomer);
                ctx.enqueue(ctx.customerCreatedMsg(saved, ctx.getSelfId()),
                        () -> log.trace("Pushed Customer Created message: {}", saved),
                        throwable -> log.warn("Failed to push Customer Created message: {}", saved, throwable));
                return saved.getId();
            } catch (DataValidationException e) {
                return customerService.findCustomerByTenantIdAndTitle(tenantId, ownerName)
                        .map(Customer::getId)
                        .orElseThrow(() -> new RuntimeException("Failed to create customer with title '" + ownerName + "'", e));
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> changeOwnerAsync(TbContext ctx, EntityId originator, EntityId targetOwnerId) {
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityId currentOwnerId = ctx.getOwner(originator);
            if (currentOwnerId == null || !currentOwnerId.equals(targetOwnerId)) {
                ctx.changeEntityOwner(targetOwnerId, originator);
            }
            return null;
        });
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) {
        return new TbPair<>(false, oldConfiguration);
    }

}
