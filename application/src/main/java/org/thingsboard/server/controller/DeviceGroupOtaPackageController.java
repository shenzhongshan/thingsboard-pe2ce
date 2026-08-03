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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * CE port of PE's device-group OTA package assignment API.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class DeviceGroupOtaPackageController extends BaseController {

    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @ApiOperation(value = "Get device group ota package (getDeviceGroupOtaPackage)",
            notes = "Returns the firmware/software package assigned to a DEVICE entity group, or null if none assigned.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/deviceGroupOtaPackage/{entityGroupId}/{type}"})
    public DeviceGroupOtaPackage getDeviceGroupOtaPackage(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable("entityGroupId") String strEntityGroupId,
            @Parameter(description = "FIRMWARE or SOFTWARE", required = true)
            @PathVariable("type") OtaPackageType type) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        return deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(
                getTenantId(), new EntityGroupId(toUUID(strEntityGroupId)), type);
    }

    @ApiOperation(value = "Save device group ota package (saveDeviceGroupOtaPackage)",
            notes = "Assigns a firmware/software package to a DEVICE entity group. Cascades the assignment " +
                    "onto every device currently in the group; devices added later are not retroactively updated.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/deviceGroupOtaPackage"})
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(
            @Parameter(description = "A JSON value representing the device group ota package", required = true)
            @RequestBody DeviceGroupOtaPackage deviceGroupOtaPackage) throws ThingsboardException {
        return deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(getTenantId(), deviceGroupOtaPackage);
    }

    @ApiOperation(value = "Delete device group ota package (deleteDeviceGroupOtaPackage)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/deviceGroupOtaPackage/{entityGroupId}/{type}"})
    public void deleteDeviceGroupOtaPackage(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable("entityGroupId") String strEntityGroupId,
            @Parameter(description = "FIRMWARE or SOFTWARE", required = true)
            @PathVariable("type") OtaPackageType type) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(getTenantId(), new EntityGroupId(toUUID(strEntityGroupId)), type);
    }

}
