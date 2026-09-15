/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDataQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 智能防雷数据查询页面级服务实现。
 *
 * <p>本类只负责页面级编排，不复制公共设备服务中的权限、属性和分页逻辑。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningDataQueryServiceImpl implements SmartLightningDataQueryService {

    private final SmartLightningDeviceService smartLightningDeviceService;

    @Override
    public PageData<DeviceInfo> getDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException {
        return smartLightningDeviceService.getLightningDevices(user, pageLink);
    }

    @Override
    public PageData<SmartLightningDeviceContextDTO> getDeviceContexts(SecurityUser user, PageLink pageLink)
            throws ThingsboardException {
        return smartLightningDeviceService.getLightningDeviceContexts(user, pageLink);
    }
}
