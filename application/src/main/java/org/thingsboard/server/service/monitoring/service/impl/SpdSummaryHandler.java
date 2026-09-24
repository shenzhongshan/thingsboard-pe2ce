/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.SpdMetricExtremaDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.monitoring.util.SpdMonitoringCalculator;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 汇总 SPD 板块的设备状态、周期指标极值和累计雷击次数。 */
@RequiredArgsConstructor
final class SpdSummaryHandler {


    /** 单次设备、告警或遥测查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;
    /** SPD 监测设备在服务器属性中的功能分类值。 */
    private static final String FUNCTION_CATEGORY = "spd_monitor";
    /** 规则链已经转换后的漏电流规范键；不再读取 I1/I2/I3 原始测点。 */
    private static final List<String> LEAKAGE_KEYS = List.of(
            "LeakCurrA", "LeakCurrB", "LeakCurrC");
    /** 直接读取规则链已转换的接地和空开实时状态，不使用旧测点键。 */
    private static final List<String> GROUND_STATUS_KEYS = List.of("GndStatus");
    private static final List<String> BREAKER_STATUS_KEYS = List.of("McbStatus");
    /** 周期温度最值查询使用的扁平键列表。 */
    private static final List<String> PERIODIC_TEMPERATURE_KEYS = List.of(
            "Temp1", "Temp2", "Temp3", "Temp4", "AmbTemp", "SpdTemp");
    /** 周期电压最值查询使用的规范键。 */
    private static final List<String> VOLTAGE_KEYS = List.of(
            "PhAVolt", "PhBVolt", "PhCVolt");
    /** 规则链输出的累计雷击次数键。 */
    private static final String LIGHTNING_COUNT_KEY = "StrikeCnt";
    private static final List<String> LIFE_KEYS = List.of("LifeLeft");

    /** 复用智能防雷设备服务，负责权限过滤和设备上下文查询。 */
    private final SmartLightningDeviceService smartLightningDeviceService;
    /** 复用公共告警查询服务，避免在本实现中重复处理告警权限分支。 */
    private final AlarmStatisticsService alarmStatisticsService;
    /** ThingsBoard 时序服务，用于查询周期指标和实时状态遥测。 */
    private final TimeseriesService timeseriesService;

    /**
     * 查询当前用户可见 SPD 设备的完整摘要。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param period 统计周期，支持 day、week、month
     * @return 实时设备状态、周期告警数、雷击次数和三类周期最值
     * @throws ThingsboardException 周期参数非法或设备、告警、遥测查询失败时抛出
     */
    public SpdMonitoringSummaryDTO getSummary(SecurityUser user, String period) throws ThingsboardException {
        // 解析周期并确定有权限查看的 SPD 设备范围。
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        List<MonitoringDeviceContextBO> devices = findVisibleSpdDevices(user);
        // 统计在线数和设备 ID，供告警聚合使用。
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
        // 发现设备已有温度键后，一次读取周期指标和各实时状态键。
        List<String> temperatureKeys = findTemperatureKeys(user, devices);
        SpdTelemetryReadResult telemetry = readSpdTelemetry(user, devices, window, temperatureKeys);
        // 周期遥测统一通过 ThingsBoard TimeseriesService 读取，保持 SQL、Timescale、Cassandra
        // 的存储抽象一致，并保留最值来源设备、参数键和原始时间戳。
        // 分别计算漏电流摘要、温度/漏流/电压周期极值、累计雷击数和实时状态。
        LeakageAggregate leakage = findMaximumLeakage(telemetry.entriesByDevice, devices);
        SpdMetricExtremaDTO temperatureExtrema = findMetricExtrema(
                telemetry.entriesByDevice, devices, PERIODIC_TEMPERATURE_KEYS, false,
                window.getStartTs(), window.getEndTs());
        SpdMetricExtremaDTO leakageExtrema = findMetricExtrema(
                telemetry.entriesByDevice, devices, LEAKAGE_KEYS, true,
                window.getStartTs(), window.getEndTs());
        SpdMetricExtremaDTO voltageExtrema = findMetricExtrema(
                telemetry.entriesByDevice, devices, VOLTAGE_KEYS, false,
                window.getStartTs(), window.getEndTs());
        long lightningCount = calculateLightningCount(telemetry.entriesByDevice, window.getStartTs());
        RealtimeStatusAggregate realtime = summarizeRealtimeStatus(telemetry.entriesByDevice, temperatureKeys);
        int total = devices.size();
        return new SpdMonitoringSummaryDTO(
                total,
                online,
                total - online,
                CountValueSupport.toIntCount(alarmAggregate.count),
                alarmAggregate.typeTotals,
                leakage.value,
                leakage.deviceName,
                leakage.phase,
                leakage.key,
                leakage.timestamp,
                leakage.reportingDeviceCount,
                realtime.groundStatus.normal(),
                realtime.groundStatus.participating(),
                realtime.breakerStatus.normal(),
                realtime.breakerStatus.participating(),
                realtime.temperature.normal(),
                realtime.temperature.participating(),
                realtime.degradation.normal(),
                realtime.degradation.participating(),
                lightningCount,
                temperatureExtrema,
                leakageExtrema,
                voltageExtrema);
    }

