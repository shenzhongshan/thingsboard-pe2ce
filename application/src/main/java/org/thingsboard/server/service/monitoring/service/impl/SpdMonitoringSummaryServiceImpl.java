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
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummary;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.service.SpdMonitoringSummaryService;
import org.thingsboard.server.service.monitoring.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * SPD 监测聚合实现。
 *
 * <p>该类只处理 {@code functionCategory=spd_monitor} 的设备，先按当前用户
 * 权限获取设备，再读取规则链输出的漏电流规范遥测键（leakageA/B/C 及原始键
 * I1/I2/I3），最后汇总设备状态、周期内告警数和最大漏电流。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SpdMonitoringSummaryServiceImpl implements SpdMonitoringSummaryService {

    /** 单次设备、告警或遥测查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;
    /** SPD 监测设备在服务器属性中的功能分类值。 */
    private static final String FUNCTION_CATEGORY = "spd_monitor";
    /** 漏电流遥测键：规范键 leakageA/B/C 与设备原始测点键 I1/I2/I3。 */
    private static final List<String> LEAKAGE_KEYS = List.of(
            "leakageA", "leakageB", "leakageC", "I1", "I2", "I3");

    /** 复用智能防雷设备服务，负责权限过滤和设备上下文查询。 */
    private final SmartLightningDeviceService smartLightningDeviceService;
    /** 复用公共告警查询服务，避免在本实现中重复处理告警权限分支。 */
    private final MonitoringAlarmQueryService alarmQueryService;
    /** ThingsBoard 时序服务，用于查询设备漏电流遥测。 */
    private final TimeseriesService timeseriesService;

    /**
     * 查询当前用户可见 SPD 设备的完整摘要。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param period 统计周期，支持 day、week、month
     * @return 设备数量、在线/离线数量、告警数、周期内最大漏电流及其设备/相位/时间
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

        int alarmCount = countAlarms(user, deviceIds, window);
        LeakageAggregate leakage = findMaximumLeakage(user, devices, window);
        int total = devices.size();
        return new SpdMonitoringSummary(
                total,
                online,
                total - online,
                alarmCount,
                leakage.value,
                leakage.deviceName,
                leakage.phase,
                leakage.timestamp,
                leakage.reportingDeviceCount);
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
                if (FUNCTION_CATEGORY.equalsIgnoreCase(
                        MonitoringValueParser.trimToEmpty(device.getFunctionCategory()))) {
                    devices.add(device);
                }
            }
            pageLink = page.hasNext() ? pageLink.nextPageLink() : null;
        }
        return devices;
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
     * @return 周期内告警条数
     */
    private int countAlarms(SecurityUser user, List<EntityId> deviceIds, MonitoringPeriodWindow window)
            throws ThingsboardException {
        if (deviceIds.isEmpty()) {
            return 0;
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
        int count = 0;
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            EntityId originator = alarm.getOriginator();
            if (originator != null
                    && originator.getEntityType() == EntityType.DEVICE
                    && deviceIdSet.contains(originator.getId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 并行读取所有 SPD 设备的漏电流遥测，并汇总最大漏电流。
     *
     * <p>每台设备对应一个异步时序查询，使用 {@link Futures#allAsList(List)}
     * 等待全部返回后按设备原顺序合并；漏电流取绝对值参与比较。</p>
     *
     * @param user 当前登录用户
     * @param devices 已经过权限和功能分类筛选的设备
     * @param window 遥测查询的起止时间
     * @return 最大漏电流及其设备、相位、时间的聚合结果
     * @throws ThingsboardException 时序查询失败时抛出
     */
    private LeakageAggregate findMaximumLeakage(
            SecurityUser user,
            List<SmartLightningDeviceContextDTO> devices,
            MonitoringPeriodWindow window) throws ThingsboardException {
        if (devices.isEmpty()) {
            return LeakageAggregate.empty();
        }
        List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
        for (SmartLightningDeviceContextDTO device : devices) {
            futures.add(timeseriesService.findAll(
                    user.getTenantId(),
                    new DeviceId(device.getDeviceId()),
                    leakageQueries(window)));
        }

        List<List<TsKvEntry>> entriesByDevice;
        try {
            entriesByDevice = Futures.allAsList(futures).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThingsboardException(
                    "Failed to read SPD leakage telemetry", e, ThingsboardErrorCode.DATABASE);
        } catch (ExecutionException e) {
            throw new ThingsboardException(
                    "Failed to read SPD leakage telemetry", e, ThingsboardErrorCode.DATABASE);
        }

        LeakageAggregate result = LeakageAggregate.empty();
        for (int index = 0; index < entriesByDevice.size(); index++) {
            List<TsKvEntry> entries = entriesByDevice.get(index);
            boolean reported = false;
            SmartLightningDeviceContextDTO device = devices.get(index);
            for (TsKvEntry entry : entries) {
                Double parsed = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
                if (parsed == null) {
                    continue;
                }
                reported = true;
                double leakage = Math.abs(parsed);
                if (result.value == null || leakage > result.value) {
                    result.value = leakage;
                    result.deviceName = device.getDeviceName();
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
     * 为每个漏电流遥测键创建周期内的时序查询条件。
     *
     * <p>按时间降序返回，便于尽快拿到最近的漏电流记录。</p>
     *
     * @param window 查询的起止时间
     * @return 各漏电流键的查询列表
     */
    private List<ReadTsKvQuery> leakageQueries(MonitoringPeriodWindow window) {
        List<ReadTsKvQuery> queries = new ArrayList<>();
        for (String key : LEAKAGE_KEYS) {
            queries.add(new BaseReadTsKvQuery(
                    key,
                    window.getStartTs(),
                    window.getEndTs(),
                    0,
                    QUERY_PAGE_SIZE,
                    Aggregation.NONE,
                    "DESC"));
        }
        return queries;
    }

    /** 根据遥测键名判定相位：规范键看后缀 A/B/C，原始键 I1/I2/I3 依次对应 A/B/C 相。 */
    private String phaseFromKey(String key) {
        if (key == null) {
            return "未知相位";
        }
        if (key.endsWith("A") || "I1".equals(key)) {
            return "A相";
        }
        if (key.endsWith("B") || "I2".equals(key)) {
            return "B相";
        }
        if (key.endsWith("C") || "I3".equals(key)) {
            return "C相";
        }
        return key;
    }

    /** 保存所有设备合并后的最大漏电流聚合结果。 */
    private static final class LeakageAggregate {
        /** 周期内最大漏电流（绝对值）；无任何记录时为 null。 */
        private Double value;
        /** 出现最大漏电流的设备名称。 */
        private String deviceName;
        /** 最大漏电流对应的相位。 */
        private String phase;
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
