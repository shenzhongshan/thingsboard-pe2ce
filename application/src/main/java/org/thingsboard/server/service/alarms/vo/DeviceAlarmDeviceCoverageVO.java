/** 文件职责：告警分析页设备告警覆盖率的统计返回模型。 */
package org.thingsboard.server.service.alarms.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 当前用户可见防雷设备在指定周期内的告警覆盖情况。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmDeviceCoverageVO {

    /** 当前用户可见防雷设备总数。 */
    private final long totalDevices;
    /** 当前周期内至少出现一条告警的设备数，按设备 ID 去重。 */
    private final long alarmedDevices;
    /** 当前周期内仍存在未清除告警的设备数，按设备 ID 去重。 */
    private final long activeAlarmDevices;
}
