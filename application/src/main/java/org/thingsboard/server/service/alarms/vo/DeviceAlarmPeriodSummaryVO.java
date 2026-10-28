package org.thingsboard.server.service.alarms.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 一个统计周期内的告警触发次数，以及上一对比周期的次数。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmPeriodSummaryVO {

    /** 当前周期（今日/本月/今年）的告警次数。 */
    private final long currentCount;
    /** 上一对比周期（昨日/上月/去年）的告警次数。 */
    private final long previousCount;
    /** 环比变化百分比，保留两位小数。 */
    private final double changePercent;
}
