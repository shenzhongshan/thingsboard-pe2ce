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
package org.thingsboard.server.dao.eventsourcing;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Set;

/**
 * Asks the permission layer to drop cached permissions for the listed user groups.
 *
 * <p>Deliberately <b>not</b> a {@link DeleteEntityEvent}: the listeners for those are
 * {@code @TransactionalEventListener}s that run after commit, by which point the user group's
 * membership relations are already gone and the members can no longer be enumerated to evict them.
 * This one is consumed by a plain {@code @EventListener} and is published <i>before</i> the
 * deletion, while the memberships are still resolvable.
 */
@Builder
@Data
public class GroupPermissionsEvictEvent {

    private final TenantId tenantId;
    private final Set<EntityGroupId> userGroupIds;

}
