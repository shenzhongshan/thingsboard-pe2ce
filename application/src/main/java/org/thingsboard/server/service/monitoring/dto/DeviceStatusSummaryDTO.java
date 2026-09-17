package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 顶部设备状态统计服务的传输对象。
 *
 * <p>normal 表示在线且没有未清除告警的设备数量；
 * abnormal 表示存在未清除告警或已经离线的设备数量。</p>
 */
@Getter
@RequiredArgsConstructor
public class DeviceStatusSummaryDTO {

    private final int normal;
    private final int abnormal;
}
