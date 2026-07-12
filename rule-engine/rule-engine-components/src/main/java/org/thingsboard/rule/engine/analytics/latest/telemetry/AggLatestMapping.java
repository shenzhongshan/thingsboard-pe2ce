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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.List;
import java.util.Optional;

/**
 * CE port note: PE's optional per-mapping TBEL/JS filter (AggLatestMappingFilter, restricting which
 * child entities feed into the aggregation) is dropped - it needs a ScriptEngine capability
 * (executeAttributesFilterAsync over a bare attribute map, not a TbMsg) that this CE port's
 * ScriptEngine interface doesn't expose. This mapping aggregates over all child entities found by
 * the relation query.
 */
@Data
public class AggLatestMapping {

    private String source;
    private String sourceScope;
    private double defaultValue;
    private String target;
    private MathFunction aggFunction;

    public ListenableFuture<Optional<JsonObject>> aggregate(TbContext ctx, ListenableFuture<List<EntityId>> entityIds) {
        return Futures.transform(entityIds, ids -> {
            var aggregation = org.thingsboard.rule.engine.analytics.latest.telemetry.TbAggFunctionFactory.createAggFunction(aggFunction);
            ids.forEach(entityId -> {
                Optional<KvEntry> entry = aggregation.fetchAttrValue() ? fetchValue(ctx, entityId) : Optional.empty();
                aggregation.update(entry, defaultValue);
            });
            Optional<JsonElement> result = aggregation.result();
            if (result.isPresent()) {
                JsonObject obj = new JsonObject();
                obj.add(target, result.get());
                return Optional.of(obj);
            }
            return Optional.<JsonObject>empty();
        }, ctx.getDbCallbackExecutor());
    }

    private Optional<KvEntry> fetchValue(TbContext ctx, EntityId entityId) {
        try {
            if ("LATEST_TELEMETRY".equals(sourceScope)) {
                List<org.thingsboard.server.common.data.kv.TsKvEntry> latestTs = ctx.getTimeseriesService()
                        .findLatest(ctx.getTenantId(), entityId, java.util.Collections.singletonList(source)).get();
                if (latestTs != null && !latestTs.isEmpty() && latestTs.get(0).getValue() != null) {
                    return Optional.of(latestTs.get(0));
                }
            } else {
                Optional<org.thingsboard.server.common.data.kv.AttributeKvEntry> latestAttr = ctx.getAttributesService()
                        .find(ctx.getTenantId(), entityId, org.thingsboard.server.common.data.AttributeScope.valueOf(sourceScope), source).get();
                if (latestAttr.isPresent()) {
                    return Optional.of(latestAttr.get());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch value of attribute/telemetry [" + source + "] of entity [" + entityId + "]", e);
        }
        return Optional.empty();
    }

}
