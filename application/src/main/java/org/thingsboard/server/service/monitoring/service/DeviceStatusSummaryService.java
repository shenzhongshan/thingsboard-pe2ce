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
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummaryDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 顶部导航栏设备正常/异常统计的业务接口。 */
public interface DeviceStatusSummaryService {

    /** 统计当前用户可见设备中正常以及存在未清除告警或离线的设备数量。 */
    DeviceStatusSummaryDTO getSummary(SecurityUser user) throws ThingsboardException;
}
