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
package org.thingsboard.server.service.solutions.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

/**
 * Flat (type, id) pair used to persist the list of entities an install created into a tenant
 * attribute. {@link EntityId} itself is an interface with per-type implementations, so it needs a
 * concrete shape to round-trip through plain JSON.
 */
public record EntityIdRef(@JsonProperty("entityType") EntityType entityType, @JsonProperty("id") String id) {

    @JsonCreator
    public EntityIdRef {
    }

    public static EntityIdRef of(EntityId entityId) {
        return new EntityIdRef(entityId.getEntityType(), entityId.getId().toString());
    }

    public EntityId toEntityId() {
        return EntityIdFactory.getByTypeAndId(entityType.name(), id);
    }

}
