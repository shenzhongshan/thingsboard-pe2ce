/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.dto.SpdAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMetricExtrema;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummary;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.service.SpdMonitoringSummaryService;
import org.thingsboard.server.service.monitoring.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.monitoring.util.SpdMonitoringCalculator;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * SPD 监测聚合实现。
 *
 * <p>该类只处理 {@code functionCategory=spd_monitor} 的设备，先按当前用户
 * 权限获取设备，再读取规则链输出的漏电流规范遥测键（leakageA/B/C），同时读取各设备最新的接地、SPD、温度和寿命参数，最后汇总设备状态、
 * 周期内告警数、雷击次数以及温度、漏电流、电压最值。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SpdMonitoringSummaryServiceImpl implements SpdMonitoringSummaryService {

    /** 单次设备、告警或遥测查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;
    /** SPD 监测设备在服务器属性中的功能分类值。 */
    private static final String FUNCTION_CATEGORY = "spd_monitor";
    /** 防雷产品大类，避免把同名功能分类的其他产品纳入 SPD 统计。 */
    private static final String PRODUCT_CATEGORY = "lightning";
    /** 规则链已经转换后的漏电流规范键；不再读取 I1/I2/I3 原始测点。 */
    private static final List<String> LEAKAGE_KEYS = List.of(
            "leakageA", "leakageB", "leakageC");
    /** 规则链已经转换后的实时状态键；不再读取或转换 Y/G 原始测点。 */
    private static final List<String> GROUND_STATUS_KEYS = List.of("groundStatus");
    private static final List<String> SPD_STATUS_KEYS = List.of("spdStatus");
    private static final List<List<String>> TEMPERATURE_KEYS = List.of(
            List.of("temperatureA"),
            List.of("temperatureB"),
            List.of("temperatureC"),
            List.of("temperatureD"));
    /** 周期温度最值查询使用的扁平键列表。 */
    private static final List<String> PERIODIC_TEMPERATURE_KEYS = List.of(
            "temperatureA", "temperatureB", "temperatureC", "temperatureD");
    /** 周期电压最值查询使用的规范键。 */
    private static final List<String> VOLTAGE_KEYS = List.of(
            "voltageA", "voltageB", "voltageC");
    /** 规则链输出的累计雷击次数键。 */
    private static final String LIGHTNING_COUNT_KEY = "lightningCount";
    private static final List<String> LIFE_KEYS = List.of("lifePercent");

    /** 复用智能防雷设备服务，负责权限过滤和设备上下文查询。 */
    private final SmartLightningDeviceService smartLightningDeviceService;
    /** 复用公共告警查询服务，避免在本实现中重复处理告警权限分支。 */
    private final MonitoringAlarmQueryService alarmQueryService;
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
    @Override
    public SpdMonitoringSummary getSummary(SecurityUser user, String period) throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        List<SmartLightningDeviceContextDTO> devices = findVisibleSpdDevices(user);
        List<EntityId> deviceIds = new ArrayList<>();
        int online = 0;
        for (SmartLightningDeviceContextDTO device : devices) {
            deviceIds.add(new DeviceId(device.getDeviceId()));
            if (Boolean.TRUE.equals(device.getActive())) {
                online++;
            }
        }

        AlarmAggregate alarmAggregate = countAlarms(user, deviceIds, window);
        SpdTelemetryReadResult telemetry = readSpdTelemetry(user, devices, window);
        LeakageAggregate leakage = findMaximumLeakage(telemetry.entriesByDevice, devices);
        SpdMetricExtrema temperatureExtrema = findMetricExtrema(
                telemetry.entriesByDevice, devices, PERIODIC_TEMPERATURE_KEYS, false,
                window.getStartTs(), window.getEndTs());
        SpdMetricExtrema leakageExtrema = findMetricExtrema(
                telemetry.entriesByDevice, devices, LEAKAGE_KEYS, true,
                window.getStartTs(), window.getEndTs());
        SpdMetricExtrema voltageExtrema = findMetricExtrema(
                telemetry.entriesByDevice, devices, VOLTAGE_KEYS, false,
                window.getStartTs(), window.getEndTs());
        long lightningCount = calculateLightningCount(telemetry.entriesByDevice, window.getStartTs());
        RealtimeStatusAggregate realtime = summarizeRealtimeStatus(telemetry.entriesByDevice);
        int total = devices.size();
        return new SpdMonitoringSummary(
                total,
                online,
                total - online,
                alarmAggregate.count,
                alarmAggregate.typeTotals,
                leakage.value,
                leakage.deviceName,
                leakage.phase,
                leakage.key,
                leakage.timestamp,
                leakage.reportingDeviceCount,
                realtime.groundStatus.normal(),
                realtime.groundStatus.participating(),
                realtime.spdStatus.normal(),
                realtime.spdStatus.participating(),
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
    private List<SmartLightningDeviceContextDTO> findVisibleSpdDevices(SecurityUser user)
            throws ThingsboardException {
        List<SmartLightningDeviceContextDTO> devices = new ArrayList<>();
        PageLink pageLink = new PageLink(QUERY_PAGE_SIZE, 0);
        while (pageLink != null) {
            PageData<SmartLightningDeviceContextDTO> page =
                    smartLightningDeviceService.getLightningDeviceContexts(user, pageLink);
            for (SmartLightningDeviceContextDTO device : page.getData()) {
                if (isSpdDevice(device)) {
                    devices.add(device);
                }
            }
            pageLink = page.hasNext() ? pageLink.nextPageLink() : null;
        }
        return devices;
    }

    /** 同时校验产品大类和功能分类，保证 SPD 卡片只统计防雷 SPD 设备。 */
    private boolean isSpdDevice(SmartLightningDeviceContextDTO device) {
        return PRODUCT_CATEGORY.equalsIgnoreCase(
                        MonitoringValueParser.trimToEmpty(device.getProductCategory()))
                && FUNCTION_CATEGORY.equalsIgnoreCase(
                        MonitoringValueParser.trimToEmpty(device.getFunctionCategory()));
    }

    /**
     * 统计周期内属于指定 SPD 设备的告警数量。
     *
     * <p>查询使用告警创建时间窗口并保留 ANY 状态，已清除的历史告警
     * 也参与周期统计；设备集合用于排除其他类型设备的告警。</p>
     *
     * @param user 当前登录用户
     * @param deviceIds SPD 设备实体 ID
     * @param window 当前统计周期的起止时间
     * @return 周期内告警条数及按告警类型降序排列的统计结果
     */
    private AlarmAggregate countAlarms(SecurityUser user, List<EntityId> deviceIds, MonitoringPeriodWindow window)
            throws ThingsboardException {
        if (deviceIds.isEmpty()) {
            return AlarmAggregate.empty();
        }
        Set<UUID> deviceIdSet = new HashSet<>();
        for (EntityId deviceId : deviceIds) {
            deviceIdSet.add(deviceId.getId());
        }
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE,
                0,
                null,
                SortOrder.BY_CREATED_TIME_DESC,
                window.getStartTs(),
                window.getEndTs());
        AlarmQueryV2 query = new AlarmQueryV2(
                null,
                pageLink,
                Collections.emptyList(),
                List.of(AlarmSearchStatus.ANY),
                Collections.emptyList(),
                null);
        Map<String, Integer> countsByType = new HashMap<>();
        int count = 0;
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            EntityId originator = alarm.getOriginator();
            if (originator != null
                    && originator.getEntityType() == EntityType.DEVICE
                    && deviceIdSet.contains(originator.getId())) {
                String type = alarm.getType() == null || alarm.getType().isBlank()
                        ? "未分类" : alarm.getType().trim();
                countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
                count++;
            }
        }
        List<SpdAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByType.entrySet()) {
            typeTotals.add(new SpdAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
        }
        typeTotals.sort(new Comparator<SpdAlarmTypeTotalDTO>() {
            @Override
            public int compare(SpdAlarmTypeTotalDTO left, SpdAlarmTypeTotalDTO right) {
                int countOrder = Integer.compare(right.getCount(), left.getCount());
                return countOrder != 0 ? countOrder : left.getType().compareTo(right.getType());
            }
        });
        return new AlarmAggregate(count, typeTotals);
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
     * @return 按设备原顺序排列的时序记录
     * @throws ThingsboardException 时序查询失败时抛出
     */
    private SpdTelemetryReadResult readSpdTelemetry(
            SecurityUser user,
            List<SmartLightningDeviceContextDTO> devices,
            MonitoringPeriodWindow window) throws ThingsboardException {
        if (devices.isEmpty()) {
            return new SpdTelemetryReadResult(Collections.emptyList());
        }
        List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
        for (SmartLightningDeviceContextDTO device : devices) {
            futures.add(timeseriesService.findAll(
                    user.getTenantId(),
                    new DeviceId(device.getDeviceId()),
                    spdTelemetryQueries(window)));
        }

        List<List<TsKvEntry>> entriesByDevice;
        try {
            entriesByDevice = Futures.allAsList(futures).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThingsboardException(
                    "Failed to read SPD telemetry", e, ThingsboardErrorCode.DATABASE);
        } catch (ExecutionException e) {
            throw new ThingsboardException(
                "Failed to read SPD telemetry", e, ThingsboardErrorCode.DATABASE);
        }
        return new SpdTelemetryReadResult(entriesByDevice);
    }

    /** 根据一次读取结果汇总周期内最大漏流及其来源信息。 */
    private LeakageAggregate findMaximumLeakage(
            List<List<TsKvEntry>> entriesByDevice,
            List<SmartLightningDeviceContextDTO> devices) {
        LeakageAggregate result = LeakageAggregate.empty();
        for (int index = 0; index < entriesByDevice.size(); index++) {
            List<TsKvEntry> entries = entriesByDevice.get(index);
            boolean reported = false;
            SmartLightningDeviceContextDTO device = devices.get(index);
            for (TsKvEntry entry : entries) {
                if (!isLeakageKey(entry.getKey())) {
                    continue;
                }
                Double parsed = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (parsed == null) {
                    continue;
                }
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
    private SpdMetricExtrema findMetricExtrema(
            List<List<TsKvEntry>> entriesByDevice,
            List<SmartLightningDeviceContextDTO> devices,
            List<String> keys,
            boolean absoluteValue,
            long windowStartTs,
            long windowEndTs) {
        MetricExtremaAggregate result = MetricExtremaAggregate.empty();
        for (int index = 0; index < entriesByDevice.size(); index++) {
            SmartLightningDeviceContextDTO device = devices.get(index);
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
        return result.toDto();
    }

    /**
     * 计算 SPD 设备在所选周期内的 lightningCount 增量。
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
            readings.sort(new Comparator<TsKvEntry>() {
                @Override
                public int compare(TsKvEntry left, TsKvEntry right) {
                    return Long.compare(left.getTs(), right.getTs());
                }
            });
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

    /** 将规则链输出的每台设备最新状态、温度和寿命值统计为四组正常数/有参数设备数。 */
    private RealtimeStatusAggregate summarizeRealtimeStatus(List<List<TsKvEntry>> entriesByDevice) {
        List<List<String>> groundValues = new ArrayList<>();
        List<List<String>> spdValues = new ArrayList<>();
        List<List<Double>> temperatureValues = new ArrayList<>();
        List<Double> lifeValues = new ArrayList<>();
        for (List<TsKvEntry> entries : entriesByDevice) {
            groundValues.add(singleValueAsList(latestStatusValue(entries, GROUND_STATUS_KEYS)));
            spdValues.add(singleValueAsList(latestStatusValue(entries, SPD_STATUS_KEYS)));

            List<Double> temperatures = new ArrayList<>();
            for (List<String> keys : TEMPERATURE_KEYS) {
                Double value = latestTemperatureValue(entries, keys);
                if (value != null) {
                    temperatures.add(value);
                }
            }
            temperatureValues.add(temperatures);
            lifeValues.add(latestLifeValue(entries));
        }
        return new RealtimeStatusAggregate(
                SpdMonitoringCalculator.binaryStatus(groundValues),
                SpdMonitoringCalculator.binaryStatus(spdValues),
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

    /** 直接读取规则链输出的最新温度值，不在后端做单位换算。 */
    private Double latestTemperatureValue(List<TsKvEntry> entries, List<String> keys) {
        return latestNumericValue(entries, keys);
    }

    /** 直接读取规则链输出的最新寿命值，不在后端做单位换算。 */
    private Double latestLifeValue(List<TsKvEntry> entries) {
        return latestNumericValue(entries, LIFE_KEYS);
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
        for (TsKvEntry entry : entries) {
            if (key.equals(entry.getKey())
                    && (latest == null || entry.getTs() > latest.getTs())) {
                latest = entry;
            }
        }
        return latest;
    }

    /** 将单个值转为状态计算器所需的设备通道列表。 */
    private List<String> singleValueAsList(String value) {
        return value == null ? Collections.emptyList() : List.of(value);
    }

    /** 仅允许漏电流规范键进入最大漏流聚合，避免混入其他实时参数。 */
    private boolean isLeakageKey(String key) {
        return LEAKAGE_KEYS.contains(key);
    }

    /** 为周期漏流和实时参数创建一次批量时序查询条件。 */
    private List<ReadTsKvQuery> spdTelemetryQueries(MonitoringPeriodWindow window) {
        List<ReadTsKvQuery> queries = new ArrayList<>();
        List<String> periodicKeys = new ArrayList<>();
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
        addLatestQueries(queries, SPD_STATUS_KEYS);
        for (List<String> keys : TEMPERATURE_KEYS) {
            addLatestQueries(queries, keys);
        }
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
     * leakageA、leakageB、leakageC 分别对应 A相、B相、C相。</p>
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

    /** 保存 SPD 周期告警总数及按类型排序后的统计项。 */
    private static final class AlarmAggregate {
        private final int count;
        private final List<SpdAlarmTypeTotalDTO> typeTotals;

        private AlarmAggregate(int count, List<SpdAlarmTypeTotalDTO> typeTotals) {
            this.count = count;
            this.typeTotals = typeTotals;
        }

        private static AlarmAggregate empty() {
            return new AlarmAggregate(0, Collections.emptyList());
        }
    }

    /** 保存四类实时参数的正常数和有参数设备数。 */
    private static final class RealtimeStatusAggregate {
        private final SpdMonitoringCalculator.StatusCount groundStatus;
        private final SpdMonitoringCalculator.StatusCount spdStatus;
        private final SpdMonitoringCalculator.StatusCount temperature;
        private final SpdMonitoringCalculator.StatusCount degradation;

        private RealtimeStatusAggregate(
                SpdMonitoringCalculator.StatusCount groundStatus,
                SpdMonitoringCalculator.StatusCount spdStatus,
                SpdMonitoringCalculator.StatusCount temperature,
                SpdMonitoringCalculator.StatusCount degradation) {
            this.groundStatus = groundStatus;
            this.spdStatus = spdStatus;
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

        private SpdMetricExtrema toDto() {
            return new SpdMetricExtrema(
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
