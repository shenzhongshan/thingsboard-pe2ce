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
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGE_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_UUID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RAW_DATA_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RAW_DATA_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RawDataEventEntity extends EventEntity<RawDataEvent> implements BaseEntity<RawDataEvent> {

    // named eventUuid, not uuid: BaseEntity already declares getUuid() returning UUID,
    // and a String uuid field would generate a clashing getter.
    @Column(name = EVENT_UUID_COLUMN_NAME)
    private String eventUuid;
    @Column(name = EVENT_MESSAGE_TYPE_COLUMN_NAME)
    private String messageType;
    @Column(name = EVENT_MESSAGE_COLUMN_NAME)
    private String message;

    public RawDataEventEntity(RawDataEvent event) {
        super(event);
        this.eventUuid = event.getUuid();
        this.messageType = event.getMessageType();
        this.message = event.getMessage();
    }

    @Override
    public RawDataEvent toData() {
        return RawDataEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .uuid(eventUuid)
                .messageType(messageType)
                .message(message).build();
    }

}
