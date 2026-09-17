/** 文件职责：保存告警分析状态分布的内部聚合结果。 */
package org.thingsboard.server.service.alarms.bo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** 只保留状态计数和设备去重计数，不保存告警明细，也不作为 HTTP DTO 返回。 */
@Getter
@RequiredArgsConstructor
public class AlarmOverviewAggregateBO {

    private final Map<String, Long> statusCounts;
    private final long alarmedDeviceCount;
    private final long activeAlarmDeviceCountBO;

    /** 无告警记录时返回零值对象，避免调用方重复判空。 */
    public static AlarmOverviewAggregateBO empty() {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("ACTIVE_UNACK", 0L);
        statusCounts.put("ACTIVE_ACK", 0L);
        statusCounts.put("CLEARED_UNACK", 0L);
        statusCounts.put("CLEARED_ACK", 0L);
        return new AlarmOverviewAggregateBO(statusCounts, 0L, 0L);
    }
}
