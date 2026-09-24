/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.alarms.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** 一个设备直属父资产及其直接挂载设备在统计周期内的告警数量和处理率。 */
@Getter
@RequiredArgsConstructor
public class DeviceAlarmLeafAssetCountDTO {

    /** 设备直属父资产实体 ID。 */
    private final UUID assetId;
    /** 设备直属父资产名称。 */
    private final String assetName;
    /** 设备直属父资产从根资产到当前节点的完整路径，使用 / 分隔。 */
    private final String assetPath;
    /** 周期内该资产下属设备的告警总数（按触发时间 startTs 统计）。 */
    private final long alarmCount;
    /** 其中已清除（已处理）的告警数量。 */
    private final long handledCount;
    /** 处理率百分比（已清除数 / 总数 × 100），保留两位小数。 */
    private final double handledRate;
}
