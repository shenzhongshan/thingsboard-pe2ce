package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 地网监测所选周期内按告警类型汇总的数量。 */
@Getter
@RequiredArgsConstructor
public class GroundAlarmTypeTotalDTO {

    /** 告警类型；空类型统一为“未分类”。 */
    private final String type;
    /** 该告警类型在所选周期内的数量。 */
    private final int count;
}
