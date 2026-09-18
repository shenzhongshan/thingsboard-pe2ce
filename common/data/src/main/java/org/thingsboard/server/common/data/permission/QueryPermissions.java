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
package org.thingsboard.server.common.data.permission;

import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * What a querying user is allowed to see, reduced to plain data so the query engines can consume it.
 * PE passes its whole MergedUserPermissions down; in CE that class lives in the application module,
 * which dao and common/edqs cannot depend on, so only the two decisions the engines actually need
 * are carried across:
 * <ul>
 *   <li>{@code genericReadTypes} — entity types the user may READ anywhere inside their own owner
 *       scope. Absent the type, owner scope grants nothing: this is what makes enforcement
 *       deny-by-default rather than "owner scope always allowed".</li>
 *   <li>{@code readableEntityGroupIds} — entity groups granted through a GROUP role. Unioned with
 *       owner scope, and able to reach across owners, because that is what sharing a group means.</li>
 * </ul>
 *
 * {@link #UNRESTRICTED} is for internal callers (rule engine, API limits, entity-info lookups) that
 * run at tenant scope and are not acting on behalf of a user.
 */
public record QueryPermissions(Set<EntityType> genericReadTypes, Set<UUID> readableEntityGroupIds,
                               Set<EntityType> attributeReadTypes, Set<EntityType> timeseriesReadTypes,
                               boolean unrestricted) {

    public static final QueryPermissions UNRESTRICTED =
            new QueryPermissions(Collections.emptySet(), Collections.emptySet(),
                    Collections.emptySet(), Collections.emptySet(), true);

    public static QueryPermissions of(Set<EntityType> genericReadTypes, Set<UUID> readableEntityGroupIds,
                                      Set<EntityType> attributeReadTypes, Set<EntityType> timeseriesReadTypes) {
        return new QueryPermissions(
                genericReadTypes == null ? Collections.emptySet() : genericReadTypes,
                readableEntityGroupIds == null ? Collections.emptySet() : readableEntityGroupIds,
                attributeReadTypes == null ? Collections.emptySet() : attributeReadTypes,
                timeseriesReadTypes == null ? Collections.emptySet() : timeseriesReadTypes,
                false);
    }

    /** READ on an entity does not imply reading its attribute values - PE grants those separately. */
    public boolean hasAttributeRead(EntityType entityType) {
        return unrestricted || (entityType != null && attributeReadTypes.contains(entityType));
    }

    public boolean hasTimeseriesRead(EntityType entityType) {
        return unrestricted || (entityType != null && timeseriesReadTypes.contains(entityType));
    }

    /** Whether the user's own owner scope may contribute results for this entity type. */
    public boolean hasGenericRead(EntityType entityType) {
        return unrestricted || (entityType != null && genericReadTypes.contains(entityType));
    }

    public boolean hasGroupGrants() {
        return !readableEntityGroupIds.isEmpty();
    }

}
