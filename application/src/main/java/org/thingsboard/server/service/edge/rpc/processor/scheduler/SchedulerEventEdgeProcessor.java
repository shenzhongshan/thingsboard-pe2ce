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
package org.thingsboard.server.service.edge.rpc.processor.scheduler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class SchedulerEventEdgeProcessor extends BaseSchedulerEventProcessor implements SchedulerEventProcessor {

    @Override
    public ListenableFuture<Void> processSchedulerEventMsgFromEdge(TenantId tenantId, Edge edge, SchedulerEventUpdateMsg updateMsg) {
        SchedulerEventId entityId = new SchedulerEventId(new UUID(updateMsg.getIdMSB(), updateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (updateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateSchedulerEvent(tenantId, entityId, updateMsg);
                    if (created) {
                        SchedulerEvent entity = edgeCtx.getSchedulerEventService().findSchedulerEventById(tenantId, entityId);
                        if (entity != null) {
                            pushEntityEventToRuleEngine(tenantId, edge, entity, TbMsgType.ENTITY_CREATED);
                        }
                    }
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteSchedulerEvent(tenantId, edge, entityId);
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
        SchedulerEventId entityId = new SchedulerEventId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                SchedulerEvent entity = edgeCtx.getSchedulerEventService().findSchedulerEventById(edgeEvent.getTenantId(), entityId);
                if (entity != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addSchedulerEventUpdateMsg(EdgeMsgConstructorUtils.constructSchedulerEventUpdatedMsg(msgType, entity))
                            .build();
                }
            }
            case DELETED -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addSchedulerEventUpdateMsg(EdgeMsgConstructorUtils.constructSchedulerEventDeleteMsg(entityId))
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.SCHEDULER_EVENT;
    }

}
