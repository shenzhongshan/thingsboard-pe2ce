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

import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;

import java.util.List;

/**
 * Deliberately not the generic {@code Dao<T>} framework interface — device simulators are a
 * self-contained feature with no relations/audit-log/VC-export needs, so a plain CRUD contract
 * is enough.
 */
public interface DeviceSimulatorDao {

    DeviceSimulator save(TenantId tenantId, DeviceSimulator simulator);

    DeviceSimulator findById(TenantId tenantId, DeviceSimulatorId id);

    List<DeviceSimulator> findByTenantId(TenantId tenantId);

    void removeById(TenantId tenantId, DeviceSimulatorId id);

}
