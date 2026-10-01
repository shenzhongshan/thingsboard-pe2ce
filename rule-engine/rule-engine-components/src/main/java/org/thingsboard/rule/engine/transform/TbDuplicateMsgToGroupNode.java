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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group",
        configClazz = TbDuplicateMsgToGroupNodeConfiguration.class,
        version = 1,
        nodeDescription = "Duplicates message to all entities belonging to specific entity group",
        nodeDetails = "Entities are fetched from entity group that is detected according to the configuration. Entity group can be " +
                "specified directly or can be message originator entity itself. For each entity from group new message is created " +
                "with entity as originator and message parameters copied from original message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        icon = "call_split"
)
public class TbDuplicateMsgToGroupNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToGroupNodeConfiguration> {

    @Override
    protected TbDuplicateMsgToGroupNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbDuplicateMsgToGroupNodeConfiguration config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupNodeConfiguration.class);
        if (!config.isEntityGroupIsMessageOriginator()) {
            if (config.getEntityGroupId() == null || config.getEntityGroupId().isNullUid()) {
                throw new IllegalArgumentException("EntityGroupId should be specified!");
            }
            ctx.checkTenantEntity(config.getEntityGroupId());
        }
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        return duplicate(ctx, msg);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg) {
        EntityGroupId entityGroupId = detectTargetEntityGroupId(msg.getOriginator());
        return ctx.getDbCallbackExecutor().executeAsync(() -> ctx.findAllEntityIds(entityGroupId));
    }

    private EntityGroupId detectTargetEntityGroupId(EntityId original) {
        if (config.isEntityGroupIsMessageOriginator()) {
            if (EntityType.ENTITY_GROUP.equals(original.getEntityType())) {
                return new EntityGroupId(original.getId());
            }
            throw new RuntimeException("Message originator is not an entity group!");
        }
        return config.getEntityGroupId();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has("groupOwnerId")) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove("groupOwnerId");
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
