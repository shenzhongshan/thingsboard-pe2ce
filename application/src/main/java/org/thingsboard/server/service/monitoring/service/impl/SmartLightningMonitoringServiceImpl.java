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
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningDeviceContextDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeResponse;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeAnalysisDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeArcFaultDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeCurrentDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeGroundDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeOverallDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeSpdDTO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringService;
import org.thingsboard.server.service.monitoring.util.MonitoringValueParser;
import org.thingsboard.server.service.security.model.SecurityUser;

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

    private static final int QUERY_PAGE_SIZE = 10_000;
    private static final String SPD_CATEGORY = "spd_monitor";
    private static final String GROUND_CATEGORY = "ground_resistance";
    private static final String CURRENT_CATEGORY = "lightning_current";
    private static final String ARC_FAULT_CATEGORY = "arc_fault";
    private static final double TEMPERATURE_LIMIT = 65D;
    private static final double LIFE_NORMAL_LIMIT = 60D;
    private static final List<String> TELEMETRY_KEYS = List.of(
            "spdStatus", "groundStatus",
            "temperatureA", "temperatureB", "temperatureC", "temperatureD",
            "lifePercent", "leakageA", "leakageB", "leakageC",
            "groundResistance",
            "surgeCount", "surgePeak", "surgePolarity",
            "deviceStatus", "arcFaultCount");

    private final SmartLightningDeviceService deviceService;
    private final SmartLightningMonitoringTreeService treeService;
    private final DeviceAlarmQueryService alarmQueryService;
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
        List<SmartLightningDeviceContextDTO> allDevices = deviceService.getAllLightningDeviceContexts(user);
        List<SmartLightningDeviceContextDTO> scopedDevices = filterDevicesByAsset(user, allDevices, assetId);
        Map<UUID, Map<String, TsKvEntry>> telemetryByDevice = readLatestTelemetry(user, scopedDevices);
        Set<UUID> activeAlarmDeviceIds = findActiveAlarmDeviceIds(user, scopedDevices);

        List<SmartLightningDeviceContextDTO> spdDevices = filterByFunctionCategory(scopedDevices, SPD_CATEGORY);
        List<SmartLightningDeviceContextDTO> groundDevices = filterByFunctionCategory(scopedDevices, GROUND_CATEGORY);
        List<SmartLightningDeviceContextDTO> currentDevices = filterByFunctionCategory(scopedDevices, CURRENT_CATEGORY);
        List<SmartLightningDeviceContextDTO> arcFaultDevices = filterByFunctionCategory(scopedDevices, ARC_FAULT_CATEGORY);

        return new SmartLightningRealtimeAnalysisDTO(
                buildOverall(scopedDevices, activeAlarmDeviceIds),
                buildSpd(spdDevices, telemetryByDevice, activeAlarmDeviceIds),
                buildGround(groundDevices, telemetryByDevice, activeAlarmDeviceIds),
                buildCurrent(currentDevices, telemetryByDevice, activeAlarmDeviceIds),
                buildArcFault(arcFaultDevices, telemetryByDevice, activeAlarmDeviceIds));
    }

    /** 根据选中资产裁剪设备范围；找不到资产时返回空集合，避免越权回退为全局。 */
    private List<SmartLightningDeviceContextDTO> filterDevicesByAsset(
            SecurityUser user,
            List<SmartLightningDeviceContextDTO> devices,
            UUID assetId) throws ThingsboardException {
        if (assetId == null) {
            return devices;
        }
        SmartLightningMonitoringTreeResponse tree = treeService.getTree(user);
        SmartLightningMonitoringTreeResponse.TreeNode selectedNode = findAssetNode(tree.roots(), assetId);
        if (selectedNode == null) {
            return Collections.emptyList();
        }
        Set<UUID> descendantDeviceIds = new HashSet<>();
        collectDeviceIds(selectedNode, descendantDeviceIds);
        List<SmartLightningDeviceContextDTO> result = new ArrayList<>();
        for (SmartLightningDeviceContextDTO device : devices) {
            if (descendantDeviceIds.contains(device.getDeviceId())) {
                result.add(device);
            }
        }
        return result;
    }

    /** 递归查找指定资产节点；设备节点不允许作为数据监测页的统计范围。 */
    private SmartLightningMonitoringTreeResponse.TreeNode findAssetNode(
            List<SmartLightningMonitoringTreeResponse.TreeNode> nodes,
            UUID assetId) {
        if (nodes == null) {
            return null;
        }
        for (SmartLightningMonitoringTreeResponse.TreeNode node : nodes) {
            if (node.entityType() == EntityType.ASSET && assetId.equals(node.id())) {
                return node;
            }
            SmartLightningMonitoringTreeResponse.TreeNode found = findAssetNode(node.children(), assetId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 收集选中资产下所有层级的设备 ID，资产本身不进入设备统计。 */
    private void collectDeviceIds(SmartLightningMonitoringTreeResponse.TreeNode node, Set<UUID> deviceIds) {
        if (node.entityType() == EntityType.DEVICE) {
            deviceIds.add(node.id());
            return;
        }
        if (node.children() == null) {
            return;
        }
        for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
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
            List<SmartLightningDeviceContextDTO> devices) throws ThingsboardException {
        if (devices.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ListenableFuture<List<TsKvEntry>>> futures = new ArrayList<>();
        for (SmartLightningDeviceContextDTO device : devices) {
            futures.add(timeseriesService.findLatest(
                    user.getTenantId(), new DeviceId(device.getDeviceId()), TELEMETRY_KEYS));
        }
        List<List<TsKvEntry>> entriesByDevice;
        try {
            entriesByDevice = Futures.allAsList(futures).get();
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

        Map<UUID, Map<String, TsKvEntry>> result = new HashMap<>();
        for (int index = 0; index < devices.size(); index++) {
            Map<String, TsKvEntry> values = new HashMap<>();
            for (TsKvEntry entry : entriesByDevice.get(index)) {
                // findLatest 会为未上报的键返回 value=null 的占位对象；这里只索引真实遥测，
                // 避免 SPD、接地、温度和寿命的“有参数设备数”被错误统计为全部设备数。
                if (MonitoringValueParser.hasStoredValue(entry)) {
                    values.put(entry.getKey(), entry);
                }
            }
            result.put(devices.get(index).getDeviceId(), values);
        }
        return result;
    }

    /** 查询未清除告警并按设备 ID 去重，只保留当前资产范围内的设备。 */
    private Set<UUID> findActiveAlarmDeviceIds(
            SecurityUser user,
            List<SmartLightningDeviceContextDTO> devices) throws ThingsboardException {
        if (devices.isEmpty()) {
            return Collections.emptySet();
        }
        Set<UUID> scopedDeviceIds = new HashSet<>();
        for (SmartLightningDeviceContextDTO device : devices) {
            scopedDeviceIds.add(device.getDeviceId());
        }
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        AlarmQueryV2 query = new AlarmQueryV2(
                null,
                pageLink,
                Collections.emptyList(),
                List.of(AlarmSearchStatus.ACTIVE),
                Collections.emptyList(),
                null);
        Set<UUID> result = new HashSet<>();
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            EntityId originator = alarm.getOriginator();
            if (originator != null
                    && originator.getEntityType() == EntityType.DEVICE
                    && scopedDeviceIds.contains(originator.getId())) {
                result.add(originator.getId());
            }
        }
        return result;
    }

    /** 按服务器属性 functionCategory 分类，名称和设备配置不参与判断。 */
    private List<SmartLightningDeviceContextDTO> filterByFunctionCategory(
            List<SmartLightningDeviceContextDTO> devices,
            String category) {
        List<SmartLightningDeviceContextDTO> result = new ArrayList<>();
        for (SmartLightningDeviceContextDTO device : devices) {
            if (category.equalsIgnoreCase(MonitoringValueParser.trimToEmpty(device.getFunctionCategory()))) {
                result.add(device);
            }
        }
        return result;
    }

    /** 组装选中范围的整体设备状态。 */
    private SmartLightningRealtimeOverallDTO buildOverall(
            List<SmartLightningDeviceContextDTO> devices,
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

    /** 组装 SPD 状态参数正常数、参与数和最新最大漏流。 */
    private SmartLightningRealtimeSpdDTO buildSpd(
            List<SmartLightningDeviceContextDTO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        int spdStatusDevices = 0;
        int spdStatusNormal = 0;
        int groundStatusDevices = 0;
        int groundStatusNormal = 0;
        int temperatureDevices = 0;
        int temperatureNormal = 0;
        int lifeDevices = 0;
        int lifeNormal = 0;
        boolean hasAbnormalTelemetry = false;
        Double maximumLeakage = null;
        String maximumLeakageDeviceName = null;
        String maximumLeakageParameter = null;
        Long maximumLeakageTime = null;

        for (SmartLightningDeviceContextDTO device : devices) {
            Map<String, TsKvEntry> values = valuesOf(telemetryByDevice, device.getDeviceId());
            TsKvEntry spdStatus = values.get("spdStatus");
            if (spdStatus != null) {
                spdStatusDevices++;
                if (isNormalStatus(spdStatus)) {
                    spdStatusNormal++;
                } else {
                    hasAbnormalTelemetry = true;
                }
            }
            TsKvEntry groundStatus = values.get("groundStatus");
            if (groundStatus != null) {
                groundStatusDevices++;
                if (isNormalStatus(groundStatus)) {
                    groundStatusNormal++;
                } else {
                    hasAbnormalTelemetry = true;
                }
            }

            boolean hasTemperature = false;
            boolean normalTemperature = true;
            for (String key : List.of("temperatureA", "temperatureB", "temperatureC", "temperatureD")) {
                TsKvEntry entry = values.get(key);
                if (entry == null) {
                    continue;
                }
                hasTemperature = true;
                Double value = numericValue(entry);
                if (value == null || value >= TEMPERATURE_LIMIT) {
                    normalTemperature = false;
                }
            }
            if (hasTemperature) {
                temperatureDevices++;
                if (normalTemperature) {
                    temperatureNormal++;
                } else {
                    hasAbnormalTelemetry = true;
                }
            }

            TsKvEntry life = values.get("lifePercent");
            if (life != null) {
                lifeDevices++;
                Double value = numericValue(life);
                if (value != null && value >= LIFE_NORMAL_LIMIT) {
                    lifeNormal++;
                } else {
                    hasAbnormalTelemetry = true;
                }
            }

            for (String key : List.of("leakageA", "leakageB", "leakageC")) {
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

        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        String status = resolveStatus(
                devices.size(), devices.size() - online, alarmDevices, hasAbnormalTelemetry);
        if (!devices.isEmpty()
                && alarmDevices == 0
                && devices.size() - online == 0
                && spdStatusDevices == 0
                && groundStatusDevices == 0
                && temperatureDevices == 0
                && lifeDevices == 0
                && maximumLeakage == null) {
            status = "数据不足";
        }
        return new SmartLightningRealtimeSpdDTO(
                devices.size(), online, devices.size() - online, alarmDevices,
                spdStatusNormal, spdStatusDevices,
                groundStatusNormal, groundStatusDevices,
                temperatureNormal, temperatureDevices,
                lifeNormal, lifeDevices,
                maximumLeakage, maximumLeakageDeviceName, maximumLeakageParameter, maximumLeakageTime,
                status);
    }

    /** 组装接地电阻设备的最新阻值最大值和最小值。 */
    private SmartLightningRealtimeGroundDTO buildGround(
            List<SmartLightningDeviceContextDTO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        int reportingDevices = 0;
        Double maximum = null;
        String maximumDeviceName = null;
        Long maximumTime = null;
        Double minimum = null;
        String minimumDeviceName = null;
        Long minimumTime = null;
        for (SmartLightningDeviceContextDTO device : devices) {
            TsKvEntry entry = valuesOf(telemetryByDevice, device.getDeviceId()).get("groundResistance");
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
            List<SmartLightningDeviceContextDTO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        long totalSurgeCount = 0;
        Double latestPeak = null;
        String latestPeakDeviceName = null;
        String latestPeakPolarity = null;
        Long latestPeakTime = null;
        for (SmartLightningDeviceContextDTO device : devices) {
            Map<String, TsKvEntry> values = valuesOf(telemetryByDevice, device.getDeviceId());
            Double count = numericValue(values.get("surgeCount"));
            if (count != null && count >= 0) {
                totalSurgeCount += Math.round(count);
            }
            TsKvEntry peakEntry = values.get("surgePeak");
            Double peak = numericValue(peakEntry);
            if (peak != null && (latestPeakTime == null || peakEntry.getTs() > latestPeakTime)) {
                latestPeak = Math.abs(peak);
                latestPeakDeviceName = device.getDeviceName();
                latestPeakTime = peakEntry.getTs();
                TsKvEntry polarity = values.get("surgePolarity");
                latestPeakPolarity = polarity == null ? null : polarity.getValueAsString();
            }
        }
        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
        return new SmartLightningRealtimeCurrentDTO(
                devices.size(), online, devices.size() - online, alarmDevices,
                totalSurgeCount,
                latestPeak, latestPeakDeviceName, latestPeakPolarity, latestPeakTime,
                "kA",
                resolveStatus(devices.size(), devices.size() - online, alarmDevices, false));
    }

    /** 组装故障电弧设备最新状态、累计次数和最近上报时间。 */
    private SmartLightningRealtimeArcFaultDTO buildArcFault(
            List<SmartLightningDeviceContextDTO> devices,
            Map<UUID, Map<String, TsKvEntry>> telemetryByDevice,
            Set<UUID> activeAlarmDeviceIds) {
        int statusDevices = 0;
        int normalStatus = 0;
        long totalArcFaultCount = 0;
        Long latestReportTime = null;
        boolean hasAbnormalTelemetry = false;
        for (SmartLightningDeviceContextDTO device : devices) {
            Map<String, TsKvEntry> values = valuesOf(telemetryByDevice, device.getDeviceId());
            TsKvEntry status = values.get("deviceStatus");
            if (status != null) {
                statusDevices++;
                if (isNormalStatus(status)) {
                    normalStatus++;
                } else {
                    hasAbnormalTelemetry = true;
                }
            }
            Double count = numericValue(values.get("arcFaultCount"));
            if (count != null && count >= 0) {
                totalArcFaultCount += Math.round(count);
            }
            for (TsKvEntry entry : values.values()) {
                if (latestReportTime == null || entry.getTs() > latestReportTime) {
                    latestReportTime = entry.getTs();
                }
            }
        }
        int online = countOnline(devices);
        int alarmDevices = countAlarmDevices(devices, activeAlarmDeviceIds);
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

    /** 统计设备服务器属性 active=true 的数量。 */
    private int countOnline(List<SmartLightningDeviceContextDTO> devices) {
        int count = 0;
        for (SmartLightningDeviceContextDTO device : devices) {
            if (Boolean.TRUE.equals(device.getActive())) {
                count++;
            }
        }
        return count;
    }

    /** 统计指定设备集合中存在未清除告警的设备数。 */
    private int countAlarmDevices(
            List<SmartLightningDeviceContextDTO> devices,
            Set<UUID> activeAlarmDeviceIds) {
        int count = 0;
        for (SmartLightningDeviceContextDTO device : devices) {
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
