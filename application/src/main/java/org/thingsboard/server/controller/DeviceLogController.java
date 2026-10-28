/**
 * 文件职责：自定义设备日志页面的 HTTP 入口。
 *
 * <p>该控制器只做权限注解、参数接收和服务委托，不修改 ThingsBoard 原生设备控制器。</p>
 */
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.deviceLog.DeviceLogService;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogDeviceDTO;
import org.thingsboard.server.service.deviceLog.dto.DeviceLogTimelinePageDTO;

import java.util.UUID;

/** 自定义设备日志页面接口。 */
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/device-log")
public class DeviceLogController extends BaseController {

    private final DeviceLogService deviceLogService;

    /** 返回当前登录用户可见设备，前端只用该列表渲染设备名称、配置类型和操作按钮。 */
    @GetMapping("/devices")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<DeviceLogDeviceDTO> getDevices(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String textSearch
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, null, null);
        return deviceLogService.findVisibleDevices(getCurrentUser(), pageLink);
    }

    /** 页面打开时读取最近窗口内的审计和告警事件。 */
    @GetMapping("/devices/{deviceId}/timeline/recent")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceLogTimelinePageDTO getRecentTimeline(
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "50") int pageSize
    ) throws ThingsboardException {
        return deviceLogService.findRecentTimeline(getCurrentUser(), deviceId, days, pageSize);
    }

    /** 用户点击“查看更多”后读取更早的历史事件，使用游标避免全量加载。 */
    @GetMapping("/devices/{deviceId}/timeline/history")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceLogTimelinePageDTO getHistoryTimeline(
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String beforeTs
    ) throws ThingsboardException {
        return deviceLogService.findHistoryTimeline(getCurrentUser(), deviceId, pageSize, beforeTs);
    }
}
