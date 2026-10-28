package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 地网阻值最值及其来源信息，是 GroundMonitoringSummaryVO 的 HTTP 响应片段。 */
@Getter
@RequiredArgsConstructor
public class GroundResistanceExtremaVO {

    /** 最大阻值。 */
    private final Double maximum;
    /** 最大阻值对应的设备名称。 */
    private final String maximumDeviceName;
    /** 最大阻值对应的遥测参数键。 */
    private final String maximumParameter;
    /** 最大阻值发生时间。 */
    private final Long maximumTime;
    /** 最小阻值。 */
    private final Double minimum;
    /** 最小阻值对应的设备名称。 */
    private final String minimumDeviceName;
    /** 最小阻值对应的遥测参数键。 */
    private final String minimumParameter;
    /** 最小阻值发生时间。 */
    private final Long minimumTime;
}
