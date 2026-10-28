package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 顶部设备状态统计接口的 HTTP 响应。
 *
 * <p>normal 表示在线且没有未清除告警的设备数量；
 * abnormal 表示存在未清除告警或已经离线的设备数量。</p>
 */
@Getter
@RequiredArgsConstructor
public class DeviceStatusSummaryVO {

    private final int normal;
    private final int abnormal;
}
