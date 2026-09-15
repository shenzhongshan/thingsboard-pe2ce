/** 文件职责：告警分析模块的严重程度聚合返回对象，仅承载接口数据。 */
package org.thingsboard.server.service.alarms.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 告警分析页按 ThingsBoard 严重程度聚合后的数量。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmSeverityTotalDTO {

    /** 严重程度代码（CRITICAL、MAJOR、MINOR、WARNING、INDETERMINATE）。 */
    private final String severity;
    /** 当前统计周期内该严重程度的告警记录数量。 */
    private final int count;
}
