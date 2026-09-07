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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Poll-loop AWS SQS consumer. Configuration JSON key is "sqsConfiguration" (not
 * "clientConfiguration", matching PE): {queueUrl, region, accessKeyId, secretAccessKey,
 * pollingPeriodSeconds?}.
 */
@Slf4j
public class AwsSqsIntegrationConsumer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private volatile AmazonSQS sqs;
    private volatile Thread pollThread;
    private volatile boolean stopped;

    public AwsSqsIntegrationConsumer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() {
        JsonNode cfg = integration.getConfiguration();
        JsonNode sqsCfg = cfg != null && cfg.has("sqsConfiguration") ? cfg.get("sqsConfiguration") : cfg;
        String queueUrl = IntegrationConfigUtil.required(sqsCfg, "queueUrl");
        String region = IntegrationConfigUtil.required(sqsCfg, "region");
        long pollingPeriodSec = sqsCfg.has("pollingPeriodSeconds") ? sqsCfg.get("pollingPeriodSeconds").asLong() : 5L;

        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard().withRegion(region);
        if (sqsCfg.has("accessKeyId") && StringUtils.isNotEmpty(sqsCfg.get("accessKeyId").asText())) {
            String accessKeyId = sqsCfg.get("accessKeyId").asText();
            String secretAccessKey = sqsCfg.path("secretAccessKey").asText("");
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)));
        }
        AmazonSQS client = builder.build();
        // fail fast if the queue/credentials are wrong, rather than only surfacing errors from the poll loop.
        client.getQueueAttributes(queueUrl, java.util.List.of("QueueArn"));

        this.sqs = client;
        this.stopped = false;
        this.pollThread = new Thread(() -> pollLoop(queueUrl, pollingPeriodSec), "aws-sqs-integration-" + IntegrationConfigUtil.safeId(integration));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
        log.info("[{}][{}] AWS SQS integration polling queue [{}]", tenantId, integration.getName(), queueUrl);
    }

    private void pollLoop(String queueUrl, long pollingPeriodSec) {
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl)
                .withMaxNumberOfMessages(10)
                .withWaitTimeSeconds((int) Math.min(20, Math.max(1, pollingPeriodSec)));
        while (!stopped) {
            try {
                ReceiveMessageResult result = sqs.receiveMessage(request);
                for (Message message : result.getMessages()) {
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("messageId", message.getMessageId());
                    try {
                        uplinkSink.processUplink(integration,
                                message.getBody().getBytes(StandardCharsets.UTF_8), metadata);
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to process SQS uplink: {}", tenantId, integration.getName(), e.getMessage());
                    } finally {
                        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, message.getReceiptHandle()));
                    }
                }
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("[{}][{}] AWS SQS poll failed: {}", tenantId, integration.getName(), e.getMessage());
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
        AmazonSQS client = this.sqs;
        if (client != null) {
            client.shutdown();
        }
    }

}
