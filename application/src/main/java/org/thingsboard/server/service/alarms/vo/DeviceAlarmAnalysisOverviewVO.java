/** 文件职责：告警分析页右侧状态分布与设备覆盖率的统一返回模型。 */
package org.thingsboard.server.service.alarms.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 当前统计周期的告警状态和设备覆盖率聚合结果。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmAnalysisOverviewVO {

    /** ACTIVE_UNACK、ACTIVE_ACK、CLEARED_UNACK、CLEARED_ACK 四种状态统计。 */
    private final List<DeviceAlarmStatusTotalVO> statusDistribution;
    /** 设备总数、告警设备数和未清除告警设备数。 */
    private final DeviceAlarmDeviceCoverageVO deviceCoverage;
}
