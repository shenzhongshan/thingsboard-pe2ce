/** 文件职责：告警分析趋势图的单个时间桶返回对象，仅承载标签和数量。 */
package org.thingsboard.server.service.alarms.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 告警趋势图的时间桶统计点。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmTrendPointDTO {

    /** 时间桶标签，例如小时、日期或月份。 */
    private final String label;
    /** 该时间桶内按告警触发时间统计的告警数量。 */
    private final long count;
}
