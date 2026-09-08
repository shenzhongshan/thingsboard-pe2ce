/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Set;
import java.util.UUID;

/** 智能防雷设备列表的业务接口。 */
public interface SmartLightningDeviceService {

    /** 返回当前用户可见且 productCategory=lightning 的设备，并在过滤后分页。 */
    PageData<DeviceInfo> getLightningDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException;

    /** 批量返回当前用户可见防雷设备及其服务器属性，并在 productCategory 过滤后分页。 */
    PageData<SmartLightningDeviceContextDTO> getLightningDeviceContexts(
            SecurityUser user, PageLink pageLink) throws ThingsboardException;

    /** 返回当前用户可见且 productCategory=lightning 的全部设备 ID。 */
    Set<UUID> getLightningDeviceIds(SecurityUser user) throws ThingsboardException;

    /** 返回当前用户有读取权限的全部设备 ID，不按产品分类过滤。 */
    Set<UUID> getVisibleDeviceIds(SecurityUser user) throws ThingsboardException;
}
