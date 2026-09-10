package org.thingsboard.server.service.monitoring.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmAnalysisSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SmartLightningAlarmPeriodSummaryDTO;
import org.thingsboard.server.service.monitoring.service.SmartLightningAlarmAnalysisService;
import org.thingsboard.server.service.monitoring.service.MonitoringAlarmQueryService;
import org.thingsboard.server.service.monitoring.service.SmartLightningDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
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
public class SmartLightningAlarmAnalysisServiceImpl implements SmartLightningAlarmAnalysisService {

    /** 单次告警或设备查询使用的最大记录数。 */
    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningDeviceService deviceService;
    private final MonitoringAlarmQueryService alarmQueryService;

    /**
     * 统计当前用户可见设备在日、月、年三个周期的告警次数及环比。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @return 日、月、年三个周期各自的当前次数、上一周期次数和变化百分比
     */
    @Override
    public SmartLightningAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        // 全部可见设备（不区分产品分类），与告警分析页“全体设备”的口径一致。
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        PeriodWindow window = PeriodWindow.current(ZoneId.systemDefault());
        // counts 依次存放：今日、昨日、本月、上月、今年、去年的告警次数。
        long[] counts = new long[6];
        if (!visibleDeviceIds.isEmpty()) {
            countAlarms(user, visibleDeviceIds, window, counts);
        }
        return new SmartLightningAlarmAnalysisSummaryDTO(
                createPeriodSummary(counts[0], counts[1]),
                createPeriodSummary(counts[2], counts[3]),
                createPeriodSummary(counts[4], counts[5])
        );
    }

    /**
     * 查询上一年度起至今的告警，再按 startTs 精确落入日/月/年窗口，避免按 createdTime 误统计。
     *
     * <p>原生告警查询的时间过滤基于 createdTime（入库时间），
     * 而业务统计要求按触发时间 startTs 分桶；因此查询窗口放宽到去年年初，
     * 拿到全部候选告警后再逐条判断 startTs 落在哪个周期。</p>
     */
    private void countAlarms(SecurityUser user, Set<UUID> deviceIds, PeriodWindow window,
                             long[] counts) throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                window.previousYearStart, window.now);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            // 只统计可见设备的告警，排除其他实体（资产、客户等）产生的告警。
            if (isVisibleLightningDeviceAlarm(alarm, deviceIds)) {
                addToPeriods(alarm.getStartTs(), window, counts);
            }
        }
    }

    /** 判断告警是否由当前用户可见的设备产生。 */
    private boolean isVisibleLightningDeviceAlarm(AlarmInfo alarm, Set<UUID> deviceIds) {
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

    /** 组装单个周期的统计 DTO：当前次数、上一周期次数和环比百分比。 */
    private SmartLightningAlarmPeriodSummaryDTO createPeriodSummary(long currentCount, long previousCount) {
        return new SmartLightningAlarmPeriodSummaryDTO(
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
        private final long now;
        private final long dayStart;
        private final long previousDayStart;
        private final long monthStart;
        private final long previousMonthStart;
        private final long yearStart;
        private final long previousYearStart;

        private PeriodWindow(long now, long dayStart, long previousDayStart, long monthStart,
                             long previousMonthStart, long yearStart, long previousYearStart) {
            this.now = now;
            this.dayStart = dayStart;
            this.previousDayStart = previousDayStart;
            this.monthStart = monthStart;
            this.previousMonthStart = previousMonthStart;
            this.yearStart = yearStart;
            this.previousYearStart = previousYearStart;
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
            return new PeriodWindow(
                    current.toInstant().toEpochMilli(),
                    day.toInstant().toEpochMilli(),
                    previousDay.toInstant().toEpochMilli(),
                    month.toInstant().toEpochMilli(),
                    previousMonth.toInstant().toEpochMilli(),
                    year.toInstant().toEpochMilli(),
                    previousYear.toInstant().toEpochMilli()
            );
        }
    }
}
