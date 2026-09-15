/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.GroundMonitoringSummary;
import org.thingsboard.server.service.monitoring.dto.LightningCurrentMonitoringSummary;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummary;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 智能防雷看板页面级服务。
 *
 * <p>统一组织地网监测、SPD 监测和雷电流监测三个看板板块，
 * 控制器只通过本接口获取看板摘要数据。</p>
 */
public interface SmartLightningDashboardService {

    /** 返回地网监测板块的实时状态和指定周期统计。 */
    GroundMonitoringSummary getGroundSummary(SecurityUser user, String period) throws ThingsboardException;

    /** 返回 SPD 监测板块的实时状态和指定周期统计。 */
    SpdMonitoringSummary getSpdSummary(SecurityUser user, String period) throws ThingsboardException;

    /** 返回雷电流监测板块的实时状态和指定周期统计。 */
    LightningCurrentMonitoringSummary getLightningCurrentSummary(SecurityUser user, String period)
            throws ThingsboardException;
}

