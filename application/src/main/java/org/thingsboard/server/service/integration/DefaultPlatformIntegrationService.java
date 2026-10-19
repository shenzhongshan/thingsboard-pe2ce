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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
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
import org.thingsboard.server.common.data.event.ConverterDebugEvent;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
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
    private final EventService eventService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final IntegrationActivityManager activityManager;
    private final AttributesService attributesService;
    private final TelemetrySubscriptionService telemetryService;
    // present only when integrations.rpc.enabled=true
    private final Optional<TbRemoteIntegrationRpcService> remoteIntegrationRpcService;

    /**
     * Sink for the throwaway runtime used by {@link #checkConnection}: whatever arrives during the
     * few hundred ms the probe is open belongs to nobody - the integration may be unsaved, and its
     * converter must not run.
     */
    private static final IntegrationUplinkSink NOOP_UPLINK_SINK = (integration, payload, metadata) -> { };

    // Debug-event direction labels; these land in the event table's "type" column.
    private static final String UPLINK = "Uplink";
    private static final String DOWNLINK = "Downlink";

    private final Map<IntegrationType, IntegrationRuntimeFactory> factoryByType = new ConcurrentHashMap<>();

    // integrationId -> runtime status ("ACTIVE"/"FAILED"/"STOPPED", error)
    private final ConcurrentMap<IntegrationId, IntegrationStatus> statuses = new ConcurrentHashMap<>();
    // integrationId -> running runtime instance (passive types have no entry here)
    private final ConcurrentMap<IntegrationId, IntegrationRuntime> runtimes = new ConcurrentHashMap<>();
    // integrationId -> today's uplink message count (PE's "daily activity" metric)
    private final ConcurrentMap<IntegrationId, DailyCounter> dailyEventCounts = new ConcurrentHashMap<>();
    // integrationId -> counters accumulated since the last statistics flush
    private final ConcurrentMap<IntegrationId, PendingStats> pendingStats = new ConcurrentHashMap<>();
    // converterId+deviceId+key -> last value this converter produced for that key, used to
    // enforce a converter's "updateOnlyKeys" (only persist a key if it changed since the
    // previous converted message for that device).
    private final ConcurrentMap<String, JsonNode> lastConvertedValues = new ConcurrentHashMap<>();

    record IntegrationStatus(String status, String error) {}

    /**
     * Uplink counters accumulated between two statistics flushes. Drained into a single
     * {@code stats_event} row per integration per flush; those rows are what
     * {@code EventService#findAggregatedDailyStats} buckets by hour for the "daily activity"
     * sparkline, so the flush interval also sets the sparkline's resolution.
     */
    private static final class PendingStats {
        private final TenantId tenantId;
        private final AtomicLong messagesProcessed = new AtomicLong();
        private final AtomicLong errorsOccurred = new AtomicLong();

        PendingStats(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        boolean isEmpty() {
            return messagesProcessed.get() == 0 && errorsOccurred.get() == 0;
        }
    }

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
        if (integration.isEdgeTemplate()) {
            // 模板只存不跑：它是给 edge 用的蓝本，连接配置里通常还带着 ${{attr}} 占位符，
            // 在云上真去连只会一路报错，还白占一份连接。启动路径已由
            // IntegrationRepository.findAllEnabledNonTemplate 排除，这里挡的是保存路径 ——
            // 少了这道，enabled=true 的模板一保存就会被 startIntegration 拉起来（实测见过）。
            stopIntegration(integration.getId());
            return;
        }
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
                startIntegration(integration, ComponentLifecycleEvent.UPDATED);
            } else {
                // flush whatever it processed since the last interval before it stops counting as
                // started, so those messages are not silently lost (PE: persistCurrentStatistics)
                doPersistStatistics(integration.getId(), System.currentTimeMillis(), true);
                statuses.put(integration.getId(), new IntegrationStatus("STOPPED", null));
                persistLifecycleEvent(integration, ComponentLifecycleEvent.STOPPED, null);
            }
        });
    }

    @Override
    public void onIntegrationDeleted(Integration integration) {
        scheduledExecutor.submit(() -> {
            stopIntegration(integration.getId());
            statuses.remove(integration.getId());
            dailyEventCounts.remove(integration.getId());
            pendingStats.remove(integration.getId());
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

    @Override
    public ObjectNode getIntegrationInfoStatus(Integration integration) {
        if (!integration.isEnabled()) {
            // The UI renders a disabled integration from the `enabled` flag alone; reporting a
            // stale success/failure from before it was disabled would only be misleading.
            return null;
        }
        if (integration.isRemote()) {
            // A remote integration runs elsewhere: "started" means its process has an open gRPC
            // session with us. Until one shows up it is pending, not failed - the remote may simply
            // not have been launched yet.
            boolean connected = remoteIntegrationRpcService
                    .map(rpc -> rpc.isConnected(integration.getId()))
                    .orElse(false);
            if (!connected) {
                return null;
            }
            ObjectNode result = JacksonUtil.newObjectNode();
            result.put("success", true);
            return result;
        }
        IntegrationStatus status = statuses.get(integration.getId());
        if (status == null || "STOPPED".equals(status.status())) {
            return null;
        }
        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("success", "ACTIVE".equals(status.status()));
        if (status.error() != null) {
            result.put("error", status.error());
        }
        return result;
    }

    @Override
    public void checkConnection(Integration integration) throws Exception {
        IntegrationType type = integration.getType();
        if (PASSIVE_TYPES.contains(type)) {
            // Nothing to dial: these are inbound webhooks resolved by routing key.
            return;
        }
        IntegrationRuntimeFactory factory = factoryByType.get(type);
        if (factory == null) {
            throw new UnsupportedOperationException("Integration type " + type + " is not implemented in this build!");
        }
        // A throwaway runtime that is never registered in `runtimes`: it must not receive uplinks
        // (the integration may not even exist yet) and must be stopped whatever happens, or a
        // failed check would leak a broker connection per click.
        IntegrationRuntime runtime = factory.create(withResolvedSecrets(integration), NOOP_UPLINK_SINK);
        try {
            runtime.start();
        } finally {
            try {
                runtime.stop();
            } catch (Exception e) {
                log.debug("[{}] Failed to stop the connection-check runtime", integration.getName(), e);
            }
        }
    }

    /**
     * Drains the accumulated per-integration counters. Mirrors PE's
     * {@code DefaultIntegrationManagerService#persistStatistics}: every *started* integration gets
     * a row each interval even when its counters are zero, because the telemetry this also writes
     * doubles as a per-node heartbeat - skipping idle integrations would leave their {@code _state}
     * series stale rather than showing them as running.
     */
    @Scheduled(initialDelayString = "${integrations.statistics.persist_frequency:3600000}",
            fixedDelayString = "${integrations.statistics.persist_frequency:3600000}")
    public void persistStatistics() {
        long ts = System.currentTimeMillis();
        for (IntegrationId integrationId : pendingStats.keySet()) {
            doPersistStatistics(integrationId, ts, false);
        }
    }

    /**
     * @param skipEmpty when true, nothing is written unless something happened since the last
     *                  flush - used for the extra flush on shutdown so a stop does not append an
     *                  all-zero row (PE's {@code persistCurrentStatistics})
     */
    private void doPersistStatistics(IntegrationId integrationId, long ts, boolean skipEmpty) {
        PendingStats stats = pendingStats.get(integrationId);
        if (stats == null) {
            return;
        }
        IntegrationStatus status = statuses.get(integrationId);
        // PE returns early unless the integration's current lifecycle state is STARTED
        if (status == null || !"ACTIVE".equals(status.status())) {
            return;
        }
        if (skipEmpty && stats.isEmpty()) {
            return;
        }
        long messages = stats.messagesProcessed.getAndSet(0);
        long errors = stats.errorsOccurred.getAndSet(0);
        String serviceId = serviceInfoProvider.getServiceId();
        try {
            eventService.saveAsync(StatisticsEvent.builder()
                    .tenantId(stats.tenantId)
                    .entityId(integrationId.getId())
                    .serviceId(serviceId)
                    .messagesProcessed(messages)
                    .errorsOccurred(errors)
                    .build());
        } catch (Exception e) {
            log.warn("[{}] Failed to persist integration statistics", integrationId, e);
        }
        // The same numbers as timeseries on the integration entity itself, keyed by service id so a
        // clustered deployment gets one series per node. PE's exact key names and value types.
        try {
            List<TsKvEntry> statsTs = List.of(
                    new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_messagesCount", messages)),
                    new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_errorsCount", errors)),
                    new BasicTsKvEntry(ts, new StringDataEntry(serviceId + "_state", ComponentLifecycleEvent.STARTED.name())));
            telemetryService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                    .tenantId(stats.tenantId)
                    .entityId(integrationId)
                    .entries(statsTs)
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(Void result) {
                            log.trace("[{}] Persisted statistics telemetry: {}/{}", integrationId, messages, errors);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.warn("[{}] Failed to persist statistics telemetry", integrationId, t);
                        }
                    })
                    .build());
        } catch (Exception e) {
            log.warn("[{}] Failed to persist integration statistics telemetry", integrationId, e);
        }
    }

    private void startIntegration(Integration integration) {
        startIntegration(integration, ComponentLifecycleEvent.STARTED);
    }

    /**
     * @param successEvent the lifecycle event to record when the start succeeds - STARTED on a cold
     *                     start, UPDATED when this start is the second half of a config change
     *                     (PE records the same distinction)
     */
    private void startIntegration(Integration integration, ComponentLifecycleEvent successEvent) {
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
            IntegrationStatus status = statuses.get(integration.getId());
            persistLifecycleEvent(integration, started ? successEvent : ComponentLifecycleEvent.FAILED,
                    started || status == null ? null : status.error());
        }
    }

    /**
     * Writes an {@code lc_event} row for this integration and mirrors it into the
     * {@code integration_status_<serviceId>} SERVER_SCOPE attribute, the way PE does - that
     * attribute is what makes the integration's own "Attributes" tab show which node is running it
     * and whether the last start succeeded. Never throws: a failed audit trail must not abort a
     * lifecycle transition.
     */
    private void persistLifecycleEvent(Integration integration, ComponentLifecycleEvent lcEvent, String error) {
        String serviceId = serviceInfoProvider.getServiceId();
        try {
            eventService.saveAsync(LifecycleEvent.builder()
                    .tenantId(integration.getTenantId())
                    .entityId(integration.getId().getId())
                    .serviceId(serviceId)
                    .lcEventType(lcEvent.name())
                    .success(error == null)
                    .error(error)
                    .build());
        } catch (Exception e) {
            log.debug("[{}] Failed to persist integration lifecycle event", integration.getId(), e);
        }
        String key = "integration_status_" + serviceId.toLowerCase();
        try {
            if (lcEvent == ComponentLifecycleEvent.STOPPED) {
                attributesService.removeAll(integration.getTenantId(), integration.getId(),
                        AttributeScope.SERVER_SCOPE, Collections.singletonList(key));
            } else {
                ObjectNode value = JacksonUtil.newObjectNode();
                value.put("success", error == null);
                if (error != null) {
                    value.put("serviceId", serviceId);
                    value.put("error", error);
                }
                attributesService.save(integration.getTenantId(), integration.getId(), AttributeScope.SERVER_SCOPE,
                        new BaseAttributeKvEntry(new JsonDataEntry(key, JacksonUtil.toString(value)), System.currentTimeMillis()));
            }
        } catch (Exception e) {
            log.debug("[{}] Failed to update integration status attribute", integration.getId(), e);
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
            // Created here, not on the first uplink, so the statistics flush can find every started
            // integration - including ones that never receive a message - and still emit their
            // heartbeat telemetry.
            pendingStats.computeIfAbsent(integration.getId(), id -> new PendingStats(integration.getTenantId()));
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
        PendingStats stats = pendingStats.computeIfAbsent(integration.getId(), id -> new PendingStats(integration.getTenantId()));
        try {
            doProcessUplink(integration, payload, metadata);
            stats.messagesProcessed.incrementAndGet();
            statisticsService.onUplinkMsg(integration.getType(), true);
            persistDebugEvent(integration, UPLINK, payload, metadata, null);
        } catch (Exception e) {
            stats.errorsOccurred.incrementAndGet();
            statisticsService.onUplinkMsg(integration.getType(), false);
            persistDebugEvent(integration, UPLINK, payload, metadata, e);
            throw e;
        }
    }

    /**
     * Writes an {@code integration_debug_event} row when this integration's debug settings ask for
     * it: every message while "debug all" is within its window, or only failures when just
     * "failures" is on. Never throws - debug persistence must not break message processing.
     */
    private void persistDebugEvent(Integration integration, String direction, byte[] payload,
                                   Map<String, String> metadata, Exception error) {
        boolean wanted = error != null
                ? DebugModeUtil.isDebugFailuresAvailable(integration)
                : DebugModeUtil.isDebugAllAvailable(integration);
        if (!wanted) {
            return;
        }
        try {
            eventService.saveAsync(IntegrationDebugEvent.builder()
                    .tenantId(integration.getTenantId())
                    .entityId(integration.getId().getId())
                    .serviceId(serviceInfoProvider.getServiceId())
                    .eventType(direction)
                    .messageType(integration.getType().name())
                    .message(payload == null ? null : new String(payload, StandardCharsets.UTF_8))
                    .status(error == null ? "OK" : "FAILURE")
                    .error(error == null ? null : toErrorString(error))
                    .build());
        } catch (Exception e) {
            log.debug("[{}] Failed to persist integration debug event", integration.getId(), e);
        }
    }

    private static String toErrorString(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }

    /** PE's downlink debug-event input shape: {@code [{"msg":..,"msgType":..,"metadata":{..}}]}. */
    private static byte[] downlinkDebugInput(JsonNode msg, String msgType, Map<String, String> metadata) {
        ObjectNode entry = JacksonUtil.newObjectNode();
        entry.set("msg", msg == null ? JacksonUtil.newObjectNode() : msg);
        entry.put("msgType", msgType);
        entry.set("metadata", JacksonUtil.valueToTree(metadata == null ? Map.of() : metadata));
        ArrayNode array = JacksonUtil.newArrayNode();
        array.add(entry);
        return JacksonUtil.toString(array).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Classifies a raw converter input the way PE's debug events label it, so the "test with this
     * message" action can put the payload back into the right editor mode. PE knows the content
     * type from the integration that produced the message; this build does not carry it that far,
     * so it is inferred: parseable as JSON -> JSON, otherwise decodable as UTF-8 -> TEXT, else
     * BINARY (the dialog then treats the value as Base64, matching how it encodes BINARY payloads).
     */
    private static String contentTypeOf(byte[] in) {
        if (in == null) {
            return null;
        }
        String text = new String(in, StandardCharsets.UTF_8);
        if (text.getBytes(StandardCharsets.UTF_8).length != in.length) {
            return "BINARY";
        }
        try {
            // throws IllegalArgumentException on anything that is not valid JSON
            return JacksonUtil.toJsonNode(text) != null ? "JSON" : "TEXT";
        } catch (Exception e) {
            return "TEXT";
        }
    }

    /**
     * The converter's own debug row: the payload it was handed and the JSON it produced. Gated on
     * the converter's debug settings, which are separate from the integration's - a converter is a
     * shared entity and you often want to trace it without tracing every integration that uses it.
     *
     * <p>{@code inMsgType} carries the payload's <b>content type</b> (JSON/TEXT/BINARY), not the
     * converter type - that is what PE stores there and what its "test with this message" action
     * reads back to pick the right editor mode in the test dialog.
     */
    private void persistConverterDebugEvent(Converter converter, String direction, byte[] in, JsonNode out,
                                            Map<String, String> metadata, Exception error) {
        boolean wanted = error != null
                ? DebugModeUtil.isDebugFailuresAvailable(converter)
                : DebugModeUtil.isDebugAllAvailable(converter);
        if (!wanted) {
            return;
        }
        try {
            eventService.saveAsync(ConverterDebugEvent.builder()
                    .tenantId(converter.getTenantId())
                    .entityId(converter.getId().getId())
                    .serviceId(serviceInfoProvider.getServiceId())
                    .eventType(direction)
                    .inMsgType(contentTypeOf(in))
                    .inMsg(in == null ? null : new String(in, StandardCharsets.UTF_8))
                    .outMsgType(out == null ? null : out.getNodeType().name())
                    .outMsg(out == null ? null : JacksonUtil.toString(out))
                    .metadata(metadata == null ? null : JacksonUtil.toString(metadata))
                    .error(error == null ? null : toErrorString(error))
                    .build());
        } catch (Exception e) {
            log.debug("[{}] Failed to persist converter debug event", converter.getId(), e);
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
        JsonNode result;
        try {
            result = converterExecutorService.executeUplink(integration.getTenantId(), lang, decoder, effectivePayload, md);
        } catch (Exception e) {
            persistConverterDebugEvent(converter, UPLINK, effectivePayload, null, md, e);
            throw e;
        }
        persistConverterDebugEvent(converter, UPLINK, effectivePayload, result, md, null);
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
            // PE 上行转换器约定：返回对象中的 attributes 属于目标设备的服务器端属性。
            // 使用独立元数据副本，避免属性作用域影响同一报文中的遥测消息。
            TbMsgMetaData attributesMetaData = msgMetaData.copy();
            attributesMetaData.putValue(DataConstants.SCOPE, AttributeScope.SERVER_SCOPE.name());
            pushMsg(tenantId, device, TbMsgType.POST_ATTRIBUTES_REQUEST, attributes, attributesMetaData);
        }
        if ((telemetry != null && !telemetry.isEmpty()) || (attributes != null && !attributes.isEmpty())) {
            // An integration-fed device never opens a transport session, so this is the only thing
            // that will ever mark it active - without it the device stays "inactive" forever no
            // matter how much data arrives. Throttled by IntegrationActivityManager's strategy.
            activityManager.onUplink(new IntegrationActivityKey(tenantId, device.getId()));
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
            persistDebugEvent(integration, DOWNLINK, payload, effectiveMetadata, null);
        } catch (Exception e) {
            persistDebugEvent(integration, DOWNLINK, null, metadata, e);
            throw e;
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
        // PE records the downlink converter's input as an array of {msg, msgType, metadata}; the
        // "test with this message" action replays the last element of it into the test dialog, so
        // the msgType and metadata have to travel inside the payload rather than beside it.
        byte[] debugIn = downlinkDebugInput(msg, msgType, metadata);
        JsonNode result;
        try {
            result = converterExecutorService.executeDownlink(integration.getTenantId(), lang,
                    cfg.get("encoder").asText(), msg, msgType, metadata, integrationMetadata);
        } catch (Exception e) {
            persistConverterDebugEvent(converter, DOWNLINK, debugIn, null, metadata, e);
            throw e;
        }
        persistConverterDebugEvent(converter, DOWNLINK, debugIn, result, metadata, null);
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
