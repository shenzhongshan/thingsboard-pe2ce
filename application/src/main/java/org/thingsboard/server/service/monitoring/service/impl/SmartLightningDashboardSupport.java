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
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.AlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringDeviceFilters;
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
     * 统一分批读取一组设备的周期遥测，并保持返回结果与设备列表顺序一致。
     *
     * <p>ThingsBoard 当前没有“多个实体 + 多个历史键”的同步批量接口，因此这里仍通过
     * {@link TimeseriesService} 并发读取，但统一限制每批 Future 数量。单台设备失败时返回
     * 空列表，不影响同批其他设备；调用方只负责自己的指标聚合。</p>
     */
    static List<List<TsKvEntry>> readTelemetryInBatches(
            TimeseriesService timeseriesService,
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices,
            List<ReadTsKvQuery> queries,
            String errorMessage) throws ThingsboardException {
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
        // 按固定上限切分设备列表，控制每轮并发请求数并保持原始设备顺序。
        List<List<TsKvEntry>> entriesByDevice = new ArrayList<>();
        for (int batchStart = 0; batchStart < devices.size(); batchStart += TELEMETRY_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + TELEMETRY_BATCH_SIZE, devices.size());
            List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
            for (int index = batchStart; index < batchEnd; index++) {
                MonitoringDeviceContextBO device = devices.get(index);
                futures.add(timeseriesService.findAll(
                        user.getTenantId(), new DeviceId(device.getDeviceId()), queries));
            }
            try {
                // 等待当前批次完成；成功列表会把单项失败表示为空结果，便于保留其他设备数据。
                List<List<TsKvEntry>> batchEntries = Futures.successfulAsList(futures).get();
                for (List<TsKvEntry> entries : batchEntries) {
                    entriesByDevice.add(entries == null ? Collections.emptyList() : entries);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ThingsboardException(errorMessage, e, ThingsboardErrorCode.DATABASE);
            } catch (ExecutionException e) {
                throw new ThingsboardException(errorMessage, e, ThingsboardErrorCode.DATABASE);
            }
        }
        return entriesByDevice;
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
