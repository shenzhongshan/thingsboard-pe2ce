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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
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
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.monitoring.dto.GroundAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummary;
import org.thingsboard.server.service.monitoring.dto.GroundResistanceExtrema;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummary;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.dto.SpdAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMetricExtrema;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummary;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
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

    private final GroundSummaryHandler groundSummaryHandler;
    private final SpdSummaryHandler spdSummaryHandler;
    private final LightningCurrentSummaryHandler lightningCurrentSummaryHandler;

    public SmartLightningDashboardServiceImpl(
            SmartLightningDeviceService smartLightningDeviceService,
            DeviceAlarmQueryService alarmQueryService,
            TimeseriesService timeseriesService
    ) {
        this.groundSummaryHandler = new GroundSummaryHandler(
                smartLightningDeviceService, alarmQueryService, timeseriesService);
        this.spdSummaryHandler = new SpdSummaryHandler(
                smartLightningDeviceService, alarmQueryService, timeseriesService);
        this.lightningCurrentSummaryHandler = new LightningCurrentSummaryHandler(
                smartLightningDeviceService, alarmQueryService, timeseriesService);
    }

    @Override
    public GroundMonitoringSummary getGroundSummary(SecurityUser user, String period)
            throws ThingsboardException {
        return groundSummaryHandler.getSummary(user, period);
    }

    @Override
    public SpdMonitoringSummary getSpdSummary(SecurityUser user, String period)
            throws ThingsboardException {
        return spdSummaryHandler.getSummary(user, period);
    }

    @Override
    public LightningCurrentMonitoringSummary getLightningCurrentSummary(SecurityUser user, String period)
            throws ThingsboardException {
        return lightningCurrentSummaryHandler.getSummary(user, period);
    }

    @RequiredArgsConstructor
    private static final class GroundSummaryHandler {
    
    
        private static final int QUERY_PAGE_SIZE = 10_000;
        private static final String GROUND_RESISTANCE_FUNCTION_CATEGORY = "ground_resistance";
        private static final String GROUND_RESISTANCE_TELEMETRY_KEY = "groundResistance";
    
        private final SmartLightningDeviceService smartLightningDeviceService;
        private final DeviceAlarmQueryService alarmQueryService;
        private final TimeseriesService timeseriesService;
    
        /**
         * 查询当前用户可见地网设备（functionCategory=ground_resistance）的完整摘要。
         *
         * @param user 当前登录用户，用于执行权限过滤
         * @param period 告警统计窗口，仅接受 day、week、month
         * @return 设备总数、在线/离线数量、周期内告警数、告警类型统计、报警率和阻值范围
         * @throws ThingsboardException 周期参数非法或设备、告警、遥测查询失败时抛出
         */
        public GroundMonitoringSummary getSummary(SecurityUser user, String period) throws ThingsboardException {
            MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
            List<SmartLightningDeviceContextDTO> groundDevices = findVisibleGroundDevices(user);
            List<EntityId> groundDeviceIds = new ArrayList<>();
            int online = 0;
            for (SmartLightningDeviceContextDTO device : groundDevices) {
                groundDeviceIds.add(new DeviceId(device.getDeviceId()));
                if (Boolean.TRUE.equals(device.getActive())) {
                    online++;
                }
            }
    
            GroundAlarmAggregate alarms = countGroundAlarmsSince(user, groundDeviceIds, window);
            GroundResistanceExtrema resistanceExtrema = findGroundResistanceRange(user, groundDevices, window);
    
            int total = groundDevices.size();
            double alarmRate = total == 0 ? 0D : alarms.alarmDeviceIds.size() * 100D / total;
            return new GroundMonitoringSummary(
                    total, online, total - online, alarms.count, alarms.typeTotals, alarmRate,
                    resistanceExtrema.getMaximum(), resistanceExtrema.getMinimum(), resistanceExtrema);
        }
    
        /** 复用智能防雷设备服务的权限、批量属性和 productCategory 过滤。 */
        private List<SmartLightningDeviceContextDTO> findVisibleGroundDevices(SecurityUser user)
                throws ThingsboardException {
            PageData<SmartLightningDeviceContextDTO> pageData = smartLightningDeviceService.getLightningDeviceContexts(
                    user, new PageLink(QUERY_PAGE_SIZE, 0));
            List<SmartLightningDeviceContextDTO> groundDevices = new ArrayList<>();
            for (SmartLightningDeviceContextDTO device : pageData.getData()) {
                if (GROUND_RESISTANCE_FUNCTION_CATEGORY.equalsIgnoreCase(
                        MonitoringValueParser.trimToEmpty(device.getFunctionCategory()))) {
                    groundDevices.add(device);
                }
            }
            return groundDevices;
        }
    
        /** 告警按 createdTime（触发时间）筛选，包含在窗口中已清除的历史告警。 */
        private GroundAlarmAggregate countGroundAlarmsSince(
                SecurityUser user, List<EntityId> groundDeviceIds, MonitoringPeriodWindow window)
                throws ThingsboardException {
            if (groundDeviceIds.isEmpty()) {
                return GroundAlarmAggregate.empty();
            }
            Set<UUID> groundDeviceIdValues = new HashSet<>();
            for (EntityId groundDeviceId : groundDeviceIds) {
                groundDeviceIdValues.add(groundDeviceId.getId());
            }
    
            TimePageLink pageLink = new TimePageLink(
                    QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                    window.getStartTs(), window.getEndTs());
            AlarmQueryV2 query = new AlarmQueryV2(
                    null, pageLink, Collections.emptyList(), List.of(AlarmSearchStatus.ANY), Collections.emptyList(), null);
            GroundAlarmAggregate result = GroundAlarmAggregate.empty();
            for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
                EntityId originator = alarm.getOriginator();
                if (originator != null && originator.getEntityType() == EntityType.DEVICE
                        && groundDeviceIdValues.contains(originator.getId())) {
                    result.count++;
                    result.alarmDeviceIds.add(originator.getId());
                    String type = alarm.getType() == null || alarm.getType().isBlank()
                            ? "未分类" : alarm.getType().trim();
                    result.countsByType.put(type, result.countsByType.getOrDefault(type, 0) + 1);
                }
            }
            for (Map.Entry<String, Integer> entry : result.countsByType.entrySet()) {
                result.typeTotals.add(new GroundAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
            }
            result.typeTotals.sort(new Comparator<GroundAlarmTypeTotalDTO>() {
                @Override
                public int compare(GroundAlarmTypeTotalDTO left, GroundAlarmTypeTotalDTO right) {
                    int countOrder = Integer.compare(right.getCount(), left.getCount());
                    return countOrder != 0 ? countOrder : left.getType().compareTo(right.getType());
                }
            });
            return result;
        }
    
        /**
         * 同时读取每台地网设备在当前周期的阻值。TimeseriesService 没有按实体保留结果的
         * 单次批量 API，故由服务层并行发起查询并统一等待，前端始终只请求一个汇总接口。
         */
        private GroundResistanceExtrema findGroundResistanceRange(
                SecurityUser user, List<SmartLightningDeviceContextDTO> groundDevices,
                MonitoringPeriodWindow window)
                throws ThingsboardException {
            List<EntityId> groundDeviceIds = new ArrayList<>();
            for (SmartLightningDeviceContextDTO device : groundDevices) {
                groundDeviceIds.add(new DeviceId(device.getDeviceId()));
            }
            if (groundDeviceIds.isEmpty()) {
                return new GroundResistanceExtrema(null, null, null, null, null, null, null, null);
            }
            List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
            for (EntityId groundDeviceId : groundDeviceIds) {
                futures.add(timeseriesService.findAll(
                        user.getTenantId(), new DeviceId(groundDeviceId.getId()), List.of(new BaseReadTsKvQuery(
                                GROUND_RESISTANCE_TELEMETRY_KEY,
                                window.getStartTs(),
                                window.getEndTs(),
                                0,
                                QUERY_PAGE_SIZE,
                                Aggregation.NONE,
                                "ASC"))));
            }
    
            List<List<TsKvEntry>> entriesByDevice;
            try {
                entriesByDevice = Futures.allAsList(futures).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ThingsboardException("Failed to read ground resistance telemetry", e, ThingsboardErrorCode.DATABASE);
            } catch (ExecutionException e) {
                throw new ThingsboardException("Failed to read ground resistance telemetry", e, ThingsboardErrorCode.DATABASE);
            }
    
            ResistanceExtremaAggregate result = new ResistanceExtremaAggregate();
            for (int index = 0; index < entriesByDevice.size(); index++) {
                SmartLightningDeviceContextDTO device = groundDevices.get(index);
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
            return result.toDto();
        }
    
        /** 同一轮告警查询同时保留条数和按设备去重后的集合，避免统计口径分叉。 */
        private static final class GroundAlarmAggregate {
            private int count;
            private final Set<UUID> alarmDeviceIds = new HashSet<>();
            private final Map<String, Integer> countsByType = new HashMap<>();
            private final List<GroundAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
    
            private static GroundAlarmAggregate empty() {
                return new GroundAlarmAggregate();
            }
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
    
            private GroundResistanceExtrema toDto() {
                return new GroundResistanceExtrema(
                        maximum, maximumDeviceName, maximumParameter, maximumTime,
                        minimum, minimumDeviceName, minimumParameter, minimumTime);
            }
        }
    }

    @RequiredArgsConstructor
    private static final class SpdSummaryHandler {
    
    
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
        private final DeviceAlarmQueryService alarmQueryService;
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

    @RequiredArgsConstructor
    private static final class LightningCurrentSummaryHandler {
    
    
        /** 单次设备、告警或遥测查询使用的最大记录数。 */
        private static final int QUERY_PAGE_SIZE = 10_000;
    
        /** 雷电流监测设备在服务器属性中的功能分类值。 */
        private static final String FUNCTION_CATEGORY = "lightning_current";
        /** 防雷产品大类，避免把同名功能分类的其他产品纳入雷电统计。 */
        private static final String PRODUCT_CATEGORY = "lightning";
        /** 规则链输出的雷电监测规范键，不直接查询设备原始测点键。 */
        private static final List<String> TELEMETRY_KEYS = List.of(
                "surgePeak", "surgeCount", "surgePolarity");
    
        /** 复用智能防雷设备服务，负责权限过滤和设备上下文查询。 */
        private final SmartLightningDeviceService smartLightningDeviceService;
    
        /** 复用公共告警查询服务，避免在本实现中重复处理告警权限。 */
        private final DeviceAlarmQueryService alarmQueryService;
    
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
        public LightningCurrentMonitoringSummary getSummary(SecurityUser user, String period)
                throws ThingsboardException {
            MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
            List<SmartLightningDeviceContextDTO> devices = findVisibleLightningDevices(user);
            List<EntityId> deviceIds = new ArrayList<>();
            int online = 0;
            for (SmartLightningDeviceContextDTO device : devices) {
                deviceIds.add(new DeviceId(device.getDeviceId()));
                if (Boolean.TRUE.equals(device.getActive())) {
                    online++;
                }
            }
    
            AlarmAggregate alarmAggregate = countAlarms(user, deviceIds, window);
            LightningAggregate aggregate = findLightningAggregate(user, devices, window);
            int total = devices.size();
            return new LightningCurrentMonitoringSummary(
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
        private List<SmartLightningDeviceContextDTO> findVisibleLightningDevices(SecurityUser user)
                throws ThingsboardException {
            List<SmartLightningDeviceContextDTO> devices = new ArrayList<>();
            PageLink pageLink = new PageLink(QUERY_PAGE_SIZE, 0);
            while (pageLink != null) {
                PageData<SmartLightningDeviceContextDTO> page =
                        smartLightningDeviceService.getLightningDeviceContexts(user, pageLink);
                for (SmartLightningDeviceContextDTO device : page.getData()) {
                    if (isLightningCurrentDevice(device)) {
                        devices.add(device);
                    }
                }
                pageLink = page.hasNext() ? pageLink.nextPageLink() : null;
            }
            return devices;
        }
    
        /** 同时校验产品大类和功能分类，保证雷电流卡片只统计防雷雷电流设备。 */
        private boolean isLightningCurrentDevice(SmartLightningDeviceContextDTO device) {
            return PRODUCT_CATEGORY.equalsIgnoreCase(
                            MonitoringValueParser.trimToEmpty(device.getProductCategory()))
                    && FUNCTION_CATEGORY.equalsIgnoreCase(
                            MonitoringValueParser.trimToEmpty(device.getFunctionCategory()));
        }
    
        /**
         * 统计周期内属于指定雷电流设备的告警数量。
         *
         * <p>查询使用告警创建时间作为触发时间，并保留 ANY 状态，确保已清除告警
         * 也能参与周期统计；设备集合用于排除其他类型设备的告警。</p>
         *
         * @param user 当前登录用户
         * @param deviceIds 雷电流设备实体 ID
         * @param window 当前统计周期的起止时间
         * @return 周期内告警条数和按数量降序排列的告警类型统计
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
            AlarmAggregate result = AlarmAggregate.empty();
            for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
                EntityId originator = alarm.getOriginator();
                if (originator != null
                        && originator.getEntityType() == EntityType.DEVICE
                        && deviceIdSet.contains(originator.getId())) {
                    result.count++;
                    String type = alarm.getType() == null || alarm.getType().isBlank()
                            ? "未分类" : alarm.getType().trim();
                    result.countsByType.put(type, result.countsByType.getOrDefault(type, 0) + 1);
                }
            }
            for (Map.Entry<String, Integer> entry : result.countsByType.entrySet()) {
                result.typeTotals.add(new DeviceAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
            }
            result.typeTotals.sort(new Comparator<DeviceAlarmTypeTotalDTO>() {
                @Override
                public int compare(DeviceAlarmTypeTotalDTO left, DeviceAlarmTypeTotalDTO right) {
                    int countOrder = Integer.compare(right.getCount(), left.getCount());
                    return countOrder != 0 ? countOrder : left.getType().compareTo(right.getType());
                }
            });
            return result;
        }
    
        /**
         * 并行读取所有雷电流设备的规范遥测，并汇总事件数与峰值范围。
         *
         * <p>每台设备对应一个异步时序查询，使用 {@link Futures#allAsList(List)} 等待
         * 全部设备返回，再按设备原顺序合并结果。</p>
         *
         * @param user 当前登录用户
         * @param devices 已经过权限和功能分类筛选的设备
         * @param window 遥测查询的起止时间
         * @return 雷电事件和峰值的中间聚合结果
         * @throws ThingsboardException 时序查询失败时抛出
         */
        private LightningAggregate findLightningAggregate(
                SecurityUser user,
                List<SmartLightningDeviceContextDTO> devices,
                MonitoringPeriodWindow window) throws ThingsboardException {
            if (devices.isEmpty()) {
                return LightningAggregate.empty();
            }
            List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
            for (SmartLightningDeviceContextDTO device : devices) {
                futures.add(timeseriesService.findAll(
                        user.getTenantId(),
                        new DeviceId(device.getDeviceId()),
                        telemetryQueries(window)));
            }
    
            List<List<TsKvEntry>> entriesByDevice;
            try {
                entriesByDevice = Futures.allAsList(futures).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ThingsboardException(
                        "Failed to read lightning telemetry", e, ThingsboardErrorCode.DATABASE);
            } catch (ExecutionException e) {
                throw new ThingsboardException(
                        "Failed to read lightning telemetry", e, ThingsboardErrorCode.DATABASE);
            }
    
            LightningAggregate result = LightningAggregate.empty();
            for (int index = 0; index < entriesByDevice.size(); index++) {
                SmartLightningDeviceContextDTO device = devices.get(index);
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
                queries.add(new BaseReadTsKvQuery(
                        key,
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
                double normalizedValue = normalizePeakValue(value);
                normalizedValue = Math.abs(normalizedValue);
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
         * 返回规则链已经统一单位后的峰值，不再重复处理原始测点换算。
         *
         * @param value surgePeak 的数值
         * @return 可直接用于比较和返回前端的峰值
         */
        private double normalizePeakValue(double value) {
            // surgePeak 已由规则链完成单位和精度转换，后端只保留数值，不再重复换算。
            return value;
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
    
        /** 保存一次告警查询得到的总数及类型分组，保证卡片数字和悬浮详情口径一致。 */
        private static final class AlarmAggregate {
            private int count;
            private final Map<String, Integer> countsByType = new HashMap<>();
            private final List<DeviceAlarmTypeTotalDTO> typeTotals = new ArrayList<>();
    
            /** 创建空告警统计，供无雷电流设备时直接返回。 */
            private static AlarmAggregate empty() {
                return new AlarmAggregate();
            }
        }
    }
}
