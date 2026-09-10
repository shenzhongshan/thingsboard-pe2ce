package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 智能防雷看板当前未清除告警统计。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmSummaryDTO {

    /** 当前未清除（ACTIVE）告警总数。 */
    private final int activeCount;
    /** 各告警类型的数量列表，数量降序排列。 */
    private final List<SmartLightningAlarmTypeTotalDTO> typeTotals;
}
