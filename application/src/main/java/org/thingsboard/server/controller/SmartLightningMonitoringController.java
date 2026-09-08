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
import org.thingsboard.server.service.monitoring.dto.DeviceStatusSummary;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummary;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningLeafAssetAlarmCountDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeResponse;
import org.thingsboard.server.service.monitoring.service.SmartLightningAlarmSummaryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningAlarmAnalysisService;
import org.thingsboard.server.service.monitoring.service.SmartLightningLeafAssetAlarmService;
import org.thingsboard.server.service.monitoring.service.DeviceStatusSummaryService;
import org.thingsboard.server.service.monitoring.service.GroundMonitoringSummaryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;

import java.util.List;

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
    private final GroundMonitoringSummaryService groundMonitoringSummaryService;
    private final SmartLightningDeviceService smartLightningDeviceService;
    private final SmartLightningAlarmSummaryService smartLightningAlarmSummaryService;
    private final SmartLightningAlarmAnalysisService smartLightningAlarmAnalysisService;
    private final SmartLightningLeafAssetAlarmService smartLightningLeafAssetAlarmService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SmartLightningMonitoringTreeResponse getMonitoringTree() throws ThingsboardException {
        return monitoringTreeService.getTree(getCurrentUser());
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
    public DeviceStatusSummary getDeviceStatusSummary() throws ThingsboardException {
        return deviceStatusSummaryService.getSummary(getCurrentUser());
    }

    /** 地网监测接口仅负责接收请求并委托给业务服务。 */
    @GetMapping("/ground-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public GroundMonitoringSummary getGroundMonitoringSummary(
            @RequestParam(defaultValue = "day") String period
    ) throws ThingsboardException {
        return groundMonitoringSummaryService.getSummary(getCurrentUser(), period);
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
        return smartLightningDeviceService.getLightningDevices(getCurrentUser(), pageLink);
    }

    /**
     * 返回当前用户可见防雷设备及页面所需服务器属性。
     * 属性由后端按设备 ID 批量查询，避免前端逐台请求。
     */
    @GetMapping("/device-contexts")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<SmartLightningDeviceContextDTO> getLightningDeviceContexts(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, null, null);
        return smartLightningDeviceService.getLightningDeviceContexts(getCurrentUser(), pageLink);
    }

    /** 返回当前用户可见防雷设备的未清除告警总数及按类型统计。 */
    @GetMapping("/alarm-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SmartLightningAlarmSummaryDTO getLightningAlarmSummary() throws ThingsboardException {
        return smartLightningAlarmSummaryService.getSummary(getCurrentUser());
    }

    /** 返回告警分析页的日、月、年触发次数及其对比周期统计。 */
    @GetMapping("/alarm-analysis-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public SmartLightningAlarmAnalysisSummaryDTO getLightningAlarmAnalysisSummary() throws ThingsboardException {
        return smartLightningAlarmAnalysisService.getSummary(getCurrentUser());
    }

    /** 返回每个末级资产下属设备在指定周期内的告警数量、已处理数量和处理率。 */
    @GetMapping("/alarm-analysis/leaf-assets")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<SmartLightningLeafAssetAlarmCountDTO> getLeafAssetAlarmCounts(
            @RequestParam(defaultValue = "day") String period
    ) throws ThingsboardException {
        return smartLightningLeafAssetAlarmService.getCounts(getCurrentUser(), period);
    }
}
