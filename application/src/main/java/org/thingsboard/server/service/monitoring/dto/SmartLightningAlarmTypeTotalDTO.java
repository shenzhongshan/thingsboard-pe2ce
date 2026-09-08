package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 按 ThingsBoard 告警类型聚合后的数量。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningAlarmTypeTotalDTO {

    private final String type;
    private final int count;
}
