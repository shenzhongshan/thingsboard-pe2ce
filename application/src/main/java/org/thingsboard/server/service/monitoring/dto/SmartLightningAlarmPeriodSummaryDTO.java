package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 一个统计周期内的告警触发次数，以及上一对比周期的次数。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmPeriodSummaryDTO {

    private final long currentCount;
    private final long previousCount;
    private final double changePercent;
}
