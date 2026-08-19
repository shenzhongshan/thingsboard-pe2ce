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
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerClient;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure Event Hub consumer for an AZURE_EVENT_HUB integration. Configuration JSON
 * (clientConfiguration): {connectionString, consumerGroup? (default "$Default")}.
 * CE simplification vs PE: checkpoints are kept in-memory (see InMemoryCheckpointStore) instead of
 * Azure Blob Storage, and downlink via a linked IoT Hub is not implemented in this build.
 */
@Slf4j
public class AzureEventHubIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile EventProcessorClient client;

    public AzureEventHubIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = KafkaIntegrationConsumer.clientConfig(integration.getConfiguration());
        String connectionString = KafkaIntegrationConsumer.required(cfg, "connectionString");
        String consumerGroup = cfg.has("consumerGroup") && !cfg.get("consumerGroup").asText().isEmpty()
                ? cfg.get("consumerGroup").asText() : "$Default";

        // EventProcessorClient.start() is fire-and-forget (retries connectivity issues forever in
        // the background, with a generously long default retry policy), so it never surfaces a bad
        // connection string/unreachable namespace as a thrown exception. Do a synchronous metadata
        // fetch first, with a short no-retry policy, so failures show up immediately instead of
        // only leaving the integration status stuck on STOPPED for minutes.
        AmqpRetryOptions fastFailRetry = new AmqpRetryOptions().setMaxRetries(0).setTryTimeout(Duration.ofSeconds(10));
        try (EventHubConsumerClient probe = new EventHubClientBuilder()
                .connectionString(connectionString)
                .consumerGroup(consumerGroup)
                .retryOptions(fastFailRetry)
                .buildConsumerClient()) {
            probe.getEventHubProperties();
        }

        EventProcessorClient processorClient = new EventProcessorClientBuilder()
                .connectionString(connectionString)
                .consumerGroup(consumerGroup)
                .checkpointStore(new InMemoryCheckpointStore())
                .processEvent(eventContext -> {
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("partitionId", eventContext.getPartitionContext().getPartitionId());
                    metadata.put("offset", String.valueOf(eventContext.getEventData().getOffset()));
                    try {
                        uplinkSink.processUplink(integration, eventContext.getEventData().getBody(), metadata);
                        eventContext.updateCheckpoint();
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to process Event Hub uplink: {}", tenantId, integration.getName(), e.getMessage());
                    }
                })
                .processError(errorContext -> log.warn("[{}][{}] Event Hub processor error on partition [{}]: {}",
                        tenantId, integration.getName(), errorContext.getPartitionContext().getPartitionId(),
                        errorContext.getThrowable().getMessage()))
                .buildEventProcessorClient();
        processorClient.start();
        this.client = processorClient;
        log.info("[{}][{}] Azure Event Hub integration started, consumer group [{}]", tenantId, integration.getName(), consumerGroup);
    }

    @Override
    public void stop() {
        EventProcessorClient c = this.client;
        if (c != null) {
            c.stop();
        }
    }

}
