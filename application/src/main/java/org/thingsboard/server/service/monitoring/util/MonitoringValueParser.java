/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.util;

/**
 * 监测模块共用的文本和数值解析工具。
 *
 * 设备属性和时序值都可能为空、带空格或不是合法数字，统一在这里处理，
 * 避免地网、SPD、雷电流三个实现类各自维护一份相同的解析逻辑。
 */
public final class MonitoringValueParser {

    private MonitoringValueParser() {
    }

    /** 将可为空文本安全转换为去除首尾空格的字符串。 */
    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** 将文本解析为有限 double；空值、非法值和 Infinity/NaN 都返回 null。 */
    public static Double parseFiniteDouble(String value) {
        String text = trimToEmpty(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(text);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
