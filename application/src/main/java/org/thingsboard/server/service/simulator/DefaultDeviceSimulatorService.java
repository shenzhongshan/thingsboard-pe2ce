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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceSimulatorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.simulator.DeviceSimulator;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.simulator.DeviceSimulatorDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultDeviceSimulatorService implements DeviceSimulatorService {

    private final DeviceSimulatorDao deviceSimulatorDao;
    private final DeviceService deviceService;
    private final DeviceSimulatorRunner runner;

    @Override
    public DeviceSimulator save(TenantId tenantId, DeviceSimulator simulator) throws ThingsboardException {
        if (simulator.getId() != null && runner.isRunning(simulator.getId())) {
            throw new ThingsboardException("Stop the simulator before editing it", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (simulator.getDeviceId() == null || deviceService.findDeviceById(tenantId, simulator.getDeviceId()) == null) {
            throw new ThingsboardException("Device not found", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        validateDefinition(simulator);
        simulator.setTenantId(tenantId);
        return deviceSimulatorDao.save(tenantId, simulator);
    }

    private void validateDefinition(DeviceSimulator simulator) throws ThingsboardException {
        try {
            EmulatorDefinition definition = JacksonUtil.treeToValue(simulator.getDefinition(), EmulatorDefinition.class);
            if (definition == null || definition.getTelemetryProfiles().isEmpty()) {
                throw new IllegalArgumentException("At least one telemetry profile is required");
            }
        } catch (Exception e) {
            throw new ThingsboardException("Invalid simulator definition: " + e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @Override
    public DeviceSimulator findById(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException {
        DeviceSimulator simulator = deviceSimulatorDao.findById(tenantId, id);
        if (simulator == null) {
            throw new ThingsboardException("Device simulator not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return simulator;
    }

    @Override
    public DeviceSimulatorInfo findInfoById(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException {
        return toInfo(tenantId, findById(tenantId, id));
    }

    @Override
    public List<DeviceSimulatorInfo> findInfosByTenantId(TenantId tenantId) {
        return deviceSimulatorDao.findByTenantId(tenantId).stream()
                .map(simulator -> toInfo(tenantId, simulator))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException {
        findById(tenantId, id);
        runner.stop(id);
        deviceSimulatorDao.removeById(tenantId, id);
    }

    @Override
    public DeviceSimulatorInfo start(TenantId tenantId, DeviceSimulatorId id, RunMode mode) throws ThingsboardException {
        DeviceSimulator simulator = findById(tenantId, id);
        Device device = deviceService.findDeviceById(tenantId, simulator.getDeviceId());
        if (device == null) {
            throw new ThingsboardException("Device not found", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        try {
            runner.start(tenantId, device, simulator, mode);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return toInfo(tenantId, simulator);
    }

    @Override
    public DeviceSimulatorInfo stop(TenantId tenantId, DeviceSimulatorId id) throws ThingsboardException {
        DeviceSimulator simulator = findById(tenantId, id);
        runner.stop(id);
        return toInfo(tenantId, simulator);
    }

    private DeviceSimulatorInfo toInfo(TenantId tenantId, DeviceSimulator simulator) {
        DeviceSimulatorInfo info = new DeviceSimulatorInfo();
        info.setId(simulator.getId());
        info.setCreatedTime(simulator.getCreatedTime());
        info.setName(simulator.getName());
        info.setDeviceId(simulator.getDeviceId());
        info.setDefinition(simulator.getDefinition());
        try {
            Device device = deviceService.findDeviceById(tenantId, simulator.getDeviceId());
            info.setDeviceName(device != null ? device.getName() : null);
        } catch (Exception e) {
            info.setDeviceName(null);
        }
        DeviceSimulatorRunner.RunningSession session = runner.getSession(simulator.getId());
        if (session != null) {
            info.setRunning(true);
            info.setMode(session.getMode());
            info.setStartedTime(session.getStartedTime());
            info.setPublishedCount(session.getPublishedCount());
            info.setLastError(session.getLastError());
        } else {
            info.setRunning(false);
        }
        return info;
    }

}
