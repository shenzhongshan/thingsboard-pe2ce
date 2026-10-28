/** 文件职责：告警分析页确认/清除状态的统计返回模型。 */
package org.thingsboard.server.service.alarms.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 单个告警状态及其记录数量。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmStatusTotalDTO {

    /** ThingsBoard AlarmStatus 枚举名称。 */
    private final String status;
    /** 当前统计周期内该状态的告警记录数。 */
    private final long count;
}
