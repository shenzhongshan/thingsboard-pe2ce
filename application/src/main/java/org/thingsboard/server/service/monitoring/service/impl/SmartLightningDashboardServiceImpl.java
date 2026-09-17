/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.AlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.GroundResistanceExtremaDTO;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.SpdMetricExtremaDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.monitoring.util.MonitoringDeviceFilters;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.monitoring.util.SpdMonitoringCalculator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.service.SmartLightningDashboardService;

/**
 * 智能防雷看板页面级服务实现。
 *
 * <p>对外提供一个页面级实现；内部按地网、SPD 和雷电流三个板块隔离原有逻辑。
 * 此次仅调整代码组织，遥测键、周期边界、告警口径和返回结构保持不变。</p>
 */
@Service
@TbCoreComponent
public class SmartLightningDashboardServiceImpl implements SmartLightningDashboardService {

    /** 单批时序请求上限，避免设备数量大时一次创建过多 Future。 */
    private static final int TELEMETRY_BATCH_SIZE = 50;

    private final GroundSummaryHandler groundSummaryHandler;
    private final SpdSummaryHandler spdSummaryHandler;
    private final LightningCurrentSummaryHandler lightningCurrentSummaryHandler;

    public SmartLightningDashboardServiceImpl(
            SmartLightningDeviceService smartLightningDeviceService,
            AlarmStatisticsService alarmStatisticsService,
            TimeseriesService timeseriesService
    ) {
        this.groundSummaryHandler = new GroundSummaryHandler(
                smartLightningDeviceService, alarmStatisticsService, timeseriesService);
        this.spdSummaryHandler = new SpdSummaryHandler(
                smartLightningDeviceService, alarmStatisticsService, timeseriesService);
        this.lightningCurrentSummaryHandler = new LightningCurrentSummaryHandler(
                smartLightningDeviceService, alarmStatisticsService, timeseriesService);
    }

    @Override
    public GroundMonitoringSummaryDTO getGroundSummary(SecurityUser user, String period)
            throws ThingsboardException {
        return groundSummaryHandler.getSummary(user, period);
    }

    @Override
    public SpdMonitoringSummaryDTO getSpdSummary(SecurityUser user, String period)
            throws ThingsboardException {
        return spdSummaryHandler.getSummary(user, period);
    }

    @Override
    public LightningCurrentMonitoringSummaryDTO getLightningCurrentSummary(SecurityUser user, String period)
            throws ThingsboardException {
        return lightningCurrentSummaryHandler.getSummary(user, period);
    }

