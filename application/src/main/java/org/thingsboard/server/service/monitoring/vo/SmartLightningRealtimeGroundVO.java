/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 接地电阻设备的最新状态与阻值范围。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningRealtimeGroundVO {

    private final int totalDevices;
    private final int onlineDevices;
    private final int offlineDevices;
    private final int alarmDeviceCount;
    /** 最新快照中存在 GndGridRes 键的设备数。 */
    private final int reportingDeviceCount;
    private final Double maximumResistance;
    private final String maximumResistanceDeviceName;
    private final Long maximumResistanceTime;
    private final Double minimumResistance;
    private final String minimumResistanceDeviceName;
    private final Long minimumResistanceTime;
    private final String status;
}
