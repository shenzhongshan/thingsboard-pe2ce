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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CustomTranslationCompositeKey implements Serializable {

    private UUID tenantId;
    private UUID customerId;
    private String localeCode;

    public CustomTranslationCompositeKey(TenantId tenantId, CustomerId customerId, String localeCode) {
        this(tenantId.getId(), customerId != null ? customerId.getId() : EntityId.NULL_UUID, localeCode);
    }

    public CustomTranslationCompositeKey(UUID tenantId, UUID customerId, String localeCode) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.localeCode = localeCode;
    }

}
