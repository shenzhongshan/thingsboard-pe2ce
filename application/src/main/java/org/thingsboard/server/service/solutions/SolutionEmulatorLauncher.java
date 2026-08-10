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
package org.thingsboard.server.service.solutions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.emulator.CustomEmulator;
import org.thingsboard.server.service.solutions.data.emulator.Emulator;
import org.thingsboard.server.service.solutions.data.emulator.EmulatorFactory;
import org.thingsboard.server.service.solutions.data.emulator.SimpleEmulator;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Backfills the synthetic history a freshly installed solution template needs, so its dashboards
 * open with data instead of empty charts.
 * <p>
 * Telemetry is pushed as {@code POST_TELEMETRY_REQUEST} through the rule engine rather than written
 * straight to storage: the template's own rule chains, alarm rules and calculated fields are meant
 * to see this data, and several dashboards show alarms that only exist because of it.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class SolutionEmulatorLauncher {

    private static final int POOL_SIZE = 10;

    private final TbClusterService tbClusterService;
    private final TelemetrySubscriptionService tsSubService;

    private final ExecutorService executor =
            ThingsBoardExecutors.newWorkStealingPool(POOL_SIZE, "solution-emulators-executor");

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    /**
     * Generates and publishes one entity's history. The returned future completes once every
     * message has been acknowledged by the queue, so an install can wait for all of them.
     */
    public CompletableFuture<Void> launch(TenantId tenantId, EntityId entityId, String entityName,
                                          EmulatorDefinition definition, Device device) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.submit(() -> {
            // Held at 1 for the duration of generation so the future cannot complete early while
            // messages are still being produced; released in the finally block.
            AtomicInteger pending = new AtomicInteger(1);
            try {
                Emulator emulator = EmulatorFactory.create(definition);
                if (emulator instanceof CustomEmulator customEmulator) {
                    publishScriptedSeries(tenantId, entityId, entityName, customEmulator, pending, future);
                } else if (emulator instanceof SimpleEmulator simpleEmulator) {
                    publishSampledSeries(tenantId, entityId, entityName, definition, simpleEmulator, pending, future);
                }
            } catch (Exception e) {
                log.warn("[{}] Telemetry emulation failed for {}", tenantId, entityName, e);
                future.completeExceptionally(e);
            } finally {
                if (pending.decrementAndGet() == 0 && !future.isDone()) {
                    future.complete(null);
                }
            }
            future.whenComplete((v, t) -> keepDeviceActive(device, definition));
        });
        return future;
    }

    private void publishScriptedSeries(TenantId tenantId, EntityId entityId, String entityName,
                                       CustomEmulator emulator, AtomicInteger pending, CompletableFuture<Void> future) {
        for (Pair<Long, ObjectNode> sample = emulator.getNextValue(); sample != null; sample = emulator.getNextValue()) {
            pending.incrementAndGet();
            publish(tenantId, entityId, entityName, sample.getFirst(), sample.getSecond(), pending, future);
        }
    }

    private void publishSampledSeries(TenantId tenantId, EntityId entityId, String entityName,
                                      EmulatorDefinition definition, SimpleEmulator emulator,
                                      AtomicInteger pending, CompletableFuture<Void> future) throws InterruptedException {
        long publishFrequency = TimeUnit.SECONDS.toMillis(Math.max(definition.getPublishFrequencyInSeconds(), 1));
        long now = System.currentTimeMillis();
        for (long ts = definition.getOldestTs(now); ts < now; ts += publishFrequency) {
            pending.incrementAndGet();
            publish(tenantId, entityId, entityName, ts, emulator.getValue(ts), pending, future);
            // Paced so a large backfill does not starve the rule engine of everything else.
            if (definition.getPublishPauseInMillis() > 0) {
                Thread.sleep(definition.getPublishPauseInMillis());
            }
        }
    }

    private void publish(TenantId tenantId, EntityId entityId, String entityName, long ts, ObjectNode values,
                         AtomicInteger pending, CompletableFuture<Void> future) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        // The rule engine reads the sample time from metadata; without it every point lands at now.
        metaData.putValue("ts", Long.toString(ts));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(entityId)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(values))
                .build();
        tbClusterService.pushMsgToRuleEngine(tenantId, entityId, msg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                complete();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to publish emulated telemetry for {} at {}", tenantId, entityName, ts, t);
                complete();
            }

            private void complete() {
                if (pending.decrementAndGet() == 0 && !future.isDone()) {
                    future.complete(null);
                }
            }
        });
    }

    /**
     * Without this the device reports inactive the moment the backfill ends, because its last
     * activity is a week old. Templates set the timeout to span the whole demo period.
     */
    private void keepDeviceActive(Device device, EmulatorDefinition definition) {
        if (device == null || definition.getActivityPeriodInMillis() <= 0) {
            return;
        }
        try {
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(device.getTenantId())
                    .entityId(device.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new LongDataEntry("inactivityTimeout", definition.getActivityPeriodInMillis()))
                    .build());
        } catch (Exception e) {
            log.warn("[{}] Failed to set inactivity timeout for {}", device.getTenantId(), device.getName(), e);
        }
    }

}
