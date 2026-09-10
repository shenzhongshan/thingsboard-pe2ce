/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** 一个末级资产及其下属设备在统计周期内的告警数量和处理率。 */
@Getter
@RequiredArgsConstructor
public class SmartLightningLeafAssetAlarmCountDTO {

    /** 末级资产实体 ID。 */
    private final UUID assetId;
    /** 末级资产名称。 */
    private final String assetName;
    /** 周期内该资产下属设备的告警总数（按触发时间 startTs 统计）。 */
    private final long alarmCount;
    /** 其中已清除（已处理）的告警数量。 */
    private final long handledCount;
    /** 处理率百分比（已清除数 / 总数 × 100），保留两位小数。 */
    private final double handledRate;
}
