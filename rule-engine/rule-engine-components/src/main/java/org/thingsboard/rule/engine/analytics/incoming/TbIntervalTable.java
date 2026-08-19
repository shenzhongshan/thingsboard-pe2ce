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
package org.thingsboard.rule.engine.analytics.incoming;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.data.util.Pair;
import org.thingsboard.rule.engine.analytics.incoming.state.TbAvgIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbCountIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbCountUniqueIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbMaxIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbMinIntervalState;
import org.thingsboard.rule.engine.analytics.incoming.state.TbSumIntervalState;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory table of running aggregates, keyed by entity then by interval start.
 * <p>
 * State survives a restart because each interval is also written to the originator as a
 * {@code RuleNodeState_<ruleNodeId>} timeseries value at its interval-start timestamp, and read
 * back on the first message for that interval.
 */
class TbIntervalTable {

    private static final String STATE_KEY_PREFIX = "RuleNodeState_";

    private final TbContext ctx;
    private final Gson gson = new Gson();
    private final AggIntervalType aggIntervalType;
    private final ZoneId tz;
    private final long intervalDuration;
    private final long intervalTtl;
    private final MathFunction function;
    private final boolean autoCreateIntervals;

    private final ConcurrentMap<EntityId, ConcurrentMap<Long, TbIntervalState>> states = new ConcurrentHashMap<>();

    TbIntervalTable(TbContext ctx, TbSimpleAggMsgNodeConfiguration config) {
        this.ctx = ctx;
        this.aggIntervalType = config.getAggIntervalType() == null ? AggIntervalType.CUSTOM : config.getAggIntervalType();
        long tmpIntervalDuration;
        if (aggIntervalType == AggIntervalType.CUSTOM) {
            this.tz = ZoneId.systemDefault();
            TimeUnit unit = TimeUnit.valueOf(config.getAggIntervalTimeUnit());
            tmpIntervalDuration = unit.toMillis(config.getAggIntervalValue());
            this.intervalTtl = unit.toMillis(config.getAggIntervalValue() * 2);
        } else {
            this.tz = ZoneId.of(config.getTimeZoneId());
            tmpIntervalDuration = getDefaultIntervalDurationByAggType();
            this.intervalTtl = aggIntervalType.getInterval() * 2L;
        }
        // Below a minute the report/persist ticks would outpace the intervals themselves.
        this.intervalDuration = Math.max(tmpIntervalDuration, TimeUnit.MINUTES.toMillis(1L));
        this.function = MathFunction.valueOf(config.getMathFunction());
        this.autoCreateIntervals = config.isAutoCreateIntervals();
    }

    /** Drops entities this node no longer owns after a partition change. */
    void cleanupEntities(TbContext ctx) {
        Set<EntityId> keys = new HashSet<>(states.keySet());
        keys.stream().filter(entityId -> !ctx.isLocalEntity(entityId)).forEach(states::remove);
    }

