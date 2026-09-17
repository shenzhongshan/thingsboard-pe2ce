/** 文件职责：告警分析“重点设备”列表的返回对象，仅承载严重未清除告警的展示字段。 */
package org.thingsboard.server.service.alarms.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 一个设备可合并多条严重未清除告警，告警类型使用“/”连接。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmFocusDeviceVO {

    /** 设备名称。 */
    private final String deviceName;
    /** 设备直属父资产名称。 */
    private final String parentNodeName;
    /** 该设备当前严重未清除告警类型。 */
    private final String alarmType;
    /** 从最早一条严重未清除告警触发到当前时间的持续时长，单位毫秒。 */
    private final long durationMs;
}
