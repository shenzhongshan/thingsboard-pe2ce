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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;
import org.thingsboard.rule.engine.analytics.incoming.state.TbIntervalState;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * CE port of PE's "aggregate stream" node: keeps a running MIN/MAX/SUM/AVG/COUNT/COUNT_UNIQUE per
 * originator per time interval, emitting the result downstream and persisting the accumulator as
 * timeseries so it survives a restart.
 * <p>
 * <b>Deviation from PE:</b> PE can additionally <i>discover</i> entities that never sent data (its
 * "Interval entities" setting, backed by a ParentEntitiesQuery hierarchy CE does not have) and seed
 * empty intervals for them. Here {@code autoCreateIntervals} only back-fills intervals for entities
 * the node has already seen — an entity that has never produced a message is not aggregated.
 */
@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate stream",
        configClazz = TbSimpleAggMsgNodeConfiguration.class,
        version = 2,
        hasQueueName = true,
        nodeDescription = "Aggregates incoming data stream grouped by originator Entity Id",
        nodeDetails = "Calculates MIN/MAX/SUM/AVG/COUNT/UNIQUE over the incoming stream. Messages are grouped by " +
                "originator and by <b>\"aggregation interval\"</b> into Intervals.<br/><br/>Intervals are reported " +
                "downstream based on <b>\"interval persistence policy\"</b> and <b>\"interval check value\"</b>, and " +
                "cached in memory for twice the interval length.<br/><br/>Interval state is persisted as timeseries " +
                "on the originator based on <b>\"state persistence policy\"</b>, so a restart resumes mid-interval " +
                "rather than losing the accumulator.<br/><br/>By default an outgoing message is generated with " +
                "'POST_TELEMETRY_REQUEST' type; change it under \"<b>Output message type</b>\".",
        icon = "functions"
)
public class TbSimpleAggMsgNode implements TbNode {

    private final Gson gson = new Gson();

