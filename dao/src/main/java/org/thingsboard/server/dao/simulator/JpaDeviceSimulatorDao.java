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
package org.thingsboard.server.dao.simulator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.DeviceSimulatorEntity;
import org.thingsboard.server.dao.sql.simulator.DeviceSimulatorRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaDeviceSimulatorDao implements DeviceSimulatorDao {

    private final DeviceSimulatorRepository deviceSimulatorRepository;

    @Override
    @Transactional
    public DeviceSimulator save(TenantId tenantId, DeviceSimulator simulator) {
        DeviceSimulatorEntity entity = new DeviceSimulatorEntity(simulator);
        if (entity.getId() == null) {
            entity.setUuid(UUID.randomUUID());
        }
        if (entity.getCreatedTime() == 0) {
            entity.setCreatedTime(System.currentTimeMillis());
        }
        return DaoUtil.getData(deviceSimulatorRepository.save(entity));
    }

    @Override
    public DeviceSimulator findById(TenantId tenantId, DeviceSimulatorId id) {
        return DaoUtil.getData(deviceSimulatorRepository.findByIdAndTenantId(id.getId(), tenantId.getId()));
    }

    @Override
    public List<DeviceSimulator> findByTenantId(TenantId tenantId) {
        return deviceSimulatorRepository.findByTenantIdOrderByCreatedTimeDesc(tenantId.getId()).stream()
                .map(DeviceSimulatorEntity::toData)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeById(TenantId tenantId, DeviceSimulatorId id) {
        deviceSimulatorRepository.deleteByIdAndTenantId(id.getId(), tenantId.getId());
    }

}
