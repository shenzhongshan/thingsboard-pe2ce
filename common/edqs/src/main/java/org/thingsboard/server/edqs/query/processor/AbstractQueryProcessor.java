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
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.query.EdqsDataQuery;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.query.SortableEntityData;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.edqs.util.RepositoryUtils.checkFilters;
import static org.thingsboard.server.edqs.util.RepositoryUtils.getSortValue;

public abstract class AbstractQueryProcessor<T extends EntityFilter> implements EntityQueryProcessor {

    protected final TenantRepo repository;
    protected final QueryContext ctx;
    protected final EdqsQuery query;
    protected final DataKey sortKey;
    protected final T filter;
    private Set<UUID> customerScope;
    private Collection<EntityData<?>> grantedMembers;
    private Set<UUID> grantedMemberIds;

    public AbstractQueryProcessor(TenantRepo repository, QueryContext ctx, EdqsQuery query, T filter) {
        this.repository = repository;
        this.ctx = ctx;
        this.query = query;
        this.sortKey = query instanceof EdqsDataQuery dataQuery ? dataQuery.getSortKey() : null;
        this.filter = filter;
    }

    protected SortableEntityData toSortData(EntityData<?> ed) {
        SortableEntityData sortData = new SortableEntityData(ed);
        sortData.setSortValue(getSortValue(ed, sortKey, ctx));
        return sortData;
    }

    protected void process(Collection<EntityData<?>> entities, Consumer<EntityData<?>> processor) {
        for (EntityData<?> ed : entities) {
            if (matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    /**
     * The querying customer plus its whole sub-customer subtree. Computed once per processor: the
     * scope is fixed for the query, and this is consulted once per candidate entity.
     */
    protected Set<UUID> customerScope(UUID customerId) {
        Set<UUID> scope = customerScope;
        if (scope == null) {
            scope = customerScope = repository.getAllCustomers(customerId);
        }
        return scope;
    }

    /**
     * Whether {@code ed} is visible to a user scoped to {@code customerId}. Ownership is checked
     * against the whole sub-customer subtree, not just the exact customer, matching the SQL engine's
     * recursive customers_ids CTE. Instance method (was static) because it needs the repository's
     * hierarchy index.
     */
    protected boolean checkCustomerId(UUID customerId, EntityData<?> ed) {
        // Deny-by-default: owner scope only counts if the role grants READ on this entity type.
        if (!ctx.getQueryPermissions().hasGenericRead(ed.getEntityType())) {
            return isInGrantedGroup(ed);
        }
        Set<UUID> scope = customerScope(customerId);
        return scope.contains(ed.getCustomerId())
                || (ed.getEntityType() == EntityType.DASHBOARD
                        && ed.getFields().getAssignedCustomerIds().stream().anyMatch(scope::contains))
                || (ed.getEntityType() == EntityType.CUSTOMER && scope.contains(ed.getId()))
                || isInGrantedGroup(ed);
    }

    /**
     * Members of every entity group the user was granted READ on, across all owners. Resolved once
     * per processor and cached: the grant set is fixed for the query, and checkCustomerId consults it
     * once per candidate entity, so resolving per entity would be quadratic.
     */
    protected Collection<EntityData<?>> grantedGroupMembers() {
        Collection<EntityData<?>> members = grantedMembers;
        if (members == null) {
            Set<UUID> granted = ctx.getQueryPermissions().readableEntityGroupIds();
            if (granted.isEmpty()) {
                members = List.of();
            } else {
                Set<EntityData<?>> collected = new LinkedHashSet<>();
                for (UUID groupId : granted) {
                    for (RelationInfo relationInfo : repository.getRelations(RelationTypeGroup.FROM_ENTITY_GROUP).getFrom(groupId)) {
                        collected.add(relationInfo.getTarget());
                    }
                }
                members = collected;
            }
            grantedMembers = members;
            grantedMemberIds = members.stream().map(EntityData::getId).collect(Collectors.toSet());
        }
        return members;
    }

    /**
     * Whether a GROUP role grant makes this entity readable. Unioned with owner scope, never
     * intersected: sharing an entity group is precisely how access reaches across owners, which is
     * what CustomerUserPermissions#hasGroupOrGenericPermission allows on the REST path.
     */
    protected boolean isInGrantedGroup(EntityData<?> ed) {
        if (!ctx.getQueryPermissions().hasGroupGrants()) {
            return false;
        }
        grantedGroupMembers();
        return grantedMemberIds.contains(ed.getId());
    }

    protected boolean matches(EntityData<?> ed) {
        return checkFilters(query, ed);
    }

}
