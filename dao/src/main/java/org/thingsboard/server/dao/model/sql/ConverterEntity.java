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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "converter")
public class ConverterEntity extends BaseVersionedEntity<Converter> implements BaseEntity<Converter> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ConverterType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type")
    private IntegrationType integrationType;

    @Column(name = "debug_mode")
    private boolean debugMode;

    @Column(name = "debug_settings")
    private String debugSettings;

    @Convert(converter = JsonConverter.class)
    @Column(name = "configuration")
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    @Column(name = "external_id")
    private UUID externalId;

    public ConverterEntity() {
        super();
    }

    public ConverterEntity(Converter converter) {
        super(converter);
        if (converter.getTenantId() != null) {
            this.tenantId = converter.getTenantId().getId();
        }
        this.name = converter.getName();
        this.type = converter.getType();
        this.integrationType = converter.getIntegrationType();
        this.debugMode = converter.isDebugMode();
        this.debugSettings = JacksonUtil.toString(converter.getDebugSettings());
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
        if (converter.getExternalId() != null) {
            this.externalId = converter.getExternalId().getId();
        }
    }

    @Override
    public Converter toData() {
        Converter converter = new Converter(new ConverterId(id));
        converter.setCreatedTime(createdTime);
        converter.setVersion(version);
        if (tenantId != null) {
            converter.setTenantId(TenantId.fromUUID(tenantId));
        }
        converter.setName(name);
        converter.setType(type);
        converter.setIntegrationType(integrationType);
        converter.setDebugMode(debugMode);
        converter.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        converter.setConfiguration(configuration);
        converter.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            converter.setExternalId(new ConverterId(externalId));
        }
        return converter;
    }

}
