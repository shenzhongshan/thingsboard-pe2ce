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

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@RuleNode(
        type = ComponentType.ACTION,
        name = "add to group",
        configClazz = TbAddToGroupConfiguration.class,
        nodeDescription = "Adds Message Originator Entity to Entity Group",
        nodeDetails = "Finds target Entity Group by group name pattern and then adds Originator Entity to this group. " +
                "Will create new Entity Group if it doesn't exist and 'Create new group if not exists' is set to true.",
        icon = "add_circle"
)
public class TbAddToGroupNode extends TbAbstractGroupActionNode<TbAddToGroupConfiguration> {

    @Override
    protected boolean createGroupIfNotExists() {
        return config.isCreateGroupIfNotExists();
    }

    @Override
    protected TbAddToGroupConfiguration loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAddToGroupConfiguration.class);
    }

    @Override
    protected void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        EntityId originatorId = msg.getOriginator();
        if (config.isRemoveFromCurrentGroups()) {
            EntityId ownerId = ctx.getOwner(originatorId);
            CustomerId ownerCustomerId = ownerId != null && ownerId.getEntityType() == org.thingsboard.server.common.data.EntityType.CUSTOMER
                    ? new CustomerId(ownerId.getId()) : null;
            List<EntityGroupId> currentGroups = ctx.findEntityGroupsForEntity(originatorId, ownerCustomerId);
            for (EntityGroupId groupId : currentGroups) {
                if (groupId.equals(entityGroupId)) {
                    continue;
                }
                try {
                    ctx.removeEntityFromEntityGroup(groupId, originatorId);
                } catch (Exception e) {
                    // the virtual "All" group (and any other non-removable group) is skipped
                }
            }
        }
        ctx.addEntityToEntityGroup(entityGroupId, originatorId);
    }

}
