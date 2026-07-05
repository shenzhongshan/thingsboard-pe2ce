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
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * CE port of PE's scheduler event metadata (no configuration payload — see {@link SchedulerEvent}).
 * CE simplifications: no EntityType.SCHEDULER_EVENT, no external id / version (no VC export),
 * no edge assignment.
 */
@Slf4j
@Schema
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventInfo extends BaseDataWithAdditionalInfo<SchedulerEventId> implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 2807343040519549363L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @Schema(description = "JSON object with Customer Id", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @Getter @Setter
    @Schema(description = "JSON object with the target (originator) entity Id")
    private EntityId originatorId;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "scheduler event name", example = "Nightly attributes update")
    private String name;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "type")
    @Schema(description = "scheduler event type", example = "updateAttributes")
    private String type;

    @Getter @Setter
    @Schema(description = "a JSON value with schedule time configuration", implementation = JsonNode.class)
    private JsonNode schedule;

    @Getter @Setter
    @Schema(description = "Enable/disable scheduler", example = "true")
    private boolean enabled = true;

    public SchedulerEventInfo() {
        super();
    }

    public SchedulerEventInfo(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEventInfo(SchedulerEventInfo schedulerEventInfo) {
        super(schedulerEventInfo);
        this.tenantId = schedulerEventInfo.getTenantId();
        this.customerId = schedulerEventInfo.getCustomerId();
        this.originatorId = schedulerEventInfo.getOriginatorId();
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.enabled = schedulerEventInfo.isEnabled();
        this.schedule = schedulerEventInfo.getSchedule();
    }

    @Override
    @Schema(description = "JSON object with the scheduler event Id. Omit this field to create a new scheduler event")
    public SchedulerEventId getId() {
        return super.getId();
    }

    @Override
    @Schema(description = "Timestamp of the scheduler event creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getName() {
        return name;
    }

    @JsonIgnore
    public SchedulerEventDescriptor toDescriptor() {
        long startTime = schedule.get("startTime").asLong();
        String timezone = schedule.get("timezone").asText();
        JsonNode repeatNode = schedule.get("repeat");
        SchedulerRepeat repeat = null;
        if (repeatNode != null && !repeatNode.isNull()) {
            try {
                repeat = mapper.treeToValue(repeatNode, SchedulerRepeat.class);
            } catch (Exception e) {
                log.error("Failed to read scheduler config for {}", this, e);
            }
        }
        return new SchedulerEventDescriptor(startTime, timezone, repeat);
    }

    @Override
    public String toString() {
        return "SchedulerEventInfo [tenantId=" + tenantId + ", customerId=" + customerId
                + ", originatorId=" + originatorId + ", name=" + name + ", type=" + type
                + ", enabled=" + enabled + ", schedule=" + schedule + "]";
    }

}
