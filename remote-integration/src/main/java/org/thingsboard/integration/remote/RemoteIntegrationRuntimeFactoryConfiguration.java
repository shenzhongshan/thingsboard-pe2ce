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
package org.thingsboard.integration.remote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.service.integration.AwsKinesisIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.AwsSqsIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.AzureEventHubIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.AzureServiceBusIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.CoapIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.CustomIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.KafkaIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.MqttIntegrationClientFactory;
import org.thingsboard.server.service.integration.OpcUaIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.PubSubIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.PulsarIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.RabbitMqIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.TcpIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.TuyaIntegrationRuntimeFactory;
import org.thingsboard.server.service.integration.UdpIntegrationRuntimeFactory;

/**
 * This process's half of the runtime-factory registration.
 * <p>
 * The factory classes live in common/integration and carry no Spring annotations on purpose: the
 * platform gates the very same classes behind {@code @TbCoreComponent}, a condition on
 * {@code service.type} that has no meaning out here. So each host declares them itself - see
 * {@code IntegrationRuntimeFactoryConfiguration} in the application module for the platform's
 * version. Keep the two lists in sync when a protocol is added.
 */
@Configuration
public class RemoteIntegrationRuntimeFactoryConfiguration {

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
        // Optional on purpose: this process runs no CoAP server, so the bean is absent and the
        // runtime falls back to legacy port mode. Mirrors IntegrationRuntimeFactoryConfiguration.
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
