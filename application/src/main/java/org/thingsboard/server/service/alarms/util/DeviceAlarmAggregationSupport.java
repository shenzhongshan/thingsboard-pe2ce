/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.alarms.util;

/**
 * 设备告警聚合公共工具。
 *
 * <p>统一告警类型空值处理，供告警分析复用。</p>
 */
public final class DeviceAlarmAggregationSupport {

    private DeviceAlarmAggregationSupport() {
    }

    /** 将空告警类型转换为统一的展示文本。 */
    public static String normalizeType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未分类";
        }
        return value.trim();
    }

}
