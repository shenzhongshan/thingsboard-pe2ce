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

/** 全体设备告警分析周期统计实现；按告警 startTs（触发时间）计数。 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class SmartLightningAlarmAnalysisServiceImpl implements SmartLightningAlarmAnalysisService {

    private static final int QUERY_PAGE_SIZE = 10_000;

    private final SmartLightningDeviceService deviceService;
    private final MonitoringAlarmQueryService alarmQueryService;

    @Override
    public SmartLightningAlarmAnalysisSummaryDTO getSummary(SecurityUser user) throws ThingsboardException {
        Set<UUID> visibleDeviceIds = deviceService.getVisibleDeviceIds(user);
        PeriodWindow window = PeriodWindow.current(ZoneId.systemDefault());
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

    /** 查询上一年度起至今的告警，再用 startTs 精确落入日/月/年窗口，避免按 createdTime 误统计。 */
    private void countAlarms(SecurityUser user, Set<UUID> deviceIds, PeriodWindow window,
                             long[] counts) throws ThingsboardException {
        TimePageLink pageLink = new TimePageLink(
                QUERY_PAGE_SIZE, 0, null, SortOrder.BY_CREATED_TIME_DESC,
                window.previousYearStart, window.now);
        AlarmQueryV2 query = new AlarmQueryV2(
                null, pageLink, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null);
        for (AlarmInfo alarm : alarmQueryService.findAll(user, query)) {
            if (isVisibleLightningDeviceAlarm(alarm, deviceIds)) {
                addToPeriods(alarm.getStartTs(), window, counts);
            }
        }
    }

    private boolean isVisibleLightningDeviceAlarm(AlarmInfo alarm, Set<UUID> deviceIds) {
        EntityId originator = alarm.getOriginator();
        return originator != null
                && originator.getEntityType() == EntityType.DEVICE
                && deviceIds.contains(originator.getId());
    }

    private void addToPeriods(long triggerTime, PeriodWindow window, long[] counts) {
        if (triggerTime >= window.dayStart && triggerTime < window.now) {
            counts[0]++;
        }
        if (triggerTime >= window.previousDayStart && triggerTime < window.dayStart) {
            counts[1]++;
        }
        if (triggerTime >= window.monthStart && triggerTime < window.now) {
            counts[2]++;
        }
        if (triggerTime >= window.previousMonthStart && triggerTime < window.monthStart) {
            counts[3]++;
        }
        if (triggerTime >= window.yearStart && triggerTime < window.now) {
            counts[4]++;
        }
        if (triggerTime >= window.previousYearStart && triggerTime < window.yearStart) {
            counts[5]++;
        }
    }

    private SmartLightningAlarmPeriodSummaryDTO createPeriodSummary(long currentCount, long previousCount) {
        return new SmartLightningAlarmPeriodSummaryDTO(
                currentCount,
                previousCount,
                calculateChangePercent(currentCount, previousCount)
        );
    }

    private double calculateChangePercent(long currentCount, long previousCount) {
        if (previousCount == 0) {
            return currentCount == 0 ? 0D : 100D;
        }
        double percent = (currentCount - previousCount) * 100D / previousCount;
        return Math.round(percent * 100D) / 100D;
    }

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

        private static PeriodWindow current(ZoneId zoneId) {
            ZonedDateTime current = ZonedDateTime.now(zoneId);
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
