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
import org.thingsboard.server.service.monitoring.converter.MonitoringDeviceDTOConverter;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDataQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;

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

    /** 查询当前用户可见的防雷设备页，筛选和分页规则由公共设备服务统一处理。 */
    @Override
    public PageData<DeviceInfo> getDevices(SecurityUser user, PageLink pageLink) throws ThingsboardException {
        return smartLightningDeviceService.getLightningDevices(user, pageLink);
    }

    @Override
    public PageData<SmartLightningDeviceContextDTO> getDeviceContexts(SecurityUser user, PageLink pageLink)
            throws ThingsboardException {
        // 先批量取得设备上下文页，再逐项转换为接口 DTO。
        PageData<MonitoringDeviceContextBO> contexts =
                smartLightningDeviceService.getLightningDeviceContexts(user, pageLink);
        List<SmartLightningDeviceContextDTO> response = new ArrayList<>();
        for (MonitoringDeviceContextBO context : contexts.getData()) {
            response.add(MonitoringDeviceDTOConverter.toDTO(context));
        }
        // 转换数据时保留原页的分页元信息，避免前端分页状态发生变化。
        return new PageData<>(
                response, contexts.getTotalPages(), contexts.getTotalElements(), contexts.hasNext());
    }
}
