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
package org.thingsboard.server.service.integration.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Platform-side integration metrics, registered with the shared {@link StatsFactory} so they show
 * up wherever the rest of the platform's metrics do (Prometheus, JMX, ...), and dumped to the log
 * on a schedule.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultIntegrationStatisticsService implements IntegrationStatisticsService {

    private static final String STATS_KEY_COUNTER = StatsType.INTEGRATION.getName() + "_stats_counter";
    private static final String STATS_KEY_GAUGE = StatsType.INTEGRATION.getName() + "_stats_gauge";

    private final Map<IntegrationStatisticsKey, DefaultCounter> counters = new ConcurrentHashMap<>();
    private final Map<IntegrationStatisticsKey, AtomicLong> gauges = new ConcurrentHashMap<>();

    private final StatsFactory statsFactory;

    @Value("${integrations.stats.enabled:true}")
    private boolean statsEnabled;

    @Override
    public void onIntegrationStateUpdate(IntegrationType integrationType, ComponentLifecycleEvent state, boolean success) {
        try {
            if (ComponentLifecycleEvent.STARTED.equals(state)) {
                incrementCounter(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, success, integrationType));
            } else if (!success || ComponentLifecycleEvent.FAILED.equals(state)) {
                incrementCounter(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, false, integrationType));
            }
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to record integration state update", integrationType, state, success, e);
        }
    }

    @Override
    public void onIntegrationsCountUpdate(IntegrationType integrationType, int started, int failed) {
        try {
            setGaugeValue(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, true, integrationType), started);
            setGaugeValue(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, false, integrationType), failed);
        } catch (Exception e) {
            log.error("Failed to record integration count update, type [{}] started [{}] failed [{}]",
                    integrationType, started, failed, e);
        }
    }

    @Override
    public void onUplinkMsg(IntegrationType integrationType, boolean success) {
        onMsg(IntegrationStatisticsMetricName.MSGS_UPLINK, integrationType, success);
    }

    @Override
    public void onDownlinkMsg(IntegrationType integrationType, boolean success) {
        onMsg(IntegrationStatisticsMetricName.MSGS_DOWNLINK, integrationType, success);
    }

    // NB: PE's version passes a hardcoded `true` here and drops the caller's `success` flag, so
    // failed messages are counted as successful. Passing it through is the whole point of the metric.
    private void onMsg(IntegrationStatisticsMetricName metric, IntegrationType integrationType, boolean success) {
        try {
            incrementCounter(new IntegrationStatisticsKey(metric, success, integrationType));
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to record integration message", metric, integrationType, success, e);
        }
    }

    @Scheduled(initialDelayString = "${integrations.stats.print_interval_ms:60000}",
            fixedDelayString = "${integrations.stats.print_interval_ms:60000}")
    @Override
    public void printStats() {
        if (!statsEnabled || counters.isEmpty()) {
            return;
        }
        StringBuilder stats = new StringBuilder();
        counters.forEach((key, value) -> {
            long count = value.get();
            if (count > 0) {
                stats.append(key).append(" = [").append(count).append("] ");
            }
        });
        if (!stats.isEmpty()) {
            log.info("Integration stats: {}", stats);
        }
    }

    @Override
    public void reset() {
        counters.values().forEach(DefaultCounter::clear);
    }

    private void incrementCounter(IntegrationStatisticsKey key) {
        counters.computeIfAbsent(key, k -> statsFactory.createDefaultCounter(STATS_KEY_COUNTER, k.getTags())).increment();
    }

    private void setGaugeValue(IntegrationStatisticsKey key, int value) {
        gauges.computeIfAbsent(key, k -> statsFactory.createGauge(STATS_KEY_GAUGE, new AtomicLong(0), k.getTags()))
                .set(value);
    }

}
