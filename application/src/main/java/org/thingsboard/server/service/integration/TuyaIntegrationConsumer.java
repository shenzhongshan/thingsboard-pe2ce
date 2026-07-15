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
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tuya cloud consumer for a TUYA integration. Configuration JSON (clientConfiguration):
 * {region: CN|US|EU|IN, accessId, accessKey, env: PROD|TEST}. Ports PE's MqConsumer/MqAuthentication/
 * TuyaMessageUtil: Tuya doesn't use standard Pulsar auth/encryption — it wraps an Apache Pulsar
 * client with a custom auth1 handshake and AES-encrypted message envelopes.
 * CE simplification: downlink (Tuya Cloud REST device-command API, HMAC-SHA256 request signing)
 * is not implemented in this build — uplink-only.
 */
@Slf4j
public class TuyaIntegrationConsumer implements IntegrationRuntime {

    private static final Map<String, String> REGION_MESSAGING_URLS = Map.of(
            "CN", "pulsar+ssl://mqe.tuyacn.com:7285/",
            "US", "pulsar+ssl://mqe.tuyaus.com:7285/",
            "EU", "pulsar+ssl://mqe.tuyaeu.com:7285/",
            "IN", "pulsar+ssl://mqe.tuyain.com:7285/"
    );

    private final Integration integration;
    private final PlatformIntegrationService platformIntegrationService;
    @Getter
    private final TenantId tenantId;
    private String accessKey;

    private volatile PulsarClient client;
    private volatile Consumer<byte[]> consumer;

    public TuyaIntegrationConsumer(Integration integration, PlatformIntegrationService platformIntegrationService) {
        this.integration = integration;
        this.platformIntegrationService = platformIntegrationService;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = KafkaIntegrationConsumer.clientConfig(integration.getConfiguration());
        String region = KafkaIntegrationConsumer.required(cfg, "region").toUpperCase();
        String accessId = KafkaIntegrationConsumer.required(cfg, "accessId");
        this.accessKey = KafkaIntegrationConsumer.required(cfg, "accessKey");
        String env = cfg.has("env") && "TEST".equalsIgnoreCase(cfg.get("env").asText()) ? "event-test" : "event";
        String serviceUrl = REGION_MESSAGING_URLS.get(region);
        if (serviceUrl == null) {
            throw new IllegalArgumentException("Unknown Tuya region: " + region);
        }
        String topic = accessId + "/out/" + env;

        PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .allowTlsInsecureConnection(true)
                .authentication(new TuyaAuthentication(accessId, accessKey))
                .build();
        Consumer<byte[]> pulsarConsumer = pulsarClient.newConsumer()
                .topic(topic)
                .subscriptionName(accessId + "-sub")
                .subscriptionType(SubscriptionType.Failover)
                .subscriptionTopicsMode(RegexSubscriptionMode.AllTopics)
                .autoUpdatePartitions(false)
                .messageListener((c, msg) -> {
                    try {
                        onMessage(msg);
                        c.acknowledge(msg);
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to process Tuya uplink: {}", tenantId, integration.getName(), e.getMessage());
                    }
                })
                .subscribe();
        this.client = pulsarClient;
        this.consumer = pulsarConsumer;
        log.info("[{}][{}] Tuya integration subscribed to [{}]", tenantId, integration.getName(), topic);
    }

    private void onMessage(Message<byte[]> msg) throws Exception {
        JsonNode vo = JacksonUtil.fromBytes(msg.getData());
        String encodedData = vo.path("data").asText(null);
        if (encodedData == null) {
            return;
        }
        String em = msg.getProperties().getOrDefault("em", "aes_ecb");
        String key16 = accessKey.substring(8, 24);
        String decrypted = "aes_gcm".equalsIgnoreCase(em) ? decryptGcm(encodedData, key16) : decryptEcb(encodedData, key16);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("topic", msg.getTopicName());
        platformIntegrationService.processUplink(integration, decrypted.getBytes(StandardCharsets.UTF_8), metadata);
    }

    private static String decryptEcb(String base64, String key16) throws Exception {
        byte[] raw = Base64.getDecoder().decode(base64);
        Key key = new SecretKeySpec(key16.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(raw), StandardCharsets.UTF_8);
    }

    private static String decryptGcm(String base64, String key16) throws Exception {
        byte[] raw = Base64.getDecoder().decode(base64);
        int nonceLen = 12;
        byte[] nonce = Arrays.copyOfRange(raw, 0, nonceLen);
        byte[] ctTag = Arrays.copyOfRange(raw, nonceLen, raw.length);
        Key key = new SecretKeySpec(key16.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        return new String(cipher.doFinal(ctTag), StandardCharsets.UTF_8);
    }

    @Override
    public void stop() {
        try {
            if (consumer != null) {
                consumer.close();
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to close Tuya consumer", integration.getId(), e);
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to close Tuya Pulsar client", integration.getId(), e);
        }
    }

    /** Port of PE's MqAuthentication/MqAuthenticationDataProvider: Tuya's custom "auth1" handshake. */
    private static class TuyaAuthentication implements Authentication {
        private final String accessId;
        private final String accessKey;

        TuyaAuthentication(String accessId, String accessKey) {
            this.accessId = accessId;
            this.accessKey = accessKey;
        }

        @Override
        public String getAuthMethodName() {
            return "auth1";
        }

        @Override
        public AuthenticationDataProvider getAuthData() {
            String password = md5Hex(accessId + md5Hex(accessKey)).substring(8, 24);
            String commandData = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", accessId, password);
            return new AuthenticationDataProvider() {
                @Override
                public boolean hasDataForHttp() {
                    return false;
                }

                @Override
                public boolean hasDataFromCommand() {
                    return true;
                }

                @Override
                public String getCommandData() {
                    return commandData;
                }
            };
        }

        @Override
        public void configure(Map<String, String> authParams) {
        }

        @Override
        public void start() {
        }

        @Override
        public void close() throws IOException {
        }

        private static String md5Hex(String value) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                return HexFormat.of().formatHex(md5.digest(value.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
