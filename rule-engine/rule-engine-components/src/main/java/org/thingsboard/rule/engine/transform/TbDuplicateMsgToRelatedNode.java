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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to related",
        configClazz = TbDuplicateMsgToRelatedNodeConfiguration.class,
        nodeDescription = "Duplicates message to related entities fetched by relation query",
        nodeDetails = "Related entities found using configured relation direction and Relation type. For each found related entity " +
                "new message is created with related entity as originator and message parameters copied from original message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        icon = "call_split"
)
public class TbDuplicateMsgToRelatedNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToRelatedNodeConfiguration> {

    @Override
    protected TbDuplicateMsgToRelatedNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbDuplicateMsgToRelatedNodeConfiguration config = TbNodeUtils.convert(configuration, TbDuplicateMsgToRelatedNodeConfiguration.class);
        if (config.getRelationsQuery() == null) {
            throw new IllegalArgumentException("Relation query should be specified!");
        }
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        return duplicate(ctx, msg);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityId>> newOriginatorsFuture =
                EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(ctx, msg.getOriginator(), config.getRelationsQuery());
        return Futures.transform(newOriginatorsFuture, newOriginators -> {
            if (newOriginators == null || newOriginators.isEmpty()) {
                throw new RuntimeException("No related entities were found!");
            }
            return newOriginators;
        }, MoreExecutors.directExecutor());
    }

}
