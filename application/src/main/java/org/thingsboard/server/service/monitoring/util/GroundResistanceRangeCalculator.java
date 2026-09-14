package org.thingsboard.server.service.monitoring.util;

import java.util.List;

/** 聚合所选时间窗口内所有有效地网阻值的最大值和最小值。 */
public final class GroundResistanceRangeCalculator {

    private GroundResistanceRangeCalculator() {
    }

    /** 同一统计窗口内的阻值范围；没有有效遥测时两个值均为空。 */
    public record Range(Double maximum, Double minimum) {
    }

    /** 将已完成数值校验的阻值汇总为一个范围。 */
    public static Range calculate(List<Double> values) {
        Double maximum = null;
        Double minimum = null;
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            maximum = maximum == null ? value : Math.max(maximum, value);
            minimum = minimum == null ? value : Math.min(minimum, value);
        }
        return new Range(maximum, minimum);
    }
}
