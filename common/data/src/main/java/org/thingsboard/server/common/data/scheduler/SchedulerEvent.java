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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.SchedulerEventId;

@Schema
@EqualsAndHashCode(callSuper = true)
public class SchedulerEvent extends SchedulerEventInfo {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    @Schema(description = "a JSON value representing the scheduler event configuration (originatorId, msgType, msgBody, metadata)", implementation = JsonNode.class)
    private transient JsonNode configuration;

    public SchedulerEvent() {
        super();
    }

    public SchedulerEvent(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEvent(SchedulerEvent event) {
        super(event);
        this.configuration = event.getConfiguration();
    }

    public SchedulerEvent(SchedulerEventInfo schedulerEventInfo, JsonNode configuration) {
        super(schedulerEventInfo);
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return "SchedulerEvent [" + super.toString() + ", configuration=" + configuration + "]";
    }

}
