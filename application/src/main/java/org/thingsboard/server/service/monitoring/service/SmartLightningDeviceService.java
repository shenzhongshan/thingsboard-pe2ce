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
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.query.VisibleDeviceScopeService;

import java.util.List;

/**
 * 智能防雷模块公共设备服务。
 *
 * <p>由看板、数据监测、数据查询以及告警分析复用。带 Lightning 的方法会追加
 * productCategory=lightning 过滤；getVisibleDeviceIds 只执行当前用户权限过滤，
 * 供面向全体设备的告警统计使用。</p>
 */
public interface SmartLightningDeviceService extends VisibleDeviceScopeService {

    /** 返回当前用户可见且 productCategory=lightning 的设备，并在过滤后分页。 */
    PageData<DeviceInfo> getLightningDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException;

    /** 批量返回当前用户可见防雷设备及其服务器属性，并在 productCategory 过滤后分页。 */
    PageData<MonitoringDeviceContextBO> getLightningDeviceContexts(
            SecurityUser user, PageLink pageLink) throws ThingsboardException;

    /**
     * 返回当前用户可见的全部防雷设备上下文，不做接口分页。
     *
     * <p>供后端聚合服务复用同一套权限、产品分类和服务器属性读取逻辑，
     * 避免各聚合实现自行循环请求分页接口。</p>
     */
    List<MonitoringDeviceContextBO> getAllLightningDeviceContexts(SecurityUser user)
            throws ThingsboardException;

}
