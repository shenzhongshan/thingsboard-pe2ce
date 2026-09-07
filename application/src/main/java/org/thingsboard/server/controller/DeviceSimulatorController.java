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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.simulator.DeviceSimulatorInfo;
import org.thingsboard.server.service.simulator.DeviceSimulatorService;
import org.thingsboard.server.service.simulator.RunMode;

import java.util.List;

/**
 * A standalone device-simulator manager, independent of the solution-templates feature: users
 * configure telemetry generators for their own devices (same JSON shape as a solution template's
 * {@code device_emulators.json}) and start/stop them on demand.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/device-simulators")
@RequiredArgsConstructor
@Slf4j
public class DeviceSimulatorController extends BaseController {

    public static final String DEVICE_SIMULATOR_ID = "deviceSimulatorId";

    private final DeviceSimulatorService deviceSimulatorService;

    @ApiOperation(value = "Get device simulators (getDeviceSimulators)",
            notes = "Lists this tenant's device simulator configs, each merged with its current run status.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping
    public List<DeviceSimulatorInfo> getDeviceSimulators() throws ThingsboardException {
        return deviceSimulatorService.findInfosByTenantId(getTenantId());
    }

    @ApiOperation(value = "Get device simulator by id (getDeviceSimulatorById)",
            notes = "Returns the full config (including definition JSON) plus its current run status.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{deviceSimulatorId}")
    public DeviceSimulatorInfo getDeviceSimulatorById(
            @Parameter(description = "Device simulator id", required = true)
            @PathVariable(DEVICE_SIMULATOR_ID) String strDeviceSimulatorId) throws ThingsboardException {
        checkParameter(DEVICE_SIMULATOR_ID, strDeviceSimulatorId);
        return deviceSimulatorService.findInfoById(getTenantId(), DeviceSimulatorId.fromString(strDeviceSimulatorId));
    }

    @ApiOperation(value = "Create device simulator (saveDeviceSimulator)",
            notes = "Creates a new device simulator config. The definition field must match " +
                    "the solution templates' EmulatorDefinition JSON shape.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping
    public DeviceSimulator saveDeviceSimulator(@RequestBody DeviceSimulator simulator) throws ThingsboardException {
        simulator.setId(null);
        return deviceSimulatorService.save(getTenantId(), simulator);
    }

    @ApiOperation(value = "Update device simulator (updateDeviceSimulator)",
            notes = "Updates an existing device simulator config. Fails if the simulator is currently running.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PutMapping("/{deviceSimulatorId}")
    public DeviceSimulator updateDeviceSimulator(
            @Parameter(description = "Device simulator id", required = true)
            @PathVariable(DEVICE_SIMULATOR_ID) String strDeviceSimulatorId,
            @RequestBody DeviceSimulator simulator) throws ThingsboardException {
        checkParameter(DEVICE_SIMULATOR_ID, strDeviceSimulatorId);
        simulator.setId(DeviceSimulatorId.fromString(strDeviceSimulatorId));
        return deviceSimulatorService.save(getTenantId(), simulator);
    }

    @ApiOperation(value = "Delete device simulator (deleteDeviceSimulator)",
            notes = "Stops the simulator if it is running, then deletes its config.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{deviceSimulatorId}")
    public void deleteDeviceSimulator(
            @Parameter(description = "Device simulator id", required = true)
            @PathVariable(DEVICE_SIMULATOR_ID) String strDeviceSimulatorId) throws ThingsboardException {
        checkParameter(DEVICE_SIMULATOR_ID, strDeviceSimulatorId);
        deviceSimulatorService.delete(getTenantId(), DeviceSimulatorId.fromString(strDeviceSimulatorId));
    }

    @ApiOperation(value = "Start device simulator (startDeviceSimulator)",
            notes = "Starts publishing telemetry for the target device. mode=LIVE publishes forward from now " +
                    "until stopped; mode=HISTORICAL backfills the configured window and stops on its own.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{deviceSimulatorId}/start")
    public DeviceSimulatorInfo startDeviceSimulator(
            @Parameter(description = "Device simulator id", required = true)
            @PathVariable(DEVICE_SIMULATOR_ID) String strDeviceSimulatorId,
            @Parameter(description = "Run mode", required = true)
            @RequestParam RunMode mode) throws ThingsboardException {
        checkParameter(DEVICE_SIMULATOR_ID, strDeviceSimulatorId);
        return deviceSimulatorService.start(getTenantId(), DeviceSimulatorId.fromString(strDeviceSimulatorId), mode);
    }

    @ApiOperation(value = "Stop device simulator (stopDeviceSimulator)",
            notes = "Stops a running simulator. No-op if it is already stopped.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/{deviceSimulatorId}/stop")
    public DeviceSimulatorInfo stopDeviceSimulator(
            @Parameter(description = "Device simulator id", required = true)
            @PathVariable(DEVICE_SIMULATOR_ID) String strDeviceSimulatorId) throws ThingsboardException {
        checkParameter(DEVICE_SIMULATOR_ID, strDeviceSimulatorId);
        return deviceSimulatorService.stop(getTenantId(), DeviceSimulatorId.fromString(strDeviceSimulatorId));
    }

    @ApiOperation(value = "Get device simulator status (getDeviceSimulatorStatus)",
            notes = "Lightweight polling endpoint returning just the run status, without the definition JSON.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{deviceSimulatorId}/status")
    public DeviceSimulatorInfo getDeviceSimulatorStatus(
            @Parameter(description = "Device simulator id", required = true)
            @PathVariable(DEVICE_SIMULATOR_ID) String strDeviceSimulatorId) throws ThingsboardException {
        checkParameter(DEVICE_SIMULATOR_ID, strDeviceSimulatorId);
        return deviceSimulatorService.findInfoById(getTenantId(), DeviceSimulatorId.fromString(strDeviceSimulatorId));
    }

}