    /**
     * 分页查询当前用户可见设备，并筛选 SPD 监测设备。
     *
     * <p>设备查询委托给公共设备服务，因此租户、客户用户、共享实体等
     * 权限规则不在本类重复实现；循环翻页直到没有下一页。</p>
     *
     * @param user 当前登录用户
     * @return 功能分类为 spd_monitor 的设备上下文列表
     */
    private List<MonitoringDeviceContextBO> findVisibleSpdDevices(SecurityUser user)
            throws ThingsboardException {
        return SmartLightningDashboardSupport.findVisibleDevicesByFunctionCategory(
                smartLightningDeviceService, user, FUNCTION_CATEGORY);
    }

    /**
     * 一次读取所有 SPD 设备的周期漏流和最新实时参数。
     *
     * <p>每台设备只发起一次异步时序查询，周期指标使用所选窗口，状态、温度和寿命
     * 的实时判断使用最近一条记录，避免前端或服务层按参数重复访问数据库。</p>
     *
     * @param user 当前登录用户
     * @param devices 已经过权限和功能分类筛选的设备
     * @param window 漏流统计窗口
     * @param temperatureKeys 当前设备实际存在的温度键，用于追加最新值查询
     * @return 按设备原顺序排列的时序记录
     * @throws ThingsboardException 时序查询失败时抛出
     */
    private SpdTelemetryReadResult readSpdTelemetry(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices,
            MonitoringPeriodWindow window,
            List<String> temperatureKeys) throws ThingsboardException {
        return new SpdTelemetryReadResult(SmartLightningDashboardSupport.readTelemetryInBatches(
                timeseriesService,
                user,
                devices,
                spdTelemetryQueries(window, temperatureKeys),
                "Failed to read SPD telemetry"));
    }


    /**
     * 从当前用户可见的 SPD 设备已存在遥测键中发现所有温度键。
     *
     * <p>每批使用 ThingsBoard 时序服务查询键名，不假设温度键固定为 Temp1 至 Temp4；
     * 后续型号新增任何包含 Temp 的键也会进入最新状态统计。</p>
     *
     * @param user 当前登录用户
     * @param devices 当前看板权限范围内的 SPD 设备
     * @return 按名称排序的温度遥测键
     */
    private List<String> findTemperatureKeys(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices) {
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量构造设备 ID，在固定批次内查询已有遥测键。
        List<EntityId> entityIds = new ArrayList<>(devices.size());
        for (MonitoringDeviceContextBO device : devices) {
            entityIds.add(new DeviceId(device.getDeviceId()));
        }
        Set<String> temperatureKeys = new HashSet<>();
        for (int batchStart = 0; batchStart < entityIds.size();
             batchStart += SmartLightningDashboardSupport.TELEMETRY_BATCH_SIZE) {
            int batchEnd = Math.min(
                    batchStart + SmartLightningDashboardSupport.TELEMETRY_BATCH_SIZE, entityIds.size());
            List<String> keys = timeseriesService.findAllKeysByEntityIds(
                    user.getTenantId(), entityIds.subList(batchStart, batchEnd));
            // 只保留温度类键并去重，最终按键名排序保证结果稳定。
            if (keys != null) {
                keys.stream()
                        .filter(SpdMonitoringCalculator::isTemperatureKey)
                        .forEach(temperatureKeys::add);
            }
        }
        return temperatureKeys.stream().sorted().toList();
    }
    /** 根据一次读取结果汇总周期内最大漏流及其来源信息。 */
    private LeakageAggregate findMaximumLeakage(
            List<List<TsKvEntry>> entriesByDevice,
            List<MonitoringDeviceContextBO> devices) {
        LeakageAggregate result = LeakageAggregate.empty();
        // 逐台设备扫描漏电流规范键；每台设备只要有有效记录就计入上报设备数。
        for (int index = 0; index < entriesByDevice.size(); index++) {
            List<TsKvEntry> entries = entriesByDevice.get(index);
            boolean reported = false;
            MonitoringDeviceContextBO device = devices.get(index);
            for (TsKvEntry entry : entries) {
                if (!isLeakageKey(entry.getKey())) {
                    continue;
                }
                Double parsed = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (parsed == null) {
                    continue;
                }
                // 漏电流沿用绝对值比较口径，同时保存本次最大值的来源信息。
                reported = true;
                double leakage = Math.abs(parsed);
                if (result.value == null || leakage > result.value) {
                    result.value = leakage;
                    result.deviceName = device.getDeviceName();
                    result.key = entry.getKey();
                    result.phase = phaseFromKey(entry.getKey());
                    result.timestamp = entry.getTs();
                }
            }
            if (reported) {
                result.reportingDeviceCount++;
            }
        }
        return result;
    }

