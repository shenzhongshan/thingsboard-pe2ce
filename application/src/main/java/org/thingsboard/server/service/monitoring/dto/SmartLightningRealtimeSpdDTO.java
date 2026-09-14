/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** SPD 类设备最新状态与关键指标。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningRealtimeSpdDTO {

    private final int totalDevices;
    private final int onlineDevices;
    private final int offlineDevices;
    private final int alarmDeviceCount;
    private final int spdStatusNormalCount;
    private final int spdStatusDeviceCount;
    private final int groundStatusNormalCount;
    private final int groundStatusDeviceCount;
    private final int temperatureNormalCount;
    private final int temperatureDeviceCount;
    private final int lifeNormalCount;
    private final int lifeDeviceCount;
    /** 所有 SPD 设备最新 leakageA/B/C 中的最大绝对值，单位由规则链统一为 mA。 */
    private final Double maximumLeakage;
    private final String maximumLeakageDeviceName;
    private final String maximumLeakageParameter;
    private final Long maximumLeakageTime;
    private final String status;
}
