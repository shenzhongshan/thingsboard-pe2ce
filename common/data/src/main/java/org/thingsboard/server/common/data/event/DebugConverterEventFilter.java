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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema
public class DebugConverterEventFilter extends DebugEventFilter {

    @Schema(description = "String value representing the event type", example = "UPLINK")
    protected String type;

    @Schema(description = "String value representing the inbound message")
    protected String in;

    @Schema(description = "String value representing the outbound message")
    protected String out;

    @Schema(description = "String value representing the message metadata")
    protected String metadata;

    @Override
    public EventType getEventType() {
        return EventType.DEBUG_CONVERTER;
    }

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(type) || !StringUtils.isEmpty(in)
                || !StringUtils.isEmpty(out) || !StringUtils.isEmpty(metadata);
    }
}
