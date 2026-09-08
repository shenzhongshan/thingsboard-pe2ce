package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 智能防雷看板当前未清除告警统计。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmSummaryDTO {

    private final int activeCount;
    private final List<SmartLightningAlarmTypeTotalDTO> typeTotals;
}
