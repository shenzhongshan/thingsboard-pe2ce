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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.thingsboard.server.common.data.converter.Converter;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultPlatformIntegrationService extends AbstractPartitionBasedService<TenantId> implements PlatformIntegrationService {

    private final IntegrationService integrationService;
    private final ConverterService converterService;
    private final ConverterExecutorService converterExecutorService;
    private final DeviceService deviceService;
    private final TbClusterService clusterService;
    private final MqttIntegrationClientFactory mqttClientFactory;

    // integrationId -> runtime status ("ACTIVE"/"FAILED"/"STOPPED", error)
    private final ConcurrentMap<IntegrationId, IntegrationStatus> statuses = new ConcurrentHashMap<>();
    // integrationId -> running MQTT client (HTTP integrations have no runtime state)
    private final ConcurrentMap<IntegrationId, MqttIntegrationClient> mqttClients = new ConcurrentHashMap<>();
    // integrationId -> running TCP server
    private final ConcurrentMap<IntegrationId, TcpIntegrationServer> tcpServers = new ConcurrentHashMap<>();

    record IntegrationStatus(String status, String error) {}

    @PostConstruct
    public void init() {
        super.init();
    }

    @PreDestroy
    public void stop() {
        mqttClients.values().forEach(MqttIntegrationClient::stop);
        mqttClients.clear();
        tcpServers.values().forEach(TcpIntegrationServer::stop);
        tcpServers.clear();
        super.stop();
    }

    @Override
    protected String getServiceName() {
        return "Integrations";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "integration-service";
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        log.info("Integration service initializing");
        for (Integration integration : integrationService.findAllEnabledIntegrations()) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, integration.getTenantId(), integration.getTenantId());
            if (!addedPartitions.contains(tpi)) {
                continue;
            }
            partitionedEntities.computeIfAbsent(tpi, key -> ConcurrentHashMap.newKeySet()).add(integration.getTenantId());
            scheduledExecutor.submit(() -> startIntegration(integration));
        }
        log.info("Integration service initialized.");
        return Collections.emptyMap();
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(TenantId tenantId) {
        // Stop runtimes of tenants we no longer own.
        mqttClients.entrySet().removeIf(e -> {
            if (tenantId.equals(e.getValue().getTenantId())) {
                e.getValue().stop();
                statuses.put(e.getKey(), new IntegrationStatus("STOPPED", null));
                return true;
            }
            return false;
        });
        tcpServers.entrySet().removeIf(e -> {
            if (tenantId.equals(e.getValue().getTenantId())) {
                e.getValue().stop();
                statuses.put(e.getKey(), new IntegrationStatus("STOPPED", null));
                return true;
            }
            return false;
        });
    }

    @Override
    public void onIntegrationUpdated(Integration integration) {
        scheduledExecutor.submit(() -> {
            stopIntegration(integration.getId());
            if (integration.isEnabled()) {
                startIntegration(integration);
            } else {
                statuses.put(integration.getId(), new IntegrationStatus("STOPPED", null));
            }
        });
    }

    @Override
    public void onIntegrationDeleted(Integration integration) {
        scheduledExecutor.submit(() -> {
            stopIntegration(integration.getId());
            statuses.remove(integration.getId());
        });
    }

    @Override
    public JsonNode getIntegrationStatus(IntegrationId integrationId) {
        IntegrationStatus status = statuses.getOrDefault(integrationId, new IntegrationStatus("STOPPED", null));
        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("status", status.status());
        if (status.error() != null) {
            result.put("error", status.error());
        }
        return result;
    }

    private void startIntegration(Integration integration) {
        try {
            if (integration.getType() == IntegrationType.MQTT) {
                MqttIntegrationClient client = mqttClientFactory.create(integration, this);
                client.start();
                mqttClients.put(integration.getId(), client);
            } else if (integration.getType() == IntegrationType.TCP) {
                TcpIntegrationServer server = new TcpIntegrationServer(integration, this);
                server.start();
                tcpServers.put(integration.getId(), server);
            }
            // HTTP integrations are passive: the inbound endpoint resolves them by routing key.
            statuses.put(integration.getId(), new IntegrationStatus("ACTIVE", null));
            log.info("[{}][{}] Integration started", integration.getTenantId(), integration.getName());
        } catch (Exception e) {
            statuses.put(integration.getId(), new IntegrationStatus("FAILED", e.getMessage()));
            log.warn("[{}][{}] Failed to start integration: {}", integration.getTenantId(), integration.getName(), e.getMessage());
        }
    }

    private void stopIntegration(IntegrationId integrationId) {
        MqttIntegrationClient client = mqttClients.remove(integrationId);
        if (client != null) {
            client.stop();
        }
        TcpIntegrationServer server = tcpServers.remove(integrationId);
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void processUplink(Integration integration, byte[] payload, Map<String, String> metadata) throws Exception {
        Converter converter = converterService.findConverterById(integration.getTenantId(), integration.getDefaultConverterId());
        if (converter == null || converter.getConfiguration() == null) {
            throw new IllegalStateException("Uplink converter is not configured!");
        }
        JsonNode cfg = converter.getConfiguration();
        ScriptLanguage lang = cfg.has("scriptLang") ? ScriptLanguage.valueOf(cfg.get("scriptLang").asText()) : ScriptLanguage.TBEL;
        String decoder = cfg.get("decoder").asText();
        Map<String, String> md = new HashMap<>(metadata);
        md.put("integrationName", integration.getName());
        JsonNode result = converterExecutorService.executeUplink(integration.getTenantId(), lang, decoder, payload, md);
        if (result == null || result.isNull()) {
            return;
        }
        if (result.isArray()) {
            for (JsonNode item : result) {
                processUplinkData(integration, item, md);
            }
        } else {
            processUplinkData(integration, result, md);
        }
    }

    private void processUplinkData(Integration integration, JsonNode data, Map<String, String> metadata) {
        if (data == null || !data.has("deviceName")) {
            log.debug("[{}] Uplink result has no deviceName, skipping: {}", integration.getId(), data);
            return;
        }
        TenantId tenantId = integration.getTenantId();
        String deviceName = data.get("deviceName").asText();
        String deviceType = data.has("deviceType") ? data.get("deviceType").asText() : "default";
        Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
        if (device == null) {
            if (!integration.isAllowCreateDevicesOrAssets()) {
                log.debug("[{}] Device [{}] not found and creation is not allowed", integration.getId(), deviceName);
                return;
            }
            device = new Device();
            device.setTenantId(tenantId);
            device.setName(deviceName);
            device.setType(deviceType);
            device = deviceService.saveDevice(device);
        }
        TbMsgMetaData msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("integrationName", integration.getName());
        msgMetaData.putValue("deviceName", deviceName);
        msgMetaData.putValue("deviceType", deviceType);
        metadata.forEach(msgMetaData::putValue);
        if (data.has("telemetry") && data.get("telemetry").isObject() && !data.get("telemetry").isEmpty()) {
            pushMsg(tenantId, device, TbMsgType.POST_TELEMETRY_REQUEST, data.get("telemetry"), msgMetaData);
        }
        if (data.has("attributes") && data.get("attributes").isObject() && !data.get("attributes").isEmpty()) {
            pushMsg(tenantId, device, TbMsgType.POST_ATTRIBUTES_REQUEST, data.get("attributes"), msgMetaData);
        }
    }

    private void pushMsg(TenantId tenantId, Device device, TbMsgType msgType, JsonNode body, TbMsgMetaData metaData) {
        TbMsg tbMsg = TbMsg.newMsg()
                .type(msgType)
                .originator(device.getId())
                .metaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(body))
                .build();
        clusterService.pushMsgToRuleEngine(tenantId, device.getId(), tbMsg, null);
    }

}