    private TbSimpleAggMsgNodeConfiguration config;
    private StatePersistPolicy statePersistPolicy;
    private IntervalPersistPolicy intervalPersistPolicy;
    private TbIntervalTable intervals;
    private UUID nextReportTickId;
    private UUID nextPersistTickId;
    private long intervalReportCheckPeriod;
    private long statePersistCheckPeriod;
    private String outMsgType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSimpleAggMsgNodeConfiguration.class);
        this.statePersistPolicy = StatePersistPolicy.valueOf(config.getStatePersistencePolicy());
        this.intervalPersistPolicy = IntervalPersistPolicy.valueOf(config.getIntervalPersistencePolicy());
        this.intervalReportCheckPeriod = TimeUnit.valueOf(config.getIntervalCheckTimeUnit())
                .toMillis(config.getIntervalCheckValue());
        this.statePersistCheckPeriod = TimeUnit.valueOf(config.getStatePersistenceTimeUnit())
                .toMillis(config.getStatePersistenceValue());
        this.outMsgType = StringUtils.isNotBlank(config.getOutMsgType())
                ? config.getOutMsgType() : TbMsgType.POST_TELEMETRY_REQUEST.name();
        this.intervals = new TbIntervalTable(ctx, config);
        scheduleReportTickMsg(ctx, null);
        scheduleStatePersistTickMsg(ctx, null);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        switch (msg.getInternalType()) {
            case TB_SIMPLE_AGG_REPORT_SELF_MSG -> onIntervalTickMsg(ctx, msg);
            case TB_SIMPLE_AGG_PERSIST_SELF_MSG -> onPersistTickMsg(ctx, msg);
            default -> onDataMsg(ctx, msg);
        }
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.trace("Cluster change msg received: {}", msg);
        intervals.cleanupEntities(ctx);
    }

    private void onDataMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityId entityId = msg.getOriginator();
        long ts = extractTs(msg);
        JsonElement value = extractValue(msg);
        Pair<Long, TbIntervalState> statePair = intervals.getByEntityIdAndTs(entityId, ts);
        TbIntervalState state = statePair.getSecond();
        state.update(value);
        if (state.hasChangesToPersist() && statePersistPolicy == StatePersistPolicy.ON_EACH_CHANGE) {
            DonAsynchron.withCallback(intervals.saveIntervalState(entityId, ts, state), v -> {
                ctx.ack(msg);
                state.clearChangesToPersist();
            }, t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } else {
            ctx.ack(msg);
        }
        if (state.hasChangesToReport() && intervalPersistPolicy == IntervalPersistPolicy.ON_EACH_MESSAGE) {
            reportInterval(ctx, entityId, statePair.getFirst(), state);
            state.clearChangesToReport();
        }
    }

    private void onIntervalTickMsg(TbContext ctx, TbMsg msg) {
        // A rescheduled tick can race with an older one still in flight; only the newest counts.
        if (!msg.getId().equals(nextReportTickId)) {
            return;
        }
        scheduleReportTickMsg(ctx, msg);
        intervals.getStatesToReport(intervalPersistPolicy).forEach((entityId, entityStates) ->
                entityStates.forEach((ts, interval) -> reportInterval(ctx, entityId, ts, interval)));
        intervals.cleanupStatesUsingTTL();
    }

    private void reportInterval(TbContext ctx, EntityId entityId, Long ts, TbIntervalState interval) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", Long.toString(ts));
        // The interval is re-reported as it accumulates, so downstream must replace rather than append.
        metaData.putValue("overwriteValue", "true");
        ctx.enqueueForTellNext(TbMsg.newMsg()
                .queueName(ctx.getQueueName())
                .type(outMsgType)
                .originator(entityId)
                .copyMetaData(metaData)
                .data(interval.toValueJson(gson, config.getOutputValueKey()))
                .build(), "Success");
    }

    private void onPersistTickMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getId().equals(nextPersistTickId)) {
            return;
        }
        scheduleStatePersistTickMsg(ctx, msg);
        intervals.getStatesToPersist().forEach((entityId, entityStates) ->
                entityStates.forEach((ts, state) -> intervals.saveIntervalState(entityId, ts, state)));
        intervals.cleanupStatesUsingTTL();
    }

    private void scheduleReportTickMsg(TbContext ctx, TbMsg msg) {
        TbMsg tickMsg = ctx.newMsg(ctx.getQueueName(), TbMsgType.TB_SIMPLE_AGG_REPORT_SELF_MSG,
                ctx.getSelfId(), getCustomerId(msg), TbMsgMetaData.EMPTY, "");
        nextReportTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, intervalReportCheckPeriod);
    }

    private void scheduleStatePersistTickMsg(TbContext ctx, TbMsg msg) {
        TbMsg tickMsg = ctx.newMsg(ctx.getQueueName(), TbMsgType.TB_SIMPLE_AGG_PERSIST_SELF_MSG,
                ctx.getSelfId(), getCustomerId(msg), TbMsgMetaData.EMPTY, "");
        nextPersistTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, statePersistCheckPeriod);
    }

    private org.thingsboard.server.common.data.id.CustomerId getCustomerId(TbMsg msg) {
        return msg != null ? msg.getCustomerId() : null;
    }

    private long extractTs(TbMsg msg) {
        String ts = msg.getMetaData().getValue("ts");
        return StringUtils.isEmpty(ts) ? msg.getTs() : Long.parseLong(ts);
    }

    private JsonElement extractValue(TbMsg msg) {
        JsonElement jsonElement = JsonParser.parseString(msg.getData());
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("Incoming message is not a json object!");
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has(config.getInputValueKey())) {
            throw new IllegalArgumentException("Incoming message does not contain " + config.getInputValueKey() + "!");
        }
        JsonElement je = jsonObject.get(config.getInputValueKey());
        if (je.isJsonNull()) {
            throw new IllegalArgumentException("Found JSON null for [" + config.getInputValueKey() + "] key!");
        }
        return je;
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.hasNonNull("outMsgType")) {
                    ((ObjectNode) oldConfiguration).put("outMsgType", TbMsgType.POST_TELEMETRY_REQUEST.name());
                    hasChanges = true;
                }
            case 1:
                if (oldConfiguration.has("queueName")) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove("queueName");
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
