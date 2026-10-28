/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.monitoring.util.MonitoringTelemetryAccumulator;
import org.thingsboard.server.service.monitoring.util.LightningCurrentProtocolSupport;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 汇总雷电流板块的周期告警、累计雷击次数和峰值范围。 */
@RequiredArgsConstructor
final class LightningCurrentSummaryHandler {


    /** 单次设备、告警或遥测查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;

    /** 雷电流监测设备在服务器属性中的功能分类值。 */
    private static final String FUNCTION_CATEGORY = "lightning_current";
    /** 规则链输出的雷电监测规范键，不直接查询设备原始测点键。 */
    private static final List<String> TELEMETRY_KEYS = List.of(
            "PeakCur", "StrikeCnt", "SurgeCnt");

    /** 复用智能防雷设备服务，负责权限过滤和设备上下文查询。 */
    private final SmartLightningDeviceService smartLightningDeviceService;

    /** 复用公共告警查询服务，避免在本实现中重复处理告警权限。 */
    private final AlarmStatisticsService alarmStatisticsService;

    /** ThingsBoard 时序服务，用于查询设备规范遥测。 */
    private final TimeseriesService timeseriesService;

    /**
     * 查询当前用户可见雷电流设备的完整摘要。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param period 统计周期，支持 day、week、month
     * @return 设备数量、在线/离线数量、告警数、雷击次数和最大峰值
     * @throws ThingsboardException 周期参数非法或设备、告警、遥测查询失败时抛出
     */
    public LightningCurrentMonitoringSummaryDTO getSummary(SecurityUser user, String period)
            throws ThingsboardException {
        // 解析统计窗口，并按统一权限及功能分类规则取得雷电流设备。
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        List<MonitoringDeviceContextBO> devices = findVisibleLightningDevices(user);
        // 建立设备 ID 范围并统计在线设备，后续告警和遥测查询均使用这批设备。
        Set<UUID> deviceIds = new HashSet<>();
        int online = 0;
        for (MonitoringDeviceContextBO device : devices) {
            deviceIds.add(device.getDeviceId());
            if (Boolean.TRUE.equals(device.getActive())) {
                online++;
            }
        }

        SmartLightningDashboardSupport.AlarmRecordAggregate alarmAggregate = SmartLightningDashboardSupport.collectAlarmRecords(
                user, deviceIds, window, alarmStatisticsService);
        // 汇总遥测事件数、峰值极值及其设备、时间和极性来源。
        LightningAggregate aggregate = findLightningAggregate(user, devices, window);
        // 将设备、告警和遥测结果合并成对外摘要，计数在 DTO 边界转换为接口类型。
        int total = devices.size();
        return new LightningCurrentMonitoringSummaryDTO(
                total,
                online,
                total - online,
                CountValueSupport.toIntCount(alarmAggregate.count),
                alarmAggregate.typeTotals,
                aggregate.eventCount,
                aggregate.maxPeakCurrent,
                "kA",
                aggregate.maxPeakDeviceName,
                aggregate.maxPeakTime,
                aggregate.maxPeakPolarity,
                aggregate.minPeakCurrent,
                aggregate.minPeakDeviceName,
                aggregate.minPeakTime,
                aggregate.minPeakPolarity,
                aggregate.reportingDeviceCount);
    }

    /**
     * 分页查询当前用户可见设备，并筛选雷电流监测设备。
     *
     * <p>设备查询委托给公共设备服务，因此租户、客户用户、共享实体等权限规则
     * 不在本类重复实现。</p>
     *
     * @param user 当前登录用户
     * @return 功能分类为 lightning_current 的设备上下文列表
     */
    private List<MonitoringDeviceContextBO> findVisibleLightningDevices(SecurityUser user)
            throws ThingsboardException {
        return SmartLightningDashboardSupport.findVisibleDevicesByFunctionCategory(
                smartLightningDeviceService, user, FUNCTION_CATEGORY);
    }

