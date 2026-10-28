/** 文件职责：监测和告警页面共用的日、周、月滚动时间窗口。 */
package org.thingsboard.server.service.query.util;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

/**
 * 按当前时刻向前回溯的监测周期。
 *
 * <p>该窗口与告警分析的自然日/月/年窗口不同；这里只表示页面日、周、月筛选，
 * 放到 query 公共包后由 monitoring 和 alarms 共同使用，避免业务包相互依赖。</p>
 */
public final class MonitoringPeriodWindow {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long WEEK_MILLIS = 7L * DAY_MILLIS;
    private static final long MONTH_MILLIS = 30L * DAY_MILLIS;

    private final long startTs;
    private final long endTs;

    private MonitoringPeriodWindow(long startTs, long endTs) {
        this.startTs = startTs;
        this.endTs = endTs;
    }

    /** 根据 day、week、month 创建滚动窗口。 */
    public static MonitoringPeriodWindow from(String period) throws ThingsboardException {
        long endTs = System.currentTimeMillis();
        long duration;
        if ("day".equalsIgnoreCase(period)) {
            duration = DAY_MILLIS;
        } else if ("week".equalsIgnoreCase(period)) {
            duration = WEEK_MILLIS;
        } else if ("month".equalsIgnoreCase(period)) {
            duration = MONTH_MILLIS;
        } else {
            throw new ThingsboardException(
                    "Unsupported monitoring period: " + period,
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return new MonitoringPeriodWindow(endTs - duration, endTs);
    }

    public long getStartTs() {
        return startTs;
    }

    public long getEndTs() {
        return endTs;
    }

    /** 判断时间戳是否落在左闭右开区间内。 */
    public boolean contains(long timestamp) {
        return timestamp >= startTs && timestamp < endTs;
    }
}
