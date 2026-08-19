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
package org.thingsboard.server.common.data.converter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * CE port of PE's data converter. Dropped vs PE: edge templates, converter_version.
 * Configuration JSON carries the decoder/encoder script:
 * {"scriptLang":"TBEL"|"JS","decoder":"...","encoder":"..."}.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Converter extends BaseData<ConverterId> implements HasName, HasTenantId, HasDebugSettings,
        HasVersion, ExportableEntity<ConverterId> {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "converter name", example = "MQTT uplink converter")
    private String name;

    @Getter @Setter
    @Schema(description = "converter type")
    private ConverterType type;

    @Getter @Setter
    @Schema(description = "integration type this converter is intended for (optional)")
    private IntegrationType integrationType;

    @Deprecated
    @Schema(description = "enable persisting debug events", example = "true", deprecated = true)
    private boolean debugMode;

    @Getter @Setter
    @Schema(description = "Debug settings object.")
    private DebugSettings debugSettings;

    @Getter @Setter
    @Schema(description = "a JSON value with the converter scripts", implementation = JsonNode.class)
    private transient JsonNode configuration;

    @Getter @Setter
    @NoXss
    @Schema(description = "Additional parameters of the converter", implementation = JsonNode.class)
    private transient JsonNode additionalInfo;

    @Getter @Setter
    private ConverterId externalId;

    @Getter @Setter
    private Long version;

    public Converter() {
        super();
    }

    public Converter(ConverterId id) {
        super(id);
    }

    public Converter(Converter converter) {
        super(converter);
        this.tenantId = converter.getTenantId();
        this.name = converter.getName();
        this.type = converter.getType();
        this.integrationType = converter.getIntegrationType();
        this.debugMode = converter.debugMode;
        this.debugSettings = converter.debugSettings;
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
        this.externalId = converter.getExternalId();
        this.version = converter.getVersion();
    }

    @Override
    @Schema(description = "JSON object with the Converter Id. Omit this field to create a new converter.")
    public ConverterId getId() {
        return super.getId();
    }

    @Override
    public String getName() {
        return name;
    }

    // Getter is ignored for serialization
    @Override
    @JsonIgnore
    public boolean isDebugMode() {
        return debugMode;
    }

    // Setter is annotated for deserialization (backward compat with old boolean clients)
    @Override
    @JsonSetter
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public String toString() {
        return "Converter [tenantId=" + tenantId + ", name=" + name + ", type=" + type
                + ", integrationType=" + integrationType + ", debugMode=" + debugMode + "]";
    }

}
