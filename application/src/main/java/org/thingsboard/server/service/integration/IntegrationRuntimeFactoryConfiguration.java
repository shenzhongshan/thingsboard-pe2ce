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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Registers the protocol runtime factories as beans for {@link DefaultPlatformIntegrationService}.
 * <p>
 * The factory classes themselves live in common/integration, which deliberately carries no Spring
 * stereotype annotations and no dependency on common/queue: a remote integration process hosts the
 * very same classes but has no {@code service.type} property, so a {@code @TbCoreComponent} baked
 * into the class would exclude them there. Registration therefore belongs to each host — this class
 * is the platform's half, and it keeps the original tb-core-only condition intact.
 */
@Configuration
@TbCoreComponent
public class IntegrationRuntimeFactoryConfiguration {

    @Bean
    public MqttIntegrationClientFactory mqttIntegrationClientFactory() {
        return new MqttIntegrationClientFactory();
    }

    @Bean
    public OpcUaIntegrationRuntimeFactory opcUaIntegrationRuntimeFactory() {
        return new OpcUaIntegrationRuntimeFactory();
    }

    @Bean
    public TcpIntegrationRuntimeFactory tcpIntegrationRuntimeFactory() {
        return new TcpIntegrationRuntimeFactory();
    }

    @Bean
    public UdpIntegrationRuntimeFactory udpIntegrationRuntimeFactory() {
        return new UdpIntegrationRuntimeFactory();
    }

    @Bean
    public CoapIntegrationRuntimeFactory coapIntegrationRuntimeFactory(
            @Autowired(required = false) CoapServerService coapServerService) {
        // Optional on purpose: TbCoapServerComponent only publishes this bean in monolith and
        // tb-transport processes. When absent, CoAP integrations fall back to legacy port mode.
        return new CoapIntegrationRuntimeFactory(coapServerService);
    }

    @Bean
    public KafkaIntegrationRuntimeFactory kafkaIntegrationRuntimeFactory() {
        return new KafkaIntegrationRuntimeFactory();
    }

    @Bean
    public RabbitMqIntegrationRuntimeFactory rabbitMqIntegrationRuntimeFactory() {
        return new RabbitMqIntegrationRuntimeFactory();
    }

    @Bean
    public PulsarIntegrationRuntimeFactory pulsarIntegrationRuntimeFactory() {
        return new PulsarIntegrationRuntimeFactory();
    }

    @Bean
    public TuyaIntegrationRuntimeFactory tuyaIntegrationRuntimeFactory() {
        return new TuyaIntegrationRuntimeFactory();
    }

    @Bean
    public AwsSqsIntegrationRuntimeFactory awsSqsIntegrationRuntimeFactory() {
        return new AwsSqsIntegrationRuntimeFactory();
    }

    @Bean
    public AwsKinesisIntegrationRuntimeFactory awsKinesisIntegrationRuntimeFactory() {
        return new AwsKinesisIntegrationRuntimeFactory();
    }

    @Bean
    public AzureEventHubIntegrationRuntimeFactory azureEventHubIntegrationRuntimeFactory() {
        return new AzureEventHubIntegrationRuntimeFactory();
    }

    @Bean
    public AzureServiceBusIntegrationRuntimeFactory azureServiceBusIntegrationRuntimeFactory() {
        return new AzureServiceBusIntegrationRuntimeFactory();
    }

    @Bean
    public PubSubIntegrationRuntimeFactory pubSubIntegrationRuntimeFactory() {
        return new PubSubIntegrationRuntimeFactory();
    }

    @Bean
    public CustomIntegrationRuntimeFactory customIntegrationRuntimeFactory(
            @Value("${integration.custom.allowed-classes:}") String allowedClasses) {
        return new CustomIntegrationRuntimeFactory(allowedClasses);
    }

}
