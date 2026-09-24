/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 雷电流设备的最新累计次数和最近峰值。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningRealtimeCurrentVO {

    private final int totalDevices;
    private final int onlineDevices;
    private final int offlineDevices;
    private final int alarmDeviceCount;
    /** 各设备最新 SurgeCnt 累计值之和。 */
    private final long totalSurgeCount;
    /** 所有设备中时间戳最新的一条 PeakValue。 */
    private final Double latestPeakCurrent;
    private final String latestPeakDeviceName;
    private final String latestPeakPolarity;
    private final Long latestPeakTime;
    private final String peakCurrentUnit;
    private final String status;
}
