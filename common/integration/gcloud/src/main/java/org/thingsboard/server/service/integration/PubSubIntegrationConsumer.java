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
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GCP Pub/Sub subscriber for a PUB_SUB integration. Configuration JSON (clientConfiguration):
 * {projectId, subscriptionId, serviceAccountKey} (serviceAccountKey is the raw service-account
 * JSON key content, not a file path).
 */
@Slf4j
public class PubSubIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile Subscriber subscriber;

    public PubSubIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = IntegrationConfigUtil.clientConfig(integration.getConfiguration());
        String projectId = IntegrationConfigUtil.required(cfg, "projectId");
        String subscriptionId = IntegrationConfigUtil.required(cfg, "subscriptionId");
        String serviceAccountKey = IntegrationConfigUtil.required(cfg, "serviceAccountKey");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8)));
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        MessageReceiver receiver = (message, consumer) -> {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("messageId", message.getMessageId());
            message.getAttributesMap().forEach((k, v) -> metadata.put("attr:" + k, v));
            try {
                uplinkSink.processUplink(integration, message.getData().toByteArray(), metadata);
                consumer.ack();
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to process Pub/Sub uplink: {}", tenantId, integration.getName(), e.getMessage());
                consumer.nack();
            }
        };
        Subscriber sub = Subscriber.newBuilder(subscriptionName, receiver)
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        sub.addListener(new Subscriber.Listener() {
            @Override
            public void failed(Subscriber.State from, Throwable failure) {
                log.warn("[{}][{}] Pub/Sub subscriber failed: {}", tenantId, integration.getName(), failure.getMessage());
            }
        }, Runnable::run);
        sub.startAsync().awaitRunning(15, TimeUnit.SECONDS);
        this.subscriber = sub;
        log.info("[{}][{}] Pub/Sub integration subscribed to {}", tenantId, integration.getName(), subscriptionName);
    }

    @Override
    public void stop() {
        Subscriber sub = this.subscriber;
        if (sub != null) {
            sub.stopAsync();
        }
    }

}
