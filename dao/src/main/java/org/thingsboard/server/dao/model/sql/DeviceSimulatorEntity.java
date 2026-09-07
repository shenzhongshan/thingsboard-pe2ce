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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "device_simulator")
public class DeviceSimulatorEntity extends BaseSqlEntity<DeviceSimulator> {

    @Column(name = ModelConstants.TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.DEVICE_ID_PROPERTY)
    private UUID deviceId;

    @Column(name = ModelConstants.NAME_PROPERTY)
    private String name;

    @Convert(converter = JsonConverter.class)
    @Column(name = "definition")
    private JsonNode definition;

    public DeviceSimulatorEntity() {
        super();
    }

    public DeviceSimulatorEntity(DeviceSimulator simulator) {
        if (simulator.getId() != null) {
            this.setUuid(simulator.getId().getId());
        }
        this.setCreatedTime(simulator.getCreatedTime());
        this.tenantId = getUuid(simulator.getTenantId());
        this.deviceId = getUuid(simulator.getDeviceId());
        this.name = simulator.getName();
        this.definition = simulator.getDefinition();
    }

    @Override
    public DeviceSimulator toData() {
        DeviceSimulator simulator = new DeviceSimulator();
        simulator.setId(new DeviceSimulatorId(id));
        simulator.setCreatedTime(createdTime);
        simulator.setTenantId(TenantId.fromUUID(tenantId));
        simulator.setDeviceId(new DeviceId(deviceId));
        simulator.setName(name);
        simulator.setDefinition(definition);
        return simulator;
    }

}
