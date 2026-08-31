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
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Resolves an "entity group" alias filter (dashboard data source pointed at a PE-style entity
 * group) by walking the FROM_ENTITY_GROUP relation index populated in {@link TenantRepo}.
 * Note: unlike PE, this does not re-check per-group ACL grants (GroupPermissions) - it relies on
 * the same tenant/customer scoping every other single-entity-type processor uses. See the CE
 * dashboard-parity plan for why that's an accepted simplification, not an oversight.
 */
public class EntityGroupQueryProcessor extends AbstractSingleEntityTypeQueryProcessor<EntityGroupFilter> {

    private final UUID groupId;
    private final EntityType groupType;

    public EntityGroupQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EntityGroupFilter) query.getEntityFilter());
        // A groupStateEntity filter with nothing resolved yet (no dashboard state, no default) comes
        // through with an empty entityGroup - that must mean an empty result, not a parse error.
        // Mirrors DefaultEntityQueryRepository#entityGroupMembershipQuery's "1=0" case on the SQL path.
        String entityGroup = filter.getEntityGroup();
        this.groupId = (entityGroup == null || entityGroup.isEmpty()) ? null : UUID.fromString(entityGroup);
        this.groupType = filter.getGroupType();
    }

    private Set<RelationInfo> members() {
        return groupId == null ? Set.of() : repository.getRelations(RelationTypeGroup.FROM_ENTITY_GROUP).getFrom(groupId);
    }

    @Override
    protected void processCustomerQuery(UUID customerId, Consumer<EntityData<?>> processor) {
        for (RelationInfo relationInfo : members()) {
            EntityData<?> ed = relationInfo.getTarget();
            if (ed.getEntityType() == groupType && checkCustomerId(customerId, ed) && matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected void processAll(Consumer<EntityData<?>> processor) {
        for (RelationInfo relationInfo : members()) {
            EntityData<?> ed = relationInfo.getTarget();
            if (ed.getEntityType() == groupType && matches(ed)) {
                processor.accept(ed);
            }
        }
    }

    @Override
    protected int getProbableResultSize() {
        return 64;
    }

}
