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
import org.thingsboard.server.service.alarms.converter.DeviceAlarmVOConverter;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmAnalysisOverviewVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmAnalysisSummaryVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmLeafAssetCountVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmSeverityTotalVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmSummaryVO;
import org.thingsboard.server.service.alarms.vo.DeviceAlarmTypeTotalVO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmAnalysisService;
import org.thingsboard.server.service.alarms.service.DeviceAlarmSummaryService;

import java.util.List;

/**
 * 智能防雷模块的自定义告警统计接口。
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

    /**
     * 返回当前用户可见防雷设备的 ACTIVE 未清除告警条数及按告警类型分布。
     *
     * @return 防雷设备范围内的未清除告警摘要；无可见设备时返回零值和空类型列表
     * @throws ThingsboardException 设备可见范围或告警聚合查询失败时抛出
     */
    @GetMapping("/alarm-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceAlarmSummaryVO getAlarmSummary() throws ThingsboardException {
        return DeviceAlarmVOConverter.toVO(alarmSummaryService.getSummary(getCurrentUser()));
    }

    /**
     * 按告警 startTs 触发时间返回防雷设备的日、月、年告警次数、对应上一周期数据和趋势。
     *
     * @return 自然日、自然月和自然年的告警次数、环比、趋势及未清除严重告警设备列表
     * @throws ThingsboardException 设备、资产树或告警统计查询失败时抛出
     */
    @GetMapping("/alarm-analysis-summary")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceAlarmAnalysisSummaryVO getAlarmAnalysisSummary() throws ThingsboardException {
        return DeviceAlarmVOConverter.toVO(alarmAnalysisService.getSummary(getCurrentUser()));
    }

    /**
     * 按告警触发时间返回直属设备属于防雷分类的末级父资产告警数量及已清除处理率。
     *
     * @param period 滚动统计周期，支持 day、week、month；省略时默认为 day
     * @return 按处理率和告警数量排序的末级资产统计列表
     * @throws ThingsboardException 周期参数无效、资产树读取或告警聚合失败时抛出
     */
    @GetMapping("/alarm-analysis/leaf-assets")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<DeviceAlarmLeafAssetCountVO> getLeafAssetAlarmCounts(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return DeviceAlarmVOConverter.toLeafAssetVOs(
                alarmAnalysisService.getLeafAssetCounts(getCurrentUser(), period));
    }

    /**
     * 按告警 startTs 触发时间返回指定滚动周期内防雷设备各告警类型的记录条数。
     *
     * @param period 滚动统计周期，支持 day、week、month；省略时默认为 day
     * @return 按类型聚合的告警记录数列表
     * @throws ThingsboardException 周期参数无效或告警聚合查询失败时抛出
     */
    @GetMapping("/alarm-analysis/types")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<DeviceAlarmTypeTotalVO> getAlarmTypeRanking(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return DeviceAlarmVOConverter.toTypeVOs(
                alarmAnalysisService.getTypeRanking(getCurrentUser(), period));
    }

    /**
     * 按告警 startTs 触发时间返回指定滚动周期内防雷设备各严重程度的记录条数。
     *
     * @param period 滚动统计周期，支持 day、week、month；省略时默认为 day
     * @return 按平台严重程度枚举排序的告警记录数列表
     * @throws ThingsboardException 周期参数无效或告警聚合查询失败时抛出
     */
    @GetMapping("/alarm-analysis/severities")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public List<DeviceAlarmSeverityTotalVO> getAlarmSeverityRanking(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return DeviceAlarmVOConverter.toSeverityVOs(
                alarmAnalysisService.getSeverityRanking(getCurrentUser(), period));
    }

    /**
     * 按告警 startTs 触发时间返回指定滚动周期内的状态分布及按设备 ID 去重的告警覆盖率。
     *
     * @param period 滚动统计周期，支持 day、week、month；省略时默认为 day
     * @return 四种告警状态的记录数，以及周期告警设备数和未清除告警设备数
     * @throws ThingsboardException 周期参数无效或告警聚合查询失败时抛出
     */
    @GetMapping("/alarm-analysis/overview")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public DeviceAlarmAnalysisOverviewVO getAlarmAnalysisOverview(
            @RequestParam(defaultValue = "day") String period) throws ThingsboardException {
        return DeviceAlarmVOConverter.toVO(alarmAnalysisService.getOverview(getCurrentUser(), period));
    }
}