    /**
     * 汇总周期内某一组规范遥测键的最大值和最小值。
     *
     * <p>每个极值都保存来源设备、参数键和时间戳，方便前端悬浮提示直接展示完整来源。
     * 漏电流沿用看板现有口径，比较前取绝对值；温度和电压保持规则链输出值。</p>
     */
    private SpdMetricExtremaDTO findMetricExtrema(
            List<List<TsKvEntry>> entriesByDevice,
            List<MonitoringDeviceContextBO> devices,
            List<String> keys,
            boolean absoluteValue,
            long windowStartTs,
            long windowEndTs) {
        MetricExtremaAggregate result = MetricExtremaAggregate.empty();
        // 过滤键和时间范围后解析有限数值，再按指定口径比较极值。
        for (int index = 0; index < entriesByDevice.size(); index++) {
            MonitoringDeviceContextBO device = devices.get(index);
            for (TsKvEntry entry : entriesByDevice.get(index)) {
                if (!keys.contains(entry.getKey())
                        || entry.getTs() < windowStartTs
                        || entry.getTs() > windowEndTs) {
                    continue;
                }
                Double parsed = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (parsed == null) {
                    continue;
                }
                double value = absoluteValue ? Math.abs(parsed) : parsed;
                if (result.maximum == null || value > result.maximum) {
                    result.maximum = value;
                    result.maximumDeviceName = device.getDeviceName();
                    result.maximumParameter = entry.getKey();
                    result.maximumTime = entry.getTs();
                }
                if (result.minimum == null || value < result.minimum) {
                    result.minimum = value;
                    result.minimumDeviceName = device.getDeviceName();
                    result.minimumParameter = entry.getKey();
                    result.minimumTime = entry.getTs();
                }
            }
        }
        return result.toDTO();
    }

    /**
     * 计算 SPD 设备在所选周期内的 StrikeCnt 增量。
     *
     * <p>该键是累计计数器，不直接把每次上报值相加；先取周期开始前的最近值作为基线，
     * 再按时间顺序计算相邻差值。设备重启导致计数回退时由公共计算器按新计数段处理。</p>
     */
    private long calculateLightningCount(List<List<TsKvEntry>> entriesByDevice, long windowStartTs) {
        long total = 0;
        for (List<TsKvEntry> entries : entriesByDevice) {
            Double baseline = null;
            long baselineTs = Long.MIN_VALUE;
            List<TsKvEntry> readings = new ArrayList<>();
            // 分离周期开始前的最近基线和周期内样本，并忽略无效或负计数。
            for (TsKvEntry entry : entries) {
                if (!LIGHTNING_COUNT_KEY.equals(entry.getKey())) {
                    continue;
                }
                Double value = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (value == null || value < 0) {
                    continue;
                }
                if (entry.getTs() < windowStartTs) {
                    if (entry.getTs() > baselineTs) {
                        baselineTs = entry.getTs();
                        baseline = value;
                    }
                } else {
                    readings.add(entry);
                }
            }
            // 计数器按时间排序后交给公共计算器处理回退、重启和增量累加。
            readings.sort(Comparator.comparingLong(TsKvEntry::getTs));
            List<Double> samples = new ArrayList<>();
            for (TsKvEntry reading : readings) {
                Double value = MonitoringValueParser.parseFiniteDouble(reading.getValueAsString());
                if (value != null) {
                    samples.add(value);
                }
            }
            total += SpdMonitoringCalculator.counterDelta(baseline, samples);
        }
        return total;
    }

