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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.SchedulerEventId;

import java.util.List;

@Schema
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventWithCustomerInfo extends SchedulerEventInfo {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    @Schema(description = "Title of the customer that owns the scheduler event", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;

    @Getter @Setter
    @Schema(description = "Indicates if the customer is public", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    // Fire timestamps within the requested time window; populated only by the calendar time-filter query.
    @Getter @Setter
    @Schema(description = "Event fire times within the requested time range, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    private List<Long> timestamps;

    public SchedulerEventWithCustomerInfo() {
        super();
    }

    public SchedulerEventWithCustomerInfo(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEventWithCustomerInfo(SchedulerEventInfo eventInfo, String customerTitle, boolean customerIsPublic) {
        super(eventInfo);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }

}
