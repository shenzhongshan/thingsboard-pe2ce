/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** SPD 监测卡片的聚合结果。null 表示没有任何设备上报该测点。 */
@Getter
@RequiredArgsConstructor
public class SpdMonitoringSummary {

    /** 可见 SPD 设备总数。 */
    private final int totalDevices;
    /** 在线设备数（active 属性为 true/1）。 */
    private final int onlineDevices;
    /** 离线设备数。 */
    private final int offlineDevices;
    /** 统计周期内 SPD 设备的告警数。 */
    private final int alarmCount;
    /** 周期内最大漏电流（取绝对值）。 */
    private final Double maxLeakage;
    /** 出现最大漏电流的设备名称。 */
    private final String maxLeakageDeviceName;
    /** 最大漏电流对应的相位（A相/B相/C相），按键名后缀或原始键 I1/I2/I3 判定。 */
    private final String maxLeakagePhase;
    /** 最大漏电流发生时间（毫秒时间戳）。 */
    private final Long maxLeakageTime;
    /** 周期内上报过漏电流遥测的设备数量。 */
    private final int leakageReportingDeviceCount;
}
