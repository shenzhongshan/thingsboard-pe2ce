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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "device_group_ota_package")
public class DeviceGroupOtaPackageEntity extends BaseSqlEntity<DeviceGroupOtaPackage> implements BaseEntity<DeviceGroupOtaPackage> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "group_id")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ota_package_type")
    private OtaPackageType otaPackageType;

    @Column(name = "ota_package_id")
    private UUID otaPackageId;

    @Column(name = "ota_package_update_time")
    private long otaPackageUpdateTime;

    public DeviceGroupOtaPackageEntity() {
        super();
    }

    public DeviceGroupOtaPackageEntity(DeviceGroupOtaPackage data) {
        if (data.getId() != null) {
            this.setUuid(data.getId());
        }
        this.setCreatedTime(System.currentTimeMillis());
        if (data.getTenantId() != null) {
            this.tenantId = data.getTenantId().getId();
        }
        if (data.getGroupId() != null) {
            this.groupId = data.getGroupId().getId();
        }
        this.otaPackageType = data.getOtaPackageType();
        if (data.getOtaPackageId() != null) {
            this.otaPackageId = data.getOtaPackageId().getId();
        }
        this.otaPackageUpdateTime = data.getOtaPackageUpdateTime();
    }

    @Override
    public DeviceGroupOtaPackage toData() {
        DeviceGroupOtaPackage data = new DeviceGroupOtaPackage();
        data.setId(id);
        if (tenantId != null) {
            data.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (groupId != null) {
            data.setGroupId(new EntityGroupId(groupId));
        }
        data.setOtaPackageType(otaPackageType);
        if (otaPackageId != null) {
            data.setOtaPackageId(new OtaPackageId(otaPackageId));
        }
        data.setOtaPackageUpdateTime(otaPackageUpdateTime);
        return data;
    }

}
