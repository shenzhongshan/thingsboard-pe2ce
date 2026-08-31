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
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
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
 * Resolves entity groups of {@code groupType} whose name matches {@code entityGroupNameFilter}
 * (a dynamic "any group named like X" data source, as opposed to {@link EntityGroupQueryProcessor}'s
 * fixed group id), then unions their members - same relation-index approach as the other
 * entity-group processors, plus a group-name lookup pass over the ENTITY_GROUP entity set.
 */
public class EntityGroupNameQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntityGroupNameFilter> {

    private final EntityType groupType;
    private final Pattern namePattern;

    public EntityGroupNameQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupNameFilter) query.getEntityFilter());
        this.groupType = filter.getGroupType();
        this.namePattern = RepositoryUtils.toEntityNameSqlLikePattern(filter.getEntityGroupNameFilter());
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
        Set<EntityData<?>> members = new HashSet<>();
        for (EntityData<?> group : repository.getEntitySet(EntityType.ENTITY_GROUP)) {
            String type = group.getFields() != null ? group.getFields().getType() : null;
            if (!groupType.name().equals(type)) {
                continue;
            }
            String name = group.getFields().getName();
            if (namePattern != null && (name == null || !namePattern.matcher(name).matches())) {
                continue;
            }
            for (RelationInfo relationInfo : repository.getRelations(RelationTypeGroup.FROM_ENTITY_GROUP).getFrom(group.getId())) {
                EntityData<?> member = relationInfo.getTarget();
                if (member.getEntityType() == groupType) {
                    members.add(member);
                }
            }
        }
        return members;
    }

    @Override
    protected int getProbableResultSize() {
        return 64;
    }

}
