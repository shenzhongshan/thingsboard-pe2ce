/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据监测页实时分析聚合响应。
 *
 * <p>五个板块使用同一资产范围和同一次最新遥测快照；不包含日、周、月历史统计。</p>
 */
@Getter
@RequiredArgsConstructor
public class SmartLightningRealtimeAnalysisVO {

    private final SmartLightningRealtimeOverallVO overall;
    private final SmartLightningRealtimeSpdVO spd;
    private final SmartLightningRealtimeGroundVO ground;
    private final SmartLightningRealtimeCurrentVO lightningCurrent;
    private final SmartLightningRealtimeArcFaultVO arcFault;
}
