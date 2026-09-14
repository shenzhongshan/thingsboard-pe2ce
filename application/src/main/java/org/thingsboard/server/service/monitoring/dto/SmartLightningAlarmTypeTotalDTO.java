package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 按 ThingsBoard 告警类型聚合后的数量。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmTypeTotalDTO {

    /** 告警类型（alarm.type），空类型统一显示为“未分类”。 */
    private final String type;
    /** 该类型在调用方统计范围内的告警数量。 */
    private final int count;
}
