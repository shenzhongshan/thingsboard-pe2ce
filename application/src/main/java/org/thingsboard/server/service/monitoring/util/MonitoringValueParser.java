/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.thingsboard.server.service.monitoring.util;

import org.thingsboard.server.common.data.kv.TsKvEntry;

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

    /**
     * 解析设备 SERVER_SCOPE 中的 active 值。
     *
     * <p>平台和历史数据中可能同时存在 true/false 与 1/0 文本，所有监测页面统一按
     * 这一规则解析，避免不同页面出现在线数量不一致。</p>
     */
    public static boolean isActive(String value) {
        String text = trimToEmpty(value);
        return "true".equalsIgnoreCase(text) || "1".equals(text);
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

    /**
     * 判断最新遥测结果是否包含真实存储值。
     *
     * <p>ThingsBoard 批量调用 findLatest 时，会为未上报的键返回 value=null 的占位
     * TsKvEntry。占位对象本身不为 null，不能据此认定设备已经上报该参数；实际存储的
     * 空字符串仍属于有效上报，因此这里只排除 entry 或其原始值为 null 的情况。</p>
     */
    public static boolean hasStoredValue(TsKvEntry entry) {
        return entry != null && entry.getValueAsString() != null;
    }
}
