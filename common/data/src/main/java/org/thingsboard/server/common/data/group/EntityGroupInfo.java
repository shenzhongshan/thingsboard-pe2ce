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
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Set;

/**
 * An {@link EntityGroup} enriched with its owner chain, so the UI can render/filter groups by
 * ownership without a second round trip. {@code ownerIds} runs from the group's direct owner up
 * through any parent customers to the tenant.
 */
@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityGroupInfo extends EntityGroup {

    private static final long serialVersionUID = 4771264926280948453L;

    @Getter
    @Setter
    @Schema(description = "List of the entity group owners, from the direct owner up to the tenant.")
    private Set<EntityId> ownerIds;

    public EntityGroupInfo() {
        super();
    }

    public EntityGroupInfo(EntityGroupId id) {
        super(id);
    }

    public EntityGroupInfo(EntityGroup entityGroup) {
        super(entityGroup);
    }

    public EntityGroupInfo(EntityGroup entityGroup, Set<EntityId> ownerIds) {
        super(entityGroup);
        this.ownerIds = ownerIds;
    }

    @Override
    public String toString() {
        return "EntityGroupInfo [type=" + getType() + ", name=" + getName() + ", ownerIds=" + ownerIds + "]";
    }

}
