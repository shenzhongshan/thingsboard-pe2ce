/** 文件职责：告警分析使用的自然日、自然月和自然年时间窗口。 */
package org.thingsboard.server.service.alarms.util;

import lombok.Getter;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 告警分析的自然周期边界。
 *
 * <p>与监测页面的“向前回溯 N 天”窗口不同，告警分析按系统时区的自然日、月、年
 * 计算当前周期和上一周期，避免统计口径混用。</p>
 */
@Getter
public final class AlarmPeriodWindow {

    private final ZoneId zoneId;
    private final long now;
    private final long dayStart;
    private final long previousDayStart;
    private final long monthStart;
    private final long previousMonthStart;
    private final long yearStart;
    private final long previousYearStart;
    private final int monthLength;

    private AlarmPeriodWindow(ZoneId zoneId, long now, long dayStart, long previousDayStart,
                              long monthStart, long previousMonthStart, long yearStart,
                              long previousYearStart, int monthLength) {
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

    /** 以指定时区的当前时间构造自然周期边界。 */
    public static AlarmPeriodWindow current(ZoneId zoneId) {
        ZonedDateTime current = ZonedDateTime.now(zoneId);
        ZonedDateTime day = current.toLocalDate().atStartOfDay(zoneId);
        ZonedDateTime previousDay = day.minusDays(1);
        ZonedDateTime month = current.withDayOfMonth(1).toLocalDate().atStartOfDay(zoneId);
        ZonedDateTime previousMonth = month.minusMonths(1);
        ZonedDateTime year = current.withDayOfYear(1).toLocalDate().atStartOfDay(zoneId);
        ZonedDateTime previousYear = year.minusYears(1);
        return new AlarmPeriodWindow(
                zoneId,
                current.toInstant().toEpochMilli(),
                day.toInstant().toEpochMilli(),
                previousDay.toInstant().toEpochMilli(),
                month.toInstant().toEpochMilli(),
                previousMonth.toInstant().toEpochMilli(),
                year.toInstant().toEpochMilli(),
                previousYear.toInstant().toEpochMilli(),
                current.toLocalDate().lengthOfMonth());
    }
}
