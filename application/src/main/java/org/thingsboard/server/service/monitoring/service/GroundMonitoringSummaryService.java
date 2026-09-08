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
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummary;
import org.thingsboard.server.service.security.model.SecurityUser;

/** 地网监测看板的业务接口。 */
public interface GroundMonitoringSummaryService {

    /**
     * 返回当前用户可见地网设备的当前状态、指定时段告警总数和阻值范围。
     *
     * @param user 当前已认证用户，用于保留 ThingsBoard 的实体授权边界
     * @param period 告警统计窗口，仅接受 day、week、month
     */
    GroundMonitoringSummary getSummary(SecurityUser user, String period) throws ThingsboardException;
}
