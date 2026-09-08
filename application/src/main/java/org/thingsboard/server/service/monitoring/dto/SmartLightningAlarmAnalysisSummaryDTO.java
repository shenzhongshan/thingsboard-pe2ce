package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 告警分析顶部日、月、年统计。统计范围已经在 service 中按用户权限过滤。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmAnalysisSummaryDTO {

    private final SmartLightningAlarmPeriodSummaryDTO day;
    private final SmartLightningAlarmPeriodSummaryDTO month;
    private final SmartLightningAlarmPeriodSummaryDTO year;
}
