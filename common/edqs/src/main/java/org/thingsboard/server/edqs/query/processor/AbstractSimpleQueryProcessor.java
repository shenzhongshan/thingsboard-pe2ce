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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.edqs.data.CustomerData;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractSimpleQueryProcessor<T extends EntityFilter> extends AbstractSingleEntityTypeQueryProcessor<T> {

    private final EntityType entityType;

    public AbstractSimpleQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query, T filter, EntityType entityType) {
        super(repo, ctx, query, filter);
        this.entityType = entityType;
    }

    @Override
    protected void processCustomerQuery(UUID customerId, Consumer<EntityData<?>> processor) {
        // Union over the customer and its sub-customers, as PE's processCustomerGenericRead does.
        var customers = repository.getEntityMap(EntityType.CUSTOMER);
        // Deny-by-default: skip the owner-scope sweep entirely without a generic READ grant; only
        // entities reached through an explicit group grant remain below.
        Set<UUID> scope = ctx.getQueryPermissions().hasGenericRead(entityType)
                ? customerScope(customerId) : java.util.Set.of();
        for (UUID scopedCustomerId : scope) {
            var customerData = (CustomerData) customers.get(scopedCustomerId);
            if (customerData == null) {
                continue;
            }
            // Querying CUSTOMER entities: a customer lives in its PARENT's bucket, so the buckets
            // alone would omit the querying customer itself (which has no parent in scope). The SQL
            // side's `e.id in (recursive customers_ids)` does include it, so emit it here too.
            if (entityType == EntityType.CUSTOMER) {
                if (matches(customerData)) {
                    processor.accept(customerData);
                }
            } else {
                process(customerData.getEntities(entityType), processor);
            }
        }
        // Entities shared in via a GROUP role live outside the customer buckets entirely.
        for (EntityData<?> ed : grantedGroupMembers()) {
            if (ed.getEntityType() == entityType && ed.getFields() != null && matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        process(repository.getEntitySet(entityType), processor);
    }

    @Override
    protected int getProbableResultSize() {
        return 1024;
    }

}
