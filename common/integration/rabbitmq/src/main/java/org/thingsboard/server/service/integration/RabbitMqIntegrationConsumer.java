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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ consumer for a RABBITMQ integration. Configuration JSON (clientConfiguration):
 * {exchangeName?, host, port?, virtualHost?, username?, password?, downlinkTopic?, queues,
 * connectionTimeout?, handshakeTimeout?, durable?, exclusive?, autoDelete?}. `queues` is a
 * comma-separated list; each is declared (idempotent) and bound to exchangeName using
 * downlinkTopic as the routing key when an exchange is configured.
 */
@Slf4j
public class RabbitMqIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile Connection connection;
    private volatile Channel channel;

    public RabbitMqIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = IntegrationConfigUtil.clientConfig(integration.getConfiguration());
        String host = IntegrationConfigUtil.required(cfg, "host");
        String queuesCsv = IntegrationConfigUtil.required(cfg, "queues");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(cfg.has("port") ? cfg.get("port").asInt() : 5672);
        if (cfg.has("virtualHost") && StringUtils.isNotEmpty(cfg.get("virtualHost").asText())) {
            factory.setVirtualHost(cfg.get("virtualHost").asText());
        }
        if (cfg.has("username")) {
            factory.setUsername(cfg.get("username").asText());
        }
        if (cfg.has("password")) {
            factory.setPassword(cfg.get("password").asText());
        }
        factory.setConnectionTimeout(cfg.has("connectionTimeout") ? cfg.get("connectionTimeout").asInt() : 30000);
        factory.setHandshakeTimeout(cfg.has("handshakeTimeout") ? cfg.get("handshakeTimeout").asInt() : 10000);

        boolean durable = !cfg.has("durable") || cfg.get("durable").asBoolean(true);
        boolean exclusive = cfg.has("exclusive") && cfg.get("exclusive").asBoolean(false);
        boolean autoDelete = cfg.has("autoDelete") && cfg.get("autoDelete").asBoolean(false);
        String exchangeName = cfg.has("exchangeName") ? cfg.get("exchangeName").asText(null) : null;
        String routingKey = cfg.has("downlinkTopic") ? cfg.get("downlinkTopic").asText("#") : "#";

        Connection conn = factory.newConnection("tb-integration-" + IntegrationConfigUtil.safeId(integration));
        Channel ch = conn.createChannel();
        DeliverCallback callback = (consumerTag, delivery) -> {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("routingKey", delivery.getEnvelope().getRoutingKey());
            metadata.put("exchange", delivery.getEnvelope().getExchange());
            try {
                uplinkSink.processUplink(integration, delivery.getBody(), metadata);
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to process RabbitMQ uplink: {}", tenantId, integration.getName(), e.getMessage());
            }
        };
        for (String queue : queuesCsv.split(",")) {
            String q = queue.trim();
            if (q.isEmpty()) {
                continue;
            }
            ch.queueDeclare(q, durable, exclusive, autoDelete, null);
            if (StringUtils.isNotEmpty(exchangeName)) {
                ch.queueBind(q, exchangeName, routingKey);
            }
            ch.basicConsume(q, true, callback, tag -> {
            });
        }
        this.connection = conn;
        this.channel = ch;
        log.info("[{}][{}] RabbitMQ integration connected to {}:{}, queues [{}]",
                tenantId, integration.getName(), host, factory.getPort(), queuesCsv);
    }

    @Override
    public void stop() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to close RabbitMQ channel", integration.getId(), e);
        }
        try {
            if (connection != null && connection.isOpen()) {
                connection.close((int) TimeUnit.SECONDS.toMillis(5));
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to close RabbitMQ connection", integration.getId(), e);
        }
    }

}
