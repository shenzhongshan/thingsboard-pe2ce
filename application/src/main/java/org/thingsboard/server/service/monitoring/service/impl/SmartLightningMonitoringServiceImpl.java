/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.service.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.bo.MonitoringDeviceContextBO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeAnalysisDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeArcFaultDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeCurrentDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeGroundDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeOverallDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeSpdDTO;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringService;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.monitoring.util.MonitoringDeviceFilters;
import org.thingsboard.server.service.monitoring.util.SpdMonitoringCalculator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.query.VisibleAssetTree;
import org.thingsboard.server.service.query.VisibleAssetTreeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * 数据监测页实时分析聚合实现。
 *
 * <p>实现只读取规则链已经输出的规范遥测键，不转换 0/1 等原始状态值。
 * 资产范围、设备列表、告警和遥测使用同一组设备 ID，保证页面各板块口径一致。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningMonitoringServiceImpl implements SmartLightningMonitoringService {

    /** 单批遥测请求上限，避免设备数量大时一次创建过多 Future。 */
    private static final int TELEMETRY_BATCH_SIZE = 50;

    private static final String SPD_CATEGORY = "spd_monitor";
    private static final String GROUND_CATEGORY = "ground_resistance";
    private static final String CURRENT_CATEGORY = "lightning_current";
    private static final String ARC_FAULT_CATEGORY = "arc_fault";
    private static final List<String> TELEMETRY_KEYS = List.of(
            "McbStatus", "GndStatus",
            "LifeLeft", "LeakCurrA", "LeakCurrB", "LeakCurrC",
            "GndGridRes",
            "SurgeCnt", "PeakValue", "Polarity",
            "DevStatus", "VoltStsA", "VoltStsB", "VoltStsC",
            "Trip1", "Trip2", "Trip3", "Trip4",
            "SampleCnt", "SamplePrd", "ResCurr1");

    private final SmartLightningDeviceService deviceService;
    private final VisibleAssetTreeService treeService;
    private final AlarmStatisticsService alarmStatisticsService;
    private final TimeseriesService timeseriesService;

    /**
     * 按当前资产范围汇总五个实时板块。
     *
     * <p>assetId 为空时直接使用全部可见防雷设备；不为空时先从权限过滤后的
     * 资产树找到该节点，再收集它的全部后代设备并与防雷设备集合取交集。</p>
     */
    @Override
    public SmartLightningRealtimeAnalysisDTO getRealtimeAnalysis(SecurityUser user, UUID assetId)
            throws ThingsboardException {
        // 先按用户权限取得防雷设备，再依据可选资产参数缩小本次统计范围。
        List<MonitoringDeviceContextBO> allDevices = deviceService.getAllLightningDeviceContexts(user);
        List<MonitoringDeviceContextBO> scopedDevices = filterDevicesByAsset(user, allDevices, assetId);
        // 对范围内设备批量读取最新遥测和未清除告警，后续板块复用相同数据。
        Map<UUID, Map<String, TsKvEntry>> telemetryByDevice = readLatestTelemetry(user, scopedDevices);
        Set<UUID> activeAlarmDeviceIds = findActiveAlarmDeviceIds(user, scopedDevices);

        // 按服务器属性中的功能分类拆分设备，分类值不从设备名称或型号推断。
        List<MonitoringDeviceContextBO> spdDevices = filterByFunctionCategory(scopedDevices, SPD_CATEGORY);
        List<MonitoringDeviceContextBO> groundDevices = filterByFunctionCategory(scopedDevices, GROUND_CATEGORY);
        List<MonitoringDeviceContextBO> currentDevices = filterByFunctionCategory(scopedDevices, CURRENT_CATEGORY);
        List<MonitoringDeviceContextBO> arcFaultDevices = filterByFunctionCategory(scopedDevices, ARC_FAULT_CATEGORY);

        // 每个板块从统一设备、遥测和告警范围生成 DTO，避免板块间统计范围不一致。
        return new SmartLightningRealtimeAnalysisDTO(
                buildOverall(scopedDevices, activeAlarmDeviceIds),
                buildSpd(spdDevices, telemetryByDevice, activeAlarmDeviceIds),
                buildGround(groundDevices, telemetryByDevice, activeAlarmDeviceIds),
                buildCurrent(currentDevices, telemetryByDevice, activeAlarmDeviceIds),
                buildArcFault(arcFaultDevices, telemetryByDevice, activeAlarmDeviceIds));
    }

    /** 根据选中资产裁剪设备范围；找不到资产时返回空集合，避免越权回退为全局。 */
    private List<MonitoringDeviceContextBO> filterDevicesByAsset(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices,
            UUID assetId) throws ThingsboardException {
        if (assetId == null) {
            return devices;
        }
        // 资产树本身按用户可见范围生成；未找到目标资产时不回退到全量设备。
        VisibleAssetTree tree = treeService.getVisibleAssetTree(user);
        VisibleAssetTree.Node selectedNode = findAssetNode(tree.roots(), assetId);
        if (selectedNode == null) {
            return Collections.emptyList();
        }
        Set<UUID> descendantDeviceIds = new HashSet<>();
        // 递归收集目标资产子树中的设备，再与已授权的防雷设备取交集。
        collectDeviceIds(selectedNode, descendantDeviceIds);
        List<MonitoringDeviceContextBO> result = new ArrayList<>();
        for (MonitoringDeviceContextBO device : devices) {
            if (descendantDeviceIds.contains(device.getDeviceId())) {
                result.add(device);
            }
        }
        return result;
    }

    /** 递归查找指定资产节点；设备节点不允许作为数据监测页的统计范围。 */
    private VisibleAssetTree.Node findAssetNode(
            List<VisibleAssetTree.Node> nodes,
            UUID assetId) {
        if (nodes == null) {
            return null;
        }
        for (VisibleAssetTree.Node node : nodes) {
            if (node.entityType() == EntityType.ASSET && assetId.equals(node.id())) {
                return node;
            }
            VisibleAssetTree.Node found = findAssetNode(node.children(), assetId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 收集选中资产下所有层级的设备 ID，资产本身不进入设备统计。 */
    private void collectDeviceIds(VisibleAssetTree.Node node, Set<UUID> deviceIds) {
        if (node.entityType() == EntityType.DEVICE) {
            deviceIds.add(node.id());
            return;
        }
        if (node.children() == null) {
            return;
        }
        for (VisibleAssetTree.Node child : node.children()) {
            collectDeviceIds(child, deviceIds);
        }
    }

    /**
     * 并行读取每台设备的最新规范遥测。
     *
     * <p>每台设备只调用一次 findLatest，并在内存中按键建立索引；不会为每个参数
     * 单独访问数据库。规则链未输出的键不会出现在结果中。</p>
     */
    private Map<UUID, Map<String, TsKvEntry>> readLatestTelemetry(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices) throws ThingsboardException {
        if (devices.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Map<String, TsKvEntry>> result = new HashMap<>();
        // 先确定实际要读取的规范键（含设备存在的温度键），再分批并发读取最新值。
        List<String> telemetryKeys = findLatestTelemetryKeys(user, devices);
        for (int batchStart = 0; batchStart < devices.size(); batchStart += TELEMETRY_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + TELEMETRY_BATCH_SIZE, devices.size());
            List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
            for (int index = batchStart; index < batchEnd; index++) {
                MonitoringDeviceContextBO device = devices.get(index);
                futures.add(timeseriesService.findLatest(
                        user.getTenantId(), new DeviceId(device.getDeviceId()), telemetryKeys));
            }

            // 等待当前小批次完成；单个查询失败保留为空结果，批次中其他设备仍可参与统计。
            List<List<TsKvEntry>> entriesByDevice;
            try {
                // 单台设备查询失败时只让该设备使用空遥测，不阻断同批其他设备的统计。
                entriesByDevice = Futures.successfulAsList(futures).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ThingsboardException(
                        "Failed to read smart lightning latest telemetry",
                        e,
                        ThingsboardErrorCode.DATABASE);
            } catch (ExecutionException e) {
                throw new ThingsboardException(
                        "Failed to read smart lightning latest telemetry",
                        e,
                        ThingsboardErrorCode.DATABASE);
            }

            // 将遥测结果按键建立索引，并按原设备顺序写入返回 Map。
            for (int index = 0; index < entriesByDevice.size(); index++) {
                Map<String, TsKvEntry> values = new HashMap<>();
                List<TsKvEntry> entries = entriesByDevice.get(index);
                if (entries == null) {
                    entries = Collections.emptyList();
                }
                for (TsKvEntry entry : entries) {
                    // findLatest 会为未上报的键返回 value=null 的占位对象；这里只索引真实遥测，
                    // 避免 SPD、接地、温度和寿命的“有参数设备数”被错误统计为全部设备数。
                    if (MonitoringValueParser.hasStoredValue(entry)) {
                        values.put(entry.getKey(), entry);
                    }
                }
                result.put(devices.get(batchStart + index).getDeviceId(), values);
            }
        }
        return result;
    }
    /**
     * 从当前资产范围内 SPD 设备的已有遥测键中发现全部温度键，并合并到实时查询。
     * 不固定 Temp1 至 Temp4；设备新增任意包含 Temp 的键时会自动纳入温度状态统计。
     */
    private List<String> findLatestTelemetryKeys(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices) {
        List<String> telemetryKeys = new ArrayList<>(TELEMETRY_KEYS);
        List<MonitoringDeviceContextBO> spdDevices = filterByFunctionCategory(devices, SPD_CATEGORY);
        if (spdDevices.isEmpty()) {
            return telemetryKeys;
        }

        List<EntityId> entityIds = new ArrayList<>(spdDevices.size());
        for (MonitoringDeviceContextBO device : spdDevices) {
            entityIds.add(new DeviceId(device.getDeviceId()));
        }
        Set<String> temperatureKeys = new HashSet<>();
        for (int batchStart = 0; batchStart < entityIds.size(); batchStart += TELEMETRY_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + TELEMETRY_BATCH_SIZE, entityIds.size());
            List<String> keys = timeseriesService.findAllKeysByEntityIds(
                    user.getTenantId(), entityIds.subList(batchStart, batchEnd));
            if (keys != null) {
                keys.stream()
                        .filter(SpdMonitoringCalculator::isTemperatureKey)
                        .forEach(temperatureKeys::add);
            }
        }
        // 温度键去重后排序，再追加到固定规范键列表，保证读取键集合稳定。
        temperatureKeys.stream().sorted().forEach(telemetryKeys::add);
        return telemetryKeys;
    }

    /** 查询未清除告警并按设备 ID 去重，只保留当前资产范围内的设备。 */
    private Set<UUID> findActiveAlarmDeviceIds(
            SecurityUser user,
            List<MonitoringDeviceContextBO> devices) throws ThingsboardException {
        if (devices.isEmpty()) {
            return Collections.emptySet();
        }
        Set<UUID> scopedDeviceIds = new HashSet<>();
        // 构造当前资产范围内的设备集合，避免将其他资产的告警计入本板块。
        for (MonitoringDeviceContextBO device : devices) {
            scopedDeviceIds.add(device.getDeviceId());
        }
        return alarmStatisticsService.findActiveAlarmDeviceIds(user, scopedDeviceIds);
    }

    /** 按服务器属性 functionCategory 分类，名称和设备配置不参与判断。 */
    private List<MonitoringDeviceContextBO> filterByFunctionCategory(
            List<MonitoringDeviceContextBO> devices,
            String category) {
        return MonitoringDeviceFilters.byFunctionCategory(devices, category);
    }

    /** 组装选中范围的整体设备状态。 */
    private SmartLightningRealtimeOverallDTO buildOverall(
            List<MonitoringDeviceContextBO> devices,
            Set<UUID> activeAlarmDeviceIds) {
        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        return new SmartLightningRealtimeOverallDTO(
                devices.size(),
                online,
                devices.size() - online,
                alarmDevices,
                resolveStatus(devices.size(), devices.size() - online, alarmDevices, false));
    }

    /** 按设备汇总 McbStatus、GndStatus、所有 Temp 键、LifeLeft 及最新最大漏流。 */
    private SmartLightningRealtimeSpdDTO buildSpd(
            List<MonitoringDeviceContextBO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        List<List<String>> breakerStatusValues = new ArrayList<>();
        List<List<String>> groundStatusValues = new ArrayList<>();
        List<List<Double>> temperatureValues = new ArrayList<>();
        List<Double> lifeValues = new ArrayList<>();
        Double maximumLeakage = null;
        String maximumLeakageDeviceName = null;
        String maximumLeakageParameter = null;
        Long maximumLeakageTime = null;

        // 逐台提取实时状态、温度、寿命和三相漏电流，并记录最大漏流来源。
        for (MonitoringDeviceContextBO device : devices) {
            Map<String, TsKvEntry> values = valuesOf(telemetryByDevice, device.getDeviceId());
            TsKvEntry breakerStatus = values.get("McbStatus");
            List<String> deviceBreakerStatuses = new ArrayList<>();
            if (breakerStatus != null) {
                deviceBreakerStatuses.add(breakerStatus.getValueAsString());
            }
            breakerStatusValues.add(deviceBreakerStatuses);

            TsKvEntry groundStatus = values.get("GndStatus");
            List<String> deviceGroundStatuses = new ArrayList<>();
            if (groundStatus != null) {
                deviceGroundStatuses.add(groundStatus.getValueAsString());
            }
            groundStatusValues.add(deviceGroundStatuses);

            List<Double> deviceTemperatures = new ArrayList<>();
            for (Map.Entry<String, TsKvEntry> entry : values.entrySet()) {
                if (!SpdMonitoringCalculator.isTemperatureKey(entry.getKey())) {
                    continue;
                }
                Double value = numericValue(entry.getValue());
                deviceTemperatures.add(value == null ? Double.NaN : value);
            }
            temperatureValues.add(deviceTemperatures);

            TsKvEntry life = values.get("LifeLeft");
            if (life == null) {
                lifeValues.add(null);
            } else {
                Double lifeValue = numericValue(life);
                lifeValues.add(lifeValue == null ? Double.NaN : lifeValue);
            }

            for (String key : List.of("LeakCurrA", "LeakCurrB", "LeakCurrC")) {
                TsKvEntry entry = values.get(key);
                Double value = numericValue(entry);
                if (value == null) {
                    continue;
                }
                double absoluteValue = Math.abs(value);
                if (maximumLeakage == null || absoluteValue > maximumLeakage) {
                    maximumLeakage = absoluteValue;
                    maximumLeakageDeviceName = device.getDeviceName();
                    maximumLeakageParameter = key;
                    maximumLeakageTime = entry.getTs();
                }
            }
        }

        // 将各设备原始状态列表交给计算器，统一统计正常数、异常数和参与设备数。
        SpdMonitoringCalculator.StatusCount breakerStatusCount =
                SpdMonitoringCalculator.binaryStatus(breakerStatusValues);
        SpdMonitoringCalculator.StatusCount groundStatusCount =
                SpdMonitoringCalculator.binaryStatus(groundStatusValues);
        SpdMonitoringCalculator.StatusCount temperatureStatusCount =
                SpdMonitoringCalculator.temperatureStatus(temperatureValues);
        SpdMonitoringCalculator.StatusCount lifeStatusCount =
                SpdMonitoringCalculator.degradationStatus(lifeValues);
        boolean hasAbnormalTelemetry = breakerStatusCount.abnormal() > 0
                || groundStatusCount.abnormal() > 0
                || temperatureStatusCount.abnormal() > 0
                || lifeStatusCount.abnormal() > 0;

        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        // 综合离线、告警和遥测状态确定板块状态；有设备但没有可用数据时标记为数据不足。
        String status = resolveStatus(
                devices.size(), devices.size() - online, alarmDevices, hasAbnormalTelemetry);
        if (!devices.isEmpty()
                && alarmDevices == 0
                && devices.size() - online == 0
                && breakerStatusCount.participating() == 0
                && groundStatusCount.participating() == 0
                && temperatureStatusCount.participating() == 0
                && lifeStatusCount.participating() == 0
                && maximumLeakage == null) {
            status = "数据不足";
        }
        return new SmartLightningRealtimeSpdDTO(
                devices.size(), online, devices.size() - online, alarmDevices,
                breakerStatusCount.normal(), breakerStatusCount.participating(),
                groundStatusCount.normal(), groundStatusCount.participating(),
                temperatureStatusCount.normal(), temperatureStatusCount.participating(),
                lifeStatusCount.normal(), lifeStatusCount.participating(),
                maximumLeakage, maximumLeakageDeviceName, maximumLeakageParameter, maximumLeakageTime,
                status);
    }

    /** 组装接地电阻设备的最新阻值最大值和最小值。 */
    private SmartLightningRealtimeGroundDTO buildGround(
            List<MonitoringDeviceContextBO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        int reportingDevices = 0;
        Double maximum = null;
        String maximumDeviceName = null;
        Long maximumTime = null;
        Double minimum = null;
        String minimumDeviceName = null;
        Long minimumTime = null;
        // 只统计可解析的接地阻值，并同步保留最大值、最小值对应的设备和时间。
        for (MonitoringDeviceContextBO device : devices) {
            TsKvEntry entry = valuesOf(telemetryByDevice, device.getDeviceId()).get("GndGridRes");
            Double value = numericValue(entry);
            if (value == null) {
                continue;
            }
            reportingDevices++;
            if (maximum == null || value > maximum) {
                maximum = value;
                maximumDeviceName = device.getDeviceName();
                maximumTime = entry.getTs();
            }
            if (minimum == null || value < minimum) {
                minimum = value;
                minimumDeviceName = device.getDeviceName();
                minimumTime = entry.getTs();
            }
        }
        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        // 先按统一规则生成状态，再区分“全部在线但从未上报阻值”的数据不足场景。
        String status = resolveStatus(devices.size(), devices.size() - online, alarmDevices, false);
        if (!devices.isEmpty() && alarmDevices == 0 && devices.size() - online == 0 && reportingDevices == 0) {
            status = "数据不足";
        }
        return new SmartLightningRealtimeGroundDTO(
                devices.size(), online, devices.size() - online, alarmDevices,
                reportingDevices,
                maximum, maximumDeviceName, maximumTime,
                minimum, minimumDeviceName, minimumTime,
                status);
    }

    /** 组装雷电流设备最新累计次数及最近一次峰值。 */
    private SmartLightningRealtimeCurrentDTO buildCurrent(
            List<MonitoringDeviceContextBO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        long totalSurgeCount = 0;
        Double latestPeak = null;
        String latestPeakDeviceName = null;
        String latestPeakPolarity = null;
        Long latestPeakTime = null;
        // 累加有效的累计次数，并从各设备最新峰值中选出全范围最近一条。
        for (MonitoringDeviceContextBO device : devices) {
            Map<String, TsKvEntry> values = valuesOf(telemetryByDevice, device.getDeviceId());
            Double count = numericValue(values.get("SurgeCnt"));
            if (count != null && count >= 0) {
                totalSurgeCount += Math.round(count);
            }
            TsKvEntry peakEntry = values.get("PeakValue");
            Double peak = numericValue(peakEntry);
            if (peak != null && (latestPeakTime == null || peakEntry.getTs() > latestPeakTime)) {
                latestPeak = Math.abs(peak);
                latestPeakDeviceName = device.getDeviceName();
                latestPeakTime = peakEntry.getTs();
                TsKvEntry polarity = values.get("Polarity");
                latestPeakPolarity = polarity == null ? null : polarity.getValueAsString();
            }
        }
        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        // 组装汇总字段，并根据设备在线及告警情况生成板块状态。
        return new SmartLightningRealtimeCurrentDTO(
                devices.size(), online, devices.size() - online, alarmDevices,
                totalSurgeCount,
                latestPeak, latestPeakDeviceName, latestPeakPolarity, latestPeakTime,
                "kA",
                resolveStatus(devices.size(), devices.size() - online, alarmDevices, false));
    }

    /** 组装故障电弧设备最新状态、累计次数和最近上报时间。 */
    private SmartLightningRealtimeArcFaultDTO buildArcFault(
            List<MonitoringDeviceContextBO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        int statusDevices = 0;
        int normalStatus = 0;
        long totalArcFaultCount = 0;
        Long latestReportTime = null;
        boolean hasAbnormalTelemetry = false;
        // 保持当前故障电弧处理口径：统计 DevStatus 正常/有状态设备，并检查上报时间。
        for (MonitoringDeviceContextBO device : devices) {
            Map<String, TsKvEntry> values = valuesOf(telemetryByDevice, device.getDeviceId());
            TsKvEntry status = values.get("DevStatus");
            if (status != null) {
                statusDevices++;
                if (isNormalStatus(status)) {
                    normalStatus++;
                } else {
                    hasAbnormalTelemetry = true;
                }
            }

            // 从当前设备已读取到的规范遥测中更新最新上报时间。
            for (TsKvEntry entry : values.values()) {
                if (latestReportTime == null || entry.getTs() > latestReportTime) {
                    latestReportTime = entry.getTs();
                }
            }
        }
        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        // 按统一状态优先级生成面板状态；没有状态或遥测的全在线设备显示为数据不足。
        String panelStatus = resolveStatus(
                devices.size(), devices.size() - online, alarmDevices, hasAbnormalTelemetry);
        if (!devices.isEmpty()
                && alarmDevices == 0
                && devices.size() - online == 0
                && statusDevices == 0
                && latestReportTime == null) {
            panelStatus = "数据不足";
        }
        return new SmartLightningRealtimeArcFaultDTO(
                devices.size(), online, devices.size() - online, alarmDevices,
                normalStatus, statusDevices, totalArcFaultCount, latestReportTime,
                panelStatus);
    }

    /** 按设备上下文中的最新 active 遥测统计在线设备数量。 */
    private int countOnline(List<MonitoringDeviceContextBO> devices) {
        int count = 0;
        for (MonitoringDeviceContextBO device : devices) {
            if (Boolean.TRUE.equals(device.getActive())) {
                count++;
            }
        }
        return count;
    }

    /** 统计指定设备集合中存在未清除告警的设备数。 */
    private int countAlarmDevices(
            List<MonitoringDeviceContextBO> devices,
            Set<UUID> activeAlarmDeviceIds) {
        int count = 0;
        for (MonitoringDeviceContextBO device : devices) {
            if (activeAlarmDeviceIds.contains(device.getDeviceId())) {
                count++;
            }
        }
        return count;
    }

    /** 返回设备的最新遥测索引；未读取到遥测时返回只读空表。 */
    private Map<String, TsKvEntry> valuesOf(
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            UUID deviceId) {
        Map<String, TsKvEntry> values = telemetryByDevice.get(deviceId);
        return values == null ? Collections.emptyMap() : values;
    }

    /** 直接判断规则链输出的中文状态，不在后端转换原始状态编码。 */
    private boolean isNormalStatus(TsKvEntry entry) {
        return entry != null && "正常".equals(MonitoringValueParser.trimToEmpty(entry.getValueAsString()));
    }

    /** 将规范遥测值解析为数值；只解析存储类型，不做单位或业务值换算。 */
    private Double numericValue(TsKvEntry entry) {
        return entry == null ? null : MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
    }

    /**
     * 统一生成板块状态：无设备为数据不足，告警或明确异常遥测为异常，
     * 存在离线设备为需关注，其余为正常。
     */
    private String resolveStatus(int total, int offline, int alarmDevices, boolean abnormalTelemetry) {
        if (total == 0) {
            return "数据不足";
        }
        if (alarmDevices > 0 || abnormalTelemetry) {
            return "异常";
        }
        if (offline > 0) {
            return "需关注";
        }
        return "正常";
    }
}
