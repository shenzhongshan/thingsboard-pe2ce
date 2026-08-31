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
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Like {@link EntityGroupQueryProcessor} but unions the members of several entity groups (a
 * dashboard data source that draws from more than one group of the same entity type).
 */
public class EntityGroupListQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntityGroupListFilter> {

    private final Set<UUID> groupIds;
    private final EntityType groupType;

    public EntityGroupListQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupListFilter) query.getEntityFilter());
        List<String> entityGroupList = filter.getEntityGroupList();
        this.groupIds = entityGroupList == null ? Set.of() :
                entityGroupList.stream().map(UUID::fromString).collect(Collectors.toSet());
        this.groupType = filter.getGroupType();
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
        for (UUID groupId : groupIds) {
            for (RelationInfo relationInfo : repository.getRelations(RelationTypeGroup.FROM_ENTITY_GROUP).getFrom(groupId)) {
                EntityData<?> ed = relationInfo.getTarget();
                if (ed.getEntityType() == groupType) {
                    members.add(ed);
                }
            }
        }
        return members;
    }

    @Override
    protected int getProbableResultSize() {
        return groupIds.size() * 64;
    }

}
