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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@Table(name = "custom_translation")
@IdClass(CustomTranslationCompositeKey.class)
public class CustomTranslationEntity implements ToData<CustomTranslation>, Serializable {

    @Id
    @Column(name = "tenant_id", columnDefinition = "uuid")
    private UUID tenantId;

    @Id
    @Column(name = "customer_id", columnDefinition = "uuid")
    private UUID customerId;

    @Id
    @Column(name = "locale_code")
    private String localeCode;

    @Convert(converter = JsonConverter.class)
    @Column(name = "value")
    private JsonNode value;

    public CustomTranslationEntity() {
    }

    public CustomTranslationEntity(CustomTranslation customTranslation) {
        this.tenantId = customTranslation.getTenantId().getId();
        this.customerId = customTranslation.getCustomerId() != null ? customTranslation.getCustomerId().getId() : EntityId.NULL_UUID;
        this.localeCode = customTranslation.getLocaleCode();
        this.value = customTranslation.getValue();
    }

    @Override
    public CustomTranslation toData() {
        CustomTranslation customTranslation = new CustomTranslation();
        customTranslation.setTenantId(TenantId.fromUUID(tenantId));
        if (!EntityId.NULL_UUID.equals(customerId)) {
            customTranslation.setCustomerId(new CustomerId(customerId));
        }
        customTranslation.setLocaleCode(localeCode);
        customTranslation.setValue(value);
        return customTranslation;
    }

}
