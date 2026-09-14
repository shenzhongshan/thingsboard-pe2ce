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
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummary;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.service.LightningCurrentMonitoringSummaryService;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * 雷电流监测摘要的业务实现。
 *
 * <p>该类只处理 {@code functionCategory=lightning_current} 的设备，
 * 先按当前用户权限获取设备，再读取规则链输出的规范遥测键，最后汇总设备状态、
 * 时间范围内告警数、雷击累计次数和最大峰值。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class LightningCurrentMonitoringSummaryServiceImpl implements LightningCurrentMonitoringSummaryService {

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
    private final MonitoringAlarmQueryService alarmQueryService;

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
    @Override
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
            result.typeTotals.add(new SmartLightningAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
        }
        result.typeTotals.sort(new Comparator<SmartLightningAlarmTypeTotalDTO>() {
            @Override
            public int compare(SmartLightningAlarmTypeTotalDTO left, SmartLightningAlarmTypeTotalDTO right) {
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
        private final List<SmartLightningAlarmTypeTotalDTO> typeTotals = new ArrayList<>();

        /** 创建空告警统计，供无雷电流设备时直接返回。 */
        private static AlarmAggregate empty() {
            return new AlarmAggregate();
        }
    }
}
