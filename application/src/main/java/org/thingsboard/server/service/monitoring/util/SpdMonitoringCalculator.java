package org.thingsboard.server.service.monitoring.util;

import java.util.ArrayList;
import java.util.List;

/** 监测汇总使用的纯计算：状态设备数、数值范围与累计计数器增量。 */
public final class SpdMonitoringCalculator {
    private SpdMonitoringCalculator() { }

    /** 参与设备数、正常设备数和异常设备数。 */
    public record StatusCount(int participating, int normal, int abnormal) { }
    /** 仅保留最大和最小值；无有效值时均为 null。 */
    public record Range(Double maximum, Double minimum) { }

    /**
     * 统计文本状态设备。
     *
     * <p>参与设备数按照“最新结果中存在状态键”计算，而不是只识别某几个中文状态，
     * 这样可以保证“有 spdStatus 参数的设备总数”不因未知文本或空白字符被漏掉。
     * 正常数只认规则链输出的“正常”，后端不把 0/1 转换成业务状态。</p>
     */
    public static StatusCount binaryStatus(List<List<String>> devices) {
        int participating = 0;
        int normal = 0;
        int abnormal = 0;
        for (List<String> device : devices) {
            boolean hasValue = false;
            boolean hasAbnormal = false;
            for (String value : device) {
                // 外层列表由查询结果中的键构造；只要元素存在，就代表该设备上报过这个键。
                if (value == null) {
                    continue;
                }
                hasValue = true;
                if (!"正常".equals(value.trim())) {
                    hasAbnormal = true;
                }
            }
            if (hasValue) {
                participating++;
                if (hasAbnormal) {
                    abnormal++;
                } else {
                    normal++;
                }
            }
        }
        return new StatusCount(participating, normal, abnormal);
    }

    /** 温度：至少一个有效通道才参与，任一通道达到 65℃ 即异常。 */
    public static StatusCount temperatureStatus(List<List<Double>> devices) {
        int participating = 0;
        int normal = 0;
        int abnormal = 0;
        for (List<Double> device : devices) {
            boolean hasValue = false;
            boolean hasAbnormal = false;
            for (Double value : device) {
                if (value == null) {
                    continue;
                }
                // NaN 仅作为“键存在但无法解析”的内部标记，仍然计入参数设备总数。
                hasValue = true;
                if (!Double.isFinite(value) || value >= 65D) {
                    hasAbnormal = true;
                }
            }
            if (hasValue) {
                participating++;
                if (hasAbnormal) {
                    abnormal++;
                } else {
                    normal++;
                }
            }
        }
        return new StatusCount(participating, normal, abnormal);
    }

    /** 劣化：有效 lifePercent 大于等于 60 为正常。 */
    public static StatusCount degradationStatus(List<Double> values) {
        int participating = 0;
        int normal = 0;
        int abnormal = 0;
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            // NaN 表示寿命键存在但值无法解析，计入总数且不计为正常。
            participating++;
            if (Double.isFinite(value) && value >= 60D) {
                normal++;
            } else {
                abnormal++;
            }
        }
        return new StatusCount(participating, normal, abnormal);
    }

    /** 数值窗口范围，不计算平均值。 */
    public static Range range(List<Double> values) {
        Double max = null;
        Double min = null;
        for (Double value : values) {
            if (value != null && Double.isFinite(value)) {
                max = max == null ? value : Math.max(max, value);
                min = min == null ? value : Math.min(min, value);
            }
        }
        return new Range(max, min);
    }

    /**
     * 计算累计计数器在统计窗口内的增量。
     *
     * <p>有窗口前基线时，从基线开始计算相邻差值；没有基线且窗口只有一条记录时，
     * 这条记录本身就是当前窗口唯一可用结果，直接采用它。没有基线但有多条记录时，
     * 第一条只作为窗口内基准，后续按差值累计。计数器回退按设备重启处理，当前值计入新段。</p>
     */
    public static long counterDelta(Double baseline, List<Double> samples) {
        List<Double> validSamples = new ArrayList<>();
        for (Double sample : samples) {
            if (validCounter(sample)) {
                validSamples.add(sample);
            }
        }
        if (validSamples.isEmpty()) {
            return 0;
        }
        if (!validCounter(baseline) && validSamples.size() == 1) {
            return Math.round(validSamples.get(0));
        }

        Double previous = validCounter(baseline) ? baseline : validSamples.get(0);
        long delta = 0;
        int firstSampleIndex = validCounter(baseline) ? 0 : 1;
        for (int index = firstSampleIndex; index < validSamples.size(); index++) {
            double sample = validSamples.get(index);
            delta += Math.round(sample >= previous ? sample - previous : sample);
            previous = sample;
        }
        return delta;
    }

    private static boolean validCounter(Double value) { return value != null && Double.isFinite(value) && value >= 0D; }
}
