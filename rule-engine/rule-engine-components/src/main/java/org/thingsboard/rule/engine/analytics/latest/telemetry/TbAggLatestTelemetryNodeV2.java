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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * CE port of PE's non-deprecated "aggregate latest" node (V2 - reactive, relation-based).
 * PE's per-mapping TBEL/JS attribute filter is not ported (see AggLatestMapping javadoc).
 */
@Slf4j
@RuleNode(
        type = ComponentType.ANALYTICS,
        name = "aggregate latest",
        hasQueueName = true,
        configClazz = TbAggLatestTelemetryNodeV2Configuration.class,
        nodeDescription = "Aggregates data based on incoming event",
        nodeDetails = "Performs aggregation of attributes or latest time-series fetched from related entities. Generates outgoing " +
                "message with aggregated values. By default, an outgoing message generates with 'POST_TELEMETRY_REQUEST' type. " +
                "The type of the outgoing messages is controlled under \"<b>Output message type</b>\" configuration parameter.",
        icon = "functions"
)
public class TbAggLatestTelemetryNodeV2 implements TbNode {

    private static final int CACHE_TTL_MULTIPLIER = 3;

    private final Gson gson = new Gson();
    private final ConcurrentMap<EntityId, AggDeduplicationData> lastMsgMap = new ConcurrentHashMap<>();

    private TbAggLatestTelemetryNodeV2Configuration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAggLatestTelemetryNodeV2Configuration.class);
        scheduleClearInactiveEntitiesMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        switch (msg.getInternalType()) {
            case TB_AGG_LATEST_SELF_MSG -> processDelayedMsg(ctx, msg.getOriginator());
            case TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG -> processClearInactiveEntitiesMsg(ctx);
            default -> processRegularMsg(ctx, msg);
        }
    }

    private void processRegularMsg(TbContext ctx, TbMsg msg) {
        long ts = System.currentTimeMillis();
        EntityId entityId = msg.getOriginator();
        AggDeduplicationData deduplicationData = lastMsgMap.get(entityId);
        if (deduplicationData == null) {
            doCalculate(ctx, msg, ts);
        } else if (deduplicationData.getMsg() == null) {
            long nextCalcTs = deduplicationData.getTs() + config.getDeduplicationInSec() * 1000L;
            if (nextCalcTs > ts) {
                lastMsgMap.put(entityId, new AggDeduplicationData(deduplicationData.getTs(), msg));
                scheduleDelayedMsg(ctx, entityId, nextCalcTs - ts);
                ctx.ack(msg);
            } else {
                doCalculate(ctx, msg, ts);
            }
        } else {
            lastMsgMap.put(entityId, new AggDeduplicationData(deduplicationData.getTs(), msg));
            ctx.ack(msg);
        }
    }

    private void processDelayedMsg(TbContext ctx, EntityId entityId) {
        AggDeduplicationData deduplicationData = lastMsgMap.get(entityId);
        if (deduplicationData != null && deduplicationData.getMsg() != null) {
            doCalculate(ctx, deduplicationData.getMsg(), System.currentTimeMillis());
        }
    }

    private void processClearInactiveEntitiesMsg(TbContext ctx) {
        try {
            long inactiveTs = System.currentTimeMillis() - config.getDeduplicationInSec() * 1000L * CACHE_TTL_MULTIPLIER;
            lastMsgMap.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue().getTs() < inactiveTs);
        } finally {
            scheduleClearInactiveEntitiesMsg(ctx);
        }
    }

    private void scheduleClearInactiveEntitiesMsg(TbContext ctx) {
        long delayMs = Math.max(TimeUnit.HOURS.toMillis(1L), config.getDeduplicationInSec() * 1000L * CACHE_TTL_MULTIPLIER);
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG, ctx.getSelfId(), TbMsgMetaData.EMPTY, ""), delayMs);
    }

    private void scheduleDelayedMsg(TbContext ctx, EntityId entityId, long delayMs) {
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.TB_AGG_LATEST_SELF_MSG, entityId, TbMsgMetaData.EMPTY, ""), delayMs);
    }

    private void doCalculate(TbContext ctx, TbMsg msg, long ts) {
        lastMsgMap.put(msg.getOriginator(), new AggDeduplicationData(ts, null));
        ListenableFuture<List<EntityRelation>> relations;
        ListenableFuture<List<EntityId>> entityIds;
        if (EntitySearchDirection.TO.equals(config.getDirection())) {
            relations = ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON);
            entityIds = Futures.transform(relations, r -> r.stream().map(EntityRelation::getFrom).toList(), MoreExecutors.directExecutor());
        } else {
            relations = ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON);
            entityIds = Futures.transform(relations, r -> r.stream().map(EntityRelation::getTo).toList(), MoreExecutors.directExecutor());
        }
        ListenableFuture<JsonObject> resultFuture = Futures.transformAsync(entityIds, ids -> aggregateAll(ctx, ids), ctx.getDbCallbackExecutor());
        Futures.addCallback(resultFuture, new FutureCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                if (result == null) {
                    ctx.ack(msg);
                    return;
                }
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("ts", Long.toString(ts));
                ctx.enqueueForTellNext(TbMsg.newMsg().queueName(ctx.getQueueName()).type(config.getOutMsgType())
                        .originator(msg.getOriginator()).copyMetaData(metaData).data(gson.toJson((JsonElement) result)).build(), "Success");
                ctx.ack(msg);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<JsonObject> aggregateAll(TbContext ctx, List<EntityId> childIds) {
        if (childIds.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        List<ListenableFuture<java.util.Optional<JsonObject>>> mappingFutures = config.getAggMappings().stream()
                .map(mapping -> mapping.aggregate(ctx, Futures.immediateFuture(childIds)))
                .toList();
        return Futures.transform(Futures.allAsList(mappingFutures), results -> {
            JsonObject merged = new JsonObject();
            results.forEach(opt -> opt.ifPresent(obj -> obj.entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()))));
            return merged.entrySet().isEmpty() ? null : merged;
        }, (Executor) MoreExecutors.directExecutor());
    }

    @Override
    public void destroy() {
        lastMsgMap.clear();
    }

}
