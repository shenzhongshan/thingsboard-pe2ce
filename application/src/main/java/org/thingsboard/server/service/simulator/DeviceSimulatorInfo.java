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
package org.thingsboard.server.service.simulator;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;

/** A {@link org.thingsboard.server.common.data.simulator.DeviceSimulator} merged with its in-memory run status. */
@Data
@Schema
public class DeviceSimulatorInfo {

    private DeviceSimulatorId id;
    private long createdTime;
    private String name;
    private DeviceId deviceId;
    private String deviceName;
    private JsonNode definition;

    private boolean running;
    private RunMode mode;
    private Long startedTime;
    private long publishedCount;
    private String lastError;

}
