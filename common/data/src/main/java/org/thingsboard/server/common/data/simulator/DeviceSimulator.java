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
package org.thingsboard.server.common.data.simulator;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

/**
 * A user-authored telemetry generator config bound to one device. {@code definition} is the same
 * JSON shape used by the solution templates' {@code device_emulators.json} files
 * ({@code org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition}), stored
 * verbatim so it can be parsed by that existing code at run time without a second DTO layer.
 */
@Data
@Schema
public class DeviceSimulator implements Serializable {

    private static final long serialVersionUID = 1L;

    private DeviceSimulatorId id;
    private long createdTime;
    private TenantId tenantId;
    private DeviceId deviceId;
    private String name;
    @Schema(description = "EmulatorDefinition-shaped JSON (see solution templates' device_emulators.json format)")
    private JsonNode definition;

}
