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

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;

import java.util.List;

public interface DeviceSimulatorService {

    DeviceSimulator save(TenantId tenantId, DeviceSimulator simulator) throws ThingsboardException;

    DeviceSimulatorInfo findInfoById(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException;

    DeviceSimulator findById(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException;

    List<DeviceSimulatorInfo> findInfosByTenantId(TenantId tenantId);

    void delete(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException;

    DeviceSimulatorInfo start(TenantId tenantId, DeviceSimulatorId id, RunMode mode) throws ThingsboardException;

    DeviceSimulatorInfo stop(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException;

}
