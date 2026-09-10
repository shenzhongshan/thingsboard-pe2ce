package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 告警分析顶部日、月、年统计。统计范围已经在 service 中按用户权限过滤。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmAnalysisSummaryDTO {

    /** 今日告警次数及与昨日对比。 */
    private final SmartLightningAlarmPeriodSummaryDTO day;
    /** 本月告警次数及与上月对比。 */
    private final SmartLightningAlarmPeriodSummaryDTO month;
    /** 今年告警次数及与去年对比。 */
    private final SmartLightningAlarmPeriodSummaryDTO year;
}
