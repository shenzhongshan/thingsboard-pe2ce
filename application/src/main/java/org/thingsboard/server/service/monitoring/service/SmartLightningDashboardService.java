/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummaryDTO;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummaryDTO;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 智能防雷看板页面级服务。
 *
 * <p>统一组织地网监测、SPD 监测和雷电流监测三个看板板块，
 * 控制器只通过本接口获取看板摘要数据。</p>
 */
public interface SmartLightningDashboardService {

    /** 返回地网监测板块的实时状态和指定周期统计。 */
    GroundMonitoringSummaryDTO getGroundSummary(SecurityUser user, String period) throws ThingsboardException;

    /** 返回 SPD 监测板块的实时状态和指定周期统计。 */
    SpdMonitoringSummaryDTO getSpdSummary(SecurityUser user, String period) throws ThingsboardException;

    /** 返回雷电流监测板块的实时状态和指定周期统计。 */
    LightningCurrentMonitoringSummaryDTO getLightningCurrentSummary(SecurityUser user, String period)
            throws ThingsboardException;
}
