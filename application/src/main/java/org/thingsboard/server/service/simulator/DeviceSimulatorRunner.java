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
package org.thingsboard.server.service.simulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.solutions.SolutionEmulatorLauncher;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.emulator.BasicEmulator;
import org.thingsboard.server.service.solutions.data.emulator.Emulator;
import org.thingsboard.server.service.solutions.data.emulator.EmulatorFactory;
import org.thingsboard.server.service.solutions.data.emulator.SimpleEmulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drives the two run modes a user-configured device simulator can start in:
 * <ul>
 *     <li>{@link RunMode#HISTORICAL} — delegates straight to {@link SolutionEmulatorLauncher}, the
 *     existing solution-template backfill component. It is already generic (tenant/entity/definition,
 *     nothing solution-specific) and terminates on its own once it reaches "now".</li>
 *     <li>{@link RunMode#LIVE} — new here. Keeps one {@link BasicEmulator} instance alive per running
 *     simulator and ticks it forward in real time on a shared scheduler, so stateful strategies
 *     (natural/counter/...) evolve continuously instead of being recomputed from scratch each call.</li>
 * </ul>
 * Run state lives only in {@link #sessions}; it is intentionally not persisted, since a live session
 * cannot survive an application restart anyway.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceSimulatorRunner {

    private static final int POOL_SIZE = 10;

    private final TbClusterService tbClusterService;
    private final SolutionEmulatorLauncher historicalLauncher;

    private final ScheduledExecutorService scheduler =
            ThingsBoardExecutors.newScheduledThreadPool(POOL_SIZE, "device-simulator-live-executor");

    private final Map<DeviceSimulatorId, RunningSession> sessions = new ConcurrentHashMap<>();

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }

    public boolean isRunning(DeviceSimulatorId id) {
        return sessions.containsKey(id);
    }

    public RunningSession getSession(DeviceSimulatorId id) {
        return sessions.get(id);
    }

    public void start(TenantId tenantId, Device device, DeviceSimulator simulator, RunMode mode) {
        DeviceSimulatorId id = simulator.getId();
        if (sessions.containsKey(id)) {
            throw new IllegalStateException("Simulator is already running, stop it first");
        }
        EmulatorDefinition definition = JacksonUtil.treeToValue(simulator.getDefinition(), EmulatorDefinition.class);
        // User-authored simulators never get to name an arbitrary emulator class.
        definition.setClazz(null);

        if (mode == RunMode.HISTORICAL) {
            startHistorical(tenantId, device, simulator, definition);
        } else {
            startLive(tenantId, device, simulator, definition);
        }
    }

    public void stop(DeviceSimulatorId id) {
        RunningSession session = sessions.remove(id);
        if (session != null && session.future != null) {
            session.future.cancel(false);
        }
    }

    private void startHistorical(TenantId tenantId, Device device, DeviceSimulator simulator, EmulatorDefinition definition) {
        RunningSession session = new RunningSession(RunMode.HISTORICAL);
        sessions.put(simulator.getId(), session);
        historicalLauncher.launch(tenantId, device.getId(), device.getName(), definition, device)
                .whenComplete((v, t) -> {
                    if (t != null) {
                        session.lastError = t.getMessage();
                        log.warn("[{}] Historical simulation failed for {}", tenantId, device.getName(), t);
                    }
                    sessions.remove(simulator.getId(), session);
                });
    }

    private void startLive(TenantId tenantId, Device device, DeviceSimulator simulator, EmulatorDefinition definition) {
        Emulator emulator = EmulatorFactory.create(definition);
        if (!(emulator instanceof SimpleEmulator simpleEmulator)) {
            throw new IllegalArgumentException("Live simulation requires a sampled emulator");
        }
        RunningSession session = new RunningSession(RunMode.LIVE);
        long periodSeconds = Math.max(definition.getPublishFrequencyInSeconds(), 1);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                long ts = System.currentTimeMillis();
                ObjectNode values = simpleEmulator.getValue(ts);
                publish(tenantId, device.getId(), device.getName(), ts, values, session);
            } catch (Exception e) {
                session.lastError = e.getMessage();
                log.warn("[{}] Live simulation tick failed for {}", tenantId, device.getName(), e);
            }
        }, 0, periodSeconds, TimeUnit.SECONDS);
        session.future = future;
        sessions.put(simulator.getId(), session);
    }

    private void publish(TenantId tenantId, DeviceId deviceId, String deviceName, long ts, ObjectNode values, RunningSession session) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", Long.toString(ts));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(values))
                .build();
        tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, msg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                session.publishedCount.incrementAndGet();
            }

            @Override
            public void onFailure(Throwable t) {
                session.lastError = t.getMessage();
                log.warn("[{}] Failed to publish live simulated telemetry for {} at {}", tenantId, deviceName, ts, t);
            }
        });
    }

    public static class RunningSession {
        final RunMode mode;
        final long startedTime = System.currentTimeMillis();
        final AtomicLong publishedCount = new AtomicLong();
        volatile String lastError;
        volatile ScheduledFuture<?> future;

        RunningSession(RunMode mode) {
            this.mode = mode;
        }

        public RunMode getMode() {
            return mode;
        }

        public long getStartedTime() {
            return startedTime;
        }

        public long getPublishedCount() {
            return publishedCount.get();
        }

        public String getLastError() {
            return lastError;
        }
    }

}