    /** 将每台设备最新的接地、空开、温度和寿命值统计为正常设备数及有参数设备总数。 */
    private RealtimeStatusAggregate summarizeRealtimeStatus(
            List<List<TsKvEntry>> entriesByDevice, List<String> temperatureKeys) {
        List<List<String>> groundValues = new ArrayList<>();
        List<List<String>> breakerValues = new ArrayList<>();
        List<List<Double>> temperatureValues = new ArrayList<>();
        List<Double> lifeValues = new ArrayList<>();
        // 对每台设备提取接地、空开、温度及寿命的最新值；缺失值保留为空或不可解析标记。
        for (List<TsKvEntry> entries : entriesByDevice) {
            String groundStatus = latestStatusValue(entries, GROUND_STATUS_KEYS);
            groundValues.add(groundStatus == null ? Collections.emptyList() : List.of(groundStatus));
            String breakerStatus = latestStatusValue(entries, BREAKER_STATUS_KEYS);
            breakerValues.add(breakerStatus == null ? Collections.emptyList() : List.of(breakerStatus));

            List<Double> temperatures = new ArrayList<>();
            for (String key : temperatureKeys) {
                Double value = latestNumericValue(entries, List.of(key));
                if (value != null) {
                    temperatures.add(value);
                }
            }
            temperatureValues.add(temperatures);
            lifeValues.add(latestNumericValue(entries, LIFE_KEYS));
        }
        // 将按设备整理的原始状态列表交给纯计算器生成正常数和参与统计数。
        return new RealtimeStatusAggregate(
                SpdMonitoringCalculator.binaryStatus(groundValues),
                SpdMonitoringCalculator.binaryStatus(breakerValues),
                SpdMonitoringCalculator.temperatureStatus(temperatureValues),
                SpdMonitoringCalculator.degradationStatus(lifeValues));
    }

    /** 直接读取规则链输出的最新状态文本，不在后端转换状态值。 */
    private String latestStatusValue(List<TsKvEntry> entries, List<String> keys) {
        for (String key : keys) {
            TsKvEntry latest = latestEntry(entries, key);
            if (latest == null) {
                continue;
            }
            // 键存在但值为空时仍然返回空字符串，交给统计器计入“有该参数的设备总数”。
            String value = latest.getValueAsString();
            return value == null ? "" : value;
        }
        return null;
    }

    /** 读取指定规范键的最新数值。 */
    private Double latestNumericValue(List<TsKvEntry> entries, List<String> keys) {
        for (String key : keys) {
            TsKvEntry latest = latestEntry(entries, key);
            if (latest == null) {
                continue;
            }
            Double value = MonitoringValueParser.parseFiniteDouble(latest.getValueAsString());
            // NaN 只在服务内部表示“键存在但值无法解析”，用于保持参数设备总数准确。
            return value == null ? Double.NaN : value;
        }
        return null;
    }

    /** 返回指定键时间戳最新的一条记录。 */
    private TsKvEntry latestEntry(List<TsKvEntry> entries, String key) {
        TsKvEntry latest = null;
        // 不能依赖查询结果顺序，显式按时间戳选择最新记录。
        for (TsKvEntry entry : entries) {
            if (key.equals(entry.getKey())
                    && (latest == null || entry.getTs() > latest.getTs())) {
                latest = entry;
            }
        }
        return latest;
    }

    /** 仅允许漏电流规范键进入最大漏流聚合，避免混入其他实时参数。 */
    private boolean isLeakageKey(String key) {
        return LEAKAGE_KEYS.contains(key);
    }

    /** 为周期漏流和实时参数创建一次批量时序查询条件。 */
    private List<ReadTsKvQuery> spdTelemetryQueries(
            MonitoringPeriodWindow window, List<String> temperatureKeys) {
        List<ReadTsKvQuery> queries = new ArrayList<>();
        List<String> periodicKeys = new ArrayList<>();
        // 周期遥测统一通过 ThingsBoard 时序服务读取，不直接依赖底层 ts_kv 表。
        periodicKeys.addAll(LEAKAGE_KEYS);
        periodicKeys.addAll(PERIODIC_TEMPERATURE_KEYS);
        periodicKeys.addAll(VOLTAGE_KEYS);
        periodicKeys.add(LIGHTNING_COUNT_KEY);
        for (String key : periodicKeys) {
            queries.add(new BaseReadTsKvQuery(
                    key,
                    window.getStartTs(),
                    window.getEndTs(),
                    0,
                    QUERY_PAGE_SIZE,
                    Aggregation.NONE,
                    "DESC"));
        }
        // 累计雷击次数需要周期开始前的最近值作为差值基线。
        queries.add(new BaseReadTsKvQuery(
                LIGHTNING_COUNT_KEY,
                0,
                window.getStartTs(),
                1,
                "DESC"));
        addLatestQueries(queries, GROUND_STATUS_KEYS);
        addLatestQueries(queries, BREAKER_STATUS_KEYS);
        addLatestQueries(queries, temperatureKeys);
        addLatestQueries(queries, LIFE_KEYS);
        return queries;
    }

