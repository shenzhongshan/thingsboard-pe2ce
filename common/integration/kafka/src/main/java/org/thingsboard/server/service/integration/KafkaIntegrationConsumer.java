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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Poll-loop Kafka consumer for a KAFKA integration. Configuration JSON (clientConfiguration):
 * {clientId, groupId, topics, bootstrapServers, autoCreateTopics?, pollInterval?, otherProperties?}.
 * `otherProperties` is a flat string map merged directly into the Kafka client Properties, letting
 * an operator configure SASL/SSL (security.protocol, sasl.jaas.config, ...) without a dedicated
 * UI field per auth mechanism, matching the live PE wizard's "other properties" section.
 */
@Slf4j
public class KafkaIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile KafkaConsumer<String, byte[]> consumer;
    private volatile Thread pollThread;
    private volatile boolean stopped;

    public KafkaIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = IntegrationConfigUtil.clientConfig(integration.getConfiguration());
        String topic = IntegrationConfigUtil.required(cfg, "topics");
        String bootstrapServers = IntegrationConfigUtil.required(cfg, "bootstrapServers");
        long pollInterval = cfg.has("pollInterval") ? cfg.get("pollInterval").asLong() : 5000L;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cfg.has("groupId") ? cfg.get("groupId").asText() : "tb-integration-" + integration.getId());
        if (cfg.has("clientId") && StringUtils.isNotEmpty(cfg.get("clientId").asText())) {
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, cfg.get("clientId").asText());
        }
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put("allow.auto.create.topics", cfg.has("autoCreateTopics") ? cfg.get("autoCreateTopics").asText() : "false");
        JsonNode otherProps = cfg.get("otherProperties");
        if (otherProps != null && otherProps.isObject()) {
            otherProps.fields().forEachRemaining(e -> props.put(e.getKey(), e.getValue().asText()));
        }

        KafkaConsumer<String, byte[]> kafkaConsumer = new KafkaConsumer<>(props);
        // fail fast if the cluster is unreachable / topic metadata can't be fetched, rather than
        // silently retrying forever inside the poll loop.
        kafkaConsumer.partitionsFor(topic, Duration.ofSeconds(10));
        kafkaConsumer.subscribe(List.of(topic));
        this.consumer = kafkaConsumer;
        this.stopped = false;
        this.pollThread = new Thread(() -> pollLoop(pollInterval), "kafka-integration-" + integration.getId().getId());
        this.pollThread.setDaemon(true);
        this.pollThread.start();
        log.info("[{}][{}] Kafka integration subscribed to topic [{}]", tenantId, integration.getName(), topic);
    }

    private void pollLoop(long pollIntervalMs) {
        try {
            while (!stopped) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(pollIntervalMs));
                Iterator<ConsumerRecord<String, byte[]>> it = records.iterator();
                while (it.hasNext()) {
                    ConsumerRecord<String, byte[]> record = it.next();
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("topic", record.topic());
                    metadata.put("partition", String.valueOf(record.partition()));
                    metadata.put("offset", String.valueOf(record.offset()));
                    if (record.key() != null) {
                        metadata.put("key", record.key());
                    }
                    try {
                        uplinkSink.processUplink(integration, record.value(), metadata);
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to process Kafka uplink: {}", tenantId, integration.getName(), e.getMessage());
                    }
                }
            }
        } catch (WakeupException e) {
            // expected on stop()
        } catch (Exception e) {
            log.warn("[{}][{}] Kafka integration poll loop terminated: {}", tenantId, integration.getName(), e.getMessage());
        } finally {
            consumer.close();
        }
    }

    @Override
    public void stop() {
        stopped = true;
        KafkaConsumer<String, byte[]> c = this.consumer;
        if (c != null) {
            c.wakeup();
        }
        Thread t = this.pollThread;
        if (t != null) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


}
