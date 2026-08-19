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
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
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
 * (clientConfiguration): {connectionString, topicName, subName}.
 * CE simplification vs PE: downlink via a separate downlinkConnectionString/downlinkTopicName is
 * not implemented in this build (uplink-only).
 */
@Slf4j
public class AzureServiceBusIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile ServiceBusProcessorClient processorClient;

    public AzureServiceBusIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = KafkaIntegrationConsumer.clientConfig(integration.getConfiguration());
        String connectionString = KafkaIntegrationConsumer.required(cfg, "connectionString");
        String topicName = KafkaIntegrationConsumer.required(cfg, "topicName");
        String subName = KafkaIntegrationConsumer.required(cfg, "subName");

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
        log.info("[{}][{}] Azure Service Bus integration started, topic [{}], subscription [{}]",
                tenantId, integration.getName(), topicName, subName);
    }

    private void onError(ServiceBusErrorContext context) {
        log.warn("[{}][{}] Service Bus processor error: {}", tenantId, integration.getName(), context.getException().getMessage());
    }

    @Override
    public void stop() {
        ServiceBusProcessorClient c = this.processorClient;
        if (c != null) {
            c.stop();
            c.close();
        }
    }

}
