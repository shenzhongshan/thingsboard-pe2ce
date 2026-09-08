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

    private final UUID assetId;
    private final String assetName;
    private final long alarmCount;
    private final long handledCount;
    private final double handledRate;
}
