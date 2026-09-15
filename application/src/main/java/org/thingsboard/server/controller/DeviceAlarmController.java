/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisOverviewDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmLeafAssetCountDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSeverityTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmAnalysisService;
import org.thingsboard.server.service.alarms.service.DeviceAlarmSummaryService;

import java.util.List;

/**
 * 当前用户全部可见设备的自定义告警统计接口。
 *
 * <p>控制器只负责 HTTP 参数、权限和当前用户上下文，所有查询及聚合均委托给告警业务服务。
 * 为保持现有前端兼容，迁移阶段继续沿用原有请求基地址。</p>
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/smart-lightning/monitoring")
@RequiredArgsConstructor
public class DeviceAlarmController extends BaseController {

    private final DeviceAlarmSummaryService alarmSummaryService;
    private final DeviceAlarmAnalysisService alarmAnalysisService;

    /** 返回当前用户全部可见设备的未清除告警总数及按类型统计。 */
    @GetMapping("/alarm-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceAlarmSummaryDTO getAlarmSummary() throws ThingsboardException {
        return alarmSummaryService.getSummary(getCurrentUser());
    }

    /** 返回告警分析页日、月、年触发次数、对比周期和趋势数据。 */
    @GetMapping("/alarm-analysis-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceAlarmAnalysisSummaryDTO getAlarmAnalysisSummary() throws ThingsboardException {
        return alarmAnalysisService.getSummary(getCurrentUser());
    }

    /** 返回直接挂载设备的末级父资产在指定周期内的告警数量和处理率。 */
    @GetMapping("/alarm-analysis/leaf-assets")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<DeviceAlarmLeafAssetCountDTO> getLeafAssetAlarmCounts(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return alarmAnalysisService.getLeafAssetCounts(getCurrentUser(), period);
    }

    /** 返回指定周期内全部可见设备按告警类型聚合的记录数。 */
    @GetMapping("/alarm-analysis/types")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<DeviceAlarmTypeTotalDTO> getAlarmTypeRanking(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return alarmAnalysisService.getTypeRanking(getCurrentUser(), period);
    }

    /** 返回指定周期内全部可见设备按告警严重程度聚合的记录数。 */
    @GetMapping("/alarm-analysis/severities")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<DeviceAlarmSeverityTotalDTO> getAlarmSeverityRanking(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return alarmAnalysisService.getSeverityRanking(getCurrentUser(), period);
    }

    /** 返回指定周期内的确认/清除状态分布和设备告警覆盖率。 */
    @GetMapping("/alarm-analysis/overview")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceAlarmAnalysisOverviewDTO getAlarmAnalysisOverview(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return alarmAnalysisService.getOverview(getCurrentUser(), period);
    }
}
