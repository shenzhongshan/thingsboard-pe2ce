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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Poll-loop AWS Kinesis consumer for an AWS_KINESIS integration. Configuration JSON
 * (clientConfiguration): {streamName, region, accessKeyId, secretAccessKey,
 * initialPositionInStream? ('LATEST'|'TRIM_HORIZON', default LATEST), maxRecords?}.
 * CE simplification vs PE: PE uses the full Kinesis Client Library (KCL) with a DynamoDB lease
 * table and CloudWatch metrics for coordinated multi-worker checkpointing. That requires
 * provisioning AWS infra beyond just stream credentials, so this port instead does plain
 * GetShardIterator/GetRecords polling per shard with an in-memory (non-persisted) shard-iterator
 * checkpoint — correct for a single-node deployment, but re-reads from the configured start
 * position after a restart rather than resuming exactly where it left off.
 */
@Slf4j
public class AwsKinesisIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile KinesisClient client;
    private final List<Thread> shardThreads = new ArrayList<>();
    private volatile boolean stopped;

    public AwsKinesisIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = KafkaIntegrationConsumer.clientConfig(integration.getConfiguration());
        String streamName = KafkaIntegrationConsumer.required(cfg, "streamName");
        String region = KafkaIntegrationConsumer.required(cfg, "region");
        ShardIteratorType iteratorType = cfg.has("initialPositionInStream")
                && "TRIM_HORIZON".equalsIgnoreCase(cfg.get("initialPositionInStream").asText())
                ? ShardIteratorType.TRIM_HORIZON : ShardIteratorType.LATEST;
        int maxRecords = cfg.has("maxRecords") ? cfg.get("maxRecords").asInt() : 100;

        var builder = KinesisClient.builder().region(Region.of(region));
        if (cfg.has("accessKeyId") && StringUtils.isNotEmpty(cfg.get("accessKeyId").asText())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    cfg.get("accessKeyId").asText(), cfg.path("secretAccessKey").asText(""))));
        }
        KinesisClient kinesisClient = builder.build();
        List<Shard> shards = kinesisClient.listShards(b -> b.streamName(streamName)).shards();
        if (shards.isEmpty()) {
            kinesisClient.close();
            throw new IllegalStateException("Kinesis stream [" + streamName + "] has no shards!");
        }
        this.client = kinesisClient;
        this.stopped = false;
        for (Shard shard : shards) {
            Thread t = new Thread(() -> pollShard(streamName, shard.shardId(), iteratorType, maxRecords),
                    "kinesis-integration-" + integration.getId().getId() + "-" + shard.shardId());
            t.setDaemon(true);
            shardThreads.add(t);
            t.start();
        }
        log.info("[{}][{}] AWS Kinesis integration polling stream [{}], {} shard(s)",
                tenantId, integration.getName(), streamName, shards.size());
    }

    private void pollShard(String streamName, String shardId, ShardIteratorType iteratorType, int maxRecords) {
        String shardIterator;
        try {
            shardIterator = client.getShardIterator(GetShardIteratorRequest.builder()
                    .streamName(streamName).shardId(shardId).shardIteratorType(iteratorType).build()).shardIterator();
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to get Kinesis shard iterator for [{}]: {}", tenantId, integration.getName(), shardId, e.getMessage());
            return;
        }
        while (!stopped && shardIterator != null) {
            try {
                GetRecordsResponse response = client.getRecords(GetRecordsRequest.builder()
                        .shardIterator(shardIterator).limit(maxRecords).build());
                for (Record record : response.records()) {
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("shardId", shardId);
                    metadata.put("sequenceNumber", record.sequenceNumber());
                    if (record.partitionKey() != null) {
                        metadata.put("partitionKey", record.partitionKey());
                    }
                    try {
                        uplinkSink.processUplink(integration, record.data().asByteArray(), metadata);
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to process Kinesis uplink: {}", tenantId, integration.getName(), e.getMessage());
                    }
                }
                shardIterator = response.nextShardIterator();
                if (response.records().isEmpty()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("[{}][{}] Kinesis poll failed for shard [{}]: {}", tenantId, integration.getName(), shardId, e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        stopped = true;
        for (Thread t : shardThreads) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        shardThreads.clear();
        KinesisClient c = this.client;
        if (c != null) {
            c.close();
        }
    }

}
