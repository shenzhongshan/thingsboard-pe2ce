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
package org.thingsboard.server.dao.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.model.sql.DeviceGroupOtaPackageEntity;
import org.thingsboard.server.dao.sql.ota.DeviceGroupOtaPackageRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseDeviceGroupOtaPackageService implements DeviceGroupOtaPackageService {

    private final DeviceGroupOtaPackageRepository deviceGroupOtaPackageRepository;
    private final EntityGroupService entityGroupService;
    private final DeviceService deviceService;

    @Override
    public DeviceGroupOtaPackage findDeviceGroupOtaPackageByGroupIdAndType(TenantId tenantId, EntityGroupId groupId, OtaPackageType type) {
        DeviceGroupOtaPackageEntity entity = deviceGroupOtaPackageRepository.findByGroupIdAndOtaPackageType(groupId.getId(), type);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public List<DeviceGroupOtaPackage> findDeviceGroupOtaPackageByGroupId(TenantId tenantId, EntityGroupId groupId) {
        return deviceGroupOtaPackageRepository.findByGroupId(groupId.getId()).stream()
                .map(DeviceGroupOtaPackageEntity::toData).toList();
    }

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        if (deviceGroupOtaPackage.getGroupId() == null) {
            throw new DataValidationException("Device group ota package group should be specified!");
        }
        if (deviceGroupOtaPackage.getOtaPackageType() == null) {
            throw new DataValidationException("Device group ota package type should be specified!");
        }
        if (deviceGroupOtaPackage.getOtaPackageId() == null) {
            throw new DataValidationException("Device group ota package should be specified!");
        }
        deviceGroupOtaPackage.setTenantId(tenantId);
        deviceGroupOtaPackage.setOtaPackageUpdateTime(System.currentTimeMillis());
        DeviceGroupOtaPackageEntity existing = deviceGroupOtaPackageRepository.findByGroupIdAndOtaPackageType(
                deviceGroupOtaPackage.getGroupId().getId(), deviceGroupOtaPackage.getOtaPackageType());
        UUID id = existing != null ? existing.getUuid() : UUID.randomUUID();
        deviceGroupOtaPackage.setId(id);
        DeviceGroupOtaPackage saved = deviceGroupOtaPackageRepository.save(new DeviceGroupOtaPackageEntity(deviceGroupOtaPackage)).toData();
        cascadeToMemberDevices(tenantId, saved);
        return saved;
    }

    @Override
    public void deleteDeviceGroupOtaPackage(TenantId tenantId, EntityGroupId groupId, OtaPackageType type) {
        deviceGroupOtaPackageRepository.deleteByGroupIdAndOtaPackageType(groupId.getId(), type);
    }

    /**
     * Applies the assigned package to every device CURRENTLY in the group. Devices added to the group
     * afterward are not retroactively updated — see the simplification note on the service interface.
     */
    private void cascadeToMemberDevices(TenantId tenantId, DeviceGroupOtaPackage assignment) {
        List<EntityId> memberIds = entityGroupService.findAllEntityIds(tenantId, assignment.getGroupId());
        for (EntityId entityId : memberIds) {
            if (entityId.getEntityType() != EntityType.DEVICE) {
                continue;
            }
            Device device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
            if (device == null) {
                continue;
            }
            if (assignment.getOtaPackageType() == OtaPackageType.FIRMWARE) {
                device.setFirmwareId(assignment.getOtaPackageId());
            } else {
                device.setSoftwareId(assignment.getOtaPackageId());
            }
            deviceService.saveDevice(device);
        }
    }

}
