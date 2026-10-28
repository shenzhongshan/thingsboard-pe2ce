/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.GroundResistanceExtremaDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.monitoring.util.MonitoringTelemetryAccumulator;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 地网板块摘要处理器。 */
@RequiredArgsConstructor
final class GroundSummaryHandler {


    private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;
    private static final String GROUND_RESISTANCE_FUNCTION_CATEGORY = "ground_resistance";
    private static final String GROUND_RESISTANCE_TELEMETRY_KEY = "GndGridRes";

    private final SmartLightningDeviceService smartLightningDeviceService;
    private final AlarmStatisticsService alarmStatisticsService;
    private final TimeseriesService timeseriesService;

    /**
     * 查询当前用户可见地网设备（functionCategory=ground_resistance）的完整摘要。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param period 告警统计窗口，仅接受 day、week、month
     * @return 设备总数、在线/离线数量、周期内告警数、告警类型统计、报警率和阻值范围
     * @throws ThingsboardException 周期参数非法或设备、告警、遥测查询失败时抛出
     */
    public GroundMonitoringSummaryDTO getSummary(SecurityUser user, String period) throws ThingsboardException {
        // 解析统计周期，并取得当前用户可见且分类匹配的地网设备。
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        List<MonitoringDeviceContextBO> groundDevices = findVisibleGroundDevices(user);
        // 统计设备 ID 和在线数量，为后续告警查询及摘要组装准备统一设备范围。
        Set<UUID> groundDeviceIds = new HashSet<>();
        int online = 0;
        for (MonitoringDeviceContextBO device : groundDevices) {
            groundDeviceIds.add(device.getDeviceId());
            if (Boolean.TRUE.equals(device.getActive())) {
                online++;
            }
        }

        SmartLightningDashboardSupport.AlarmRecordAggregate alarms = SmartLightningDashboardSupport.collectAlarmRecords(
                user, groundDeviceIds, window, alarmStatisticsService);
        // 查询周期内阻值并记录最大、最小值对应的设备、参数和时间。
        GroundResistanceExtremaDTO resistanceExtrema = findGroundResistanceRange(user, groundDevices, window);

        // 使用设备总数计算报警率，空设备集合时定义为 0，最后组装接口 DTO。
        int total = groundDevices.size();
        double alarmRate = total == 0 ? 0D : alarms.alarmDeviceCount * 100D / total;
        return new GroundMonitoringSummaryDTO(
                total, online, total - online, CountValueSupport.toIntCount(alarms.count), alarms.typeTotals, alarmRate,
                resistanceExtrema.getMaximum(), resistanceExtrema.getMinimum(), resistanceExtrema);
    }

    /** 复用智能防雷设备服务的权限、批量属性和 productCategory 过滤。 */
    private List<MonitoringDeviceContextBO> findVisibleGroundDevices(SecurityUser user)
            throws ThingsboardException {
        return SmartLightningDashboardSupport.findVisibleDevicesByFunctionCategory(
                smartLightningDeviceService, user, GROUND_RESISTANCE_FUNCTION_CATEGORY);
    }

    /**
     * 同时读取每台地网设备在当前周期的阻值。TimeseriesService 没有按实体保留结果的
     * 单次批量 API，故由服务层并行发起查询并统一等待，前端始终只请求一个汇总接口。
     */
    private GroundResistanceExtremaDTO findGroundResistanceRange(
            SecurityUser user, List<MonitoringDeviceContextBO> groundDevices,
            MonitoringPeriodWindow window)
            throws ThingsboardException {
        if (groundDevices.isEmpty()) {
            return new GroundResistanceExtremaDTO(null, null, null, null, null, null, null, null);
        }
        // 按周期建立规范键查询，并通过公共支持类分批读取设备遥测。
        List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(
                GROUND_RESISTANCE_TELEMETRY_KEY,
                window.getStartTs(),
                window.getEndTs(),
                0,
                QUERY_PAGE_SIZE,
                Aggregation.NONE,
                "ASC"));
        List<MonitoringTelemetryAccumulator> summaries = SmartLightningDashboardSupport.readTelemetryInBatches(
                timeseriesService,
                user,
                groundDevices,
                queries,
                Set.of(),
                "Failed to read ground resistance telemetry");
        ResistanceExtremaAggregate result = new ResistanceExtremaAggregate();
        // 逐设备检查有效数值；解析失败或非有限值不参与极值比较。
        for (int index = 0; index < summaries.size(); index++) {
            MonitoringDeviceContextBO device = groundDevices.get(index);
            for (TsKvEntry entry : summaries.get(index).representativeEntries()) {
                Double resistanceValue = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (resistanceValue == null) {
                    continue;
                }
                if (result.maximum == null || resistanceValue > result.maximum) {
                    result.maximum = resistanceValue;
                    result.maximumDeviceName = device.getDeviceName();
                    result.maximumParameter = entry.getKey();
                    result.maximumTime = entry.getTs();
                }
                if (result.minimum == null || resistanceValue < result.minimum) {
                    result.minimum = resistanceValue;
                    result.minimumDeviceName = device.getDeviceName();
                    result.minimumParameter = entry.getKey();
                    result.minimumTime = entry.getTs();
                }
            }
        }
        return result.toDTO();
    }

    /** 保存地网阻值最值及其来源信息，最终转换为接口 DTO。 */
    private static final class ResistanceExtremaAggregate {
        private Double maximum;
        private String maximumDeviceName;
        private String maximumParameter;
        private Long maximumTime;
        private Double minimum;
        private String minimumDeviceName;
        private String minimumParameter;
        private Long minimumTime;

        private GroundResistanceExtremaDTO toDTO() {
            return new GroundResistanceExtremaDTO(
                    maximum, maximumDeviceName, maximumParameter, maximumTime,
                    minimum, minimumDeviceName, minimumParameter, minimumTime);
        }
    }
}
