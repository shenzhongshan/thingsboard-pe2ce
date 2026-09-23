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
package org.thingsboard.server.service.edge.rpc.processor.wl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 白标下发。一个 processor 服务三种事件类型（WHITE_LABELING / LOGIN_WHITE_LABELING /
 * MAIL_TEMPLATES）—— 见 EdgeContextComponent 里对这三者的登记。
 *
 * <p>ADDED/UPDATED 时 body 里只有归属实体 id，内容在这里现查；DELETED 时上游已经查不到了，
 * 所以 body 直接就是 WhiteLabeling 本身。
 */
@Slf4j
@Component
@TbCoreComponent
public class WhiteLabelingEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    protected WhiteLabelingService whiteLabelingService;

    @Autowired
    protected DomainService domainService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        try {
            switch (edgeEvent.getAction()) {
                case ADDED, UPDATED -> {
                    EntityId entityId = JacksonUtil.convertValue(edgeEvent.getBody(), EntityId.class);
                    if (entityId == null) {
                        return null;
                    }
                    TenantId tenantId = EntityType.TENANT.equals(entityId.getEntityType())
                            ? TenantId.fromUUID(entityId.getId()) : edgeEvent.getTenantId();
                    CustomerId customerId = EntityType.CUSTOMER.equals(entityId.getEntityType())
                            ? new CustomerId(entityId.getId()) : null;
                    WhiteLabeling whiteLabeling = whiteLabelingService.findByEntityId(tenantId, customerId, getWhiteLabelingType(edgeEvent.getType()));
                    if (whiteLabeling == null) {
                        return null;
                    }
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setWhiteLabelingProto(EdgeMsgConstructorUtils.constructWhiteLabeling(msgType, whiteLabeling));
                    // 登录页白标绑定的是某个域名；edge 上没有这个 Domain 就解析不出 OAuth2 入口。
                    if (WhiteLabelingType.LOGIN.equals(whiteLabeling.getType()) && whiteLabeling.getDomainId() != null) {
                        DomainInfo domainInfo = domainService.findDomainInfoById(tenantId, whiteLabeling.getDomainId());
                        if (domainInfo != null) {
                            OAuth2DomainUpdateMsg domainUpdateMsg = EdgeMsgConstructorUtils
                                    .constructOAuth2DomainUpdateMsg(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, domainInfo);
                            builder.addOAuth2DomainUpdateMsg(domainUpdateMsg);
                        }
                    }
                    return builder.build();
                }
                case DELETED -> {
                    WhiteLabeling whiteLabeling = JacksonUtil.convertValue(edgeEvent.getBody(), WhiteLabeling.class);
                    if (whiteLabeling == null) {
                        return null;
                    }
                    WhiteLabelingProto proto = EdgeMsgConstructorUtils
                            .constructWhiteLabeling(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, whiteLabeling);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setWhiteLabelingProto(proto).build();
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Can't process white labeling msg [{}]", edgeEvent, e);
            return null;
        }
    }

    private WhiteLabelingType getWhiteLabelingType(EdgeEventType type) {
        return switch (type) {
            case WHITE_LABELING -> WhiteLabelingType.GENERAL;
            case MAIL_TEMPLATES -> WhiteLabelingType.MAIL_TEMPLATES;
            case LOGIN_WHITE_LABELING -> WhiteLabelingType.LOGIN;
            default -> null;
        };
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId sourceEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        // 删除时内容随消息带过来，其余情况只带归属 id，由 convertEdgeEventToDownlink 现查。
        JsonNode body = actionType.equals(EdgeEventActionType.DELETED)
                ? JacksonUtil.toJsonNode(edgeNotificationMsg.getBody())
                : JacksonUtil.valueToTree(entityId);
        switch (entityId.getEntityType()) {
            case TENANT -> {
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    // 系统级白标变更要落到每个租户的每台 edge。
                    PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(link -> edgeCtx.getTenantService().findTenantsIds(link), 1024);
                    for (TenantId currentTenantId : tenantIds) {
                        futures.addAll(processActionForAllEdgesByTenantId(currentTenantId, type, actionType, null, body, sourceEdgeId));
                    }
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, null, body, sourceEdgeId);
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            }
            case CUSTOMER -> {
                return processNotificationToRelatedEdges(tenantId, new CustomerId(entityId.getId()), null, type, actionType, sourceEdgeId, body);
            }
            default -> {
                return Futures.immediateFuture(null);
            }
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.WHITE_LABELING;
    }

}