    /**
     * 分批并行读取雷电流设备的规范遥测，并汇总事件数与峰值范围。
     *
     * <p>每批最多创建固定数量的异步查询，当前批次完成后立即合并并释放结果，
     * 避免大租户一次性创建大量 Future 或把全部设备的时序结果同时保留在内存中。</p>
     *
     * @param user 当前登录用户
     * @param devices 已经过权限和功能分类筛选的设备
     * @param window 遥测查询的起止时间
     * @return 雷电事件和峰值的中间聚合结果
     * @throws ThingsboardException 时序查询失败时抛出
     */
    private LightningAggregate findLightningAggregate(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices,
            MonitoringPeriodWindow window) throws ThingsboardException {
        if (devices.isEmpty()) {
            return LightningAggregate.empty();
        }
        LightningAggregate result = LightningAggregate.empty();
        // 按设备分批读取周期规范键，并确保结果索引与设备列表一一对应。
        List<MonitoringTelemetryAccumulator> summaries = SmartLightningDashboardSupport.readTelemetryInBatches(
                timeseriesService,
                user,
                devices,
                telemetryQueries(window),
                Set.of("StrikeCnt", "SurgeCnt"),
                "Failed to read lightning telemetry");
        for (int index = 0; index < summaries.size(); index++) {
            MonitoringDeviceContextBO device = devices.get(index);
            List<TsKvEntry> entries = summaries.get(index).representativeEntries();
            // 先计算单设备峰值范围，再将该设备的极值合并到全部设备结果。
            PeakRangeResult peakRange = findPeakRange(entries);
            if (peakRange.maximum != null) {
                result.reportingDeviceCount++;
                if (result.maxPeakCurrent == null || peakRange.maximum > result.maxPeakCurrent) {
                    result.maxPeakCurrent = peakRange.maximum;
                    result.maxPeakDeviceName = device.getDeviceName();
                    result.maxPeakDevice = device;
                    result.maxPeakTime = peakRange.maximumTime;
                }
                if (peakRange.minimum != null
                        && (result.minPeakCurrent == null || peakRange.minimum < result.minPeakCurrent)) {
                    result.minPeakCurrent = peakRange.minimum;
                    result.minPeakDeviceName = device.getDeviceName();
                    result.minPeakDevice = device;
                    result.minPeakTime = peakRange.minimumTime;
                }
            }
            // 按型号计算增量；无基线的单条累计值不可确定，整个汇总保持未知，避免少计后显示为完整总数。
            Long delta = summaries.get(index).counterDelta(LightningCurrentProtocolSupport.counterKey(device));
            if (delta == null) {
                result.eventCount = null;
            } else if (result.eventCount != null) {
                result.eventCount += delta;
            }
        }
        resolveAggregatePolarities(user, result);
        return result;
    }

    /**
     * 为峰值及不同型号计数键创建周期查询条件；极性在最终极值确定后精确读取。
     *
     * @param window 查询的起止时间
     * @return 周期内峰值和两种计数键，以及周期开始前的计数基线查询
     */
    private List<ReadTsKvQuery> telemetryQueries(MonitoringPeriodWindow window) {
        List<ReadTsKvQuery> queries = new ArrayList<>();
        for (String key : TELEMETRY_KEYS) {
            queries.add(new BaseReadTsKvQuery(key,
                    window.getStartTs(),
                    window.getEndTs(),
                    0,
                    QUERY_PAGE_SIZE,
                    Aggregation.NONE,
                    "ASC"));
        }
        // 仅为累计计数器读取周期开始前最近一条记录，供周期增量计算使用。
        // 两种型号分别使用各自的计数键，聚合时只选择当前型号对应的一组记录。
        for (String countKey : List.of("StrikeCnt", "SurgeCnt")) {
            queries.add(new BaseReadTsKvQuery(countKey, 0, window.getStartTs(), 1, "DESC"));
        }
        return queries;
    }

    /**
     * 从单台设备的规范峰值代表样本中确定最大值、最小值及各自时间戳。
     *
     * @param entries 当前设备在统计周期内的遥测记录
     * @return 该设备的峰值范围及最大、最小值对应的时间；极性留待全局极值确定后读取
     */
    private PeakRangeResult findPeakRange(List<TsKvEntry> entries) {
        PeakRangeResult result = PeakRangeResult.empty();
        for (TsKvEntry entry : entries) {
            if (!LightningCurrentProtocolSupport.PEAK_CURRENT_KEY.equals(entry.getKey())) {
                continue;
            }
            Double value = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
            if (value == null) {
                continue;
            }
            // 峰值已为设备上报工程值，后端仅取绝对值比较，不进行单位换算。
            double normalizedValue = Math.abs(value);
            if (result.maximum == null || normalizedValue > result.maximum) {
                result.maximum = normalizedValue;
                result.maximumTime = entry.getTs();
            }
            if (result.minimum == null || normalizedValue < result.minimum) {
                result.minimum = normalizedValue;
                result.minimumTime = entry.getTs();
            }
        }
        return result;
    }

