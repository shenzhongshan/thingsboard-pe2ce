package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 顶部设备状态统计接口的返回 DTO。
 *
 * <p>normal 表示在线且没有未清除告警的设备数量；
 * abnormal 表示至少存在一条未清除告警的设备数量。</p>
 */
@Getter
@RequiredArgsConstructor
public class DeviceStatusSummary {

    private final int normal;
    private final int abnormal;
}
