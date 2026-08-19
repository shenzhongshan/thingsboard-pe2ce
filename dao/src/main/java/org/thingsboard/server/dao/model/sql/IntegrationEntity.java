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
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "integration")
public class IntegrationEntity extends BaseVersionedEntity<Integration> implements BaseEntity<Integration> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private IntegrationType type;

    @Column(name = "converter_id")
    private UUID converterId;

    @Column(name = "downlink_converter_id")
    private UUID downlinkConverterId;

    @Column(name = "routing_key")
    private String routingKey;

    @Column(name = "secret")
    private String secret;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "allow_create_devices_or_assets")
    private boolean allowCreateDevicesOrAssets;

    @Column(name = ModelConstants.INTEGRATION_IS_REMOTE_PROPERTY)
    private boolean remote;

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

    public IntegrationEntity() {
        super();
    }

    public IntegrationEntity(Integration integration) {
        super(integration);
        if (integration.getTenantId() != null) {
            this.tenantId = integration.getTenantId().getId();
        }
        this.name = integration.getName();
        this.type = integration.getType();
        if (integration.getDefaultConverterId() != null) {
            this.converterId = integration.getDefaultConverterId().getId();
        }
        if (integration.getDownlinkConverterId() != null) {
            this.downlinkConverterId = integration.getDownlinkConverterId().getId();
        }
        this.routingKey = integration.getRoutingKey();
        this.secret = integration.getSecret();
        this.enabled = integration.isEnabled();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.remote = integration.isRemote();
        this.debugMode = integration.isDebugMode();
        this.debugSettings = JacksonUtil.toString(integration.getDebugSettings());
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
        if (integration.getExternalId() != null) {
            this.externalId = integration.getExternalId().getId();
        }
    }

    @Override
    public Integration toData() {
        Integration integration = new Integration(new IntegrationId(id));
        integration.setCreatedTime(createdTime);
        integration.setVersion(version);
        if (tenantId != null) {
            integration.setTenantId(TenantId.fromUUID(tenantId));
        }
        integration.setName(name);
        integration.setType(type);
        if (converterId != null) {
            integration.setDefaultConverterId(new ConverterId(converterId));
        }
        if (downlinkConverterId != null) {
            integration.setDownlinkConverterId(new ConverterId(downlinkConverterId));
        }
        integration.setRoutingKey(routingKey);
        integration.setSecret(secret);
        integration.setEnabled(enabled);
        integration.setAllowCreateDevicesOrAssets(allowCreateDevicesOrAssets);
        integration.setRemote(remote);
        integration.setDebugMode(debugMode);
        integration.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        integration.setConfiguration(configuration);
        integration.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            integration.setExternalId(new IntegrationId(externalId));
        }
        return integration;
    }

}
