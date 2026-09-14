/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
    /** 统计周期内按告警类型汇总的数量，按数量降序排列。 */
    private final List<SpdAlarmTypeTotalDTO> alarmTypeTotals;
    /** 周期内最大漏电流（取绝对值）。 */
    private final Double maxLeakage;
    /** 出现最大漏电流的设备名称。 */
    private final String maxLeakageDeviceName;
    /** 最大漏电流对应的相位（A相/B相/C相），由规范遥测键最后一个字符判定。 */
    private final String maxLeakagePhase;
    /** 产生最大漏电流的规范遥测键，例如 leakageB；前端详情据此显示相位。 */
    private final String maxLeakageKey;
    /** 最大漏电流发生时间（毫秒时间戳）。 */
    private final Long maxLeakageTime;
    /** 周期内上报过漏电流遥测的设备数量。 */
    private final int leakageReportingDeviceCount;
    /** 接地状态为正常的设备数。 */
    private final int groundStatusNormalCount;
    /** 至少上报过接地状态的设备数。 */
    private final int groundStatusDeviceCount;
    /** SPD 状态为正常的设备数。 */
    private final int spdStatusNormalCount;
    /** 至少上报过 SPD 状态的设备数。 */
    private final int spdStatusDeviceCount;
    /** 温度未达到 65℃ 阈值的设备数。 */
    private final int temperatureNormalCount;
    /** 至少上报过温度的设备数。 */
    private final int temperatureDeviceCount;
    /** 寿命不低于 60% 的设备数。 */
    private final int degradationNormalCount;
    /** 至少上报过寿命的设备数。 */
    private final int degradationDeviceCount;
    /** 周期内 lightningCount 累计增量。 */
    private final long lightningCount;
    /** 周期温度最大值和最小值。 */
    private final SpdMetricExtrema temperatureExtrema;
    /** 周期漏电流最大值和最小值。 */
    private final SpdMetricExtrema leakageExtrema;
    /** 周期电压最大值和最小值。 */
    private final SpdMetricExtrema voltageExtrema;
}
