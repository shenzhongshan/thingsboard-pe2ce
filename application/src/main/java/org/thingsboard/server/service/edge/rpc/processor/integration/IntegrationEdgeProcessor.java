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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;


@Slf4j
@Component
@TbCoreComponent
public class IntegrationEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        IntegrationId entityId = new IntegrationId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Integration entity = edgeCtx.getIntegrationService().findIntegrationById(edgeEvent.getTenantId(), entityId);
                if (entity != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    // 模板里的 ${{attrKey}} 用这台 edge 自己的 SERVER_SCOPE 属性替换 ——
                    // 一份模板下发到 N 台 edge，每台拿到的是自己的 broker 地址/凭据。
                    Integration copy = new Integration(entity);
                    copy.setConfiguration(replaceAttributePlaceholders(edgeEvent, entity.getConfiguration()));
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addIntegrationMsg(EdgeMsgConstructorUtils.constructIntegrationUpdatedMsg(msgType, copy));
                    // 转换器随集成一起下发：edge 拿到集成时它引用的转换器必须已在场，
                    // 否则是个引用悬空的集成。
                    addConverterMsgs(builder, edgeEvent.getTenantId(), entity, msgType);
                    return builder.build();
                }
            }
            case DELETED -> {
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addIntegrationMsg(EdgeMsgConstructorUtils.constructIntegrationDeleteMsg(entityId))
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.INTEGRATION;
    }


    private void addConverterMsgs(DownlinkMsg.Builder builder, TenantId tenantId, Integration integration, UpdateMsgType msgType) {
        if (integration.getDefaultConverterId() != null) {
            Converter uplink = edgeCtx.getConverterService().findConverterById(tenantId, integration.getDefaultConverterId());
            if (uplink != null) {
                builder.addConverterMsg(EdgeMsgConstructorUtils.constructConverterUpdatedMsg(msgType, uplink));
            }
        }
        if (integration.getDownlinkConverterId() != null) {
            Converter downlink = edgeCtx.getConverterService().findConverterById(tenantId, integration.getDownlinkConverterId());
            if (downlink != null) {
                builder.addConverterMsg(EdgeMsgConstructorUtils.constructConverterUpdatedMsg(msgType, downlink));
            }
        }
    }

    /**
     * 把配置里的 ${{attrKey}} 换成该 edge 的同名 SERVER_SCOPE 属性值。
     * 没有占位符时原样返回，不做无谓的 JSON 往返。
     */
    private JsonNode replaceAttributePlaceholders(EdgeEvent edgeEvent, JsonNode configuration) {
        if (configuration == null) {
            return null;
        }
        String raw = configuration.toString();
        Set<String> keys = EdgeUtils.getAttributeKeysFromConfiguration(raw);
        if (keys.isEmpty()) {
            return configuration;
        }
        try {
            List<AttributeKvEntry> attributes = edgeCtx.getAttributesService()
                    .find(edgeEvent.getTenantId(), edgeEvent.getEdgeId(), AttributeScope.SERVER_SCOPE, keys).get();
            String updated = raw;
            for (AttributeKvEntry attribute : attributes) {
                updated = updated.replaceAll(
                        EdgeUtils.formatAttributeKeyToRegexpPlaceholderFormat(attribute.getKey()),
                        Matcher.quoteReplacement(attribute.getValueAsString()));
            }
            return JacksonUtil.toJsonNode(updated);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[{}] Interrupted while resolving edge attributes for integration template", edgeEvent.getTenantId(), e);
            return configuration;
        } catch (Exception e) {
            // 属性查不到时保留原配置：下发一个带字面量占位符的配置，比下发一个被截断的配置
            // 更容易在 edge 侧被发现。
            log.error("[{}] Failed to resolve edge attributes for integration template", edgeEvent.getTenantId(), e);
            return configuration;
        }
    }

}
