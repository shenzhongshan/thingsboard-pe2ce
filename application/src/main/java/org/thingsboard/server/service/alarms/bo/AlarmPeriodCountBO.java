/** 文件职责：告警自然周期计数的内部业务对象。 */
package org.thingsboard.server.service.alarms.bo;

/** 告警分析页汇总计数，字段已经由数据库聚合完成。 */
public final class AlarmPeriodCountBO {

    private final long dayCurrentCount;
    private final long dayPreviousCount;
    private final long monthCurrentCount;
    private final long monthPreviousCount;
    private final long yearCurrentCount;
    private final long yearPreviousCount;

    public AlarmPeriodCountBO(long dayCurrentCount, long dayPreviousCount,
                              long monthCurrentCount, long monthPreviousCount,
                              long yearCurrentCount, long yearPreviousCount) {
        this.dayCurrentCount = dayCurrentCount;
        this.dayPreviousCount = dayPreviousCount;
        this.monthCurrentCount = monthCurrentCount;
        this.monthPreviousCount = monthPreviousCount;
        this.yearCurrentCount = yearCurrentCount;
        this.yearPreviousCount = yearPreviousCount;
    }

    public long getDayCurrentCount() {
        return dayCurrentCount;
    }

    public long getDayPreviousCount() {
        return dayPreviousCount;
    }

    public long getMonthCurrentCount() {
        return monthCurrentCount;
    }

    public long getMonthPreviousCount() {
        return monthPreviousCount;
    }

    public long getYearCurrentCount() {
        return yearCurrentCount;
    }

    public long getYearPreviousCount() {
        return yearPreviousCount;
    }

    public static AlarmPeriodCountBO empty() {
        return new AlarmPeriodCountBO(0L, 0L, 0L, 0L, 0L, 0L);
    }
}