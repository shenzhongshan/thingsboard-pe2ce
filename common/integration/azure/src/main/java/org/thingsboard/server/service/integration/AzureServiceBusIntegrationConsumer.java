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

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure Service Bus consumer for an AZURE_SERVICE_BUS integration. Configuration JSON
 * (clientConfiguration): {connectionString, topicName, subName, downlinkConnectionString?,
 * downlinkTopicName?}.
 * <p>
 * Downlink mirrors PE's AzureServiceBusIntegration#initSenderClient: a separate
 * {@link ServiceBusSenderClient} built from the downlink connection string + topic, because the
 * downlink topic usually lives in a different namespace (or at least needs Send rather than Listen
 * rights) than the uplink subscription. A blank downlink connection string falls back to the uplink
 * one, which is the common single-namespace setup.
 */
@Slf4j
public class AzureServiceBusIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile ServiceBusProcessorClient processorClient;
    private volatile ServiceBusSenderClient senderClient;

    public AzureServiceBusIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = IntegrationConfigUtil.clientConfig(integration.getConfiguration());
        String connectionString = IntegrationConfigUtil.required(cfg, "connectionString");
        String topicName = IntegrationConfigUtil.required(cfg, "topicName");
        String subName = IntegrationConfigUtil.required(cfg, "subName");

        // ServiceBusProcessorClient.start() is fire-and-forget (retries connectivity issues forever
        // in the background, with a generously long default retry policy), so it never surfaces a
        // bad connection string/unreachable namespace as a thrown exception. Do a synchronous peek
        // first, with a short no-retry policy, so failures show up immediately instead of only
        // leaving the integration status stuck on STOPPED for minutes.
        AmqpRetryOptions fastFailRetry = new AmqpRetryOptions().setMaxRetries(0).setTryTimeout(Duration.ofSeconds(10));
        try (ServiceBusReceiverClient probe = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .retryOptions(fastFailRetry)
                .receiver()
                .topicName(topicName)
                .subscriptionName(subName)
                .buildClient()) {
            probe.peekMessage();
        }

        ServiceBusProcessorClient processor = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(topicName)
                .subscriptionName(subName)
                .processMessage(context -> {
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("messageId", context.getMessage().getMessageId());
                    try {
                        uplinkSink.processUplink(integration, context.getMessage().getBody().toBytes(), metadata);
                        context.complete();
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to process Service Bus uplink: {}", tenantId, integration.getName(), e.getMessage());
                        context.abandon();
                    }
                })
                .processError(this::onError)
                .buildProcessorClient();
        processor.start();
        this.processorClient = processor;

        // Only built when a downlink topic is configured; without it pushDownlink() reports the
        // same "not supported" error as any other uplink-only runtime rather than failing obscurely.
        String downlinkTopic = cfg.has("downlinkTopicName") ? cfg.get("downlinkTopicName").asText("") : "";
        if (!downlinkTopic.isEmpty()) {
            String downlinkCs = cfg.has("downlinkConnectionString") ? cfg.get("downlinkConnectionString").asText("") : "";
            if (downlinkCs.isEmpty()) {
                downlinkCs = connectionString;
            }
            this.senderClient = new ServiceBusClientBuilder()
                    .connectionString(downlinkCs)
                    .sender()
                    .topicName(downlinkTopic)
                    .buildClient();
            log.info("[{}][{}] Azure Service Bus downlink sender ready, topic [{}]",
                    tenantId, integration.getName(), downlinkTopic);
        }

        log.info("[{}][{}] Azure Service Bus integration started, topic [{}], subscription [{}]",
                tenantId, integration.getName(), topicName, subName);
    }

    private void onError(ServiceBusErrorContext context) {
        log.warn("[{}][{}] Service Bus processor error: {}", tenantId, integration.getName(), context.getException().getMessage());
    }

    @Override
    public void pushDownlink(byte[] payload, Map<String, String> metadata) {
        ServiceBusSenderClient sender = this.senderClient;
        if (sender == null) {
            throw new IllegalStateException("Downlink topic is not configured for this Service Bus integration!");
        }
        ServiceBusMessage message = new ServiceBusMessage(payload);
        if (metadata != null) {
            // Same convention as the MQTT runtime: whatever the downlink converter puts in the
            // returned metadata wins, and is forwarded as Service Bus application properties.
            String contentType = metadata.get("contentType");
            if (contentType != null && !contentType.isEmpty()) {
                message.setContentType(contentType);
            }
            String messageId = metadata.get("messageId");
            if (messageId != null && !messageId.isEmpty()) {
                message.setMessageId(messageId);
            }
            metadata.forEach((k, v) -> {
                if (v != null && !"contentType".equals(k) && !"messageId".equals(k)) {
                    message.getApplicationProperties().put(k, v);
                }
            });
        }
        sender.sendMessage(message);
    }

    @Override
    public void stop() {
        ServiceBusProcessorClient c = this.processorClient;
        if (c != null) {
            c.stop();
            c.close();
        }
        ServiceBusSenderClient s = this.senderClient;
        if (s != null) {
            s.close();
            this.senderClient = null;
        }
    }

}
