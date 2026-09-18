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
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class IntegrationFields extends AbstractEntityFields {

    private String type;
    private String additionalInfo;

    /**
     * Constructor used by the JPA projection in {@code IntegrationRepository#findNextBatch} - the
     * parameter order must stay in sync with the SELECT NEW clause there. Integrations are
     * tenant-level (the integration table has no customer_id), hence no customerId parameter.
     */
    public IntegrationFields(UUID id, long createdTime, UUID tenantId, String name, Long version,
                             IntegrationType type, JsonNode additionalInfo) {
        super(id, createdTime, tenantId, name, version);
        this.type = type != null ? type.name() : null;
        this.additionalInfo = FieldsUtil.getText(additionalInfo);
    }

}
