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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * In-process MQTT client for an MQTT-family integration (MQTT, AWS_IOT, AZURE_IOT_HUB, TTN, TTI):
 * connects to the external broker described in the integration configuration and feeds received
 * messages to the uplink processing chain. Configuration JSON: {host, port, ssl?, clientId?,
 * credentials: {type: 'anonymous'|'basic'|'cert.PEM'|'sas', username?, password?, caCert?, cert?,
 * privateKey?, privateKeyPassword?, sasKey?}, topicFilters: [{filter, qos}], downlinkTopic?}.
 * cert.PEM does mutual-TLS (client cert + key); sas does server-only TLS with the broker password
 * built by the caller (AzureIotHubRuntimeFactory computes the time-limited SAS token) before the
 * config JSON is handed to this client. CE simplification vs PE: no separate downlink-converter
 * execution pipeline — downlink publishes the raw rule-node message payload to downlinkTopic.
 */
@Slf4j
public class MqttIntegrationClient implements IntegrationRuntime {

    private static final int CONNECT_TIMEOUT_SEC = 10;
    private static final int RETRANSMISSION_MAX_ATTEMPTS = 3;
    private static final long RETRANSMISSION_INITIAL_DELAY_MS = 5000L;
    private static final double RETRANSMISSION_JITTER_FACTOR = 0.15d;

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile MqttClient client;
    private volatile ExecutorService handlerExecutorService;

