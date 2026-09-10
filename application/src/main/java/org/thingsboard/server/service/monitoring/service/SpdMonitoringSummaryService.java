/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.service;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.service.monitoring.dto.SpdMonitoringSummary;
import org.thingsboard.server.service.security.model.SecurityUser;

/** SPD 监测卡片的业务接口。 */
public interface SpdMonitoringSummaryService {

    /**
     * 查询当前用户可见 SPD 设备（functionCategory=spd_monitor）的完整摘要。
     *
     * @param user 当前登录用户，用于执行权限过滤
     * @param period 统计周期，支持 day、week、month
     * @return 设备数量、在线/离线数量、告警数、周期内最大漏电流及其设备/相位/时间
     * @throws ThingsboardException 周期参数非法或设备、告警、遥测查询失败时抛出
     */
    SpdMonitoringSummary getSummary(SecurityUser user, String period) throws ThingsboardException;
}
