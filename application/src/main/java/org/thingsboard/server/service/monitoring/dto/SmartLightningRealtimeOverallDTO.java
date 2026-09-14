/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 当前资产范围内全部防雷设备的实时状态概览。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningRealtimeOverallDTO {

    private final int totalDevices;
    private final int onlineDevices;
    private final int offlineDevices;
    /** 存在至少一条未清除告警的设备数，按设备 ID 去重。 */
    private final int alarmDeviceCount;
    /** 正常、需关注、异常或数据不足。 */
    private final String status;
}