    /** 为每个规范遥测键读取最新一条记录。 */
    private void addLatestQueries(List<ReadTsKvQuery> queries, List<String> keys) {
        long now = System.currentTimeMillis();
        for (String key : keys) {
            queries.add(new BaseReadTsKvQuery(key, 0, now, 1, "DESC"));
        }
    }

    /**
     * 根据规范遥测键的最后一个字符判定相位。
     *
     * <p>详情展示不依赖设备型号或参数在列表中的位置，只认参数键后缀：
     * LeakCurrA、LeakCurrB、LeakCurrC 分别对应 A相、B相、C相。</p>
     */
    private String phaseFromKey(String key) {
        if (key == null || key.isBlank()) {
            return "未知相位";
        }
        char suffix = Character.toUpperCase(key.trim().charAt(key.trim().length() - 1));
        if (suffix == 'A' || suffix == 'B' || suffix == 'C') {
            return suffix + "相";
        }
        return "未知相位";
    }

    /** 保存一次批量时序查询返回的各设备记录，避免同一设备重复请求。 */
    private static final class SpdTelemetryReadResult {
        private final List<List<TsKvEntry>> entriesByDevice;

        private SpdTelemetryReadResult(List<List<TsKvEntry>> entriesByDevice) {
            this.entriesByDevice = entriesByDevice;
        }
    }

    /** 保存四类实时参数的正常数和有参数设备数。 */
    private static final class RealtimeStatusAggregate {
        private final SpdMonitoringCalculator.StatusCount groundStatus;
        private final SpdMonitoringCalculator.StatusCount breakerStatus;
        private final SpdMonitoringCalculator.StatusCount temperature;
        private final SpdMonitoringCalculator.StatusCount degradation;

        private RealtimeStatusAggregate(
                SpdMonitoringCalculator.StatusCount groundStatus,
                SpdMonitoringCalculator.StatusCount breakerStatus,
                SpdMonitoringCalculator.StatusCount temperature,
                SpdMonitoringCalculator.StatusCount degradation) {
            this.groundStatus = groundStatus;
            this.breakerStatus = breakerStatus;
            this.temperature = temperature;
            this.degradation = degradation;
        }
    }

    /** 保存一组周期数值的最大值、最小值及其来源，最终转换为 DTO。 */
    private static final class MetricExtremaAggregate {
        private Double maximum;
        private String maximumDeviceName;
        private String maximumParameter;
        private Long maximumTime;
        private Double minimum;
        private String minimumDeviceName;
        private String minimumParameter;
        private Long minimumTime;

        private static MetricExtremaAggregate empty() {
            return new MetricExtremaAggregate();
        }

        private SpdMetricExtremaDTO toDTO() {
            return new SpdMetricExtremaDTO(
                    maximum,
                    maximumDeviceName,
                    maximumParameter,
                    maximumTime,
                    minimum,
                    minimumDeviceName,
                    minimumParameter,
                    minimumTime);
        }
    }

    /** 保存所有设备合并后的最大漏电流聚合结果。 */
    private static final class LeakageAggregate {
        /** 周期内最大漏电流（绝对值）；无任何记录时为 null。 */
        private Double value;
        /** 出现最大漏电流的设备名称。 */
        private String deviceName;
        /** 最大漏电流对应的相位。 */
        private String phase;
        /** 产生最大漏电流的规范遥测键，用于详情按键名后缀显示相位。 */
        private String key;
        /** 最大漏电流发生时间（毫秒时间戳）。 */
        private Long timestamp;
        /** 周期内上报过漏电流遥测的设备数量。 */
        private int reportingDeviceCount;

        /** 创建一个没有任何遥测结果的空聚合对象。 */
        private static LeakageAggregate empty() {
            return new LeakageAggregate();
        }
    }
}
