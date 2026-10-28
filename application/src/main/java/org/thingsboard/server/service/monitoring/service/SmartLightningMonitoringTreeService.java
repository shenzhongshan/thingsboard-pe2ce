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
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 自定义页面公共资产树服务。
 *
 * <p>由数据监测、数据查询、设备详情和告警处理等页面复用，统一维护资产、设备及
 * Contains 关系的树结构，页面服务不得重复构造资产层级。</p>
 */
public interface SmartLightningMonitoringTreeService {

    /** 读取当前用户有 READ 权限的资产、设备及其 Contains 关系并返回树结构。 */
    SmartLightningMonitoringTreeDTO getTree(SecurityUser user) throws ThingsboardException;
}
