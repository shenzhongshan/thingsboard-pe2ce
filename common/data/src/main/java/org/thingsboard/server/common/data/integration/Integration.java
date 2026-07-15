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
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * CE port of PE's integration entity. Dropped vs PE: remote integrations (isRemote always false),
 * edge templates, VC external id / version.
 * Configuration JSON is type-specific (MQTT: host/port/credentials/topicFilters; HTTP: none —
 * inbound endpoint is derived from the routing key).
 */
@Schema
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Integration extends BaseData<IntegrationId> implements HasName, HasTenantId, HasDebugSettings {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "integration name", example = "MQTT integration")
    private String name;

    @Getter @Setter
    @Schema(description = "integration type")
    private IntegrationType type;

    @Getter @Setter
    @Schema(description = "JSON object with the uplink converter Id")
    private ConverterId defaultConverterId;

    @Getter @Setter
    @Schema(description = "JSON object with the downlink converter Id (optional)")
    private ConverterId downlinkConverterId;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "routingKey")
    @Schema(description = "routing key used to identify the integration in inbound endpoints", accessMode = Schema.AccessMode.READ_ONLY)
    private String routingKey;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "secret")
    @Schema(description = "integration secret", accessMode = Schema.AccessMode.READ_ONLY)
    private String secret;

    @Getter @Setter
    @Schema(description = "enable/disable the integration", example = "true")
    private boolean enabled;

    @Getter @Setter
    @Schema(description = "allow the integration to create devices or assets for unknown uplink entities", example = "true")
    private boolean allowCreateDevicesOrAssets;

    @Deprecated
    @Schema(description = "enable persisting debug events", example = "false", deprecated = true)
    private boolean debugMode;

    @Getter @Setter
    @Schema(description = "Debug settings object.")
    private DebugSettings debugSettings;

    @Getter @Setter
    @Schema(description = "a JSON value with type-specific connection configuration", implementation = JsonNode.class)
    private transient JsonNode configuration;

    @Getter @Setter
    @NoXss
    @Schema(description = "Additional parameters of the integration", implementation = JsonNode.class)
    private transient JsonNode additionalInfo;

    public Integration() {
        super();
    }

    public Integration(IntegrationId id) {
        super(id);
    }

    public Integration(Integration integration) {
        super(integration);
        this.tenantId = integration.getTenantId();
        this.name = integration.getName();
        this.type = integration.getType();
        this.defaultConverterId = integration.getDefaultConverterId();
        this.downlinkConverterId = integration.getDownlinkConverterId();
        this.routingKey = integration.getRoutingKey();
        this.secret = integration.getSecret();
        this.enabled = integration.isEnabled();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.debugMode = integration.debugMode;
        this.debugSettings = integration.debugSettings;
        this.configuration = integration.getConfiguration() != null ? integration.getConfiguration().deepCopy() : null;
        this.additionalInfo = integration.getAdditionalInfo();
    }

    @Override
    @Schema(description = "JSON object with the Integration Id. Omit this field to create a new integration.")
    public IntegrationId getId() {
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
        return "Integration [tenantId=" + tenantId + ", name=" + name + ", type=" + type
                + ", routingKey=" + routingKey + ", enabled=" + enabled + "]";
    }

}
