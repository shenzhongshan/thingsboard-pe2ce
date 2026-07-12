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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * CE port of PE's group-action base node. Simplified vs PE: no in-memory group-id cache (direct
 * lookup per message - group membership changes are infrequent rule-engine actions, not a
 * telemetry-rate hot path), and no DeviceGroupOtaPackage firmware/software refresh side effect
 * (per-group OTA assignment was never ported into this CE build).
 */
public abstract class TbAbstractGroupActionNode<C extends TbAbstractGroupActionConfiguration> implements TbNode {

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadGroupNodeActionConfig(configuration);
    }

    protected abstract boolean createGroupIfNotExists();

    protected abstract C loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processEntityGroupAction(ctx, msg),
                v -> ctx.tellNext(msg, "Success"),
                t -> ctx.tellFailure(msg, t),
                ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> processEntityGroupAction(TbContext ctx, TbMsg msg) {
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityId ownerId = ctx.getOwner(msg.getOriginator());
            EntityGroupId entityGroupId = getEntityGroup(ctx, msg, ownerId != null ? ownerId : ctx.getTenantId());
            doProcessEntityGroupAction(ctx, msg, entityGroupId);
            return null;
        });
    }

    protected abstract void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId);

    private EntityGroupId getEntityGroup(TbContext ctx, TbMsg msg, EntityId ownerId) {
        String groupName = TbNodeUtils.processPattern(config.getGroupNamePattern(), msg);
        EntityType groupType = msg.getOriginator().getEntityType();
        CustomerId ownerCustomerId = EntityType.CUSTOMER.equals(ownerId.getEntityType()) ? new CustomerId(ownerId.getId()) : null;
        EntityGroup group = ctx.findEntityGroupByTypeAndName(ownerCustomerId, groupType, groupName);
        if (group != null) {
            return group.getId();
        }
        if (createGroupIfNotExists()) {
            EntityGroup newGroup = new EntityGroup();
            newGroup.setName(groupName);
            newGroup.setType(groupType);
            return ctx.saveEntityGroup(ownerCustomerId, newGroup).getId();
        }
        throw new RuntimeException("No entity group found with type '" + groupType + "' and name '" + groupName + "'.");
    }

    @Override
    public void destroy() {
    }

}
