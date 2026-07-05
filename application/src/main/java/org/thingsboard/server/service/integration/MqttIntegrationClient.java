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
import com.google.common.util.concurrent.Futures;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * In-process MQTT client for an MQTT integration: connects to the external broker described in
 * the integration configuration and feeds received messages to the uplink processing chain.
 * Configuration JSON: {host, port, ssl?, clientId?, credentials: {type: 'anonymous'|'basic',
 * username?, password?}, topicFilters: [{filter, qos}]}. CE simplifications vs PE: no cert-based
 * credentials, no downlink publishing yet.
 */
@Slf4j
public class MqttIntegrationClient {

    private static final int CONNECT_TIMEOUT_SEC = 10;

    private final Integration integration;
    private final PlatformIntegrationService platformIntegrationService;
    @Getter
    private final TenantId tenantId;

    private volatile MqttClient client;

    public MqttIntegrationClient(Integration integration, PlatformIntegrationService platformIntegrationService) {
        this.integration = integration;
        this.platformIntegrationService = platformIntegrationService;
        this.tenantId = integration.getTenantId();
    }

    public void start() throws Exception {
        JsonNode cfg = integration.getConfiguration();
        if (cfg == null || !cfg.has("host")) {
            throw new IllegalArgumentException("MQTT host is not configured!");
        }
        String host = cfg.get("host").asText();
        int port = cfg.has("port") ? cfg.get("port").asInt() : 1883;
        MqttClientConfig config = new MqttClientConfig();
        String clientId = cfg.has("clientId") ? cfg.get("clientId").asText() : null;
        config.setClientId(StringUtils.isNotEmpty(clientId) ? clientId : "tb-integration-" + integration.getId().getId().toString().substring(0, 8));
        JsonNode credentials = cfg.get("credentials");
        if (credentials != null && "basic".equals(credentials.path("type").asText())) {
            config.setUsername(credentials.path("username").asText(null));
            config.setPassword(credentials.path("password").asText(null));
        }
        config.setReconnect(true);
        MqttClient mqttClient = MqttClient.create(config, (topic, payload) -> {
            byte[] data = new byte[payload.readableBytes()];
            payload.readBytes(data);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("topic", topic);
            try {
                platformIntegrationService.processUplink(integration, data, metadata);
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to process uplink from topic [{}]: {}",
                        tenantId, integration.getName(), topic, e.getMessage());
            }
            return Futures.immediateVoidFuture();
        }, null);
        MqttConnectResult result = mqttClient.connect(host, port).get(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!result.isSuccess()) {
            mqttClient.disconnect();
            throw new RuntimeException("Failed to connect to MQTT broker at " + host + ":" + port
                    + ". Result code: " + result.getReturnCode());
        }
        this.client = mqttClient;
        JsonNode topicFilters = cfg.get("topicFilters");
        if (topicFilters != null && topicFilters.isArray()) {
            for (JsonNode tf : topicFilters) {
                String filter = tf.has("filter") ? tf.get("filter").asText() : tf.asText();
                int qos = tf.has("qos") ? tf.get("qos").asInt() : 0;
                mqttClient.on(filter, (topic, payload) -> Futures.immediateVoidFuture(),
                        MqttQoS.valueOf(qos));
            }
        }
        log.info("[{}][{}] MQTT integration connected to {}:{}", tenantId, integration.getName(), host, port);
    }

    public void stop() {
        MqttClient mqttClient = this.client;
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (Exception e) {
                log.debug("[{}] Failed to disconnect MQTT integration client", integration.getId(), e);
            }
            this.client = null;
        }
    }

}