    /**
     * 统一分批读取一组设备的周期遥测，并保持返回结果与设备列表顺序一致。
     *
     * <p>ThingsBoard 当前没有“多个实体 + 多个历史键”的同步批量接口，因此这里仍通过
     * {@link TimeseriesService} 并发读取，但统一限制每批 Future 数量。单台设备失败时返回
     * 空列表，不影响同批其他设备；调用方只负责自己的指标聚合。</p>
     */
    private static List<List<TsKvEntry>> readTelemetryInBatches(
            TimeseriesService timeseriesService,
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices,
            List<ReadTsKvQuery> queries,
            String errorMessage) throws ThingsboardException {
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
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
    private static AlarmRecordAggregate collectAlarmRecords(
            SecurityUser user, Set<UUID> deviceIds, MonitoringPeriodWindow window,
            AlarmStatisticsService alarmStatisticsService) {
        AlarmRecordAggregate result = AlarmRecordAggregate.empty();
        if (deviceIds.isEmpty()) {
            return result;
        }
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
                user, deviceIds, window.getStartTs(), window.getEndTs(), false);
        for (AlarmDimensionCountBO row : rows) {
            int count = CountValueSupport.toIntCount(row.getCount());
            result.count += count;
            result.typeTotals.add(new AlarmTypeTotalDTO(row.getDimension(), count));
        }
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
    private static List<MonitoringDeviceContextBO> findVisibleDevicesByFunctionCategory(
            SmartLightningDeviceService deviceService,
            SecurityUser user,
            String functionCategory) throws ThingsboardException {
        List<MonitoringDeviceContextBO> visibleDevices =
                deviceService.getAllLightningDeviceContexts(user);
        return MonitoringDeviceFilters.byFunctionCategory(visibleDevices, functionCategory);
    }

    /** 保存看板告警聚合结果，不保存告警明细或设备 ID 集合。 */
    private static final class AlarmRecordAggregate {
        private int count;
        private int alarmDeviceCount;
        private final List<AlarmTypeTotalDTO> typeTotals = new ArrayList<>();

        private static AlarmRecordAggregate empty() {
            return new AlarmRecordAggregate();
        }
    }

    @RequiredArgsConstructor
    private static final class GroundSummaryHandler {
    
    
        private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;
        private static final String GROUND_RESISTANCE_FUNCTION_CATEGORY = "ground_resistance";
        private static final String GROUND_RESISTANCE_TELEMETRY_KEY = "groundResistance";
    
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
            MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
            List<MonitoringDeviceContextBO> groundDevices = findVisibleGroundDevices(user);
            Set<UUID> groundDeviceIds = new HashSet<>();
            int online = 0;
            for (MonitoringDeviceContextBO device : groundDevices) {
                groundDeviceIds.add(device.getDeviceId());
                if (Boolean.TRUE.equals(device.getActive())) {
                    online++;
                }
            }
    
            AlarmRecordAggregate alarms = collectAlarmRecords(
                    user, groundDeviceIds, window, alarmStatisticsService);
            GroundResistanceExtremaDTO resistanceExtrema = findGroundResistanceRange(user, groundDevices, window);
    
            int total = groundDevices.size();
            double alarmRate = total == 0 ? 0D : alarms.alarmDeviceCount * 100D / total;
            return new GroundMonitoringSummaryDTO(
                    total, online, total - online, alarms.count, alarms.typeTotals, alarmRate,
                    resistanceExtrema.getMaximum(), resistanceExtrema.getMinimum(), resistanceExtrema);
        }
    
        /** 复用智能防雷设备服务的权限、批量属性和 productCategory 过滤。 */
        private List<MonitoringDeviceContextBO> findVisibleGroundDevices(SecurityUser user)
                throws ThingsboardException {
            return findVisibleDevicesByFunctionCategory(
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
            List<ReadTsKvQuery> queries = List.of(new BaseReadTsKvQuery(
                    GROUND_RESISTANCE_TELEMETRY_KEY,
                    window.getStartTs(),
                    window.getEndTs(),
                    0,
                    QUERY_PAGE_SIZE,
                    Aggregation.NONE,
                    "ASC"));
            List<List<TsKvEntry>> entriesByDevice = readTelemetryInBatches(
                    timeseriesService,
                    user,
                    groundDevices,
                    queries,
                    "Failed to read ground resistance telemetry");
            ResistanceExtremaAggregate result = new ResistanceExtremaAggregate();
            for (int index = 0; index < entriesByDevice.size(); index++) {
                MonitoringDeviceContextBO device = groundDevices.get(index);
                for (TsKvEntry entry : entriesByDevice.get(index)) {
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

    @RequiredArgsConstructor
    private static final class SpdSummaryHandler {
    
    
        /** 单次设备、告警或遥测查询使用的最大记录数。 */
        private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;
        /** SPD 监测设备在服务器属性中的功能分类值。 */
        private static final String FUNCTION_CATEGORY = "spd_monitor";
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
            MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
            List<MonitoringDeviceContextBO> devices = findVisibleSpdDevices(user);
            Set<UUID> deviceIds = new HashSet<>();
            int online = 0;
            for (MonitoringDeviceContextBO device : devices) {
                deviceIds.add(device.getDeviceId());
                if (Boolean.TRUE.equals(device.getActive())) {
                    online++;
                }
            }
    
            AlarmRecordAggregate alarmAggregate = collectAlarmRecords(
                    user, deviceIds, window, alarmStatisticsService);
            SpdTelemetryReadResult telemetry = readSpdTelemetry(user, devices, window);
            // 周期遥测统一通过 ThingsBoard TimeseriesService 读取，保持 SQL、Timescale、Cassandra
            // 的存储抽象一致，并保留最值来源设备、参数键和原始时间戳。
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
            RealtimeStatusAggregate realtime = summarizeRealtimeStatus(telemetry.entriesByDevice);
            int total = devices.size();
            return new SpdMonitoringSummaryDTO(
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
        private List<MonitoringDeviceContextBO> findVisibleSpdDevices(SecurityUser user)
                throws ThingsboardException {
            return findVisibleDevicesByFunctionCategory(
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
         * @return 按设备原顺序排列的时序记录
         * @throws ThingsboardException 时序查询失败时抛出
         */
        private SpdTelemetryReadResult readSpdTelemetry(
                SecurityUser user,
                List<MonitoringDeviceContextBO> devices,
                MonitoringPeriodWindow window) throws ThingsboardException {
            return new SpdTelemetryReadResult(readTelemetryInBatches(
                    timeseriesService,
                    user,
                    devices,
                    spdTelemetryQueries(window),
                    "Failed to read SPD telemetry"));
        }
    
        /** 根据一次读取结果汇总周期内最大漏流及其来源信息。 */
        private LeakageAggregate findMaximumLeakage(
                List<List<TsKvEntry>> entriesByDevice,
                List<MonitoringDeviceContextBO> devices) {
            LeakageAggregate result = LeakageAggregate.empty();
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
    
        /** 将规则链输出的每台设备最新状态、温度和寿命值统计为四组正常数/有参数设备数。 */
        private RealtimeStatusAggregate summarizeRealtimeStatus(List<List<TsKvEntry>> entriesByDevice) {
            List<List<String>> groundValues = new ArrayList<>();
            List<List<String>> spdValues = new ArrayList<>();
            List<List<Double>> temperatureValues = new ArrayList<>();
            List<Double> lifeValues = new ArrayList<>();
            for (List<TsKvEntry> entries : entriesByDevice) {
                String groundStatus = latestStatusValue(entries, GROUND_STATUS_KEYS);
                groundValues.add(groundStatus == null ? Collections.emptyList() : List.of(groundStatus));
                String spdStatus = latestStatusValue(entries, SPD_STATUS_KEYS);
                spdValues.add(spdStatus == null ? Collections.emptyList() : List.of(spdStatus));
    
                List<Double> temperatures = new ArrayList<>();
                for (List<String> keys : TEMPERATURE_KEYS) {
                    Double value = latestNumericValue(entries, keys);
                    if (value != null) {
                        temperatures.add(value);
                    }
                }
                temperatureValues.add(temperatures);
                lifeValues.add(latestNumericValue(entries, LIFE_KEYS));
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
    
        /** 仅允许漏电流规范键进入最大漏流聚合，避免混入其他实时参数。 */
        private boolean isLeakageKey(String key) {
            return LEAKAGE_KEYS.contains(key);
        }
    
        /** 为周期漏流和实时参数创建一次批量时序查询条件。 */
        private List<ReadTsKvQuery> spdTelemetryQueries(MonitoringPeriodWindow window) {
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

    @RequiredArgsConstructor
    private static final class LightningCurrentSummaryHandler {
    
    
        /** 单次设备、告警或遥测查询使用的最大记录数。 */
        private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ENTITY_PAGE_SIZE;
    
        /** 雷电流监测设备在服务器属性中的功能分类值。 */
        private static final String FUNCTION_CATEGORY = "lightning_current";
        /** 规则链输出的雷电监测规范键，不直接查询设备原始测点键。 */
        private static final List<String> TELEMETRY_KEYS = List.of(
                "surgePeak", "surgeCount", "surgePolarity");
    
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
            MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
            List<MonitoringDeviceContextBO> devices = findVisibleLightningDevices(user);
            Set<UUID> deviceIds = new HashSet<>();
            int online = 0;
            for (MonitoringDeviceContextBO device : devices) {
                deviceIds.add(device.getDeviceId());
                if (Boolean.TRUE.equals(device.getActive())) {
                    online++;
                }
            }
    
            AlarmRecordAggregate alarmAggregate = collectAlarmRecords(
                    user, deviceIds, window, alarmStatisticsService);
            LightningAggregate aggregate = findLightningAggregate(user, devices, window);
            int total = devices.size();
            return new LightningCurrentMonitoringSummaryDTO(
                    total,
                    online,
                    total - online,
                    alarmAggregate.count,
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
            return findVisibleDevicesByFunctionCategory(
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
            List<List<TsKvEntry>> entriesByDevice = readTelemetryInBatches(
                    timeseriesService,
                    user,
                    devices,
                    telemetryQueries(window),
                    "Failed to read lightning telemetry");
            for (int index = 0; index < entriesByDevice.size(); index++) {
                MonitoringDeviceContextBO device = devices.get(index);
                List<TsKvEntry> entries = entriesByDevice.get(index);
                PeakRangeResult peakRange = findPeakRange(entries);
                if (peakRange.maximum != null) {
                    result.reportingDeviceCount++;
                    if (result.maxPeakCurrent == null || peakRange.maximum > result.maxPeakCurrent) {
                        result.maxPeakCurrent = peakRange.maximum;
                        result.maxPeakDeviceName = device.getDeviceName();
                        result.maxPeakTime = peakRange.maximumTime;
                        result.maxPeakPolarity = peakRange.maximumPolarity;
                    }
                    if (peakRange.minimum != null
                            && (result.minPeakCurrent == null || peakRange.minimum < result.minPeakCurrent)) {
                        result.minPeakCurrent = peakRange.minimum;
                        result.minPeakDeviceName = device.getDeviceName();
                        result.minPeakTime = peakRange.minimumTime;
                        result.minPeakPolarity = peakRange.minimumPolarity;
                    }
                }
                // surgeCount 是设备累计计数器：只有一条时直接使用该值；
                // 有多条时保留相邻差值逻辑，避免把每次上报的累计值重复相加。
                result.eventCount += calculateSurgeCountDelta(entries, window.getStartTs());
            }
            return result;
        }
    
        /**
         * 为三个规范遥测键创建时序查询条件。
         *
         * @param window 查询的起止时间
         * @return 周期内 surgePeak/surgeCount/surgePolarity 查询及周期开始前的 surgeCount 基线查询
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
            queries.add(new BaseReadTsKvQuery(
                    "surgeCount",
                    0,
                    window.getStartTs(),
                    1,
                    "DESC"));
            return queries;
        }
    
        /**
         * 从单台设备的遥测结果中找到 surgePeak 最大值和最小值，并关联最近的极性值。
         *
         * @param entries 当前设备在统计周期内的遥测记录
         * @return 该设备的峰值范围及最大、最小值对应的时间和极性
         */
        private PeakRangeResult findPeakRange(List<TsKvEntry> entries) {
            PeakRangeResult result = PeakRangeResult.empty();
            for (TsKvEntry entry : entries) {
                if (!isPeakKey(entry.getKey())) {
                    continue;
                }
                Double value = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (value == null) {
                    continue;
                }
                // surgePeak 已由规则链完成单位和精度转换，后端只取峰值绝对值参与比较。
                double normalizedValue = Math.abs(value);
                if (result.maximum == null || normalizedValue > result.maximum) {
                    result.maximum = normalizedValue;
                    result.maximumTime = entry.getTs();
                    result.maximumPolarity = findNearestPolarity(entries, entry.getTs());
                }
                if (result.minimum == null || normalizedValue < result.minimum) {
                    result.minimum = normalizedValue;
                    result.minimumTime = entry.getTs();
                    result.minimumPolarity = findNearestPolarity(entries, entry.getTs());
                }
            }
            return result;
        }
    
        /**
         * 计算单台设备在所选周期内的 surgeCount 增量。
         *
         * <p>{@code surgeCount} 是规则链输出的累计雷击次数，因此存在多条记录时
         * 仍按相邻值的正向差值累计，避免重复计算；只有一条有效记录时没有前值可作差，
         * 直接使用这条记录。若后续值小于前值，说明设备计数器发生归零或重启，
         * 当前值作为新一段计数加入结果。</p>
         *
         * @param entries 当前设备的周期遥测和周期开始前的基线记录
         * @param windowStartTs 当前统计周期起始时间
         * @return 周期内雷击次数；没有有效 surgeCount 时返回 0
         */
        private long calculateSurgeCountDelta(List<TsKvEntry> entries, long windowStartTs) {
            List<TsKvEntry> readings = new ArrayList<>();
            Double baseline = null;
            long baselineTs = Long.MIN_VALUE;
            for (TsKvEntry entry : entries) {
                if (!"surgeCount".equals(entry.getKey())) {
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
                samples.add(MonitoringValueParser.parseFiniteDouble(reading.getValueAsString()));
            }
            return SpdMonitoringCalculator.counterDelta(baseline, samples);
        }
    
        /** 判断遥测键是否为规则链输出的雷电峰值键。 */
        private boolean isPeakKey(String key) {
            return "surgePeak".equals(key);
        }
    
        /**
         * 查找距离峰值时间最近的 surgePolarity 记录。
         *
         * @param entries 当前设备的周期遥测
         * @param timestamp 峰值发生时间
         * @return 中文极性名称；没有对应记录时返回 null
         */
        private String findNearestPolarity(List<TsKvEntry> entries, long timestamp) {
            String polarity = null;
            long nearestDistance = Long.MAX_VALUE;
            for (TsKvEntry entry : entries) {
                if (!"surgePolarity".equals(entry.getKey())) {
                    continue;
                }
                long distance = Math.abs(entry.getTs() - timestamp);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    polarity = toPolarityLabel(entry.getValueAsString());
                }
            }
            return polarity;
        }
    
        /** 将规则链输出的极性编码转换为页面显示文本。 */
        private String toPolarityLabel(String value) {
            if ("1".equals(value)) {
                return "正极性";
            }
            if ("0".equals(value)) {
                return "负极性";
            }
            return value;
        }
    
        /** 保存所有设备合并后的中间聚合结果。 */
        private static final class LightningAggregate {
            private long eventCount;
            private Double maxPeakCurrent;
            private String maxPeakDeviceName;
            private Long maxPeakTime;
            private String maxPeakPolarity;
            private Double minPeakCurrent;
            private String minPeakDeviceName;
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
            private String maximumPolarity;
            private Double minimum;
            private Long minimumTime;
            private String minimumPolarity;
    
            /** 创建一个没有峰值记录的空结果。 */
            private static PeakRangeResult empty() {
                return new PeakRangeResult();
            }
        }
    
    }
}
