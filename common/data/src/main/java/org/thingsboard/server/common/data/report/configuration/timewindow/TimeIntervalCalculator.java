/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.common.data.report.configuration.timewindow;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import static java.time.LocalTime.MIN;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration;
import org.thingsboard.server.common.data.report.configuration.timewindow.FixedTimeWindow;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

public class TimeIntervalCalculator {
    public static TimeRange getTimeRange(TimeWindowConfiguration timeWindowConf, String timezone) {
        History historyConf = timeWindowConf.getHistory();
        return switch (historyConf.getHistoryType()) {
            case 0 -> {
                ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
                ZonedDateTime now = ZonedDateTime.now(zoneId);
                long currentTimeMillis = now.toInstant().toEpochMilli();
                yield new TimeRange(currentTimeMillis - historyConf.getTimewindowMs(), currentTimeMillis);
            }
            case 1 -> {
                FixedTimeWindow fixedTimeWindow = historyConf.getFixedTimewindow();
                yield new TimeRange(fixedTimeWindow.getStartTimeMs(), historyConf.getFixedTimewindow().getEndTimeMs());
            }
            case 2 -> TimeIntervalCalculator.getQuickTimeRange(historyConf.getQuickInterval(), timezone);
            case 3 -> new TimeRange(0L, 0L);
            default -> throw new IllegalArgumentException("Unknown history type: " + historyConf.getHistoryType());
        };
    }

    public static TimeRange getComparisonTimeRange(TimeRange timeWindow, TimeWindowConfiguration timeWindowConf, String timezone, ComparisonDuration timeUnit, Long customIntervalValue) {
        History historyConf = timeWindowConf.getHistory();
        if (ComparisonDuration.previousInterval.equals((Object)timeUnit)) {
            if (historyConf.getHistoryType() == 2) {
                return TimeIntervalCalculator.getComparisonQuickTimeRange(timeWindow, historyConf.getQuickInterval(), timezone);
            }
            long timeInterval = timeWindow.endTs - timeWindow.startTs;
            return new TimeRange(timeWindow.startTs - timeInterval, timeWindow.startTs);
        }
        if (ComparisonDuration.customInterval.equals((Object)timeUnit)) {
            if (customIntervalValue != null && customIntervalValue > 0L) {
                long timeInterval = timeWindow.endTs - timeWindow.startTs;
                long endTimeMs = timeWindow.endTs - customIntervalValue;
                long startTimeMs = endTimeMs - timeInterval;
                return new TimeRange(startTimeMs, endTimeMs);
            }
            return new TimeRange(timeWindow.startTs, timeWindow.endTs);
        }
        long timeInterval = timeWindow.endTs - timeWindow.startTs;
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        Instant endTimeInstant = Instant.ofEpochMilli(timeWindow.endTs);
        ZonedDateTime endDate = endTimeInstant.atZone(zoneId);
        if (ComparisonDuration.days.equals((Object)timeUnit)) {
            endDate = endDate.minusDays(1L);
        } else if (ComparisonDuration.weeks.equals((Object)timeUnit)) {
            endDate = endDate.minusWeeks(1L);
        } else if (ComparisonDuration.months.equals((Object)timeUnit)) {
            endDate = endDate.minusMonths(1L);
        } else if (ComparisonDuration.years.equals((Object)timeUnit)) {
            endDate = endDate.minusYears(1L);
        }
        long endTimeMs = endDate.toInstant().toEpochMilli();
        long startTimeMs = endTimeMs - timeInterval;
        return new TimeRange(startTimeMs, endTimeMs);
    }

