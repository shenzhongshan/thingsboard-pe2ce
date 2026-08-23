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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * Eclipse Milo-based OPC-UA client for an OPC_UA integration. Configuration JSON: {applicationName?,
 * applicationUri?, host, port, endpoint?, scanPeriodInSeconds? (subscription publishing interval),
 * security? (Milo SecurityPolicy name, default "None"), identity: {type: 'anonymous'|'username',
 * username?, password?}, mapping: [{deviceName, namespace?, subscriptionTags: [{key, nodeId,
 * required?}]}]}.
 * CE simplification vs PE: PE's `mapping` uses a regex-like `deviceNodePattern` to dynamically
 * discover many devices by browsing the server's address space at runtime (ID or Fully-Qualified-
 * Name matching). This port simplifies each mapping entry to describe exactly one device, whose
 * tags are subscribed to by their literal OPC-UA NodeId string (e.g. "ns=2;s=Device1.Temperature")
 * — no dynamic tree browsing/pattern discovery.
 */
@Slf4j
public class OpcUaIntegrationClient implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile OpcUaClient client;
    private final Map<UInteger, TagBinding> bindingsByClientHandle = new ConcurrentHashMap<>();

    private record TagBinding(String deviceName, String key) {
    }

    public OpcUaIntegrationClient(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = integration.getConfiguration();
        String host = IntegrationConfigUtil.required(cfg, "host");
        int port = cfg.has("port") ? cfg.get("port").asInt() : 49320;
        String endpointPath = cfg.has("endpoint") ? cfg.get("endpoint").asText("") : "";
        String endpointUrl = "opc.tcp://" + host + ":" + port + endpointPath;
        SecurityPolicy securityPolicy = cfg.has("security") && StringUtils.isNotEmpty(cfg.get("security").asText())
                ? SecurityPolicy.valueOf(cfg.get("security").asText()) : SecurityPolicy.None;
        double publishingInterval = cfg.has("scanPeriodInSeconds") ? cfg.get("scanPeriodInSeconds").asDouble() * 1000.0 : 1000.0;

        JsonNode identityCfg = cfg.get("identity");
        IdentityProvider identityProvider = new AnonymousProvider();
        if (identityCfg != null && "username".equals(identityCfg.path("type").asText())) {
            identityProvider = new UsernameProvider(identityCfg.path("username").asText(null), identityCfg.path("password").asText(null));
        }
        final IdentityProvider finalIdentityProvider = identityProvider;
        String applicationName = cfg.has("applicationName") ? cfg.get("applicationName").asText() : "ThingsBoard OPC-UA Integration";
        String applicationUri = cfg.has("applicationUri") ? cfg.get("applicationUri").asText() : "urn:thingsboard:opcua:client";

        OpcUaClient opcUaClient = OpcUaClient.create(
                endpointUrl,
                endpoints -> endpoints.stream()
                        .filter(e -> securityPolicy.getUri().equals(e.getSecurityPolicyUri()))
                        .findFirst()
                        .or(() -> endpoints.stream().findFirst()),
                configBuilder -> configBuilder
                        .setApplicationName(LocalizedText.english(applicationName))
                        .setApplicationUri(applicationUri)
                        .setIdentityProvider(finalIdentityProvider)
                        .setRequestTimeout(uint(10000))
                        .build());
        opcUaClient.connect().get();
        this.client = opcUaClient;

        UaSubscription subscription = opcUaClient.getSubscriptionManager().createSubscription(publishingInterval).get();
        JsonNode mapping = cfg.get("mapping");
        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        List<TagBinding> bindingsInOrder = new ArrayList<>();
        if (mapping != null && mapping.isArray()) {
            int clientHandle = 1;
            for (JsonNode deviceMapping : mapping) {
                String deviceName = deviceMapping.has("deviceName") ? deviceMapping.get("deviceName").asText()
                        : deviceMapping.path("deviceNodePattern").asText("opcua-device");
                JsonNode tags = deviceMapping.get("subscriptionTags");
                if (tags == null || !tags.isArray()) {
                    continue;
                }
                for (JsonNode tag : tags) {
                    String key = tag.path("key").asText(null);
                    String nodeIdStr = tag.path("nodeId").isMissingNode() ? tag.path("path").asText(null) : tag.path("nodeId").asText(null);
                    if (StringUtils.isEmpty(key) || StringUtils.isEmpty(nodeIdStr)) {
                        continue;
                    }
                    NodeId nodeId = NodeId.parse(nodeIdStr);
                    ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, null);
                    UInteger handle = uint(clientHandle++);
                    MonitoringParameters params = new MonitoringParameters(handle, publishingInterval, null, uint(10), true);
                    requests.add(new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, params));
                    bindingsInOrder.add(new TagBinding(deviceName, key));
                    bindingsByClientHandle.put(handle, new TagBinding(deviceName, key));
                }
            }
        }
        if (!requests.isEmpty()) {
            subscription.createMonitoredItems(TimestampsToReturn.Both, requests,
                    (item, id) -> item.setValueConsumer(this::onValueChanged)).get();
        }
        log.info("[{}][{}] OPC-UA integration connected to {}, {} monitored item(s)",
                tenantId, integration.getName(), endpointUrl, requests.size());
    }

    /**
     * Feeds a raw {deviceName, key, value} reading to the shared processUplink() pipeline, same
     * as every other integration type — the integration's (user-configurable) uplink converter
     * script is what actually reshapes this into {deviceName, telemetry: {...}}.
     */
    private void onValueChanged(org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem item, DataValue value) {
        TagBinding binding = bindingsByClientHandle.get(item.getClientHandle());
        if (binding == null || value.getValue() == null) {
            return;
        }
        try {
            Object rawValue = value.getValue().getValue();
            ObjectNode reading = JacksonUtil.newObjectNode();
            reading.put("deviceName", binding.deviceName());
            reading.put("key", binding.key());
            reading.put("value", rawValue != null ? rawValue.toString() : null);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("deviceName", binding.deviceName());
            uplinkSink.processUplink(integration, JacksonUtil.writeValueAsBytes(reading), metadata);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to process OPC-UA value change: {}", tenantId, integration.getName(), e.getMessage());
        }
    }

    @Override
    public void stop() {
        OpcUaClient c = this.client;
        if (c != null) {
            try {
                c.disconnect().get();
            } catch (Exception e) {
                log.debug("[{}] Failed to disconnect OPC-UA client", integration.getId(), e);
            }
            this.client = null;
        }
    }

}
