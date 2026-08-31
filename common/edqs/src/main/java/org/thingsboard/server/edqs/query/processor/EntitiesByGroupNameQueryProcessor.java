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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Same as {@link EntityGroupNameQueryProcessor}, but resolves to at most ONE entity group - PE's
 * EntitiesByGroupQueryProcessor#process stops at the first name/owner match instead of unioning
 * every group whose name matches the (possibly prefix) pattern. This filter type is meant for
 * "the group named X owned by Y" lookups where the name is expected to be unique per owner; a
 * union-of-all-matches would silently merge unrelated groups' members into one datasource.
 * <p>
 * PE's owner model is polymorphic (tenant/customer/user/...); EDQS only tracks a plain customerId
 * per entity, so only CUSTOMER owners are actually filtered here - a TENANT (or other) ownerId is
 * treated as "no extra scoping" beyond the tenant-wide EDQS partition. Narrower than PE, not
 * incorrect: it can only ever return more candidates than a true owner-hierarchy check would,
 * never fewer.
 */
public class EntitiesByGroupNameQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntitiesByGroupNameFilter> {

    private final EntityType groupType;
    private final Pattern namePattern;
    private final EntityId ownerId;

    public EntitiesByGroupNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntitiesByGroupNameFilter) query.getEntityFilter());
        this.groupType = filter.getGroupType();
        this.namePattern = RepositoryUtils.toEntityNameSqlLikePattern(filter.getEntityGroupNameFilter());
        this.ownerId = filter.getOwnerId();
    }

    @Override
    protected void processCustomerQuery(UUID customerId, Consumer<EntityData<?>> processor) {
        for (EntityData<?> ed : collectMembers()) {
            if (checkCustomerId(customerId, ed) && matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        for (EntityData<?> ed : collectMembers()) {
            if (matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    private Set<EntityData<?>> collectMembers() {
        boolean scopeToCustomer = ownerId != null && ownerId.getEntityType() == EntityType.CUSTOMER;
        EntityData<?> matchedGroup = null;
        for (EntityData<?> group : repository.getEntitySet(EntityType.ENTITY_GROUP)) {
            String type = group.getFields() != null ? group.getFields().getType() : null;
            if (!groupType.name().equals(type)) {
                continue;
            }
            if (scopeToCustomer && !ownerId.getId().equals(group.getCustomerId())) {
                continue;
            }
            String name = group.getFields().getName();
            if (namePattern != null && (name == null || !namePattern.matcher(name).matches())) {
                continue;
            }
            matchedGroup = group;
            break;
        }
        Set<EntityData<?>> members = new HashSet<>();
        if (matchedGroup == null) {
            return members;
        }
        for (RelationInfo relationInfo : repository.getRelations(RelationTypeGroup.FROM_ENTITY_GROUP).getFrom(matchedGroup.getId())) {
            EntityData<?> member = relationInfo.getTarget();
            if (member.getEntityType() == groupType) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    protected int getProbableResultSize() {
        return 64;
    }

}
