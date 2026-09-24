/** 文件职责：平台自定义首页设备上下文查询实现。 */
package org.thingsboard.server.service.homeoverview.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.homeoverview.HomeOverviewService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.converter.MonitoringDeviceDTOConverter;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页设备查询实现。
 *
 * <p>权限范围、服务器属性批量读取和设备配置读取均复用
 * SmartLightningDeviceService，避免首页复制一套设备查询逻辑。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class HomeOverviewServiceImpl implements HomeOverviewService {

    private final SmartLightningDeviceService deviceService;

    @Override
    public List<SmartLightningDeviceContextDTO> getVisibleDeviceContexts(SecurityUser user)
            throws ThingsboardException {
        List<MonitoringDeviceContextBO> contexts = deviceService.getAllVisibleDeviceContexts(user);
        List<SmartLightningDeviceContextDTO> result = new ArrayList<>(contexts.size());
        for (MonitoringDeviceContextBO context : contexts) {
            result.add(MonitoringDeviceDTOConverter.toDTO(context));
        }
        return result;
    }
}