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
package org.thingsboard.server.service.edge.rpc.processor.role;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.gen.edge.v1.RoleUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseRoleProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateRole(TenantId tenantId, RoleId entityId, RoleUpdateMsg updateMsg) {
        boolean isCreated = false;
        try {
            Role entity = JacksonUtil.fromString(updateMsg.getEntity(), Role.class, true);
            if (entity == null) {
                throw new RuntimeException("[{" + tenantId + "}] roleUpdateMsg {" + updateMsg + " } cannot be converted to role");
            }

            Role existing = edgeCtx.getRoleService().findRoleById(tenantId, entityId);
            if (existing == null) {
                entity.setCreatedTime(Uuids.unixTimestamp(entityId.getId()));
                isCreated = true;
            }

            entity.setId(entityId);
            entity.setTenantId(tenantId);
            edgeCtx.getRoleService().saveRole(tenantId, entity);
        } catch (Exception e) {
            log.error("[{}] Failed to process role update msg [{}]", tenantId, updateMsg, e);
            throw e;
        }
        return isCreated;
    }

    protected void deleteRole(TenantId tenantId, Edge edge, RoleId entityId) {
        Role entity = edgeCtx.getRoleService().findRoleById(tenantId, entityId);
        if (entity != null) {
            edgeCtx.getRoleService().deleteRole(tenantId, entityId);
            pushEntityEventToRuleEngine(tenantId, edge, entity, TbMsgType.ENTITY_DELETED);
        }
    }

}
