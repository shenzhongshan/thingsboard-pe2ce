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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.DeviceStateManager;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Reports device activity for messages that arrived through an integration.
 *
 * <p>Devices fed by an integration never open a transport session, so nothing else in the platform
 * would ever mark them active - they would stay "inactive" forever no matter how much telemetry
 * they produce. This is the integration-side equivalent of the transport layer's
 * {@code TransportActivityManager}: every uplink records activity, and the accumulated state is
 * flushed to core at most once per {@code integrations.activity.reporting_period} using the
 * configured strategy, so a chatty integration does not turn every message into a core message.
 *
 * <p>Reporting goes through {@link DeviceStateManager}, which already writes a
 * {@code DeviceActivityProto} to the core queue - or short-circuits to the local
 * {@code DeviceStateService} when this node owns the device's partition.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class IntegrationActivityManager extends AbstractActivityManager<IntegrationActivityKey, Void> {

    private final DeviceStateManager deviceStateManager;

    @Value("${integrations.activity.reporting_period:3000}")
    private long reportingPeriodMillis;
    @Value("${integrations.activity.reporting_strategy:LAST}")
    private ActivityStrategyType reportingStrategyType;

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }

    @Override
    protected long getReportingPeriodMillis() {
        return reportingPeriodMillis;
    }

    @Override
    protected ActivityStrategy getStrategy() {
        return reportingStrategyType.toStrategy();
    }

    @Override
    protected ActivityState<Void> updateState(IntegrationActivityKey key, ActivityState<Void> state) {
        return state;
    }

    @Override
    protected boolean hasExpired(long lastRecordedTime) {
        return System.currentTimeMillis() - reportingPeriodMillis > lastRecordedTime;
    }

    @Override
    protected void onStateExpiry(IntegrationActivityKey key, Void metadata) {
    }

    @Override
    protected void reportActivity(IntegrationActivityKey key, Void metadata, long timeToReport,
                                  ActivityReportCallback<IntegrationActivityKey> callback) {
        log.debug("[{}][{}] Reporting integration activity state. Time to report: [{}].",
                key.tenantId().getId(), key.deviceId().getId(), timeToReport);
        deviceStateManager.onDeviceActivity(key.tenantId(), key.deviceId(), timeToReport, new TbCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(key, timeToReport);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(key, t);
            }
        });
    }

    /** Records that this device produced data now; the flush to core is throttled by the strategy. */
    public void onUplink(IntegrationActivityKey key) {
        onActivity(key, null, System.currentTimeMillis());
    }

}
