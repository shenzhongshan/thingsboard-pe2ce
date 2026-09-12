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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class SchedulerEventQueryProcessor extends AbstractSimpleQueryProcessor<SchedulerEventFilter> {

    public SchedulerEventQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (SchedulerEventFilter) query.getEntityFilter(), EntityType.SCHEDULER_EVENT);
    }

    @Override
    protected boolean matches(EntityData<?> ed) {
        if (!super.matches(ed)) {
            return false;
        }
        if (StringUtils.isNotEmpty(filter.getEventType()) && !filter.getEventType().equals(ed.getFields().getType())) {
            return false;
        }
        AliasEntityId originator = filter.getOriginator();
        // An unresolved alias placeholder carries no id to compare against. Skip the constraint rather
        // than matching nothing, so this agrees with the SQL path in
        // DefaultEntityQueryRepository#schedulerEventQuery, which drops the clause in the same case.
        if (originator != null && !originator.isAliasEntityId()) {
            // AliasEntityIdImpl#equals compares against any EntityId by uuid, so this works against the
            // concrete originator id stored in SchedulerEventFields.
            return originator.equals(ed.getFields().getOriginatorId());
        }
        return true;
    }

}
