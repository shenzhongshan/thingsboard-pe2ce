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
package org.thingsboard.server.service.edge.rpc.processor.converter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class ConverterEdgeProcessor extends BaseConverterProcessor implements ConverterProcessor {

    @Override
    public ListenableFuture<Void> processConverterMsgFromEdge(TenantId tenantId, Edge edge, ConverterUpdateMsg updateMsg) {
        ConverterId entityId = new ConverterId(new UUID(updateMsg.getIdMSB(), updateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (updateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateConverter(tenantId, entityId, updateMsg);
                    if (created) {
                        Converter entity = edgeCtx.getConverterService().findConverterById(tenantId, entityId);
                        if (entity != null) {
                            pushEntityEventToRuleEngine(tenantId, edge, entity, TbMsgType.ENTITY_CREATED);
                        }
                    }
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteConverter(tenantId, edge, entityId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(updateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        ConverterId entityId = new ConverterId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Converter entity = edgeCtx.getConverterService().findConverterById(edgeEvent.getTenantId(), entityId);
                if (entity != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addConverterUpdateMsg(EdgeMsgConstructorUtils.constructConverterUpdatedMsg(msgType, entity))
                            .build();
                }
            }
            case DELETED -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addConverterUpdateMsg(EdgeMsgConstructorUtils.constructConverterDeleteMsg(entityId))
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.CONVERTER;
    }

}
