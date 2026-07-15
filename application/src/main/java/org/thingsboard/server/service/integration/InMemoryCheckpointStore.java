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

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.models.Checkpoint;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-process, non-persisted CheckpointStore: avoids pulling in the
 * azure-messaging-eventhubs-checkpointstore-blob + azure-storage-blob dependencies for a build
 * that only needs a single EventProcessorClient instance per integration (no coordinated
 * multi-worker partition ownership). Checkpoints/ownership are lost on restart, so a restarted
 * integration re-reads from the consumer group's default starting position.
 */
class InMemoryCheckpointStore implements CheckpointStore {

    private final ConcurrentHashMap<String, PartitionOwnership> ownerships = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Checkpoint> checkpoints = new ConcurrentHashMap<>();

    private static String key(String namespace, String eventHubName, String consumerGroup, String partitionId) {
        return namespace + "/" + eventHubName + "/" + consumerGroup + "/" + partitionId;
    }

    @Override
    public Flux<PartitionOwnership> listOwnership(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
        return Flux.fromStream(ownerships.values().stream()
                .filter(o -> o.getFullyQualifiedNamespace().equals(fullyQualifiedNamespace)
                        && o.getEventHubName().equals(eventHubName)
                        && o.getConsumerGroup().equals(consumerGroup)));
    }

    @Override
    public Flux<PartitionOwnership> claimOwnership(java.util.List<PartitionOwnership> requestedPartitionOwnerships) {
        return Flux.fromIterable(requestedPartitionOwnerships).map(requested -> {
            String k = key(requested.getFullyQualifiedNamespace(), requested.getEventHubName(),
                    requested.getConsumerGroup(), requested.getPartitionId());
            requested.setLastModifiedTime(System.currentTimeMillis());
            requested.setETag(java.util.UUID.randomUUID().toString());
            ownerships.put(k, requested);
            return requested;
        });
    }

    @Override
    public Flux<Checkpoint> listCheckpoints(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
        return Flux.fromStream(checkpoints.values().stream()
                .filter(c -> c.getFullyQualifiedNamespace().equals(fullyQualifiedNamespace)
                        && c.getEventHubName().equals(eventHubName)
                        && c.getConsumerGroup().equals(consumerGroup)));
    }

    @Override
    public Mono<Void> updateCheckpoint(Checkpoint checkpoint) {
        checkpoints.put(key(checkpoint.getFullyQualifiedNamespace(), checkpoint.getEventHubName(),
                checkpoint.getConsumerGroup(), checkpoint.getPartitionId()), checkpoint);
        return Mono.empty();
    }

}
