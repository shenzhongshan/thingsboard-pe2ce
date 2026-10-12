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
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.service.SmartLightningAlarmService;
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
import java.util.Arrays;
import java.util.Collections;
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
    private final SmartLightningAlarmService smartLightningAlarmService;
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

    /**
     * 查询智能防雷模块的告警页；筛选和分页参数沿用 ThingsBoard v2 告警查询格式。
     *
     * <p>productCategory 必须为 lightning；实际查询还会限制在当前用户有 READ 权限的防雷设备内。</p>
     *
     * @param productCategory 产品大类，只接受 lightning，用于防止跨模块查询
     * @param statusList 告警状态筛选，可传多个原生 AlarmSearchStatus 枚举值
     * @param severityList 告警严重程度筛选，可传多个原生 AlarmSeverity 枚举值
     * @param typeList 告警类型筛选；为空时不按告警类型限制
     * @param assigneeId 受理人用户 ID；为空时不按受理人限制
     * @param pageSize 每页记录数
     * @param page 从 0 开始的页码
     * @param textSearch ThingsBoard 告警文本搜索条件
     * @param sortProperty 排序字段；未提供时按创建时间倒序
     * @param sortOrder 排序方向
     * @param startTime 查询开始时间，Unix 毫秒时间戳
     * @param endTime 查询结束时间，Unix 毫秒时间戳
     * @return 当前用户可见防雷设备产生的告警分页，保留原生 AlarmInfo 字段和分页信息
     * @throws ThingsboardException 当前用户权限校验或告警查询失败时抛出
     */
    @GetMapping("/alarms")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<AlarmInfo> getLightningAlarms(
            @RequestParam String productCategory,
            @RequestParam(required = false) String[] statusList,
            @RequestParam(required = false) String[] severityList,
            @RequestParam(required = false) String[] typeList,
            @RequestParam(required = false) String assigneeId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime
    ) throws ThingsboardException {
        // 将 URL 中的状态名称转换成平台枚举；未传状态条件时保留空列表，表示不按状态过滤。
        List<AlarmSearchStatus> alarmStatusList = new java.util.ArrayList<>();
        if (statusList != null) {
            for (String status : statusList) {
                if (status != null && !status.isBlank()) {
                    alarmStatusList.add(AlarmSearchStatus.valueOf(status));
                }
            }
        }
        // 将严重程度名称转换成平台枚举；忽略空白项，避免它们进入底层查询条件。
        List<AlarmSeverity> alarmSeverityList = new java.util.ArrayList<>();
        if (severityList != null) {
            for (String severity : severityList) {
                if (severity != null && !severity.isBlank()) {
                    alarmSeverityList.add(AlarmSeverity.valueOf(severity));
                }
            }
        }
        // 保留调用方给出的告警类型；null 转为空列表以表达“不限制类型”。
        List<String> alarmTypeList = typeList == null ? Collections.emptyList() : Arrays.asList(typeList);
        // 将受理人字符串解析为 ThingsBoard 用户 ID；未指定受理人时传 null 表示不筛选。
        UserId alarmAssigneeId = assigneeId == null ? null : new UserId(UUID.fromString(assigneeId));
        // 统一构建平台时间分页链接，时间范围使用 Unix 毫秒并携带文本搜索和排序参数。
        TimePageLink pageLink = createTimePageLink(
                pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        // 该模块查询不绑定单一 affectedEntity，由模块 Service 再限制到当前用户可见的防雷设备集合。
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, alarmTypeList, alarmStatusList, alarmSeverityList, alarmAssigneeId);
        // 将当前用户、原生告警条件和模块类别交给业务层完成分类校验及设备权限过滤。
        return smartLightningAlarmService.findAlarms(getCurrentUser(), query, productCategory);
    }

}
