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
package org.thingsboard.server.service.edge.rpc.processor.integration;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseIntegrationProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateIntegration(TenantId tenantId, IntegrationId entityId, IntegrationUpdateMsg updateMsg) {
        boolean isCreated = false;
        try {
            Integration entity = JacksonUtil.fromString(updateMsg.getEntity(), Integration.class, true);
            if (entity == null) {
                throw new RuntimeException("[{" + tenantId + "}] integrationUpdateMsg {" + updateMsg + " } cannot be converted to integration");
            }

            Integration existing = edgeCtx.getIntegrationService().findIntegrationById(tenantId, entityId);
            if (existing == null) {
                entity.setCreatedTime(Uuids.unixTimestamp(entityId.getId()));
                isCreated = true;
            }

            entity.setId(entityId);
            entity.setTenantId(tenantId);
            edgeCtx.getIntegrationService().saveIntegration(entity);
        } catch (Exception e) {
            log.error("[{}] Failed to process integration update msg [{}]", tenantId, updateMsg, e);
            throw e;
        }
        return isCreated;
    }

    protected void deleteIntegration(TenantId tenantId, Edge edge, IntegrationId entityId) {
        Integration entity = edgeCtx.getIntegrationService().findIntegrationById(tenantId, entityId);
        if (entity != null) {
            edgeCtx.getIntegrationService().deleteIntegration(tenantId, entityId);
            pushEntityEventToRuleEngine(tenantId, edge, entity, TbMsgType.ENTITY_DELETED);
        }
    }

}
