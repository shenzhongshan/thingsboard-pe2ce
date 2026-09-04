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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Apache Pulsar consumer for an APACHE_PULSAR integration. Configuration JSON
 * (clientConfiguration): {serviceUrl, topics, subscriptionName, maxNumMessages?, maxNumBytes?,
 * timeoutInMs?, credentials: {type: 'anonymous'|'token', token?}}.
 */
@Slf4j
public class PulsarIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile PulsarClient client;
    private volatile Consumer<byte[]> consumer;
    private volatile Thread pollThread;
    private volatile boolean stopped;

    public PulsarIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = IntegrationConfigUtil.clientConfig(integration.getConfiguration());
        String serviceUrl = IntegrationConfigUtil.required(cfg, "serviceUrl");
        String topics = IntegrationConfigUtil.required(cfg, "topics");
        String subscriptionName = cfg.has("subscriptionName") ? cfg.get("subscriptionName").asText() : "tb-integration-" + IntegrationConfigUtil.safeId(integration);

        var clientBuilder = PulsarClient.builder().serviceUrl(serviceUrl);
        JsonNode credentials = cfg.get("credentials");
        if (credentials != null && "token".equals(credentials.path("type").asText())) {
            String token = credentials.path("token").asText(null);
            if (StringUtils.isNotEmpty(token)) {
                clientBuilder.authentication(org.apache.pulsar.client.api.AuthenticationFactory.token(token));
            }
        }
        PulsarClient pulsarClient = clientBuilder.build();
        java.util.List<String> topicList = java.util.Arrays.stream(topics.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        Consumer<byte[]> pulsarConsumer = pulsarClient.newConsumer()
                .topics(topicList)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.Shared)
                .subscribe();

        this.client = pulsarClient;
        this.consumer = pulsarConsumer;
        this.stopped = false;
        this.pollThread = new Thread(() -> pollLoop(cfg), "pulsar-integration-" + IntegrationConfigUtil.safeId(integration));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
        log.info("[{}][{}] Pulsar integration subscribed to [{}]", tenantId, integration.getName(), topics);
    }

    private void pollLoop(JsonNode cfg) {
        long timeoutMs = cfg.has("timeoutInMs") ? cfg.get("timeoutInMs").asLong() : 5000L;
        while (!stopped) {
            try {
                Message<byte[]> message = consumer.receive((int) timeoutMs, TimeUnit.MILLISECONDS);
                if (message == null) {
                    continue;
                }
                Map<String, String> metadata = new HashMap<>();
                metadata.put("topic", message.getTopicName());
                metadata.put("messageId", message.getMessageId().toString());
                try {
                    uplinkSink.processUplink(integration, message.getData(), metadata);
                    consumer.acknowledge(message);
                } catch (Exception e) {
                    log.warn("[{}][{}] Failed to process Pulsar uplink: {}", tenantId, integration.getName(), e.getMessage());
                    consumer.negativeAcknowledge(message);
                }
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("[{}][{}] Pulsar receive failed: {}", tenantId, integration.getName(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void stop() {
        stopped = true;
        Thread t = this.pollThread;
        if (t != null) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (consumer != null) {
                consumer.close();
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to close Pulsar consumer", integration.getId(), e);
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to close Pulsar client", integration.getId(), e);
        }
    }

}
