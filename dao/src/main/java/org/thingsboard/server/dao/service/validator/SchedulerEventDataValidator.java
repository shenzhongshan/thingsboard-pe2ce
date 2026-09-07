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
package org.thingsboard.server.dao.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerRepeat;
import org.thingsboard.server.common.data.scheduler.TimerRepeat;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.scheduler.SchedulerEventDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class SchedulerEventDataValidator extends DataValidator<SchedulerEvent> {

    @Value("${scheduler.min_interval:60}")
    private int minTimerBasedIntervalForEventInSec;

    @Autowired
    private OtaPackageService otaPackageService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceProfileService deviceProfileService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private SchedulerEventDao schedulerEventDao;
    @Autowired
    private EntityGroupService entityGroupService;

    @Override
    protected SchedulerEvent validateUpdate(TenantId tenantId, SchedulerEvent schedulerEvent) {
        SchedulerEvent old = schedulerEventDao.findById(tenantId, schedulerEvent.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing scheduler event!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, SchedulerEvent schedulerEvent) {
        if (StringUtils.isEmpty(schedulerEvent.getType())) {
            throw new DataValidationException("SchedulerEvent type should be specified!");
        }
        if (StringUtils.isEmpty(schedulerEvent.getName())) {
            throw new DataValidationException("SchedulerEvent name should be specified!");
        }
        if (schedulerEvent.getSchedule() == null) {
            throw new DataValidationException("SchedulerEvent schedule configuration should be specified!");
        }
        if (schedulerEvent.getSchedule().get("startTime") == null || schedulerEvent.getSchedule().get("timezone") == null) {
            throw new DataValidationException("SchedulerEvent schedule startTime and timezone should be specified!");
        }
        if (schedulerEvent.getConfiguration() == null) {
            throw new DataValidationException("SchedulerEvent configuration should be specified!");
        }
        if (schedulerEvent.getTenantId() == null) {
            throw new DataValidationException("SchedulerEvent should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(schedulerEvent.getTenantId())) {
            throw new DataValidationException("SchedulerEvent is referencing to non-existent tenant!");
        }
        if (schedulerEvent.getCustomerId() == null) {
            schedulerEvent.setCustomerId(new CustomerId(ModelConstants.NULL_UUID));
        } else if (!schedulerEvent.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, schedulerEvent.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign schedulerEvent to non-existent customer!");
            }
            if (!customer.getTenantId().equals(schedulerEvent.getTenantId())) {
                throw new DataValidationException("Can't assign schedulerEvent to customer from different tenant!");
            }
        }
        boolean isFirmwareUpdate = "updateFirmware".equals(schedulerEvent.getType());
        boolean isSoftwareUpdate = "updateSoftware".equals(schedulerEvent.getType());
        if (isFirmwareUpdate || isSoftwareUpdate) {
            OtaPackageId otaPackageId = JacksonUtil.convertValue(schedulerEvent.getConfiguration().get("msgBody"), OtaPackageId.class);
            if (otaPackageId == null) {
                throw new DataValidationException("SchedulerEvent firmwareId should be specified!");
            }
            OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
            if (otaPackage == null) {
                throw new DataValidationException("Can't assign non-existent firmware!");
            }
            if ((isFirmwareUpdate && !OtaPackageType.FIRMWARE.equals(otaPackage.getType()))
                    || (isSoftwareUpdate && !OtaPackageType.SOFTWARE.equals(otaPackage.getType()))) {
                throw new DataValidationException("SchedulerEvent Can't assign firmware with different type!");
            }
            EntityId originatorId = schedulerEvent.getOriginatorId();
            if (originatorId == null) {
                throw new DataValidationException("SchedulerEvent originatorId should be specified!");
            }
            switch (originatorId.getEntityType()) {
                case DEVICE -> {
                    Device device = deviceService.findDeviceById(tenantId, new DeviceId(originatorId.getId()));
                    if (device == null || !device.getDeviceProfileId().equals(otaPackage.getDeviceProfileId())) {
                        throw new DataValidationException("SchedulerEvent can't assign firmware with different deviceProfile!");
                    }
                }
                case DEVICE_PROFILE -> {
                    DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, new DeviceProfileId(originatorId.getId()));
                    if (deviceProfile == null || !deviceProfile.getId().equals(otaPackage.getDeviceProfileId())) {
                        throw new DataValidationException("SchedulerEvent can't assign firmware with different deviceProfile!");
                    }
                }
                case ENTITY_GROUP -> {
                    // A device group can span multiple profiles, so (unlike DEVICE/DEVICE_PROFILE above) there's no
                    // single deviceProfileId to compare against — the assignment is per-device at fire time
                    // (BaseDeviceGroupOtaPackageService#cascadeToMemberDevices), so only the group itself is checked here.
                    EntityGroup deviceGroup = entityGroupService.findEntityGroupById(tenantId, new EntityGroupId(originatorId.getId()));
                    if (deviceGroup == null || deviceGroup.getType() != EntityType.DEVICE) {
                        throw new DataValidationException("SchedulerEvent firmware update target group must be a device group!");
                    }
                }
                default -> throw new DataValidationException("Firmware update target must be a device, a device profile or a device group!");
            }
        } else if (!"generateReport".equals(schedulerEvent.getType()) && schedulerEvent.getOriginatorId() == null) {
            throw new DataValidationException("SchedulerEvent originatorId should be specified!");
        }
        JsonNode repeatNode = schedulerEvent.getSchedule().get("repeat");
        if (repeatNode != null && !repeatNode.isNull()) {
            SchedulerRepeat repeat = JacksonUtil.treeToValue(repeatNode, SchedulerRepeat.class);
            if (repeat instanceof TimerRepeat timerRepeat) {
                long seconds = timerRepeat.getTimeUnit().toSeconds(timerRepeat.getRepeatInterval());
                if (seconds < minTimerBasedIntervalForEventInSec) {
                    throw new DataValidationException("Timer-based repeats are too frequent (less than " + minTimerBasedIntervalForEventInSec + " seconds)!");
                }
            }
        }
    }

}
