/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** SPD 监测汇总服务的传输对象；null 表示没有任何设备上报该测点。 */
@Getter
@RequiredArgsConstructor
public class SpdMonitoringSummaryDTO {

    /** 可见 SPD 设备总数。 */
    private final int totalDevices;
    /** 在线设备数（最新 active 遥测为 true/1）。 */
    private final int onlineDevices;
    /** 离线设备数。 */
    private final int offlineDevices;
    /** 统计周期内 SPD 设备的告警数。 */
    private final int alarmCount;
    /** 统计周期内按告警类型汇总的数量，按数量降序排列。 */
    private final List<AlarmTypeTotalDTO> alarmTypeTotals;
    /** 周期内最大漏电流（取绝对值）。 */
    private final Double maxLeakage;
    /** 出现最大漏电流的设备名称。 */
    private final String maxLeakageDeviceName;
    /** 最大漏电流对应的相位（A相/B相/C相），由规范遥测键最后一个字符判定。 */
    private final String maxLeakagePhase;
    /** 产生最大漏电流的规范遥测键，例如 LeakCurrB；前端详情据此显示相位。 */
    private final String maxLeakageKey;
    /** 最大漏电流发生时间（毫秒时间戳）。 */
    private final Long maxLeakageTime;
    /** 周期内上报过漏电流遥测的设备数量。 */
    private final int leakageReportingDeviceCount;
    /** 接地状态为正常的设备数。 */
    private final int groundStatusNormalCount;
    /** 至少上报过接地状态的设备数。 */
    private final int groundStatusDeviceCount;
    /** McbStatus 为空开正常状态的设备数。 */
    private final int breakerStatusNormalCount;
    /** 至少上报过 McbStatus 的设备数。 */
    private final int breakerStatusDeviceCount;
    /** 温度未达到 65℃ 阈值的设备数。 */
    private final int temperatureNormalCount;
    /** 至少上报过温度的设备数。 */
    private final int temperatureDeviceCount;
    /** 寿命不低于 20% 的设备数。 */
    private final int degradationNormalCount;
    /** 至少上报过寿命的设备数。 */
    private final int degradationDeviceCount;
    /** 周期内StrikeCnt已观测增量之和；任一设备无基线且仅有一条有效值时为null。 */
    private final Long lightningCount;
    /** 周期温度最大值和最小值。 */
    private final SpdMetricExtremaDTO temperatureExtrema;
    /** 周期漏电流最大值和最小值。 */
    private final SpdMetricExtremaDTO leakageExtrema;
    /** 周期电压最大值和最小值。 */
    private final SpdMetricExtremaDTO voltageExtrema;
}
