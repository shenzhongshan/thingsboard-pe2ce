/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

/** 监测卡片共用的日、周、月时间窗口。窗口按当前时刻向前回溯，和地网模块保持一致。 */
@Getter
@RequiredArgsConstructor
public final class MonitoringPeriodWindow {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long WEEK_MILLIS = 7L * DAY_MILLIS;
    private static final long MONTH_MILLIS = 30L * DAY_MILLIS;

    private final long startTs;
    private final long endTs;

    /**
     * 根据周期参数创建时间窗口。
     *
     * @param period 统计周期，支持 day、week、month（不区分大小写）
     * @return 起止时间窗口：[当前时刻回溯 24 小时 / 7 天 / 30 天, 当前时刻]
     * @throws ThingsboardException 周期参数不在支持范围内时抛出
     */
    public static MonitoringPeriodWindow from(String period) throws ThingsboardException {
        long endTs = System.currentTimeMillis();
        long duration;
        if ("day".equalsIgnoreCase(period)) {
            duration = DAY_MILLIS;
        } else if ("week".equalsIgnoreCase(period)) {
            duration = WEEK_MILLIS;
        } else if ("month".equalsIgnoreCase(period)) {
            duration = MONTH_MILLIS;
        } else {
            throw new ThingsboardException(
                    "Unsupported monitoring period: " + period,
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return new MonitoringPeriodWindow(endTs - duration, endTs);
    }
}
