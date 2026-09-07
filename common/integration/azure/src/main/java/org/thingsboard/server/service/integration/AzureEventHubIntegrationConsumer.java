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
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
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
 * (clientConfiguration): {connectionString, iotHubName?, consumerGroup? (default "$Default"),
 * connectTimeoutSec?, enablePersistentCheckpoints?, storageConnectionString?, containerName?}.
 * <p>
 * Checkpoints default to in-memory (see InMemoryCheckpointStore); enabling persistent checkpoints
 * switches to PE's Azure Blob-backed store so offsets survive a restart. Downlink via a linked IoT
 * Hub service client is still not implemented in this build.
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
        JsonNode cfg = IntegrationConfigUtil.clientConfig(integration.getConfiguration());
        String connectionString = IntegrationConfigUtil.required(cfg, "connectionString");
        String consumerGroup = cfg.has("consumerGroup") && !cfg.get("consumerGroup").asText().isEmpty()
                ? cfg.get("consumerGroup").asText() : "$Default";
        // PE's iotHubName: when the connection string points at an IoT Hub's built-in Event Hub
        // endpoint, the entity name is not part of the connection string and must be supplied
        // separately. Blank means "the connection string already names the entity".
        String eventHubName = cfg.has("iotHubName") && !cfg.get("iotHubName").asText().isEmpty()
                ? cfg.get("iotHubName").asText() : null;
        int connectTimeoutSec = cfg.has("connectTimeoutSec") ? cfg.get("connectTimeoutSec").asInt() : 10;
        boolean persistentCheckpoints = cfg.has("enablePersistentCheckpoints")
                && cfg.get("enablePersistentCheckpoints").asBoolean();

        // EventProcessorClient.start() is fire-and-forget (retries connectivity issues forever in
        // the background, with a generously long default retry policy), so it never surfaces a bad
        // connection string/unreachable namespace as a thrown exception. Do a synchronous metadata
        // fetch first, with a short no-retry policy, so failures show up immediately instead of
        // only leaving the integration status stuck on STOPPED for minutes.
        AmqpRetryOptions fastFailRetry = new AmqpRetryOptions().setMaxRetries(0)
                .setTryTimeout(Duration.ofSeconds(connectTimeoutSec));
        EventHubClientBuilder probeBuilder = new EventHubClientBuilder()
                .consumerGroup(consumerGroup)
                .retryOptions(fastFailRetry);
        if (eventHubName != null) {
            probeBuilder.connectionString(connectionString, eventHubName);
        } else {
            probeBuilder.connectionString(connectionString);
        }
        try (EventHubConsumerClient probe = probeBuilder.buildConsumerClient()) {
            probe.getEventHubProperties();
        }

        EventProcessorClientBuilder processorBuilder = new EventProcessorClientBuilder()
                .consumerGroup(consumerGroup)
                .checkpointStore(buildCheckpointStore(cfg, persistentCheckpoints));
        if (eventHubName != null) {
            processorBuilder.connectionString(connectionString, eventHubName);
        } else {
            processorBuilder.connectionString(connectionString);
        }
        EventProcessorClient processorClient = processorBuilder
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

    /**
     * PE's buildCheckpointStore: an Azure Blob container makes offsets survive a restart, so the
     * integration resumes where it left off instead of replaying from the configured start position.
     * The in-memory store stays the default because the Blob one needs storage to be provisioned.
     */
    private CheckpointStore buildCheckpointStore(JsonNode cfg, boolean persistent) {
        if (!persistent) {
            return new InMemoryCheckpointStore();
        }
        String storageConnectionString = IntegrationConfigUtil.required(cfg, "storageConnectionString");
        String containerName = IntegrationConfigUtil.required(cfg, "containerName");
        BlobContainerAsyncClient containerClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName(containerName)
                .buildAsyncClient();
        // PE creates the container on demand; without this a fresh container name fails at runtime
        // inside the processor's background loop rather than here where it can be reported.
        containerClient.createIfNotExists().block();
        return new BlobCheckpointStore(containerClient);
    }

    @Override
    public void stop() {
        EventProcessorClient c = this.client;
        if (c != null) {
            c.stop();
        }
    }

}
