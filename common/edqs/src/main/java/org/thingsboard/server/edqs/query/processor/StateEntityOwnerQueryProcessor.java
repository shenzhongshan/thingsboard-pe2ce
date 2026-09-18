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

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Resolves to exactly one entity: the owner (a customer, or the tenant) of the dashboard's state entity.
 * The owner was already resolved by {@code TenantRepo#buildContext} - which is also the only place that
 * can resolve it, since the result's entity type depends on the lookup - and handed over as
 * {@link QueryContext#getOwnerId()}. This processor just emits it, applying the query's key filters to
 * the owner row exactly as {@code DefaultEntityQueryRepository#stateEntityOwnerQuery} does on the SQL side.
 */
public class StateEntityOwnerQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<StateEntityOwnerFilter> {

    private final EntityId ownerId;

    public StateEntityOwnerQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (StateEntityOwnerFilter) query.getEntityFilter());
        this.ownerId = ctx.getOwnerId();
    }

    @Override
    protected void processCustomerQuery(UUID customerId, Consumer<EntityData<?>> processor) {
        processAll(ed -> {
            if (checkCustomerId(customerId, ed)) {
                processor.accept(ed);
            }
        });
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        EntityData<?> ed = repository.getEntityMap(ownerId.getEntityType()).get(ownerId.getId());
        if (ed != null && ed.getFields() != null && matches(ed)) {
            processor.accept(ed);
        }
    }

    @Override
    protected int getProbableResultSize() {
        return 1;
    }

}
