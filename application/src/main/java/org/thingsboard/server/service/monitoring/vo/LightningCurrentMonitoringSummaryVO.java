/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 雷电流监测汇总接口的 HTTP 响应。 */
@Getter
@RequiredArgsConstructor
public class LightningCurrentMonitoringSummaryVO {

    /** 可见雷电流设备总数。 */
    private final int totalDevices;
    /** 在线设备数（最新 active 遥测为 true/1）。 */
    private final int onlineDevices;
    /** 离线设备数。 */
    private final int offlineDevices;
    /** 统计周期内雷电流设备的告警数。 */
    private final int alarmCount;
    /** 统计周期内按告警类型汇总的数量，按数量降序排列。 */
    private final List<AlarmTypeTotalVO> alarmTypeTotals;
    /** 各雷电流设备在所选周期内的 SurgeCnt 增量之和；单条记录直接采用该值。 */
    private final long eventCount;
    /** 周期内最大峰值电流；没有任何设备上报时为 null。 */
    private final Double maxPeakCurrent;
    /** 峰值电流单位，固定为 kA，与雷电流监测模块说明书保持一致。 */
    private final String peakCurrentUnit;
    /** 出现最大峰值的设备名称。 */
    private final String maxPeakDeviceName;
    /** 峰值发生时间（毫秒时间戳）。 */
    private final Long maxPeakTime;
    /** 峰值时刻最近的极性（正极性/负极性），无对应记录时为 null。 */
    private final String maxPeakPolarity;
    /** 周期内最小峰值电流；没有任何设备上报时为 null。 */
    private final Double minPeakCurrent;
    /** 出现最小峰值的设备名称。 */
    private final String minPeakDeviceName;
    /** 最小峰值发生时间（毫秒时间戳）。 */
    private final Long minPeakTime;
    /** 最小峰值时刻最近的极性（正极性/负极性），无对应记录时为 null。 */
    private final String minPeakPolarity;
    /** 周期内上报过峰值遥测的设备数量。 */
    private final int reportingDeviceCount;
}