    public MqttIntegrationClient(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = integration.getConfiguration();
        if (cfg == null || !cfg.has("host")) {
            throw new IllegalArgumentException("MQTT host is not configured!");
        }
        IntegrationType type = integration.getType();
        String host = resolveHost(cfg, type);
        int port = resolvePort(cfg, type);
        JsonNode credentials = cfg.get("credentials");
        String credType = credentials != null ? credentials.path("type").asText("anonymous") : "anonymous";
        if (type == IntegrationType.AWS_IOT && !"cert.PEM".equals(credType)) {
            throw new IllegalArgumentException("AWS IoT integrations require certificate (cert.PEM) credentials!");
        }
        if ((type == IntegrationType.TTN || type == IntegrationType.TTI) && !"basic".equals(credType)) {
            throw new IllegalArgumentException("The Things Stack integrations require Application ID / API key (basic) credentials!");
        }

        MqttClientConfig config = new MqttClientConfig(buildSslContext(cfg, credentials, credType, type));
        config.setProtocolVersion(resolveProtocolVersion(cfg));
        // MqttClientConfig leaves this null and netty-mqtt does not default it, so subscribing NPEs
        // inside RetransmissionHandler as soon as the SUBSCRIBE actually flushes (it only starts the
        // timer for a sent packet, which is why the failure looks intermittent). Every other caller
        // - TbMqttNode, the black-box tests - sets it; these values match thingsboard.yml's
        // mqtt.client.retransmission defaults.
        config.setRetransmissionConfig(new MqttClientConfig.RetransmissionConfig(
                RETRANSMISSION_MAX_ATTEMPTS, RETRANSMISSION_INITIAL_DELAY_MS, RETRANSMISSION_JITTER_FACTOR));
        String clientId = cfg.has("clientId") ? cfg.get("clientId").asText() : null;
        config.setClientId(StringUtils.isNotEmpty(clientId) ? clientId : "tb-integration-" + integration.getId().getId().toString().substring(0, 8));
        if (type == IntegrationType.AWS_IOT || type == IntegrationType.AZURE_IOT_HUB) {
            config.setCleanSession(true);
        }
        if (type == IntegrationType.AZURE_IOT_HUB && "sas".equals(credType)) {
            String sasKey = credentials.path("sasKey").asText(null);
            if (StringUtils.isEmpty(sasKey)) {
                throw new IllegalArgumentException("Azure IoT Hub SAS credentials require sasKey!");
            }
            config.setUsername(AzureIotHubUtil.buildUsername(host, config.getClientId()));
            config.setPassword(AzureIotHubUtil.buildSasToken(host, config.getClientId(), sasKey, AzureIotHubUtil.DEFAULT_TTL_SEC));
        } else if ("basic".equals(credType)) {
            config.setUsername(credentials.path("username").asText(null));
            config.setPassword(credentials.path("password").asText(null));
        }
        config.setReconnect(true);
        // The same handler must be given to both create() and on(): netty-mqtt only falls back to
        // the default handler when NO subscription matched the topic, so subscribing with anything
        // else here would silently swallow every message on a configured filter.
        MqttHandler uplinkHandler = (topic, payload) -> {
            byte[] data = new byte[payload.readableBytes()];
            payload.readBytes(data);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("topic", topic);
            try {
                uplinkSink.processUplink(integration, data, metadata);
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to process uplink from topic [{}]: {}",
                        tenantId, integration.getName(), topic, e.getMessage());
            }
            return Futures.immediateVoidFuture();
        };
        // netty-mqtt runs every inbound-message handler through this executor and does NOT null-check
        // it: passing null makes the first PUBLISH die in Futures.transform with an NPE, so the
        // client connects and subscribes and then silently drops everything. It also must not be the
        // netty event loop - processUplink runs the converter and talks to the platform.
        // Single-threaded on purpose: it preserves the broker's message order per integration and
        // bounds the thread count when a host runs many integrations.
        handlerExecutorService = Executors.newSingleThreadExecutor(
                ThingsBoardThreadFactory.forName("mqtt-integration-" + integration.getId().getId()));
        ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(handlerExecutorService);
        MqttClient mqttClient = MqttClient.create(config, uplinkHandler, new ListeningExecutor() {
            @Override
            public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
                return listeningExecutor.submit(task);
            }

            @Override
            public void execute(Runnable command) {
                listeningExecutor.execute(command);
            }
        });
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
                mqttClient.on(filter, uplinkHandler, MqttQoS.valueOf(qos));
            }
        }
        log.info("[{}][{}] MQTT integration connected to {}:{}", tenantId, integration.getName(), host, port);
    }

    /**
     * netty-mqtt's {@link MqttClientConfig} still defaults to MQTT 3.1, whose CONNECT carries the
     * legacy protocol name "MQIsdp". Strict 3.1.1 brokers reject that outright - AWS IoT Core and
     * Azure IoT Hub among them, both served by this very class. Every other MQTT consumer in
     * ThingsBoard (TbMqttNode, TbAzureIotHubNode) overrides the default for the same reason, so
     * default to 3.1.1 here too and only fall back to 3.1 if the integration explicitly asks.
     */
    private static MqttVersion resolveProtocolVersion(JsonNode cfg) {
        String configured = cfg.has("protocolVersion") ? cfg.get("protocolVersion").asText(null) : null;
        if (StringUtils.isEmpty(configured)) {
            return MqttVersion.MQTT_3_1_1;
        }
        try {
            return MqttVersion.valueOf(configured);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported MQTT protocolVersion [" + configured
                    + "]; expected one of MQTT_3_1, MQTT_3_1_1, MQTT_5");
        }
    }

    /**
     * TTN/TTI: unless customHost is set, the configured host is treated as a region (e.g. "eu1")
     * and auto-suffixed with the provider's broker domain.
     */
    private static String resolveHost(JsonNode cfg, IntegrationType type) {
        String host = cfg.get("host").asText();
        boolean customHost = cfg.has("customHost") && cfg.get("customHost").asBoolean(false);
        if (!customHost && (type == IntegrationType.TTN || type == IntegrationType.TTI)) {
            String suffix = type == IntegrationType.TTN ? ".thethings.network" : ".thethings.industries";
            return host.endsWith(suffix) ? host : host + suffix;
        }
        return host;
    }

    /** AWS IoT and Azure IoT Hub only accept MQTT-over-TLS on 8883. */
    private static int resolvePort(JsonNode cfg, IntegrationType type) {
        if (type == IntegrationType.AWS_IOT || type == IntegrationType.AZURE_IOT_HUB) {
            return 8883;
        }
        return cfg.has("port") ? cfg.get("port").asInt() : 1883;
    }

    /**
     * cert.PEM: mutual TLS using the configured CA/client cert/private key PEM strings.
     * sas / plain ssl=true: server-only TLS (trusts the configured CA cert, or the JVM default
     * trust store if none is given). AWS IoT / Azure IoT Hub always require TLS.
     */
    private SslContext buildSslContext(JsonNode cfg, JsonNode credentials, String credType, IntegrationType type) throws Exception {
        boolean needsTls = "cert.PEM".equals(credType) || "sas".equals(credType)
                || type == IntegrationType.AWS_IOT || type == IntegrationType.AZURE_IOT_HUB
                || (cfg.has("ssl") && cfg.get("ssl").asBoolean(false));
        if (!needsTls) {
            return null;
        }
        SslContextBuilder builder = SslContextBuilder.forClient();
        String caCert = credentials != null ? credentials.path("caCert").asText(null) : null;
        if (StringUtils.isNotEmpty(caCert)) {
            builder.trustManager(toStream(caCert));
        }
        if ("cert.PEM".equals(credType)) {
            String cert = credentials.path("cert").asText(null);
            String privateKey = credentials.path("privateKey").asText(null);
            if (StringUtils.isEmpty(cert) || StringUtils.isEmpty(privateKey)) {
                throw new IllegalArgumentException("Client certificate and private key are required for cert.PEM credentials!");
            }
            String keyPassword = credentials.path("privateKeyPassword").asText(null);
            builder.keyManager(toStream(cert), toStream(privateKey), StringUtils.isNotEmpty(keyPassword) ? keyPassword : null);
            builder.clientAuth(ClientAuth.NONE);
        }
        return builder.build();
    }

    private static InputStream toStream(String pem) {
        return new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    }

    public Future<Void> publish(String topic, byte[] payload) {
        MqttClient mqttClient = this.client;
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new IllegalStateException("MQTT integration is not connected!");
        }
        return mqttClient.publish(topic, Unpooled.wrappedBuffer(payload));
    }

    @Override
    public void pushDownlink(byte[] payload, Map<String, String> metadata) throws Exception {
        JsonNode cfg = integration.getConfiguration();
        String topic = null;
        if (cfg != null) {
            if (cfg.has("downlinkTopic")) {
                topic = cfg.get("downlinkTopic").asText();
            } else if (cfg.has("downlinkTopicPattern")) {
                topic = cfg.get("downlinkTopicPattern").asText();
            }
        }
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("Downlink topic is not configured for this integration!");
        }
        publish(topic, payload).get(5, TimeUnit.SECONDS);
    }

    @Override
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
        ExecutorService executor = this.handlerExecutorService;
        this.handlerExecutorService = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
