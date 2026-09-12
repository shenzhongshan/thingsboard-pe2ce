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
import org.thingsboard.server.common.data.role.RoleType;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class RoleFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;

    /**
     * Constructor used by the JPA projection in {@code RoleRepository#findNextBatch} - the parameter
     * order must stay in sync with the SELECT NEW clause there. customerId is the role's owner;
     * AbstractEntityFields#checkId maps PE's NULL_UUID sentinel back to null for us.
     */
    public RoleFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version,
                      RoleType type, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, customerId, name, version);
        this.type = type != null ? type.name() : null;
        this.additionalInfo = FieldsUtil.getText(additionalInfo);
    }

}
