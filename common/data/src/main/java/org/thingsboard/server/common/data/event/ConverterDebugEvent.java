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
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class ConverterDebugEvent extends Event {

    private static final long serialVersionUID = -3474895485648622237L;

    @Builder
    private ConverterDebugEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts,
                                String eventType, String inMsgType, String inMsg,
                                String outMsgType, String outMsg, String metadata, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.eventType = eventType;
        this.inMsgType = inMsgType;
        this.inMsg = inMsg;
        this.outMsgType = outMsgType;
        this.outMsg = outMsg;
        this.metadata = metadata;
        this.error = error;
    }

    @Getter
    private final String eventType;
    @Getter
    private final String inMsgType;
    @Getter
    @Setter
    private String inMsg;
    @Getter
    private final String outMsgType;
    @Getter
    @Setter
    private String outMsg;
    @Getter
    @Setter
    private String metadata;
    @Getter
    @Setter
    private String error;

    @Override
    public EventType getType() {
        return EventType.DEBUG_CONVERTER;
    }

    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("type", eventType);
        putNotNull(json, "inMessageType", inMsgType);
        putNotNull(json, "in", inMsg);
        putNotNull(json, "outMessageType", outMsgType);
        putNotNull(json, "out", outMsg);
        putNotNull(json, "metadata", metadata);
        putNotNull(json, "error", error);
        return eventInfo;
    }

}
