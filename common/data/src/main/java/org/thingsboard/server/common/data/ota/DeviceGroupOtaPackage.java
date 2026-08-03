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
package org.thingsboard.server.common.data.ota;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * CE port of PE's device-group OTA package assignment: the firmware/software package a DEVICE-type
 * entity group distributes to its member devices. Plain POJO (not a BaseData/EntityId-backed entity)
 * matching PE's decompiled shape — this is a per-group settings row, not a first-class entity type.
 */
@Schema
@Data
public class DeviceGroupOtaPackage {

    private UUID id;
    private TenantId tenantId;
    private EntityGroupId groupId;
    private OtaPackageType otaPackageType;
    private OtaPackageId otaPackageId;
    private long otaPackageUpdateTime;

}