    private static TimeRange getQuickTimeRange(QuickTimeInterval interval, String timezone) {
        ZonedDateTime start = null;
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime end = (switch (interval) {
            case YESTERDAY -> {
                start = now.minusDays(1L).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(1L).minusNanos(1L);
            }
            case DAY_BEFORE_YESTERDAY -> {
                start = now.minusDays(2L).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(1L).minusNanos(1L);
            }
            case THIS_DAY_LAST_WEEK -> {
                start = now.minusWeeks(1L).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(1L).minusNanos(1L);
            }
            case PREVIOUS_WEEK -> {
                start = now.minusWeeks(1L).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(7L).minusNanos(1L);
            }
            case PREVIOUS_WEEK_ISO -> {
                start = now.minusWeeks(1L).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(7L).minusNanos(1L);
            }
            case PREVIOUS_MONTH -> {
                start = now.minusMonths(1L).withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
                yield start.plusMonths(1L).minusNanos(1L);
            }
            case PREVIOUS_QUARTER -> {
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int prevQuarter = currentQuarter - 1;
                int startMonth = (prevQuarter - 1) * 3 + 1;
                ZonedDateTime refNow = now;
                if (startMonth < 1) {
                    startMonth += 12;
                    refNow = now.minusYears(1L);
                }
                start = ZonedDateTime.of(LocalDate.of(refNow.getYear(), startMonth, 1), MIN, zoneId);
                yield start.plusMonths(3L).minusNanos(1L);
            }
            case PREVIOUS_HALF_YEAR -> {
                start = now.getMonthValue() <= 6 ? ZonedDateTime.of(LocalDate.of(now.getYear() - 1, 7, 1), MIN, zoneId) : ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), MIN, zoneId);
                yield start.plusMonths(6L).minusNanos(1L);
            }
            case PREVIOUS_YEAR -> {
                start = ZonedDateTime.of(LocalDate.of(now.getYear() - 1, 1, 1), MIN, zoneId);
                yield start.plusYears(1L).minusNanos(1L);
            }
            case CURRENT_HOUR -> {
                start = now.truncatedTo(ChronoUnit.HOURS);
                yield start.plusHours(1L).minusNanos(1L);
            }
            case CURRENT_DAY -> {
                start = now.toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(1L).minusNanos(1L);
            }
            case CURRENT_DAY_SO_FAR -> {
                start = now.toLocalDate().atStartOfDay(zoneId);
                yield now;
            }
            case CURRENT_WEEK -> {
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(7L).minusNanos(1L);
            }
            case CURRENT_WEEK_ISO -> {
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zoneId);
                yield start.plusDays(7L).minusNanos(1L);
            }
            case CURRENT_WEEK_SO_FAR -> {
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(zoneId);
                yield now;
            }
            case CURRENT_WEEK_ISO_SO_FAR -> {
                start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(zoneId);
                yield now;
            }
            case CURRENT_MONTH -> {
                start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
                yield start.plusMonths(1L).minusNanos(1L);
            }
            case CURRENT_MONTH_SO_FAR -> {
                start = now.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
                yield now;
            }
            case CURRENT_QUARTER -> {
                int thisQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int quarterStartMonth = (thisQuarter - 1) * 3 + 1;
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), quarterStartMonth, 1), MIN, zoneId);
                yield start.plusMonths(3L).minusNanos(1L);
            }
            case CURRENT_QUARTER_SO_FAR -> {
                int thisQuarter = (now.getMonthValue() - 1) / 3 + 1;
                int quarterStartMonth = (thisQuarter - 1) * 3 + 1;
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), quarterStartMonth, 1), MIN, zoneId);
                yield now;
            }
            case CURRENT_HALF_YEAR -> {
                start = now.getMonthValue() <= 6 ? ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), MIN, zoneId) : ZonedDateTime.of(LocalDate.of(now.getYear(), 7, 1), MIN, zoneId);
                yield start.plusMonths(6L).minusNanos(1L);
            }
            case CURRENT_HALF_YEAR_SO_FAR -> {
                start = now.getMonthValue() <= 6 ? ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), MIN, zoneId) : ZonedDateTime.of(LocalDate.of(now.getYear(), 7, 1), MIN, zoneId);
                yield now;
            }
            case CURRENT_YEAR -> {
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), MIN, zoneId);
                yield start.plusYears(1L).minusNanos(1L);
            }
            case CURRENT_YEAR_SO_FAR -> {
                start = ZonedDateTime.of(LocalDate.of(now.getYear(), 1, 1), MIN, zoneId);
                yield now;
            }
            default -> throw new IllegalArgumentException("Unsupported interval: " + String.valueOf((Object)interval));
        });
        return new TimeRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
    }

    public static TimeRange getAggTimeRange(TimeRange timeWindow, Interval interval, Aggregation aggregation, ZoneId zoneId, long ts) {
        long endIntervalTs;
        long startIntervalTs;
        if (Aggregation.NONE.equals((Object)aggregation)) {
            return new TimeRange(ts, ts);
        }
        if (IntervalType.MILLISECONDS.equals((Object)interval.getIntervalType())) {
            long startTs = timeWindow.startTs;
            startIntervalTs = startTs + (long)(Math.floor((double)(ts - startTs) / (double)interval.getInterval()) * (double)interval.getInterval());
            endIntervalTs = startIntervalTs + interval.getInterval();
        } else {
            Instant instant = Instant.ofEpochMilli(ts);
            ZonedDateTime time = instant.atZone(zoneId);
            ZonedDateTime startInterval = TimeIntervalCalculator.startIntervalDate(time, interval.getIntervalType());
            ZonedDateTime endInterval = TimeIntervalCalculator.endIntervalDateFromStart(startInterval, interval.getIntervalType()).plus(Duration.ofMillis(1L));
            Instant startInstant = Instant.ofEpochMilli(timeWindow.startTs);
            ZonedDateTime start = startInstant.atZone(zoneId);
            if (start.isAfter(startInterval)) {
                startInterval = start;
            }
            startIntervalTs = startInterval.toInstant().toEpochMilli();
            endIntervalTs = endInterval.toInstant().toEpochMilli();
        }
        endIntervalTs = Math.min(endIntervalTs, timeWindow.endTs);
        return new TimeRange(startIntervalTs, endIntervalTs);
    }

    private static ZonedDateTime startIntervalDate(ZonedDateTime current, IntervalType intervalType) {
        switch (intervalType) {
            case WEEK: {
                return current.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(current.getZone());
            }
            case WEEK_ISO: {
                return current.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(current.getZone());
            }
            case MONTH: {
                return current.withDayOfMonth(1).toLocalDate().atStartOfDay(current.getZone());
            }
            case QUARTER: {
                int thisQuarter = (current.getMonthValue() - 1) / 3 + 1;
                int quarterStartMonth = (thisQuarter - 1) * 3 + 1;
                return ZonedDateTime.of(LocalDate.of(current.getYear(), quarterStartMonth, 1), MIN, current.getZone());
            }
            case MILLISECONDS: {
                return current;
            }
        }
        throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)intervalType));
    }

    private static ZonedDateTime endIntervalDateFromStart(ZonedDateTime start, IntervalType intervalType) {
        switch (intervalType) {
            case WEEK: 
            case WEEK_ISO: {
                return start.plusDays(7L).minusNanos(1L);
            }
            case MONTH: {
                return start.plusMonths(1L).minusNanos(1L);
            }
            case QUARTER: {
                return start.plusMonths(3L).minusNanos(1L);
            }
            case MILLISECONDS: {
                return start;
            }
        }
        throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)intervalType));
    }

    private static TimeRange getComparisonQuickTimeRange(TimeRange timeWindow, QuickTimeInterval interval, String timezone) {
        ZonedDateTime start = null;
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        Instant startTimeInstant = Instant.ofEpochMilli(timeWindow.startTs);
        ZonedDateTime startDate = startTimeInstant.atZone(zoneId);
        Instant endTimeInstant = Instant.ofEpochMilli(timeWindow.endTs);
        ZonedDateTime endDate = endTimeInstant.atZone(zoneId);
        ZonedDateTime end = (switch (interval) {
            case YESTERDAY, DAY_BEFORE_YESTERDAY, CURRENT_DAY, CURRENT_DAY_SO_FAR -> {
                start = startDate.minusDays(1L).toLocalDate().atStartOfDay(startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_DAY_SO_FAR) {
                    yield endDate.minusDays(1L);
                }
                yield start.plusDays(1L).minusNanos(1L);
            }
            case THIS_DAY_LAST_WEEK -> {
                start = startDate.minusWeeks(1L).toLocalDate().atStartOfDay(startDate.getZone());
                yield start.plusDays(1L).minusNanos(1L);
            }
            case PREVIOUS_WEEK, CURRENT_WEEK, CURRENT_WEEK_SO_FAR -> {
                start = startDate.minusWeeks(1L).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay(startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_WEEK_SO_FAR) {
                    yield endDate.minusWeeks(1L);
                }
                yield start.plusDays(7L).minusNanos(1L);
            }
            case PREVIOUS_WEEK_ISO, CURRENT_WEEK_ISO, CURRENT_WEEK_ISO_SO_FAR -> {
                start = startDate.minusWeeks(1L).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay(startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR) {
                    yield endDate.minusWeeks(1L);
                }
                yield start.plusDays(7L).minusNanos(1L);
            }
            case PREVIOUS_MONTH, CURRENT_MONTH, CURRENT_MONTH_SO_FAR -> {
                start = startDate.minusMonths(1L).withDayOfMonth(1).toLocalDate().atStartOfDay(startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_MONTH_SO_FAR) {
                    yield endDate.minusMonths(1L);
                }
                yield start.plusMonths(1L).minusNanos(1L);
            }
            case PREVIOUS_QUARTER, CURRENT_QUARTER, CURRENT_QUARTER_SO_FAR -> {
                int currentQuarter = (startDate.getMonthValue() - 1) / 3 + 1;
                int prevQuarter = currentQuarter - 1;
                int startMonth = (prevQuarter - 1) * 3 + 1;
                ZonedDateTime refNow = startDate;
                if (startMonth < 1) {
                    startMonth += 12;
                    refNow = startDate.minusYears(1L);
                }
                start = ZonedDateTime.of(LocalDate.of(refNow.getYear(), startMonth, 1), MIN, startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_QUARTER_SO_FAR) {
                    yield endDate.minusMonths(3L);
                }
                yield start.plusMonths(3L).minusNanos(1L);
            }
            case PREVIOUS_HALF_YEAR, CURRENT_HALF_YEAR, CURRENT_HALF_YEAR_SO_FAR -> {
                start = startDate.getMonthValue() <= 6 ? ZonedDateTime.of(LocalDate.of(startDate.getYear() - 1, 7, 1), MIN, startDate.getZone()) : ZonedDateTime.of(LocalDate.of(startDate.getYear(), 1, 1), MIN, startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_HALF_YEAR_SO_FAR) {
                    yield endDate.minusMonths(6L);
                }
                yield start.plusMonths(6L).minusNanos(1L);
            }
            case PREVIOUS_YEAR, CURRENT_YEAR, CURRENT_YEAR_SO_FAR -> {
                start = ZonedDateTime.of(LocalDate.of(startDate.getYear() - 1, 1, 1), MIN, startDate.getZone());
                if (interval == QuickTimeInterval.CURRENT_YEAR_SO_FAR) {
                    yield endDate.minusYears(1L);
                }
                yield start.plusYears(1L).minusNanos(1L);
            }
            case CURRENT_HOUR -> {
                start = startDate.truncatedTo(ChronoUnit.HOURS).minusHours(1L);
                yield start.plusHours(1L).minusNanos(1L);
            }
            default -> throw new IllegalArgumentException("Unsupported interval: " + String.valueOf((Object)interval));
        });
        return new TimeRange(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
    }

    public static IntervalType getIntervalType(Interval interval) {
        return interval != null ? interval.getIntervalType() : null;
    }

    public static long getIntervalTs(Interval interval) {
        return interval != null ? interval.getInterval() : 0L;
    }

    public static class TimeRange {
        public final long startTs;
        public final long endTs;

        public TimeRange(long startTs, long endTs) {
            this.startTs = startTs;
            this.endTs = endTs;
        }
    }
}