    /** 精确读取最终极值同设备、同时间戳的极性；缺失返回null，查询失败向上传播。 */
    private String readPeakPolarity(SecurityUser user, MonitoringDeviceContextBO device, long timestamp)
            throws ThingsboardException {
        List<ReadTsKvQuery> queries = List.of(
                new BaseReadTsKvQuery("Polarity", timestamp, timestamp + 1, 1, "ASC"));
        List<MonitoringTelemetryAccumulator> summaries = SmartLightningDashboardSupport.readTelemetryInBatches(
                timeseriesService, user, List.of(device), queries, Set.of(), "Failed to read peak polarity");
        return findPolarityAtTimestamp(summaries.get(0).representativeEntries(), timestamp,
                LightningCurrentProtocolSupport.isM1ai(device));
    }

    /** 只为全局最终两个极值读取极性，整个请求最多追加两个时间点查询，不逐设备重复查询。 */
    private void resolveAggregatePolarities(SecurityUser user, LightningAggregate aggregate)
            throws ThingsboardException {
        if (aggregate.maxPeakCurrent == null) {
            return;
        }
        aggregate.maxPeakPolarity = readPeakPolarity(user, aggregate.maxPeakDevice, aggregate.maxPeakTime);
        if (aggregate.maxPeakDevice.getDeviceId().equals(aggregate.minPeakDevice.getDeviceId())
                && aggregate.maxPeakTime.equals(aggregate.minPeakTime)) {
            aggregate.minPeakPolarity = aggregate.maxPeakPolarity;
        } else {
            aggregate.minPeakPolarity = readPeakPolarity(user, aggregate.minPeakDevice, aggregate.minPeakTime);
        }
    }

    /**
     * 从单台设备的遥测中匹配与峰值时间戳完全相同的Polarity。
     * 一次报文同时上报全部测点；缺少同时间戳极性时返回空，不借用其他事件的极性。
     *
     * @param entries 当前设备的周期遥测
     * @param timestamp 峰值遥测时间戳（毫秒）
     * @param m1ai 是否采用M1AI新版极性定义
     * @return 中文极性名称；没有对应记录时返回 null
     */
    private String findPolarityAtTimestamp(List<TsKvEntry> entries, long timestamp, boolean m1ai) {
        for (TsKvEntry entry : entries) {
            if ("Polarity".equals(entry.getKey()) && entry.getTs() == timestamp) {
                return LightningCurrentProtocolSupport.polarityLabel(entry.getValueAsString(), m1ai);
            }
        }
        return null;
    }


    /** 保存所有设备合并后的中间聚合结果。 */
    private static final class LightningAggregate {
        /** 任一设备增量不可确定时为null，否则汇总各设备已观测增量。 */
        private Long eventCount = 0L;
        private Double maxPeakCurrent;
        private String maxPeakDeviceName;
        /** 全局最大值来源设备，仅供聚合结束后精确读取极性。 */
        private MonitoringDeviceContextBO maxPeakDevice;
        private Long maxPeakTime;
        private String maxPeakPolarity;
        private Double minPeakCurrent;
        private String minPeakDeviceName;
        /** 全局最小值来源设备，不向接口额外暴露内部上下文。 */
        private MonitoringDeviceContextBO minPeakDevice;
        private Long minPeakTime;
        private String minPeakPolarity;
        private int reportingDeviceCount;

        /** 创建一个没有任何遥测结果的空聚合对象。 */
        private static LightningAggregate empty() {
            return new LightningAggregate();
        }
    }

    /** 保存单台设备的峰值范围及其关联信息。 */
    private static final class PeakRangeResult {
        private Double maximum;
        private Long maximumTime;
        private Double minimum;
        private Long minimumTime;

        /** 创建一个没有峰值记录的空结果。 */
        private static PeakRangeResult empty() {
            return new PeakRangeResult();
        }
    }

}
