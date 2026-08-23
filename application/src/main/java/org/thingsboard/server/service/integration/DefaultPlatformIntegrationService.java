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
import org.thingsboard.server.dao.secret.SecretConfigurationService;
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
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.integration.stats.IntegrationStatisticsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.rpc.TbRemoteIntegrationRpcService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import java.time.LocalDate;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.thingsboard.server.common.data.converter.Converter;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultPlatformIntegrationService extends AbstractPartitionBasedService<TenantId> implements PlatformIntegrationService {

    // Integration types with no in-process runtime: the inbound endpoint resolves them by
    // routing key and calls processUplink() directly (see IntegrationHttpController).
    private static final Set<IntegrationType> PASSIVE_TYPES = Set.of(
            IntegrationType.HTTP, IntegrationType.SIGFOX, IntegrationType.CHIRPSTACK,
            IntegrationType.PARTICLE, IntegrationType.KPN, IntegrationType.LORIOT,
            IntegrationType.OCEANCONNECT, IntegrationType.THINGPARK, IntegrationType.TPE,
            IntegrationType.TMOBILE_IOT_CDP);

    private final IntegrationService integrationService;
    private final SecretConfigurationService secretConfigurationService;
    private final ConverterService converterService;
    private final ConverterExecutorService converterExecutorService;
    private final IntegrationRateLimitService rateLimitService;
    private final IntegrationStatisticsService statisticsService;
    private final DeviceService deviceService;
    private final TbClusterService clusterService;
    private final List<IntegrationRuntimeFactory> runtimeFactories;
    // present only when integrations.rpc.enabled=true
    private final Optional<TbRemoteIntegrationRpcService> remoteIntegrationRpcService;

    private final Map<IntegrationType, IntegrationRuntimeFactory> factoryByType = new ConcurrentHashMap<>();

    // integrationId -> runtime status ("ACTIVE"/"FAILED"/"STOPPED", error)
    private final ConcurrentMap<IntegrationId, IntegrationStatus> statuses = new ConcurrentHashMap<>();
    // integrationId -> running runtime instance (passive types have no entry here)
    private final ConcurrentMap<IntegrationId, IntegrationRuntime> runtimes = new ConcurrentHashMap<>();
    // integrationId -> today's uplink message count (PE's "daily activity" metric)
    private final ConcurrentMap<IntegrationId, DailyCounter> dailyEventCounts = new ConcurrentHashMap<>();
    // converterId+deviceId+key -> last value this converter produced for that key, used to
    // enforce a converter's "updateOnlyKeys" (only persist a key if it changed since the
    // previous converted message for that device).
    private final ConcurrentMap<String, JsonNode> lastConvertedValues = new ConcurrentHashMap<>();

    record IntegrationStatus(String status, String error) {}

    private static final class DailyCounter {
        private volatile LocalDate day = LocalDate.now();
        private final AtomicLong count = new AtomicLong();

        synchronized void increment() {
            LocalDate today = LocalDate.now();
            if (!today.equals(day)) {
                day = today;
                count.set(0);
            }
            count.incrementAndGet();
        }

        synchronized long get() {
            return LocalDate.now().equals(day) ? count.get() : 0;
        }
    }

    @PostConstruct
    public void init() {
        for (IntegrationRuntimeFactory factory : runtimeFactories) {
            for (IntegrationType type : factory.getSupportedTypes()) {
                factoryByType.put(type, factory);
            }
        }
        super.init();
    }

    @PreDestroy
    public void stop() {
        runtimes.values().forEach(IntegrationRuntime::stop);
        runtimes.clear();
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
        runtimes.entrySet().removeIf(e -> {
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
        if (integration.isRemote()) {
            // executed by a separate process: never start it in-process, just push the new config
            // down the gRPC stream so the remote picks it up without restarting
            stopIntegration(integration.getId());
            remoteIntegrationRpcService.ifPresent(rpc -> rpc.onIntegrationUpdated(integration));
            return;
        }
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
            dailyEventCounts.remove(integration.getId());
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
        DailyCounter counter = dailyEventCounts.get(integrationId);
        result.put("eventsCountToday", counter == null ? 0 : counter.get());
        return result;
    }

    private void startIntegration(Integration integration) {
        if (integration.isRemote()) {
            statuses.put(integration.getId(), new IntegrationStatus("REMOTE", null));
            return;
        }
        boolean started = false;
        try {
            started = doStartIntegration(integration);
        } finally {
            statisticsService.onIntegrationStateUpdate(integration.getType(),
                    started ? ComponentLifecycleEvent.STARTED : ComponentLifecycleEvent.FAILED, started);
        }
    }

    /**
     * A copy of the integration with any {@code ${secret:...}} placeholders in its configuration
     * resolved to real values.
     *
     * <p>Always a copy - the caller's instance is the one cached and written back, and it must keep
     * the placeholder rather than the decrypted credential. Resolution happens here, where the
     * config is consumed to open a connection, rather than on save, matching PE
     * (TbCoreIntegrationConfigurationService).
     */
    private Integration withResolvedSecrets(Integration integration) {
        if (integration == null || integration.getConfiguration() == null) {
            return integration;
        }
        Integration copy = new Integration(integration);
        secretConfigurationService.replaceSecretUsages(integration.getTenantId(), copy.getConfiguration());
        return copy;
    }

    private boolean doStartIntegration(Integration integration) {
        try {
            IntegrationType type = integration.getType();
            if (!PASSIVE_TYPES.contains(type)) {
                IntegrationRuntimeFactory factory = factoryByType.get(type);
                if (factory == null) {
                    throw new UnsupportedOperationException("Integration type " + type + " is not implemented in this build!");
                }
                IntegrationRuntime runtime = factory.create(withResolvedSecrets(integration), this);
                runtime.start();
                runtimes.put(integration.getId(), runtime);
            }
            statuses.put(integration.getId(), new IntegrationStatus("ACTIVE", null));
            log.info("[{}][{}] Integration started", integration.getTenantId(), integration.getName());
            return true;
        } catch (Exception e) {
            statuses.put(integration.getId(), new IntegrationStatus("FAILED", e.getMessage()));
            log.warn("[{}][{}] Failed to start integration: {}", integration.getTenantId(), integration.getName(), e.getMessage());
            return false;
        }
    }

    private void stopIntegration(IntegrationId integrationId) {
        IntegrationRuntime runtime = runtimes.remove(integrationId);
        if (runtime != null) {
            runtime.stop();
        }
    }

    @Override
    public void processUplink(Integration integration, byte[] payload, Map<String, String> metadata) throws Exception {
        // checked before the converter runs, so an over-limit tenant does not pay for script execution
        rateLimitService.checkLimit(integration.getTenantId(), () -> new String(payload, StandardCharsets.UTF_8));
        dailyEventCounts.computeIfAbsent(integration.getId(), id -> new DailyCounter()).increment();
        try {
            doProcessUplink(integration, payload, metadata);
            statisticsService.onUplinkMsg(integration.getType(), true);
        } catch (Exception e) {
            statisticsService.onUplinkMsg(integration.getType(), false);
            throw e;
        }
    }

    private void doProcessUplink(Integration integration, byte[] payload, Map<String, String> metadata) throws Exception {
        Converter converter = converterService.findConverterById(integration.getTenantId(), integration.getDefaultConverterId());
        if (converter == null || converter.getConfiguration() == null) {
            throw new IllegalStateException("Uplink converter is not configured!");
        }
        byte[] effectivePayload = payload;
        Map<String, String> effectiveMetadata = metadata;
        IntegrationPayloadUnwrapper unwrapper = IntegrationPayloadUnwrapperFactory.getUnwrapper(integration.getType());
        if (unwrapper != null) {
            IntegrationPayloadUnwrapper.UnwrapResult unwrapped = unwrapper.unwrap(payload, metadata);
            effectivePayload = unwrapped.payload();
            effectiveMetadata = unwrapped.metadata();
        }
        JsonNode cfg = converter.getConfiguration();
        ScriptLanguage lang = cfg.has("scriptLang") ? ScriptLanguage.valueOf(cfg.get("scriptLang").asText()) : ScriptLanguage.TBEL;
        String decoder = cfg.get("decoder").asText();
        Map<String, String> md = new HashMap<>(effectiveMetadata);
        md.put("integrationName", integration.getName());
        JsonNode result = converterExecutorService.executeUplink(integration.getTenantId(), lang, decoder, effectivePayload, md);
        if (result == null || result.isNull()) {
            return;
        }
        if (result.isArray()) {
            for (JsonNode item : result) {
                processUplinkData(integration, converter, item, md);
            }
        } else {
            processUplinkData(integration, converter, result, md);
        }
    }

    private void processUplinkData(Integration integration, Converter converter, JsonNode data, Map<String, String> metadata) {
        if (data == null || !data.has("deviceName")) {
            log.debug("[{}] Uplink result has no deviceName, skipping: {}", integration.getId(), data);
            return;
        }
        TenantId tenantId = integration.getTenantId();
        String deviceName = data.get("deviceName").asText();
        rateLimitService.checkLimitPerDevice(tenantId, deviceName, data::toString);
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
        List<String> updateOnlyKeys = getUpdateOnlyKeys(converter);
        JsonNode telemetry = data.has("telemetry") && data.get("telemetry").isObject() ? data.get("telemetry") : null;
        JsonNode attributes = data.has("attributes") && data.get("attributes").isObject() ? data.get("attributes") : null;
        if (!updateOnlyKeys.isEmpty()) {
            String converterId = converter.getId().getId().toString();
            String deviceId = device.getId().getId().toString();
            telemetry = filterUnchangedKeys(converterId, deviceId, "ts", telemetry, updateOnlyKeys);
            attributes = filterUnchangedKeys(converterId, deviceId, "attr", attributes, updateOnlyKeys);
        }
        if (telemetry != null && !telemetry.isEmpty()) {
            pushMsg(tenantId, device, TbMsgType.POST_TELEMETRY_REQUEST, telemetry, msgMetaData);
        }
        if (attributes != null && !attributes.isEmpty()) {
            pushMsg(tenantId, device, TbMsgType.POST_ATTRIBUTES_REQUEST, attributes, msgMetaData);
        }
    }

    private List<String> getUpdateOnlyKeys(Converter converter) {
        JsonNode cfg = converter.getConfiguration();
        if (cfg != null && cfg.has("updateOnlyKeys") && cfg.get("updateOnlyKeys").isArray() && !cfg.get("updateOnlyKeys").isEmpty()) {
            List<String> keys = new java.util.ArrayList<>();
            cfg.get("updateOnlyKeys").forEach(k -> keys.add(k.asText()));
            return keys;
        }
        return Collections.emptyList();
    }

    // Only saves a key to the DB if its value differs from the value this same converter
    // produced for that device+key in the previous converted message (PE parity: "仅当提供的键
    // 对应的值与上一条转换消息中的相应值不同时，才会将其保存到数据库").
    private JsonNode filterUnchangedKeys(String converterId, String deviceId, String scope, JsonNode data, List<String> updateOnlyKeys) {
        if (data == null || !data.isObject() || data.isEmpty()) {
            return data;
        }
        ObjectNode filtered = JacksonUtil.newObjectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (updateOnlyKeys.contains(key)) {
                String cacheKey = converterId + ":" + deviceId + ":" + scope + ":" + key;
                JsonNode previous = lastConvertedValues.get(cacheKey);
                if (previous != null && previous.equals(value)) {
                    continue;
                }
                lastConvertedValues.put(cacheKey, value);
            }
            filtered.set(key, value);
        }
        return filtered;
    }

    @Override
    public void pushDownlink(Integration integration, JsonNode msg, String msgType, Map<String, String> metadata) throws Exception {
        IntegrationRuntime runtime = runtimes.get(integration.getId());
        if (runtime == null) {
            throw new UnsupportedOperationException("Downlink is not supported for this integration type in this build!");
        }
        boolean success = false;
        try {
            byte[] payload;
            Map<String, String> effectiveMetadata = metadata;
            DownlinkData converted = convertDownlink(integration, msg, msgType, metadata);
            if (converted != null) {
                payload = converted.data();
                // converter-supplied metadata wins: it is what tells the runtime where to send this
                effectiveMetadata = new HashMap<>(metadata);
                effectiveMetadata.putAll(converted.metadata());
            } else {
                payload = JacksonUtil.toString(msg).getBytes(StandardCharsets.UTF_8);
            }
            runtime.pushDownlink(payload, effectiveMetadata);
            success = true;
        } finally {
            statisticsService.onDownlinkMsg(integration.getType(), success);
        }
    }

    /** @return the encoded downlink, or null when this integration has no downlink converter */
    private DownlinkData convertDownlink(Integration integration, JsonNode msg, String msgType,
                                         Map<String, String> metadata) throws Exception {
        if (integration.getDownlinkConverterId() == null) {
            return null;
        }
        Converter converter = converterService.findConverterById(integration.getTenantId(), integration.getDownlinkConverterId());
        if (converter == null || converter.getConfiguration() == null) {
            throw new IllegalStateException("Downlink converter is not configured!");
        }
        JsonNode cfg = converter.getConfiguration();
        ScriptLanguage lang = cfg.has("scriptLang") ? ScriptLanguage.valueOf(cfg.get("scriptLang").asText()) : ScriptLanguage.TBEL;
        if (!cfg.has("encoder")) {
            throw new IllegalStateException("Downlink converter has no encoder script!");
        }
        Map<String, String> integrationMetadata = Map.of("integrationName", integration.getName());
        JsonNode result = converterExecutorService.executeDownlink(integration.getTenantId(), lang,
                cfg.get("encoder").asText(), msg, msgType, metadata, integrationMetadata);
        if (result == null || result.isNull()) {
            throw new IllegalStateException("Downlink converter returned no result!");
        }
        // an encoder may return either one downlink or an array of them; the runtime takes one
        return DownlinkData.parse(result.isArray() ? result.get(0) : result);
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
