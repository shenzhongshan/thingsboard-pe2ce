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
package org.thingsboard.server.service.edge.rpc.processor.secret;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseSecretProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateSecret(TenantId tenantId, SecretId entityId, SecretUpdateMsg updateMsg) {
        boolean isCreated = false;
        try {
            Secret entity = JacksonUtil.fromString(updateMsg.getEntity(), Secret.class, true);
            if (entity == null) {
                throw new RuntimeException("[{" + tenantId + "}] secretUpdateMsg {" + updateMsg + " } cannot be converted to secret");
            }

            Secret existing = edgeCtx.getSecretService().findSecretById(tenantId, entityId);
            if (existing == null) {
                entity.setCreatedTime(Uuids.unixTimestamp(entityId.getId()));
                isCreated = true;
            }

            entity.setId(entityId);
            entity.setTenantId(tenantId);
            edgeCtx.getSecretService().saveSecret(tenantId, entity);
        } catch (Exception e) {
            log.error("[{}] Failed to process secret update msg [{}]", tenantId, updateMsg, e);
            throw e;
        }
        return isCreated;
    }

    protected void deleteSecret(TenantId tenantId, Edge edge, SecretId entityId) {
        Secret entity = edgeCtx.getSecretService().findSecretById(tenantId, entityId);
        if (entity != null) {
            edgeCtx.getSecretService().deleteSecret(tenantId, edgeCtx.getSecretService().findSecretInfoById(tenantId, entityId));
            pushEntityEventToRuleEngine(tenantId, edge, entity, TbMsgType.ENTITY_DELETED);
        }
    }

}
