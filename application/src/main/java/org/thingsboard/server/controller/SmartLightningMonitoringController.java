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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.converter.MonitoringVOConverter;
import org.thingsboard.server.service.monitoring.vo.DeviceStatusSummaryVO;
import org.thingsboard.server.service.monitoring.vo.GroundMonitoringSummaryVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningDeviceContextVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningMonitoringTreeVO;
import org.thingsboard.server.service.monitoring.vo.SpdMonitoringSummaryVO;
import org.thingsboard.server.service.monitoring.vo.LightningCurrentMonitoringSummaryVO;
import org.thingsboard.server.service.monitoring.vo.SmartLightningRealtimeAnalysisVO;
import org.thingsboard.server.service.monitoring.service.DeviceStatusSummaryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDashboardService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDataQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;

import java.util.UUID;

/**
 * 智能防雷数据监测页专用接口。
 *
 * <p>控制器只负责 HTTP 请求、权限注解和当前用户上下文；实体查询、属性读取、告警统计及
 * 分页过滤均由 monitoring/service 下的业务服务完成。</p>
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/smart-lightning/monitoring")
@RequiredArgsConstructor
public class SmartLightningMonitoringController extends BaseController {

    private final SmartLightningMonitoringTreeService monitoringTreeService;
    private final DeviceStatusSummaryService deviceStatusSummaryService;
    private final SmartLightningDashboardService dashboardService;
    private final SmartLightningMonitoringService monitoringService;
    private final SmartLightningDataQueryService dataQueryService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SmartLightningMonitoringTreeVO getMonitoringTree() throws ThingsboardException {
        return MonitoringVOConverter.toVO(monitoringTreeService.getTree(getCurrentUser()));
    }

    /**
     * 返回数据监测页的实时聚合结果。
     * assetId 为空时统计全部可见防雷设备，传入时统计该资产及其全部后代设备。
     */
    @GetMapping("/realtime-analysis")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SmartLightningRealtimeAnalysisVO getRealtimeAnalysis(
            @RequestParam(required = false) UUID assetId) throws ThingsboardException {
        return MonitoringVOConverter.toVO(
                monitoringService.getRealtimeAnalysis(getCurrentUser(), assetId));
    }

    /**
     * 顶部导航栏的全局设备状态统计。
     *
     * <p>此接口不按 productCategory 过滤，范围是当前登录用户拥有 READ 权限的全部设备。
     * 在线状态只读取设备 SERVER_SCOPE 中的 active 属性（true/1），不使用 DeviceInfo.active；
     * 未清除告警设备与 active 不是 true/1 的设备合并去重后计入异常。</p>
     */
    @GetMapping("/device-status-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceStatusSummaryVO getDeviceStatusSummary() throws ThingsboardException {
        return MonitoringVOConverter.toVO(deviceStatusSummaryService.getSummary(getCurrentUser()));
    }

    /** 地网监测接口仅负责接收请求并委托给业务服务。 */
    @GetMapping("/ground-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public GroundMonitoringSummaryVO getGroundMonitoringSummary(
            @RequestParam(defaultValue = "day") String period
    ) throws ThingsboardException {
        return MonitoringVOConverter.toVO(dashboardService.getGroundSummary(getCurrentUser(), period));
    }

    /** SPD 监测摘要：设备状态为当前快照，告警和周期数据盒子按所选周期统计。 */
    @GetMapping("/spd-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SpdMonitoringSummaryVO getSpdMonitoringSummary(
            @RequestParam(defaultValue = "day") String period
    ) throws ThingsboardException {
        return MonitoringVOConverter.toVO(dashboardService.getSpdSummary(getCurrentUser(), period));
    }

    /** 雷电流监测摘要：单条 surgeCount 直接计数，多条记录按累计计数器差值汇总。 */
    @GetMapping("/lightning-current-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public LightningCurrentMonitoringSummaryVO getLightningCurrentMonitoringSummary(
            @RequestParam(defaultValue = "day") String period
    ) throws ThingsboardException {
        return MonitoringVOConverter.toVO(
                dashboardService.getLightningCurrentSummary(getCurrentUser(), period));
    }

    /**
     * 返回当前用户有读取权限且 productCategory=lightning 的设备列表。
     *
     * <p>设备列表本身来自 UserEntitiesService，保留租户、客户和实体组授权边界；
     * productCategory 位于设备 SERVER_SCOPE 属性中，因此先批量读取属性再在服务端过滤。
     * 过滤后重新分页，避免把其他产品计入 totalElements。</p>
     */
    @GetMapping("/devices")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<DeviceInfo> getLightningDevices(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, null, null);
        return dataQueryService.getDevices(getCurrentUser(), pageLink);
    }

    /**
     * 返回当前用户可见防雷设备及页面所需服务器属性。
     * 属性由后端按设备 ID 批量查询，避免前端逐台请求。
     */
    @GetMapping("/device-contexts")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<SmartLightningDeviceContextVO> getLightningDeviceContexts(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, null, null);
        return MonitoringVOConverter.toDeviceContextPage(
                dataQueryService.getDeviceContexts(getCurrentUser(), pageLink));
    }

}
