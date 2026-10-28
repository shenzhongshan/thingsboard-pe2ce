/** 文件职责：告警分析周期统计实现，并复用同一次告警查询生成日/月/年趋势数据。 */
package org.thingsboard.server.service.alarms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.alarms.dto.DeviceAlarmAnalysisOverviewDTO;
import org.thingsboard.server.service.alarms.bo.AlarmDimensionCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmDeviceCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmOverviewAggregateBO;
import org.thingsboard.server.service.alarms.bo.AlarmPeriodCountBO;
import org.thingsboard.server.service.alarms.bo.AlarmTrendBucketBO;
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
import org.thingsboard.server.service.query.VisibleDeviceContextService;
import org.thingsboard.server.service.query.bo.VisibleDeviceContextBO;
import org.thingsboard.server.service.query.VisibleAssetTreeService;
import org.thingsboard.server.service.query.util.MonitoringPeriodWindow;
import org.thingsboard.server.service.query.util.VisibleAssetTreeIndex;
import org.thingsboard.server.service.query.util.CountValueSupport;
import org.thingsboard.server.service.query.util.ServiceQueryDefaults;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 智能防雷设备告警分析周期统计实现；按告警 startTs（触发时间）计数。
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

    /** 智能防雷设备 SERVER_SCOPE.productCategory 的约定值，分析和汇总统一使用该筛选值。 */
    private static final String LIGHTNING_PRODUCT_CATEGORY = "lightning";

    /** 提供当前用户可见且符合防雷分类的设备上下文。 */
    private final VisibleDeviceContextService deviceContextService;
    /** 提供平台告警分页查询；重点告警使用已授权设备 ID 范围查询。 */
    private final DeviceAlarmQueryService alarmQueryService;
    /** 读取按当前用户权限裁剪的资产树，用于末级资产归属和设备路径展示。 */
    private final VisibleAssetTreeService monitoringTreeService;
    /** 在数据库侧聚合告警次数、趋势桶、状态和严重程度。 */
    private final AlarmStatisticsService alarmStatisticsService;

    /**
     * 统计当前用户可见防雷设备在日、月、年三个周期的告警次数及环比。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 日、月、年三个周期各自的当前次数、上一周期次数和变化百分比
     */
    @Override
    public DeviceAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        // 防雷分析的统计范围按用户权限和 productCategory=lightning 双重限定。
        Set<UUID> visibleDeviceIds = findVisibleLightningDeviceIds(user);
        AlarmPeriodWindow window = AlarmPeriodWindow.current(ZoneId.systemDefault());
        // counts 依次存放：今日、昨日、本月、上月、今年、去年的告警次数。
        long[] counts = new long[6];
        Map<UUID, FocusAlarmAccumulator> focusDevicesById = new LinkedHashMap<>();
        TrendBuckets trendBuckets = new TrendBuckets(window);
        if (!visibleDeviceIds.isEmpty()) {
            // 先从数据库一次取得六个自然周期计数，再写入固定顺序的数组槽位。
            AlarmPeriodCountBO periodCounts = alarmStatisticsService.countPeriodSummary(
                    user,
                    visibleDeviceIds,
                    window.getDayStart(),
                    window.getPreviousDayStart(),
                    window.getMonthStart(),
                    window.getPreviousMonthStart(),
                    window.getYearStart(),
                    window.getPreviousYearStart(),
                    window.getNow());
            copyPeriodCounts(periodCounts, counts);
            // 分别读取日、月、年趋势分桶，并在内存中补齐没有告警的时间点。
            loadTrendBuckets(user, visibleDeviceIds, window, trendBuckets);
            // 只有存在可见设备时才读取资产树，空租户无需执行额外的关系查询。
            VisibleAssetTreeIndex treeIndex = VisibleAssetTreeIndex.build(
                    monitoringTreeService.getVisibleAssetTree(user));
            Map<UUID, String> parentNodesByDevice = treeIndex.getParentNameByDevice();
            // 严重活动告警单独查询，按设备合并后生成重点告警设备列表。
            collectActiveCriticalAlarms(user, visibleDeviceIds, parentNodesByDevice, focusDevicesById);
        }
        // 将周期次数、趋势和重点设备合并为分析页面响应。
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
        // 由权限树索引识别末级资产及其直属设备，不自行拼接资产关系。
        VisibleAssetTreeIndex treeIndex = VisibleAssetTreeIndex.build(
                monitoringTreeService.getVisibleAssetTree(user));
        Map<UUID, ParentAssetCounter> counters = new LinkedHashMap<>();
        Map<UUID, UUID> deviceToParentAsset = treeIndex.getParentAssetByDevice();
        Set<UUID> visibleDeviceIds = findVisibleLightningDeviceIds(user);
        if (visibleDeviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 只保留已通过权限及 productCategory=lightning 筛选的设备，避免其他产品告警混入资产统计。
        Set<UUID> lightningParentAssetIds = new HashSet<>();
        Set<UUID> leafDeviceIds = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : deviceToParentAsset.entrySet()) {
            if (visibleDeviceIds.contains(entry.getKey())) {
                // 同时记录防雷设备及其直属父资产，后续计数只覆盖这些设备和资产。
                leafDeviceIds.add(entry.getKey());
                lightningParentAssetIds.add(entry.getValue());
            }
        }
        // 只为确实挂有当前用户可见防雷设备的末级资产建立计数器，避免输出无关资产。
        for (VisibleAssetTreeIndex.LeafAsset asset : treeIndex.getLeafAssetsById().values()) {
            if (lightningParentAssetIds.contains(asset.id())) {
                counters.put(asset.id(), new ParentAssetCounter(asset.id(), asset.name(), asset.path()));
            }
        }
        if (counters.isEmpty()) {
            return Collections.emptyList();
        }

        // 数据库按设备聚合后只返回计数；这里仅负责将设备计数归并到直属父资产。
        // 只查询末级资产直接关联的设备，避免将更深层子资产中的告警算入当前资产。
        List<AlarmDeviceCountBO> deviceCounts = alarmStatisticsService.countByDevice(
                user, leafDeviceIds, window.getStartTs(), window.getEndTs());
        for (AlarmDeviceCountBO deviceCount : deviceCounts) {
            UUID parentAssetId = deviceToParentAsset.get(deviceCount.getDeviceId());
            if (parentAssetId == null) {
                continue;
            }
            ParentAssetCounter counter = counters.get(parentAssetId);
            counter.alarmCount += deviceCount.getAlarmCount();
            counter.handledCount += deviceCount.getHandledCount();
        }

        // 先按父资产汇总告警条数和已处理数，再计算处理率并按页面优先级排序。
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

    /** 按告警触发时间统计当前用户可见防雷设备的告警类型排行。 */
    @Override
    public List<DeviceAlarmTypeTotalDTO> getTypeRanking(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        Set<UUID> visibleDeviceIds = findVisibleLightningDeviceIds(user);
        if (visibleDeviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 类型聚合由 JPA 在数据库完成，只把“类型 + 数量”返回到应用层。
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countByType(
                user, visibleDeviceIds, window.getStartTs(), window.getEndTs(), false);
        // 将内部聚合结果映射为接口 DTO，保持数据库统计口径不变。
        List<DeviceAlarmTypeTotalDTO> result = new ArrayList<>();
        for (AlarmDimensionCountBO row : rows) {
            result.add(new DeviceAlarmTypeTotalDTO(
                    row.getDimension(), CountValueSupport.toIntCount(row.getCount())));
        }
        return result;
    }

    /** 按告警触发时间统计当前用户可见防雷设备的严重程度分布。 */
    @Override
    public List<DeviceAlarmSeverityTotalDTO> getSeverityRanking(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        Set<UUID> visibleDeviceIds = findVisibleLightningDeviceIds(user);
        if (visibleDeviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 严重程度聚合由 JPA 在数据库完成；未知枚举由映射层安全跳过。
        List<AlarmDimensionCountBO> rows = alarmStatisticsService.countBySeverity(
                user, visibleDeviceIds, window.getStartTs(), window.getEndTs(), false);
        Map<AlarmSeverity, Integer> counts = new EnumMap<>(AlarmSeverity.class);
        // 将数据库维度名解析为平台严重等级；未知枚举跳过，不影响已知等级统计。
        for (AlarmDimensionCountBO row : rows) {
            try {
                AlarmSeverity severity = AlarmSeverity.valueOf(row.getDimension());
                counts.put(severity, counts.getOrDefault(severity, 0)
                        + CountValueSupport.toIntCount(row.getCount()));
            } catch (IllegalArgumentException exception) {
                // 数据库中存在未知严重程度时不阻断整个分析页面，保留已知枚举的统计。
            }
        }

        // 补全没有告警的严重等级，随后按告警数量降序输出。
        List<DeviceAlarmSeverityTotalDTO> result = new ArrayList<>();
        for (AlarmSeverity severity : AlarmSeverity.values()) {
            result.add(new DeviceAlarmSeverityTotalDTO(
                    severity.name(), counts.getOrDefault(severity, 0)));
        }
        result.sort((left, right) -> Integer.compare(right.getCount(), left.getCount()));
        return result;
    }

    /** 按触发时间统计防雷设备的四种告警状态，并按设备 ID 去重计算告警覆盖率。 */
    @Override
    public DeviceAlarmAnalysisOverviewDTO getOverview(SecurityUser user, String period)
            throws ThingsboardException {
        MonitoringPeriodWindow window = MonitoringPeriodWindow.from(period);
        Set<UUID> visibleDeviceIds = findVisibleLightningDeviceIds(user);
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
        // 按平台状态枚举逐项输出，数据库没有该状态时计数补 0。
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

    /** 查询当前用户可见且 productCategory=lightning 的设备 ID，供分析统计共用。 */
    private Set<UUID> findVisibleLightningDeviceIds(SecurityUser user) throws ThingsboardException {
        // 汇集共享权限和类别过滤后的设备 ID，供各类告警统计复用同一口径。
        Set<UUID> visibleDeviceIds = new HashSet<>();
        for (VisibleDeviceContextBO context : deviceContextService
                .findVisibleDeviceContexts(user, LIGHTNING_PRODUCT_CATEGORY)) {
            // 防御共享查询可能返回的空上下文或空设备 ID，不将无效值送入数据库查询。
            if (context != null && context.getDeviceId() != null) {
                visibleDeviceIds.add(context.getDeviceId());
            }
        }
        return visibleDeviceIds;
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
     * 使用 JPA 原生聚合直接读取趋势分桶，避免为趋势图加载完整告警对象。
     * bucketUnit 分别代表日趋势小时、月趋势日期和年趋势月份。
     */
    private void loadTrendBuckets(SecurityUser user, Set<UUID> deviceIds,
                                  AlarmPeriodWindow window, TrendBuckets trendBuckets) {
        String zoneId = window.getZoneId().getId();
        for (AlarmTrendBucketBO bucket : alarmStatisticsService.countTrendBuckets(
                user, deviceIds, window.getDayStart(), window.getNow(), "hour", zoneId)) {
            trendBuckets.addDayBucket(bucket.getBucketIndex(), bucket.getAlarmCount());
        }
        for (AlarmTrendBucketBO bucket : alarmStatisticsService.countTrendBuckets(
                user, deviceIds, window.getMonthStart(), window.getNow(), "day", zoneId)) {
            trendBuckets.addMonthBucket(bucket.getBucketIndex(), bucket.getAlarmCount());
        }
        for (AlarmTrendBucketBO bucket : alarmStatisticsService.countTrendBuckets(
                user, deviceIds, window.getYearStart(), window.getNow(), "month", zoneId)) {
            trendBuckets.addYearBucket(bucket.getBucketIndex(), bucket.getAlarmCount());
        }
    }

    /** 将数据库聚合结果写入现有分析响应使用的六个计数槽位。 */
    private void copyPeriodCounts(AlarmPeriodCountBO periodCounts, long[] counts) {
        counts[0] = periodCounts.getDayCurrentCount();
        counts[1] = periodCounts.getDayPreviousCount();
        counts[2] = periodCounts.getMonthCurrentCount();
        counts[3] = periodCounts.getMonthPreviousCount();
        counts[4] = periodCounts.getYearCurrentCount();
        counts[5] = periodCounts.getYearPreviousCount();
    }

    /**
     * 按已授权防雷设备 ID 查询当前仍未清除的严重告警。
     * 该查询不限制时间起点，避免长期未清除的严重告警被年度统计窗口截掉。
     * 查询结果按创建时间倒序逐页读取；同一设备的多条告警会在后续步骤合并成单个重点设备项。
     */
    private void collectActiveCriticalAlarms(SecurityUser user, Set<UUID> deviceIds,
                                             Map<UUID, String> parentNodesByDevice,
                                             Map<UUID, FocusAlarmAccumulator> focusDevicesById)
            throws ThingsboardException {
        // 重点告警定义为仍处于 ACTIVE 的告警，不纳入已经清除的历史记录。
        List<AlarmSearchStatus> activeStatuses = new ArrayList<>();
        activeStatuses.add(AlarmSearchStatus.ACTIVE);
        // 重点列表只展示 CRITICAL 级别；其他严重程度仍由常规分析统计接口统计。
        List<AlarmSeverity> criticalSeverity = new ArrayList<>();
        criticalSeverity.add(AlarmSeverity.CRITICAL);
        // 固定本次处理的当前时间，避免跨页查询期间持续时长的计算基准发生变化。
        long now = System.currentTimeMillis();
        for (int page = 0; ; page++) {
            // 每次只取一个固定大小的页面，并按创建时间从新到旧排序，避免加载所有重点告警。
            TimePageLink pageLink = new TimePageLink(
                    QUERY_PAGE_SIZE, page, null, SortOrder.BY_CREATED_TIME_DESC);
            // 额外设备范围来自上游权限和防雷分类过滤；空时间条件确保旧的未清除告警也会返回。
            AlarmQueryV2 query = new AlarmQueryV2(
                    null, pageLink, Collections.emptyList(), activeStatuses, criticalSeverity, null);
            List<AlarmInfo> alarms = alarmQueryService.findPageForDevices(user, query, deviceIds).getData();
            // 将本页告警即时并入按设备聚合器，当前页结束后即可释放明细列表。
            for (AlarmInfo alarm : alarms) {
                collectFocusAlarm(alarm, parentNodesByDevice, focusDevicesById, now);
            }
            // 返回条数少于页容量意味着已到最后一页；满页时继续查询下一页。
            if (alarms.size() < QUERY_PAGE_SIZE) {
                break;
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
        accumulator.addAlarmType(DeviceAlarmAggregationSupport.normalizeType(alarm.getType()));
        accumulator.updateDuration(Math.max(0L, now - alarm.getStartTs()));
    }

    /** 将重点设备按持续时间降序输出，持续时间相同再按设备名排序。 */
    private List<DeviceAlarmFocusDeviceDTO> buildFocusDevices(
            Map<UUID, FocusAlarmAccumulator> focusDevicesById) {
        List<FocusAlarmAccumulator> accumulators = new ArrayList<>(focusDevicesById.values());
        accumulators.sort((left, right) -> {
            int durationOrder = Long.compare(right.durationMs, left.durationMs);
            return durationOrder != 0 ? durationOrder : left.deviceName.compareTo(right.deviceName);
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

        /** 为设备追加尚未记录的告警类型，避免同一类型重复展示。 */
        private void addAlarmType(String alarmType) {
            if (!alarmTypes.contains(alarmType)) {
                alarmTypes.add(alarmType);
            }
        }

        /** 保存该设备多条活动告警中的最长持续时间。 */
        private void updateDuration(long duration) {
            durationMs = Math.max(durationMs, duration);
        }
    }

    /**
     * 接收数据库按 start_ts 聚合后的小时、日期和月份桶，并补齐页面所需的空桶。
     */
    private static final class TrendBuckets {
        private final long[] day = new long[24];
        private final long[] month;
        private final long[] year = new long[12];

        private TrendBuckets(AlarmPeriodWindow window) {
            this.month = new long[window.getMonthLength()];
        }

        /** 按小时索引累加日趋势数量，越界桶不进入响应。 */
        private void addDayBucket(int bucketIndex, long count) {
            if (bucketIndex >= 0 && bucketIndex < day.length) {
                day[bucketIndex] += count;
            }
        }

        /** 按日期索引累加月趋势数量，越界桶不进入响应。 */
        private void addMonthBucket(int bucketIndex, long count) {
            if (bucketIndex >= 0 && bucketIndex < month.length) {
                month[bucketIndex] += count;
            }
        }

        /** 按月份索引累加年趋势数量，越界桶不进入响应。 */
        private void addYearBucket(int bucketIndex, long count) {
            if (bucketIndex >= 0 && bucketIndex < year.length) {
                year[bucketIndex] += count;
            }
        }

        /** 按 0 至 23 点顺序将日趋势数组转换成完整 DTO 列表。 */
        private List<DeviceAlarmTrendPointDTO> dayTrend() {
            List<DeviceAlarmTrendPointDTO> points = new ArrayList<>();
            for (int index = 0; index < day.length; index++) {
                points.add(new DeviceAlarmTrendPointDTO(String.format("%02d:00", index), day[index]));
            }
            return points;
        }

        /** 按当月自然日顺序将月趋势数组转换成完整 DTO 列表。 */
        private List<DeviceAlarmTrendPointDTO> monthTrend() {
            List<DeviceAlarmTrendPointDTO> points = new ArrayList<>();
            for (int index = 0; index < month.length; index++) {
                points.add(new DeviceAlarmTrendPointDTO(String.format("%02d", index + 1), month[index]));
            }
            return points;
        }

        /** 按 1 至 12 月顺序将年趋势数组转换成完整 DTO 列表。 */
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