    Pair<Long, TbIntervalState> getByEntityIdAndTs(EntityId entityId, long ts) throws ExecutionException, InterruptedException {
        long intervalStartTs = calculateIntervalStart(ts);
        Map<Long, TbIntervalState> tsStates = states.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>());
        TbIntervalState state = tsStates.get(intervalStartTs);
        if (state == null) {
            state = fetchIntervalState(entityId, intervalStartTs).get();
            tsStates.put(intervalStartTs, state);
        }
        return Pair.of(intervalStartTs, state);
    }

    ListenableFuture<TimeseriesSaveResult> saveIntervalState(EntityId entityId, long ts, TbIntervalState state) {
        StringDataEntry kvEntry = new StringDataEntry(stateKey(), state.toStateJson(gson));
        BasicTsKvEntry tsKvEntry = new BasicTsKvEntry(calculateIntervalStart(ts), kvEntry);
        return ctx.getTimeseriesService().saveWithoutLatest(ctx.getTenantId(), entityId, List.of(tsKvEntry), 0L);
    }

    Map<EntityId, Map<Long, TbIntervalState>> getStatesToReport(IntervalPersistPolicy intervalPersistPolicy) {
        long ts = System.currentTimeMillis();
        Map<EntityId, Map<Long, TbIntervalState>> updatedStates = new HashMap<>();
        if (autoCreateIntervals) {
            // Fill the gap since the last interval that saw data, so entities that went quiet still
            // report a default value for every elapsed interval.
            states.forEach((entityId, intervals) -> {
                Optional<Long> maxIntervalTs = intervals.keySet().stream().max(Comparator.comparingLong(Long::valueOf));
                if (maxIntervalTs.isPresent()) {
                    for (long tmpTs = maxIntervalTs.get() + intervalDuration; tmpTs < ts; tmpTs += intervalDuration) {
                        intervals.put(calculateIntervalStart(tmpTs), createDefaultTbIntervalState());
                    }
                } else {
                    intervals.put(calculateIntervalStart(ts), createDefaultTbIntervalState());
                }
            });
        }
        states.forEach((entityId, intervals) -> {
            Stream<Map.Entry<Long, TbIntervalState>> entryStream =
                    intervals.entrySet().stream().filter(e -> e.getValue().hasChangesToReport());
            if (intervalPersistPolicy == IntervalPersistPolicy.ON_EACH_CHECK_AFTER_INTERVAL_END) {
                entryStream = entryStream.filter(e -> e.getKey() + intervalDuration < ts);
            }
            Map<Long, TbIntervalState> updatedIntervals =
                    entryStream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!updatedIntervals.isEmpty()) {
                updatedIntervals.values().forEach(TbIntervalState::clearChangesToReport);
                updatedStates.put(entityId, updatedIntervals);
            }
        });
        return updatedStates;
    }

    Map<EntityId, Map<Long, TbIntervalState>> getStatesToPersist() {
        Map<EntityId, Map<Long, TbIntervalState>> updatedStates = new HashMap<>();
        states.forEach((entityId, intervals) -> {
            Map<Long, TbIntervalState> updatedIntervals = intervals.entrySet().stream()
                    .filter(e -> e.getValue().hasChangesToPersist())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!updatedIntervals.isEmpty()) {
                updatedIntervals.values().forEach(TbIntervalState::clearChangesToPersist);
                updatedStates.put(entityId, updatedIntervals);
            }
        });
        return updatedStates;
    }

    void cleanupStatesUsingTTL() {
        long expTime = System.currentTimeMillis() - intervalTtl;
        states.forEach((entityId, intervals) -> intervals.keySet().stream()
                .filter(ts -> ts < expTime)
                .collect(Collectors.toList())
                .forEach(intervals::remove));
    }

    private String stateKey() {
        return STATE_KEY_PREFIX + ctx.getSelfId();
    }

    private ListenableFuture<TbIntervalState> fetchIntervalState(EntityId entityId, long intervalStartTs) {
        // PE calls TimeseriesService.findOne(tenantId, entityId, ts, key), which CE does not have —
        // an unaggregated single-point read over [ts, ts+1) is the equivalent.
        ReadTsKvQuery query = new BaseReadTsKvQuery(stateKey(), intervalStartTs, intervalStartTs + 1, 1, "DESC");
        ListenableFuture<List<TsKvEntry>> f =
                ctx.getTimeseriesService().findAll(ctx.getTenantId(), entityId, List.of(query));
        return Futures.transform(f, entries -> {
            String value = null;
            if (entries != null && !entries.isEmpty()) {
                value = entries.get(0).getStrValue().orElse(null);
            }
            return StringUtils.isEmpty(value) ? createDefaultTbIntervalState() : readTbIntervalState(value);
        }, MoreExecutors.directExecutor());
    }

    private TbIntervalState readTbIntervalState(String value) {
        JsonElement stateJson = JsonParser.parseString(value);
        return switch (function) {
            case MIN -> new TbMinIntervalState(stateJson);
            case MAX -> new TbMaxIntervalState(stateJson);
            case SUM -> new TbSumIntervalState(stateJson);
            case AVG -> new TbAvgIntervalState(stateJson);
            case COUNT -> new TbCountIntervalState(stateJson);
            case COUNT_UNIQUE -> new TbCountUniqueIntervalState(stateJson);
        };
    }

    private TbIntervalState createDefaultTbIntervalState() {
        return switch (function) {
            case MIN -> new TbMinIntervalState();
            case MAX -> new TbMaxIntervalState();
            case SUM -> new TbSumIntervalState();
            case AVG -> new TbAvgIntervalState();
            case COUNT -> new TbCountIntervalState();
            case COUNT_UNIQUE -> new TbCountUniqueIntervalState();
        };
    }

    private long calculateIntervalStart(long ts) {
        if (AggIntervalType.CUSTOM.equals(aggIntervalType)) {
            return ts / intervalDuration * intervalDuration;
        }
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), tz);
        return switch (aggIntervalType) {
            case HOUR -> zdt.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
            case DAY -> zdt.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
            case WEEK -> zdt.truncatedTo(ChronoUnit.DAYS)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toInstant().toEpochMilli();
            // previousOrSame, not with(SUNDAY): the latter moves to the Sunday that *ends* the ISO
            // week, putting the interval start in the future for Mon–Sat (PE has that bug).
            case WEEK_SUN_SAT -> zdt.truncatedTo(ChronoUnit.DAYS)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toInstant().toEpochMilli();
            case MONTH -> zdt.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant().toEpochMilli();
            case YEAR -> zdt.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1).toInstant().toEpochMilli();
            case CUSTOM -> ts / intervalDuration * intervalDuration;
        };
    }

    private long getDefaultIntervalDurationByAggType() {
        return switch (aggIntervalType) {
            case HOUR -> TimeUnit.HOURS.toMillis(1L);
            case DAY -> TimeUnit.DAYS.toMillis(1L);
            case WEEK, WEEK_SUN_SAT -> TimeUnit.DAYS.toMillis(7L);
            case MONTH -> TimeUnit.DAYS.toMillis(30L);
            // DAYS, not HOURS: PE writes HOURS.toMillis(365) here, i.e. ~15 days for a YEAR interval,
            // which would make auto-created intervals fire ~24x too often.
            case YEAR -> TimeUnit.DAYS.toMillis(365L);
            case CUSTOM -> TimeUnit.HOURS.toMillis(1L);
        };
    }

}
