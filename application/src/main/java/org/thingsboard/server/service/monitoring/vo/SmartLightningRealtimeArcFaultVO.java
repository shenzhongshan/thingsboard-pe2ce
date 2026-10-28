/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 故障电弧设备的最新状态和累计动作次数。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningRealtimeArcFaultVO {

    private final int totalDevices;
    private final int onlineDevices;
    private final int offlineDevices;
    private final int alarmDeviceCount;
    private final int normalStatusCount;
    /** 最新快照中存在 DevStatus 键的设备数。 */
    private final int statusDeviceCount;
    /** 各设备最新 arcFaultCount 累计值之和。 */
    private final long totalArcFaultCount;
    /** 该类别全部规范遥测中最新一条的时间。 */
    private final Long latestReportTime;
    private final String status;
}
