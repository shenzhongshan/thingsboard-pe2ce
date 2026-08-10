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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.ConverterDebugEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_DEBUG_EVENT_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_IN_MSG_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_IN_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_METADATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_OUT_MSG_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_OUT_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TYPE_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CONVERTER_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class ConverterDebugEventEntity extends EventEntity<ConverterDebugEvent> implements BaseEntity<ConverterDebugEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_IN_MSG_TYPE_COLUMN_NAME)
    private String inMsgType;
    @Column(name = EVENT_IN_MSG_COLUMN_NAME)
    private String inMsg;
    @Column(name = EVENT_OUT_MSG_TYPE_COLUMN_NAME)
    private String outMsgType;
    @Column(name = EVENT_OUT_MSG_COLUMN_NAME)
    private String outMsg;
    @Column(name = EVENT_METADATA_COLUMN_NAME)
    private String metadata;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public ConverterDebugEventEntity(ConverterDebugEvent event) {
        super(event);
        this.eventType = event.getEventType();
        this.inMsgType = event.getInMsgType();
        this.inMsg = event.getInMsg();
        this.outMsgType = event.getOutMsgType();
        this.outMsg = event.getOutMsg();
        this.metadata = event.getMetadata();
        this.error = event.getError();
    }

    @Override
    public ConverterDebugEvent toData() {
        return ConverterDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .eventType(eventType)
                .inMsgType(inMsgType)
                .inMsg(inMsg)
                .outMsgType(outMsgType)
                .outMsg(outMsg)
                .metadata(metadata)
                .error(error).build();
    }

}
