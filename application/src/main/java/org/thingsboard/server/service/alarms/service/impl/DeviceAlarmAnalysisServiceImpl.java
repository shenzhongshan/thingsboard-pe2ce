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
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmDeviceCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmDeviceCoverageDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmLeafAssetCountDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmPeriodSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmSeverityTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmStatusTotalDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTrendPointDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmFocusDeviceDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmTypeTotalDTO;
import org.thingsboard.server.service.alarms.service.DeviceAlarmAnalysisService;
import org.thingsboard.server.service.alarms.service.DeviceAlarmQueryService;
import org.thingsboard.server.service.alarms.service.AlarmStatisticsService;
import org.thingsboard.server.service.alarms.util.DeviceAlarmAggregationSupport;
import org.thingsboard.server.service.alarms.util.AlarmPeriodWindow;
import org.thingsboard.server.service.query.VisibleDeviceScopeService;
import org.thingsboard.server.service.query.VisibleAssetTreeService;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.query.util.VisibleAssetTreeIndex;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
    private static final int QUERY_PAGE_SIZE = ServiceQueryDefaults.ALARM_PAGE_SIZE;

    private final VisibleDeviceScopeService deviceService;
    private final DeviceAlarmQueryService alarmQueryService;
    private final VisibleAssetTreeService monitoringTreeService;
    private final AlarmStatisticsService alarmStatisticsService;

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
        AlarmPeriodWindow window = AlarmPeriodWindow.current(ZoneId.systemDefault());
        // counts 依次存放：今日、昨日、本月、上月、今年、去年的告警次数。
        long[] counts = new long[6];
        Map<UUID, FocusAlarmAccumulator> focusDevicesById = new LinkedHashMap<>();
        TrendBuckets trendBuckets = new TrendBuckets(window);
        if (!visibleDeviceIds.isEmpty()) {
            countAlarms(user, visibleDeviceIds, window, counts, trendBuckets);
            // 只有存在可见设备时才读取资产树，空租户无需执行额外的关系查询。
            VisibleAssetTreeIndex treeIndex = VisibleAssetTreeIndex.build(
                    monitoringTreeService.getVisibleAssetTree(user));
            Map<UUID, String> parentNodesByDevice = treeIndex.getParentNameByDevice();
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
        VisibleAssetTreeIndex treeIndex = VisibleAssetTreeIndex.build(
                monitoringTreeService.getVisibleAssetTree(user));
        Map<UUID, ParentAssetCounter> counters = new LinkedHashMap<>();
        Map<UUID, UUID> deviceToParentAsset = new HashMap<>(treeIndex.getParentAssetByDevice());
        for (VisibleAssetTreeIndex.LeafAsset asset : treeIndex.getLeafAssetsById().values()) {
            counters.put(asset.id(), new ParentAssetCounter(asset.id(), asset.name(), asset.path()));
        }
        if (counters.isEmpty()) {
            return Collections.emptyList();
        }

        // 数据库按设备聚合后只返回计数；这里仅负责将设备计数归并到直属父资产。
        Set<UUID> leafDeviceIds = deviceToParentAsset.keySet();
        List<AlarmDeviceCountBO> deviceCounts = alarmStatisticsService.countByDevice(
                user, leafDeviceIds, window.getStartTs(), window.getEndTs());
        for (AlarmDeviceCountBO deviceCount : deviceCounts) {
            UUID parentAssetId = deviceToParentAsset.get(deviceCount.getDeviceId());
            if (parentAssetId == null) {
                continue;
            }
            ParentAssetCounter counter = counters.get(parentAssetId);
            counter.alarmCount += CountValueSupport.toIntCount(deviceCount.getAlarmCount());
            counter.handledCount += CountValueSupport.toIntCount(deviceCount.getHandledCount());
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

        // 类型聚合由 JPA 在数据库完成，只把“类型 + 数量”返回到应用层。
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
                user, visibleDeviceIds, window.getStartTs(), window.getEndTs(), false);
        List<DeviceAlarmTypeTotalDTO> result = new ArrayList<>();
        for (AlarmDimensionCountBO row : rows) {
            result.add(new DeviceAlarmTypeTotalDTO(
                    row.getDimension(), CountValueSupport.toIntCount(row.getCount())));
        }
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

        // 严重程度聚合由 JPA 在数据库完成；未知枚举由映射层安全跳过。
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countBySeverity(
                user, visibleDeviceIds, window.getStartTs(), window.getEndTs(), false);
        Map<AlarmSeverity, Integer> counts = new EnumMap<>(AlarmSeverity.class);
        for (AlarmDimensionCountBO row : rows) {
            try {
                AlarmSeverity severity = AlarmSeverity.valueOf(row.getDimension());
                counts.put(severity, counts.getOrDefault(severity, 0)
                        + CountValueSupport.toIntCount(row.getCount()));
            } catch (IllegalArgumentException exception) {
                // 数据库中存在未知严重程度时不阻断整个分析页面，保留已知枚举的统计。
            }
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
        if (visibleDeviceIds.isEmpty() || user == null || user.getTenantId() == null) {
            return buildOverviewFromAggregate(visibleDeviceIds, AlarmOverviewAggregateBO.empty());
        }
        // 状态分布和设备覆盖率均由数据库一次聚合返回，避免把历史告警全部加载到 JVM。
        AlarmOverviewAggregateBO aggregate = alarmStatisticsService.getOverview(
                user, visibleDeviceIds, window.getStartTs(), window.getEndTs());
        return buildOverviewFromAggregate(visibleDeviceIds, aggregate);
    }

    /** 将 JPA 聚合结果转换为现有告警分析接口 DTO。 */
    private DeviceAlarmAnalysisOverviewDTO buildOverviewFromAggregate(
            Set<UUID> visibleDeviceIds, AlarmOverviewAggregateBO aggregate) {
        List<DeviceAlarmStatusTotalDTO> statusDistribution = new ArrayList<>();
        for (AlarmStatus status : AlarmStatus.values()) {
            long count = aggregate.getStatusCounts().getOrDefault(status.name(), 0L);
            statusDistribution.add(new DeviceAlarmStatusTotalDTO(status.name(), count));
        }
        DeviceAlarmDeviceCoverageDTO coverage = new DeviceAlarmDeviceCoverageDTO(
                visibleDeviceIds.size(),
                aggregate.getAlarmedDeviceCount(),
                aggregate.getActiveAlarmDeviceCountBO());
        return new DeviceAlarmAnalysisOverviewDTO(statusDistribution, coverage);
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
    private void countAlarms(SecurityUser user, Set<UUID> deviceIds, AlarmPeriodWindow window,
                             long[] counts, TrendBuckets trendBuckets) throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                window.getPreviousYearStart(), window.getNow());
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
        alarmQueryService.forEachPage(user, query, page -> {
            for (AlarmInfo alarm : page) {
                // 只统计可见设备的告警，排除其他实体（资产、客户等）产生的告警。
                if (isVisibleDeviceAlarm(alarm, deviceIds)) {
                    addToPeriods(alarm.getStartTs(), window, counts);
                    trendBuckets.add(alarm.getStartTs());
                }
            }
        });
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
        alarmQueryService.forEachPage(user, query, page -> {
            for (AlarmInfo alarm : page) {
                if (isVisibleDeviceAlarm(alarm, deviceIds)) {
                    collectFocusAlarm(alarm, parentNodesByDevice, focusDevicesById, now);
                }
            }
        });
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
        accumulator.addAlarmType(DeviceAlarmAggregationSupport.normalizeType(alarm.getType()));
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

    /** 判断告警是否由当前用户可见的设备产生。 */
    private boolean isVisibleDeviceAlarm(AlarmInfo alarm, Set<UUID> deviceIds) {
        EntityId originator = alarm.getOriginator();
        return originator != null
                && originator.getEntityType() == EntityType.DEVICE
                && deviceIds.contains(originator.getId());
    }

    /** 按触发时间把告警计入今日/昨日、本月/上月、今年/去年六个统计桶。 */
    private void addToPeriods(long triggerTime, AlarmPeriodWindow window, long[] counts) {
        // 今日：[今天0点, 当前时刻)
        if (triggerTime >= window.getDayStart() && triggerTime < window.getNow()) {
            counts[0]++;
        }
        // 昨日：[昨天0点, 今天0点)
        if (triggerTime >= window.getPreviousDayStart() && triggerTime < window.getDayStart()) {
            counts[1]++;
        }
        // 本月：[本月1日0点, 当前时刻)
        if (triggerTime >= window.getMonthStart() && triggerTime < window.getNow()) {
            counts[2]++;
        }
        // 上月：[上月1日0点, 本月1日0点)
        if (triggerTime >= window.getPreviousMonthStart() && triggerTime < window.getMonthStart()) {
            counts[3]++;
        }
        // 今年：[今年1月1日0点, 当前时刻)
        if (triggerTime >= window.getYearStart() && triggerTime < window.getNow()) {
            counts[4]++;
        }
        // 去年：[去年1月1日0点, 今年1月1日0点)
        if (triggerTime >= window.getPreviousYearStart() && triggerTime < window.getYearStart()) {
            counts[5]++;
        }
    }

    /**
     * 按自然周期把告警触发时间分配到小时、星期、日期和月份趋势数组。
     * 查询和权限过滤复用现有告警分析流程，不新增重复的告警查询接口。
     */
    private static final class TrendBuckets {
        private final AlarmPeriodWindow window;
        private final long[] day = new long[24];
        private final long[] month;
        private final long[] year = new long[12];

        private TrendBuckets(AlarmPeriodWindow window) {
            this.window = window;
            this.month = new long[window.getMonthLength()];
        }

        private void add(long triggerTime) {
            if (triggerTime >= window.getDayStart() && triggerTime < window.getNow()) {
                ZonedDateTime time = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(triggerTime), window.getZoneId());
                day[time.getHour()]++;
            }
            if (triggerTime >= window.getMonthStart() && triggerTime < window.getNow()) {
                ZonedDateTime time = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(triggerTime), window.getZoneId());
                month[time.getDayOfMonth() - 1]++;
            }
            if (triggerTime >= window.getYearStart() && triggerTime < window.getNow()) {
                ZonedDateTime time = ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(triggerTime), window.getZoneId());
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

}
