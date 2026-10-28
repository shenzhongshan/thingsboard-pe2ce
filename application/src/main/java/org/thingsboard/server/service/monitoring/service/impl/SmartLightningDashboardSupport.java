/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.thingsboard.server.service.monitoring.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.AlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringDeviceFilters;
import org.thingsboard.server.service.monitoring.util.MonitoringTelemetryAccumulator;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 看板板块共用的设备、时序和告警查询支持。 */
final class SmartLightningDashboardSupport {

    static final int TELEMETRY_BATCH_SIZE = 50;

    private SmartLightningDashboardSupport() {
    }

    /**
     * 分批、逐键、逐页读取设备周期遥测，读取后立即折叠为极值、最新状态和累计差值。
     *
     * <p>每轮只保留固定设备批次的当前单键页；历史页不会追加到结果集合。
     * 首批及续页使用同一失败策略，任何读取失败均抛出异常，禁止以部分数据冒充完整统计。
     * limit=1的基线及最新值查询只读取一次，历史查询按左闭右开时间游标继续。</p>
     */
    static List<MonitoringTelemetryAccumulator> readTelemetryInBatches(
            TimeseriesService timeseriesService,
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices,
            List<ReadTsKvQuery> queries,
            Set<String> counterKeys,
            String errorMessage) throws ThingsboardException {
        List<MonitoringTelemetryAccumulator> summaries = new ArrayList<>();
        for (int index = 0; index < devices.size(); index++) {
            summaries.add(new MonitoringTelemetryAccumulator(counterKeys));
        }
        for (int batchStart = 0; batchStart < devices.size(); batchStart += TELEMETRY_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + TELEMETRY_BATCH_SIZE, devices.size());
            for (ReadTsKvQuery query : queries) {
                List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
                for (int index = batchStart; index < batchEnd; index++) {
                    futures.add(timeseriesService.findAll(user.getTenantId(),
                            new DeviceId(devices.get(index).getDeviceId()), List.of(query)));
                }
                try {
                    // allAsList保留单项失败，不再将异常伪装为空遥测。
                    List<List<TsKvEntry>> pages = new ArrayList<>(Futures.allAsList(futures).get());
                    for (int index = batchStart; index < batchEnd; index++) {
                        List<TsKvEntry> page = pages.get(index - batchStart);
                        consumeTelemetryPages(timeseriesService, user, devices.get(index),
                                query, page, summaries.get(index));
                        // 当前设备处理结束后释放首批页引用，下一轮仅保留紧凑聚合状态。
                        pages.set(index - batchStart, Collections.emptyList());
                        futures.set(index - batchStart, null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ThingsboardException(errorMessage, e, ThingsboardErrorCode.DATABASE);
                } catch (ExecutionException e) {
                    throw new ThingsboardException(errorMessage, e, ThingsboardErrorCode.DATABASE);
                }
            }
        }
        return summaries;
    }

    /** 消费单设备、单键的全部页；只在局部变量中保存当前页，聚合器跨页保留必要状态。 */
    private static void consumeTelemetryPages(
            TimeseriesService timeseriesService, SecurityUser user,
            MonitoringDeviceContextBO device, ReadTsKvQuery query, List<TsKvEntry> firstPage,
            MonitoringTelemetryAccumulator summary) throws ExecutionException, InterruptedException {
        boolean ascending = "ASC".equalsIgnoreCase(query.getOrder());
        long startTs = query.getStartTs();
        long endTs = query.getEndTs();
        List<TsKvEntry> page = firstPage;
        while (true) {
            summary.accept(query, page);
            if (query.getAggregation() != Aggregation.NONE || query.getLimit() <= 1
                    || page.size() < query.getLimit()) {
                return;
            }
            long boundary = ascending ? Long.MIN_VALUE : Long.MAX_VALUE;
            for (TsKvEntry entry : page) {
                boundary = ascending ? Math.max(boundary, entry.getTs())
                        : Math.min(boundary, entry.getTs());
            }
            if (ascending) {
                startTs = boundary + 1;
            } else {
                endTs = boundary;
            }
            if (startTs >= endTs) {
                return;
            }
            ReadTsKvQuery nextPage = new BaseReadTsKvQuery(query, startTs, endTs);
            page = timeseriesService.findAll(user.getTenantId(),
                    new DeviceId(device.getDeviceId()), List.of(nextPage)).get();
        }
    }

    /**
     * 统一读取看板周期告警聚合结果。
     *
     * <p>类型数量、告警总数和告警设备去重数量都在数据库完成，处理器只接收聚合结果，
     * 不再把周期内的告警明细全部加载到 JVM。</p>
     */
    static AlarmRecordAggregate collectAlarmRecords(
            SecurityUser user,
            Set<UUID> deviceIds,
            MonitoringPeriodWindow window,
            AlarmStatisticsService alarmStatisticsService) {
        AlarmRecordAggregate result = AlarmRecordAggregate.empty();
        if (deviceIds.isEmpty()) {
            return result;
        }
        // 数据库返回各告警类型的计数，服务层合并总数并映射类型明细。
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
                user, deviceIds, window.getStartTs(), window.getEndTs(), false);
        for (AlarmDimensionCountBO row : rows) {
            result.count += row.getCount();
            result.typeTotals.add(new AlarmTypeTotalDTO(
                    row.getDimension(), CountValueSupport.toIntCount(row.getCount())));
        }
        // 单独读取周期内告警设备覆盖数，避免把告警条数误当作设备数。
        AlarmOverviewAggregateBO overview = alarmStatisticsService.getOverview(
                user, deviceIds, window.getStartTs(), window.getEndTs());
        result.alarmDeviceCount = CountValueSupport.toIntCount(overview.getAlarmedDeviceCount());
        return result;
    }

    /**
     * 统一筛选看板各板块的功能分类。
     *
     * <p>传入的设备已经由 {@link SmartLightningDeviceService} 完成权限校验和
     * productCategory=lightning 过滤，这里只判断 functionCategory，避免三个处理器
     * 分别维护重复且容易出现口径差异的筛选代码。</p>
     */
    static List<MonitoringDeviceContextBO> findVisibleDevicesByFunctionCategory(
            SmartLightningDeviceService deviceService,
            SecurityUser user,
            String functionCategory) throws ThingsboardException {
        List<MonitoringDeviceContextBO> visibleDevices =
                deviceService.getAllLightningDeviceContexts(user);
        // 在权限和产品分类范围确定后，再按板块功能分类筛选设备。
        return MonitoringDeviceFilters.byFunctionCategory(visibleDevices, functionCategory);
    }

    /** 保存看板告警聚合结果，不保存告警明细或设备 ID 集合。 */
    static final class AlarmRecordAggregate {
        long count;
        int alarmDeviceCount;
        final List<AlarmTypeTotalDTO> typeTotals = new ArrayList<>();

        static AlarmRecordAggregate empty() {
            return new AlarmRecordAggregate();
        }
    }
}
