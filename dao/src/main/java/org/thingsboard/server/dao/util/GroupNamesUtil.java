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
package org.thingsboard.server.dao.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Collapses (entityId, groupName) rows produced by a batch entity-group lookup query
 * into a Map keyed by entityId, with the group names for each entity comma-joined.
 */
public class GroupNamesUtil {

    private GroupNamesUtil() {
    }

    public static Map<UUID, String> groupNamesByEntityId(List<Object[]> rows) {
        Map<UUID, List<String>> namesByEntityId = new HashMap<>();
        for (Object[] row : rows) {
            UUID entityId = (UUID) row[0];
            String groupName = (String) row[1];
            namesByEntityId.computeIfAbsent(entityId, id -> new ArrayList<>()).add(groupName);
        }
        Map<UUID, String> result = new HashMap<>();
        namesByEntityId.forEach((entityId, names) -> {
            names.sort(Comparator.naturalOrder());
            result.put(entityId, String.join(", ", names));
        });
        return result;
    }

}
