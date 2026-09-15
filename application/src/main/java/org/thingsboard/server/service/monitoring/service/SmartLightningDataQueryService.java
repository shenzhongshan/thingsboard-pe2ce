/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 智能防雷数据查询页面级服务。
 *
 * <p>负责组织数据查询页需要的设备分页和设备上下文，具体权限过滤、产品分类过滤、
 * 服务器属性批量读取继续复用公共设备服务。</p>
 */
public interface SmartLightningDataQueryService {

    /** 返回当前用户可见的防雷设备分页。 */
    PageData<DeviceInfo> getDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException;

    /** 返回当前用户可见的防雷设备及页面所需服务器属性分页。 */
    PageData<SmartLightningDeviceContextDTO> getDeviceContexts(SecurityUser user, PageLink pageLink)
            throws ThingsboardException;
}
