/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SmartLightningRealtimeAnalysisDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

/** 数据监测页按资产范围查询最新设备状态与遥测指标的业务接口。 */
public interface SmartLightningMonitoringService {

    /**
     * 查询实时分析数据。
     *
     * @param user 当前登录用户，所有设备和资产均按其 READ 权限过滤
     * @param assetId 选中的资产 ID；为空时统计当前用户全部可见防雷设备
     * @return 同一设备范围下的整体、SPD、接地、雷电流和故障电弧快照
     */
    SmartLightningRealtimeAnalysisDTO getRealtimeAnalysis(SecurityUser user, UUID assetId)
            throws ThingsboardException;
}
