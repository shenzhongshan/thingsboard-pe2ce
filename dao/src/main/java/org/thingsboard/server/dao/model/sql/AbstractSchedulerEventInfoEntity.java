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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractSchedulerEventInfoEntity<T extends SchedulerEventInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "originator_id")
    private UUID originatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "originator_type")
    private EntityType originatorType;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Convert(converter = JsonConverter.class)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    @Convert(converter = JsonConverter.class)
    @Column(name = "schedule")
    private JsonNode schedule;

    @Column(name = "enabled")
    private boolean enabled;

    public AbstractSchedulerEventInfoEntity() {
        super();
    }

    public AbstractSchedulerEventInfoEntity(SchedulerEventInfo schedulerEventInfo) {
        if (schedulerEventInfo.getId() != null) {
            this.setUuid(schedulerEventInfo.getId().getId());
        }
        this.setCreatedTime(schedulerEventInfo.getCreatedTime());
        if (schedulerEventInfo.getTenantId() != null) {
            this.tenantId = schedulerEventInfo.getTenantId().getId();
        }
        if (schedulerEventInfo.getCustomerId() != null) {
            this.customerId = schedulerEventInfo.getCustomerId().getId();
        }
        if (schedulerEventInfo.getOriginatorId() != null) {
            this.originatorId = schedulerEventInfo.getOriginatorId().getId();
            this.originatorType = schedulerEventInfo.getOriginatorId().getEntityType();
        }
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.additionalInfo = schedulerEventInfo.getAdditionalInfo();
        this.schedule = schedulerEventInfo.getSchedule();
        this.enabled = schedulerEventInfo.isEnabled();
    }

    public AbstractSchedulerEventInfoEntity(AbstractSchedulerEventInfoEntity<?> other) {
        this.setId(other.getId());
        this.setCreatedTime(other.getCreatedTime());
        this.tenantId = other.getTenantId();
        this.customerId = other.getCustomerId();
        this.originatorId = other.getOriginatorId();
        this.originatorType = other.getOriginatorType();
        this.name = other.getName();
        this.type = other.getType();
        this.additionalInfo = other.getAdditionalInfo();
        this.schedule = other.getSchedule();
        this.enabled = other.isEnabled();
    }

    protected SchedulerEventInfo toSchedulerEventInfo() {
        SchedulerEventInfo schedulerEventInfo = new SchedulerEventInfo(new SchedulerEventId(id));
        schedulerEventInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            schedulerEventInfo.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            schedulerEventInfo.setCustomerId(new CustomerId(customerId));
        }
        if (originatorId != null && originatorType != null) {
            schedulerEventInfo.setOriginatorId(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        }
        schedulerEventInfo.setName(name);
        schedulerEventInfo.setType(type);
        schedulerEventInfo.setAdditionalInfo(additionalInfo);
        schedulerEventInfo.setSchedule(schedule);
        schedulerEventInfo.setEnabled(enabled);
        return schedulerEventInfo;
    }

}
