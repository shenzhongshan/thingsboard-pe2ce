/** 文件职责：自定义平台首页的 HTTP 入口，只负责权限和服务调用。 */
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.homeoverview.HomeOverviewService;
import org.thingsboard.server.service.monitoring.converter.MonitoringVOConverter;
import org.thingsboard.server.service.monitoring.vo.SmartLightningDeviceContextVO;

import java.util.List;

/**
 * 平台自定义首页接口。
 *
 * <p>首页需要跨产品大类展示设备，接口返回 SERVER_SCOPE 中真实的产品分类；
 * 缺失 productCategory 的设备仍返回，但不会被服务端归入防雷。</p>
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/home-overview")
@RequiredArgsConstructor
public class HomeOverviewController extends BaseController {

    private final HomeOverviewService homeOverviewService;

    /** 返回当前用户可见设备及其产品大类、功能分类和地图坐标。 */
    @GetMapping("/device-contexts")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<SmartLightningDeviceContextVO> getDeviceContexts() throws ThingsboardException {
        return MonitoringVOConverter.toDeviceContextList(
                homeOverviewService.getVisibleDeviceContexts(getCurrentUser()));
    }
}