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
package org.thingsboard.server.service.edge.rpc.processor.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.gen.edge.v1.CustomTranslationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CustomTranslationEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        try {
            CustomTranslation customTranslation = JacksonUtil.treeToValue(edgeEvent.getBody(), CustomTranslation.class);
            if (customTranslation == null) {
                return null;
            }
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            CustomTranslationUpdateMsg msg = EdgeMsgConstructorUtils.constructCustomTranslationMsg(msgType, customTranslation);
            return DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .setCustomTranslationUpdateMsg(msg).build();
        } catch (Exception e) {
            log.error("Can't process custom translation msg [{}]", edgeEvent, e);
            return null;
        }
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        JsonNode body = JacksonUtil.toJsonNode(edgeNotificationMsg.getBody());
        switch (entityId.getEntityType()) {
            case TENANT -> {
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    // 系统级翻译是所有租户的兜底，改动要推给每个租户的每台 edge。
                    PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(link -> edgeCtx.getTenantService().findTenantsIds(link), 1024);
                    for (TenantId currentTenantId : tenantIds) {
                        futures.addAll(processActionForAllEdgesByTenantId(currentTenantId, type, actionType, entityId, body, sourceEdgeId));
                    }
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, entityId, body, sourceEdgeId);
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            }
            case CUSTOMER -> {
                return processNotificationToRelatedEdges(tenantId, new CustomerId(entityId.getId()), entityId, type, actionType, sourceEdgeId, body);
            }
            default -> {
                return Futures.immediateFuture(null);
            }
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CUSTOM_TRANSLATION;
    }

}
