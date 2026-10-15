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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to group by name",
        configClazz = TbDuplicateMsgToGroupByNameNodeConfiguration.class,
        version = 1,
        nodeDescription = "Duplicates message to all entities belonging to resolved Entity group",
        nodeDetails = "Entities are fetched from entity group that is detected according to the configuration. When " +
                "<b>\"search entity group on Tenant level only\"</b> is enabled, the search is restricted to the Tenant level only. " +
                "If <b>\"consider originator as a group owner\"</b> is enabled and the originator is a Tenant or Customer, the search " +
                "starts from the originator's level and goes up the hierarchy to the tenant level if the group isn't found. Otherwise, " +
                "the search starts at the same level as the message originator's owner. Entity group is dynamically resolved based on " +
                "its name and type. For each entity from group new message is created with entity as originator and message parameters " +
                "copied from original message.<br><br>Output connections: <code>Success</code>, <code>Failure</code>.",
        icon = "call_split"
)
public class TbDuplicateMsgToGroupByNameNode extends TbAbstractDuplicateMsgNode<TbDuplicateMsgToGroupByNameNodeConfiguration> {

    @Override
    protected TbDuplicateMsgToGroupByNameNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbDuplicateMsgToGroupByNameNodeConfiguration config = TbNodeUtils.convert(configuration, TbDuplicateMsgToGroupByNameNodeConfiguration.class);
        if (config.getGroupType() == null) {
            throw new IllegalArgumentException("Group type should be specified!");
        }
        if (StringUtils.isEmpty(config.getGroupName())) {
            throw new IllegalArgumentException("Group name should be specified!");
        }
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        return duplicate(ctx, msg);
    }

    @Override
    protected ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg) {
        String groupName = TbNodeUtils.processPattern(config.getGroupName(), msg);
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            var entityGroupId = detectTargetEntityGroupId(ctx, msg.getOriginator(), groupName);
            return ctx.findAllEntityIds(entityGroupId);
        });
    }

    private org.thingsboard.server.common.data.id.EntityGroupId detectTargetEntityGroupId(TbContext ctx, EntityId originator, String groupName) {
        if (config.isSearchEntityGroupForTenantOnly()) {
            return tryFindGroupByOwnerId(ctx, ctx.getTenantId(), groupName);
        }
        if (config.isConsiderMessageOriginatorAsAGroupOwner()
                && (originator.getEntityType() == EntityType.TENANT || originator.getEntityType() == EntityType.CUSTOMER)) {
            return tryFindGroupByOwnerId(ctx, originator, groupName);
        }
        EntityId ownerId = ctx.getOwner(originator);
        return tryFindGroupByOwnerId(ctx, ownerId != null ? ownerId : ctx.getTenantId(), groupName);
    }

    private org.thingsboard.server.common.data.id.EntityGroupId tryFindGroupByOwnerId(TbContext ctx, EntityId ownerId, String groupName) {
        CustomerId ownerCustomerId = EntityType.CUSTOMER.equals(ownerId.getEntityType()) ? new CustomerId(ownerId.getId()) : null;
        EntityGroup group = ctx.findEntityGroupByTypeAndName(ownerCustomerId, config.getGroupType(), groupName);
        if (group != null) {
            return group.getId();
        }
        if (!EntityType.TENANT.equals(ownerId.getEntityType())) {
            EntityId parentOwnerId = ctx.getOwner(ownerId);
            return tryFindGroupByOwnerId(ctx, parentOwnerId != null ? parentOwnerId : ctx.getTenantId(), groupName);
        }
        throw new RuntimeException("Can't find group with type: " + config.getGroupType() + " name: " + groupName + "!");
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        if (fromVersion == 0 && !oldConfiguration.has("considerMessageOriginatorAsAGroupOwner")) {
            hasChanges = true;
            ((ObjectNode) oldConfiguration).put("considerMessageOriginatorAsAGroupOwner", false);
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
