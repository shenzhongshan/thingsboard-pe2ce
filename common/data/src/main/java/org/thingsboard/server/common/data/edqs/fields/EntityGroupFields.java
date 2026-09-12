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
package org.thingsboard.server.common.data.edqs.fields;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

/**
 * {@code type} carries the group's <em>member</em> entity type ("DEVICE", "ASSET", ...), which is what
 * the group-name query processors match against, not a type of the group itself.
 * <p>
 * Unlike PE, {@code customerId} is populated (from owner_id when owner_type is CUSTOMER) so the
 * inherited customer scoping works on groups the same way it does on every other entity; PE's
 * entity_group table has no tenant_id at all and needs a bespoke sync path to resolve the owner,
 * whereas CE's does, so groups ride the generic {@code edqsTenantTypes} machinery.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class EntityGroupFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;
    private UUID ownerId;
    private EntityType ownerType;

    /**
     * Constructor used by the JPA projection in {@code EntityGroupRepository#findNextBatch} - the
     * parameter order must stay in sync with the SELECT NEW clause there.
     */
    public EntityGroupFields(UUID id, long createdTime, UUID tenantId, String name, Long version,
                             EntityType type, JsonNode additionalInfo, UUID ownerId, String ownerType) {
        super(id, createdTime, tenantId,
                EntityType.CUSTOMER.name().equals(ownerType) ? ownerId : null,
                name, version);
        this.type = type != null ? type.name() : null;
        this.additionalInfo = FieldsUtil.getText(additionalInfo);
        this.ownerId = checkId(ownerId);
        this.ownerType = ownerType != null ? EntityType.valueOf(ownerType) : null;
    }

    @Override
    public UUID getOwnerId() {
        return checkId(ownerId);
    }

}
