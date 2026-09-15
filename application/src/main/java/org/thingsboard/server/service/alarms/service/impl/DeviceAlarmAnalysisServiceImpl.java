/** 文件职责：告警分析周期统计实现，并复用同一次告警查询生成日/月/年趋势数据。 */
package org.thingsboard.server.service.alarms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisOverviewDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmDeviceCoverageDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmLeafAssetCountDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmPeriodSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSeverityTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmStatusTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTrendPointDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmFocusDeviceDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningMonitoringTreeResponse;
import org.thingsboard.server.service.alarms.service.DeviceAlarmAnalysisService;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.monitoring.service.SmartLightningMonitoringTreeService;
import org.thingsboard.server.service.monitoring.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 全体设备告警分析周期统计实现；按告警 startTs（触发时间）计数。
 *
 * <p>统计今日/本月/今年的告警次数，以及昨日/上月/去年的对比次数，
 * 并计算环比变化百分比。窗口边界按系统时区的自然日/月/年计算，
 * 而不是简单的 24 小时回溯。</p>
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceAlarmAnalysisServiceImpl implements DeviceAlarmAnalysisService {

    /** 单次告警或设备查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningDeviceService deviceService;
    private final DeviceAlarmQueryService alarmQueryService;
    private final SmartLightningMonitoringTreeService monitoringTreeService;

    /**
     * 统计当前用户可见设备在日、月、年三个周期的告警次数及环比。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 日、月、年三个周期各自的当前次数、上一周期次数和变化百分比
     */
    @Override
    public DeviceAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        // 全部可见设备（不区分产品分类），与告警分析页“全体设备”的口径一致。
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        PeriodWindow window = PeriodWindow.current(ZoneId.systemDefault());
        // counts 依次存放：今日、昨日、本月、上月、今年、去年的告警次数。
        long[] counts = new long[6];
        Map<UUID, String> parentNodesByDevice = buildDeviceParentNames(monitoringTreeService.getTree(user));
        Map<UUID, FocusAlarmAccumulator> focusDevicesById = new LinkedHashMap<>();
        TrendBuckets trendBuckets = new TrendBuckets(window);
        if (!visibleDeviceIds.isEmpty()) {
            countAlarms(user, visibleDeviceIds, window, counts, trendBuckets);
            collectActiveCriticalAlarms(user, visibleDeviceIds, parentNodesByDevice, focusDevicesById);
        }
        return new DeviceAlarmAnalysisSummaryDTO(
                createPeriodSummary(counts[0], counts[1]),
                createPeriodSummary(counts[2], counts[3]),
                createPeriodSummary(counts[4], counts[5]),
                trendBuckets.dayTrend(),
                trendBuckets.monthTrend(),
                trendBuckets.yearTrend(),
                buildFocusDevices(focusDevicesById)
        );
    }

    /**
     * 统计每个末级设备父资产直接挂载设备在指定周期内的告警数量和处理率。
     * 末级父资产必须至少包含一个子节点，并且所有直接子节点均为设备。
     */
    @Override
    public List<DeviceAlarmLeafAssetCountDTO> getLeafAssetCounts(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        SmartLightningMonitoringTreeResponse tree = monitoringTreeService.getTree(user);
        Map<UUID, ParentAssetCounter> counters = new LinkedHashMap<>();
        Map<UUID, UUID> deviceToParentAsset = new HashMap<>();
        for (SmartLightningMonitoringTreeResponse.TreeNode root : tree.roots()) {
            collectDeviceParentAssets(root, counters, deviceToParentAsset, root.name());
        }
        if (counters.isEmpty()) {
            return Collections.emptyList();
        }

        AlarmQueryV2 query = createAllAlarmQuery();
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            if (alarm.getOriginator() == null
                    || alarm.getOriginator().getEntityType() != EntityType.DEVICE
                    || !window.contains(alarm.getStartTs())) {
                continue;
            }
            UUID parentAssetId = deviceToParentAsset.get(alarm.getOriginator().getId());
            if (parentAssetId == null) {
                continue;
            }
            ParentAssetCounter counter = counters.get(parentAssetId);
            counter.alarmCount++;
            if (alarm.isCleared()) {
                counter.handledCount++;
            }
        }

        List<DeviceAlarmLeafAssetCountDTO> result = new ArrayList<>();
        for (ParentAssetCounter counter : counters.values()) {
            result.add(new DeviceAlarmLeafAssetCountDTO(
                    counter.assetId,
                    counter.assetName,
                    counter.assetPath,
                    counter.alarmCount,
                    counter.handledCount,
                    counter.handledRate()));
        }
        result.sort(Comparator.comparingDouble(DeviceAlarmLeafAssetCountDTO::getHandledRate)
                .reversed()
                .thenComparing(Comparator.comparingLong(DeviceAlarmLeafAssetCountDTO::getAlarmCount)
                        .reversed())
                .thenComparing(DeviceAlarmLeafAssetCountDTO::getAssetName,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    /** 按告警触发时间统计当前用户可见设备的告警类型排行。 */
    @Override
    public List<DeviceAlarmTypeTotalDTO> getTypeRanking(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        if (visibleDeviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> countsByType = new HashMap<>();
        for (AlarmInfo alarm : alarmQueryService.findAll(user, createAllAlarmQuery())) {
            if (!isVisibleDeviceAlarm(alarm, visibleDeviceIds)
                    || !window.contains(alarm.getStartTs())) {
                continue;
            }
            String type = normalizeAlarmType(alarm.getType());
            countsByType.put(type, countsByType.getOrDefault(type, 0) + 1);
        }

        List<DeviceAlarmTypeTotalDTO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countsByType.entrySet()) {
            result.add(new DeviceAlarmTypeTotalDTO(entry.getKey(), entry.getValue()));
        }
        result.sort(new Comparator<DeviceAlarmTypeTotalDTO>() {
            @Override
            public int compare(DeviceAlarmTypeTotalDTO left, DeviceAlarmTypeTotalDTO right) {
                int countOrder = Integer.compare(right.getCount(), left.getCount());
                return countOrder != 0 ? countOrder : left.getType().compareTo(right.getType());
            }
        });
        return result;
    }

    /** 按告警触发时间统计当前用户可见设备的严重程度分布。 */
    @Override
    public List<DeviceAlarmSeverityTotalDTO> getSeverityRanking(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        if (visibleDeviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<AlarmSeverity, Integer> counts = new EnumMap<>(AlarmSeverity.class);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, createAllAlarmQuery())) {
            if (!isVisibleDeviceAlarm(alarm, visibleDeviceIds)
                    || !window.contains(alarm.getStartTs())
                    || alarm.getSeverity() == null) {
                continue;
            }
            AlarmSeverity severity = alarm.getSeverity();
            counts.put(severity, counts.getOrDefault(severity, 0) + 1);
        }

        List<DeviceAlarmSeverityTotalDTO> result = new ArrayList<>();
        for (AlarmSeverity severity : AlarmSeverity.values()) {
            result.add(new DeviceAlarmSeverityTotalDTO(
                    severity.name(), counts.getOrDefault(severity, 0)));
        }
        result.sort(new Comparator<DeviceAlarmSeverityTotalDTO>() {
            @Override
            public int compare(DeviceAlarmSeverityTotalDTO left, DeviceAlarmSeverityTotalDTO right) {
                return Integer.compare(right.getCount(), left.getCount());
            }
        });
        return result;
    }

    /** 按触发时间统计四种告警状态，并按设备 ID 去重计算告警覆盖率。 */
    @Override
    public DeviceAlarmAnalysisOverviewDTO getOverview(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        Map<AlarmStatus, Long> statusCounts = createEmptyStatusCounts();
        Set<UUID> alarmedDeviceIds = new HashSet<>();
        Set<UUID> activeAlarmDeviceIds = new HashSet<>();

        if (!visibleDeviceIds.isEmpty()) {
            collectOverviewData(user, createAllAlarmQuery(), window, visibleDeviceIds,
                    statusCounts, alarmedDeviceIds, activeAlarmDeviceIds);
        }

        List<DeviceAlarmStatusTotalDTO> statusDistribution = new ArrayList<>();
        for (AlarmStatus status : AlarmStatus.values()) {
            statusDistribution.add(new DeviceAlarmStatusTotalDTO(
                    status.name(), statusCounts.get(status)));
        }
        DeviceAlarmDeviceCoverageDTO coverage = new DeviceAlarmDeviceCoverageDTO(
                visibleDeviceIds.size(), alarmedDeviceIds.size(), activeAlarmDeviceIds.size());
        return new DeviceAlarmAnalysisOverviewDTO(statusDistribution, coverage);
    }

    /** 创建不附带时间条件的告警查询，业务周期统一按 alarm.startTs 在内存中判断。 */
    private AlarmQueryV2 createAllAlarmQuery() {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        return new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
    }

    /** 遍历周期内的可见设备告警，累计状态分布和设备覆盖集合。 */
    private void collectOverviewData(SecurityUser user, AlarmQueryV2 query,
                                     MonitoringPeriodWindow window,
                                     Set<UUID> visibleDeviceIds,
                                     Map<AlarmStatus, Long> statusCounts,
                                     Set<UUID> alarmedDeviceIds,
                                     Set<UUID> activeAlarmDeviceIds) throws ThingsboardException {
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            if (!isVisibleDeviceAlarm(alarm, visibleDeviceIds)
                    || !window.contains(alarm.getStartTs())) {
                continue;
            }
            AlarmStatus status = alarm.getStatus();
            statusCounts.put(status, statusCounts.get(status) + 1L);
            UUID deviceId = alarm.getOriginator().getId();
            alarmedDeviceIds.add(deviceId);
            if (!status.isCleared()) {
                activeAlarmDeviceIds.add(deviceId);
            }
        }
    }

    /** 初始化全部告警状态计数，确保没有记录的状态也返回 0。 */
    private Map<AlarmStatus, Long> createEmptyStatusCounts() {
        Map<AlarmStatus, Long> counts = new EnumMap<>(AlarmStatus.class);
        for (AlarmStatus status : AlarmStatus.values()) {
            counts.put(status, 0L);
        }
        return counts;
    }

    /** 递归登记末级设备父资产，并建立设备到直属父资产的映射。 */
    private void collectDeviceParentAssets(
            SmartLightningMonitoringTreeResponse.TreeNode node,
            Map<UUID, ParentAssetCounter> counters,
            Map<UUID, UUID> deviceToParentAsset,
            String currentPath) {
        if (node.entityType() == EntityType.ASSET) {
            boolean hasChildren = !node.children().isEmpty();
            boolean allChildrenAreDevices = hasChildren;
            for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
                if (child.entityType() != EntityType.DEVICE) {
                    allChildrenAreDevices = false;
                }
            }
            if (allChildrenAreDevices) {
                counters.putIfAbsent(node.id(), new ParentAssetCounter(node.id(), node.name(), currentPath));
                for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
                    deviceToParentAsset.put(child.id(), node.id());
                }
            }
        }
        for (SmartLightningMonitoringTreeResponse.TreeNode child : node.children()) {
            if (child.entityType() == EntityType.ASSET) {
                collectDeviceParentAssets(child, counters, deviceToParentAsset,
                        appendPath(currentPath, child.name()));
            }
        }
    }

    /** 拼接资产绝对路径，避免空名称产生多余分隔符。 */
    private String appendPath(String parentPath, String childName) {
        if (parentPath == null || parentPath.isBlank()) {
            return childName == null ? "" : childName;
        }
        if (childName == null || childName.isBlank()) {
            return parentPath;
        }
        return parentPath + "/" + childName;
    }

    /** 单个设备直属父资产的告警累计对象。 */
    private static final class ParentAssetCounter {
        private final UUID assetId;
        private final String assetName;
        private final String assetPath;
        private long alarmCount;
        private long handledCount;

        private ParentAssetCounter(UUID assetId, String assetName, String assetPath) {
            this.assetId = assetId;
            this.assetName = assetName;
            this.assetPath = assetPath;
        }

        /** 返回已清除告警占全部告警的百分比，保留两位小数。 */
        private double handledRate() {
            if (alarmCount == 0) {
                return 0D;
            }
            double rate = handledCount * 100D / alarmCount;
            return Math.round(rate * 100D) / 100D;
        }
    }

    /**
     * 查询上一年度起至今的告警，再按 startTs 精确落入日/月/年窗口，避免按 createdTime 误统计。
     *
     * <p>原生告警查询的时间过滤基于 createdTime（入库时间），
     * 而业务统计要求按触发时间 startTs 分桶；因此查询窗口放宽到去年年初，
     * 拿到全部候选告警后再逐条判断 startTs 落在哪个周期。</p>
     */
    private void countAlarms(SecurityUser user, Set<UUID> deviceIds, PeriodWindow window,
                             long[] counts, TrendBuckets trendBuckets) throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                window.previousYearStart, window.now);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            // 只统计可见设备的告警，排除其他实体（资产、客户等）产生的告警。
            if (isVisibleDeviceAlarm(alarm, deviceIds)) {
                addToPeriods(alarm.getStartTs(), window, counts);
                trendBuckets.add(alarm.getStartTs());
            }
        }
    }

    /**
     * 单独复用公共告警查询服务读取当前仍未清除的严重告警。
     * 该查询不限制时间起点，避免长期未清除的严重告警被年度统计窗口截掉。
     */
    private void collectActiveCriticalAlarms(SecurityUser user, Set<UUID> deviceIds,
                                             Map<UUID, String> parentNodesByDevice,
                                             Map<UUID, FocusAlarmAccumulator> focusDevicesById)
            throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC);
        List<AlarmSearchStatus> activeStatuses = new ArrayList<>();
        activeStatuses.add(AlarmSearchStatus.ACTIVE);
        List<AlarmSeverity> criticalSeverity = new ArrayList<>();
        criticalSeverity.add(AlarmSeverity.CRITICAL);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), activeStatuses, criticalSeverity, null);
        long now = System.currentTimeMillis();
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            if (isVisibleDeviceAlarm(alarm, deviceIds)) {
                collectFocusAlarm(alarm, parentNodesByDevice, focusDevicesById, now);
            }
        }
    }

    /** 收集严重等级且尚未清除的告警，并按设备合并多个告警类型。 */
    private void collectFocusAlarm(AlarmInfo alarm, Map<UUID, String> parentNodesByDevice,
                                   Map<UUID, FocusAlarmAccumulator> focusDevicesById, long now) {
        if (alarm.getSeverity() != AlarmSeverity.CRITICAL || alarm.isCleared()
                || alarm.getOriginator() == null || alarm.getOriginator().getId() == null) {
            return;
        }
        UUID deviceId = alarm.getOriginator().getId();
        String deviceName = alarm.getOriginatorName();
        if (deviceName == null || deviceName.isBlank()) {
            deviceName = deviceId.toString();
        }
        String parentNodeName = parentNodesByDevice.getOrDefault(deviceId, "未配置直属节点");
        FocusAlarmAccumulator accumulator = focusDevicesById.get(deviceId);
        if (accumulator == null) {
            accumulator = new FocusAlarmAccumulator(deviceName, parentNodeName);
            focusDevicesById.put(deviceId, accumulator);
        }
        accumulator.addAlarmType(normalizeAlarmType(alarm.getType()));
        accumulator.updateDuration(Math.max(0L, now - alarm.getStartTs()));
    }

    /** 将重点设备按持续时间降序输出，持续时间相同再按设备名排序。 */
    private List<DeviceAlarmFocusDeviceDTO> buildFocusDevices(
            Map<UUID, FocusAlarmAccumulator> focusDevicesById) {
        List<FocusAlarmAccumulator> accumulators = new ArrayList<>(focusDevicesById.values());
        accumulators.sort(new Comparator<FocusAlarmAccumulator>() {
            @Override
            public int compare(FocusAlarmAccumulator left, FocusAlarmAccumulator right) {
                int durationOrder = Long.compare(right.durationMs, left.durationMs);
                return durationOrder != 0 ? durationOrder : left.deviceName.compareTo(right.deviceName);
            }
        });
        List<DeviceAlarmFocusDeviceDTO> result = new ArrayList<>();
        for (FocusAlarmAccumulator accumulator : accumulators) {
            result.add(new DeviceAlarmFocusDeviceDTO(
                    accumulator.deviceName,
                    accumulator.parentNodeName,
                    String.join("/", accumulator.alarmTypes),
                    accumulator.durationMs
            ));
        }
        return result;
    }

    /** 从现有监测资产树提取设备直属父节点，避免再次查询关系接口。 */
    private Map<UUID, String> buildDeviceParentNames(SmartLightningMonitoringTreeResponse tree) {
        Map<UUID, String> parentNodesByDevice = new LinkedHashMap<>();
        for (SmartLightningMonitoringTreeResponse.TreeNode root : tree.roots()) {
            collectDeviceParentNames(root, parentNodesByDevice);
        }
        return parentNodesByDevice;
    }

    /** 只记录设备的直接父节点，不把更上层资产误当成直属节点。 */
    private void collectDeviceParentNames(SmartLightningMonitoringTreeResponse.TreeNode parent,
                                          Map<UUID, String> parentNodesByDevice) {
        for (SmartLightningMonitoringTreeResponse.TreeNode child : parent.children()) {
            if (child.entityType() == EntityType.DEVICE) {
                parentNodesByDevice.put(child.id(), parent.name());
            } else {
                collectDeviceParentNames(child, parentNodesByDevice);
            }
        }
    }

    /** 重点设备聚合对象，避免一个设备的多条严重告警重复占用列表行。 */
    private static final class FocusAlarmAccumulator {
        private final String deviceName;
        private final String parentNodeName;
        private final List<String> alarmTypes = new ArrayList<>();
        private long durationMs;

        private FocusAlarmAccumulator(String deviceName, String parentNodeName) {
            this.deviceName = deviceName;
            this.parentNodeName = parentNodeName;
        }

        private void addAlarmType(String alarmType) {
            if (!alarmTypes.contains(alarmType)) {
                alarmTypes.add(alarmType);
            }
        }

        private void updateDuration(long duration) {
            durationMs = Math.max(durationMs, duration);
        }
    }

    /** 空告警类型统一显示为未分类。 */
    private String normalizeAlarmType(String type) {
        return type == null || type.isBlank() ? "未分类" : type.trim();
    }

    /** 判断告警是否由当前用户可见的设备产生。 */
    private boolean isVisibleDeviceAlarm(AlarmInfo alarm, Set<UUID> deviceIds) {
        EntityId originator = alarm.getOriginator();
        return originator != null
                && originator.getEntityType() == EntityType.DEVICE
                && deviceIds.contains(originator.getId());
    }

    /** 按触发时间把告警计入今日/昨日、本月/上月、今年/去年六个统计桶。 */
    private void addToPeriods(long triggerTime, PeriodWindow window, long[] counts) {
        // 今日：[今天0点, 当前时刻)
        if (triggerTime >= window.dayStart && triggerTime < window.now) {
            counts[0]++;
        }
        // 昨日：[昨天0点, 今天0点)
        if (triggerTime >= window.previousDayStart && triggerTime < window.dayStart) {
            counts[1]++;
        }
        // 本月：[本月1日0点, 当前时刻)
        if (triggerTime >= window.monthStart && triggerTime < window.now) {
            counts[2]++;
        }
        // 上月：[上月1日0点, 本月1日0点)
        if (triggerTime >= window.previousMonthStart && triggerTime < window.monthStart) {
            counts[3]++;
        }
        // 今年：[今年1月1日0点, 当前时刻)
        if (triggerTime >= window.yearStart && triggerTime < window.now) {
            counts[4]++;
        }
        // 去年：[去年1月1日0点, 今年1月1日0点)
        if (triggerTime >= window.previousYearStart && triggerTime < window.yearStart) {
            counts[5]++;
        }
    }

    /**
     * 按自然周期把告警触发时间分配到小时、星期、日期和月份趋势数组。
     * 查询和权限过滤复用现有告警分析流程，不新增重复的告警查询接口。
     */
    private static final class TrendBuckets {
        private final PeriodWindow window;
        private final long[] day = new long[24];
        private final long[] month;
        private final long[] year = new long[12];

        private TrendBuckets(PeriodWindow window) {
            this.window = window;
            this.month = new long[window.monthLength];
        }

        private void add(long triggerTime) {
            if (triggerTime >= window.dayStart && triggerTime < window.now) {
                ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerTime), window.zoneId);
                day[time.getHour()]++;
            }
            if (triggerTime >= window.monthStart && triggerTime < window.now) {
                ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerTime), window.zoneId);
                month[time.getDayOfMonth() - 1]++;
            }
            if (triggerTime >= window.yearStart && triggerTime < window.now) {
                ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerTime), window.zoneId);
                year[time.getMonthValue() - 1]++;
            }
        }

        private List<DeviceAlarmTrendPointDTO> dayTrend() {
            List<DeviceAlarmTrendPointDTO> points = new ArrayList<>();
            for (int index = 0; index < day.length; index++) {
                points.add(new DeviceAlarmTrendPointDTO(String.format("%02d:00", index), day[index]));
            }
            return points;
        }

        private List<DeviceAlarmTrendPointDTO> monthTrend() {
            List<DeviceAlarmTrendPointDTO> points = new ArrayList<>();
            for (int index = 0; index < month.length; index++) {
                points.add(new DeviceAlarmTrendPointDTO(String.format("%02d", index + 1), month[index]));
            }
            return points;
        }

        private List<DeviceAlarmTrendPointDTO> yearTrend() {
            List<DeviceAlarmTrendPointDTO> points = new ArrayList<>();
            for (int index = 0; index < year.length; index++) {
                points.add(new DeviceAlarmTrendPointDTO(String.format("%02d", index + 1), year[index]));
            }
            return points;
        }
    }

    /** 组装单个周期的统计 DTO：当前次数、上一周期次数和环比百分比。 */
    private DeviceAlarmPeriodSummaryDTO createPeriodSummary(long currentCount, long previousCount) {
        return new DeviceAlarmPeriodSummaryDTO(
                currentCount,
                previousCount,
                calculateChangePercent(currentCount, previousCount)
        );
    }

    /**
     * 计算环比变化百分比，结果保留两位小数。
     *
     * <p>上一周期为 0 时：当前也为 0 返回 0%（没有变化），
     * 否则返回 100%（从无到有按全增处理）。</p>
     */
    private double calculateChangePercent(long currentCount, long previousCount) {
        if (previousCount == 0) {
            return currentCount == 0 ? 0D : 100D;
        }
        double percent = (currentCount - previousCount) * 100D / previousCount;
        return Math.round(percent * 100D) / 100D;
    }

    /**
     * 自然日、月、年的时间窗口。
     *
     * <p>记录当前时刻、今日/昨日、本月/上月、今年/去年各窗口的起始时间戳，
     * 边界按系统时区计算，保证统计口径是自然周期而非滚动窗口。</p>
     */
    private static final class PeriodWindow {
        private final ZoneId zoneId;
        private final long now;
        private final long dayStart;
        private final long previousDayStart;
        private final long monthStart;
        private final long previousMonthStart;
        private final long yearStart;
        private final long previousYearStart;
        private final int monthLength;

        private PeriodWindow(ZoneId zoneId, long now, long dayStart, long previousDayStart, long monthStart,
                             long previousMonthStart, long yearStart, long previousYearStart, int monthLength) {
            this.zoneId = zoneId;
            this.now = now;
            this.dayStart = dayStart;
            this.previousDayStart = previousDayStart;
            this.monthStart = monthStart;
            this.previousMonthStart = previousMonthStart;
            this.yearStart = yearStart;
            this.previousYearStart = previousYearStart;
            this.monthLength = monthLength;
        }

        /** 以当前系统时间计算各自然周期窗口的起止时间戳（毫秒）。 */
        private static PeriodWindow current(ZoneId zoneId) {
            ZonedDateTime current = ZonedDateTime.now(zoneId);
            // 各窗口起点均为当天 00:00，月/年再回退到当月 1 日、当年 1 月 1 日。
            ZonedDateTime day = current.toLocalDate().atStartOfDay(zoneId);
            ZonedDateTime previousDay = day.minusDays(1);
            ZonedDateTime month = current.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
            ZonedDateTime previousMonth = month.minusMonths(1);
            ZonedDateTime year = current.withDayOfYear(1).toLocalDate().atStartOfDay(zoneId);
            ZonedDateTime previousYear = year.minusYears(1);
            int monthLength = current.toLocalDate().lengthOfMonth();
            return new PeriodWindow(
                    zoneId,
                    current.toInstant().toEpochMilli(),
                    day.toInstant().toEpochMilli(),
                    previousDay.toInstant().toEpochMilli(),
                    month.toInstant().toEpochMilli(),
                    previousMonth.toInstant().toEpochMilli(),
                    year.toInstant().toEpochMilli(),
                    previousYear.toInstant().toEpochMilli(),
                    monthLength
            );
        }
    }
}
