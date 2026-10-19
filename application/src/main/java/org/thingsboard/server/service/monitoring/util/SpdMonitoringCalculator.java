package org.thingsboard.server.service.monitoring.util;

import java.util.List;
import java.util.Map;

/** 监测汇总使用的纯计算：状态设备数、数值范围与累计计数器增量。 */
public final class SpdMonitoringCalculator {
    /** SPD剩余寿命正常下限，20%本身仍计为正常。 */
    private static final double MINIMUM_NORMAL_LIFE_PERCENT = 20D;
    private SpdMonitoringCalculator() { }

    /** 参与综合状态判断的规范键；查询层还会按型号白名单裁剪，不要求每台设备都具备全部键。 */
    public static final List<String> PROTOCOL_STATUS_KEYS = List.of(
            "RemoteSig", "McbStatus", "GndStatus", "PhAStatus", "PhBStatus", "PhCStatus",
            "TripSW1", "TripSW2", "TripSW3", "TripSW4", "SPD_Status", "SPD_Alarm");

    /** 仅用于卡片提示的遥测诊断结果，不代表平台已创建告警，不用于增加告警设备数。 */
    public record ProtocolStatus(boolean reported, boolean abnormal, boolean needsAttention) { }

    /**
     * 判断单台设备已上报的协议状态；输入仅包含本型号支持的最新值，不读取历史旧键。
     * 二态状态0正常1异常；告警序号0正常、1至14异常；设备状态4正常、5关注、6至8异常。
     * 初始化、学习、事件和未知编码仅提示关注；缺失字段不当作正常或异常。
     * 退化原因和最差相不独立触发异常；温度及寿命数值阈值由原有统计方法保持处理。
     */
    public static ProtocolStatus protocolStatus(Map<String, String> values) {
        boolean reported = false;
        boolean abnormal = false;
        boolean needsAttention = false;
        for (String key : PROTOCOL_STATUS_KEYS) {
            String rawValue = values.get(key);
            if (rawValue == null) {
                continue;
            }
            reported = true;
            Double value = MonitoringValueParser.parseFiniteDouble(rawValue);
            if (value == null || value != Math.rint(value)) {
                needsAttention = true;
                continue;
            }
            if ("SPD_Status".equals(key)) {
                if (value >= 6D && value <= 8D) {
                    abnormal = true;
                } else if (value != 4D) {
                    needsAttention = true;
                }
            } else if ("SPD_Alarm".equals(key)) {
                if (value >= 1D && value <= 14D) {
                    abnormal = true;
                } else if (value != 0D) {
                    needsAttention = true;
                }
            } else if (value == 1D) {
                abnormal = true;
            } else if (value != 0D) {
                needsAttention = true;
            }
        }
        return new ProtocolStatus(reported, abnormal, needsAttention);
    }

    /** 仅识别当前规范温度键Temp、Temp1至Temp4，排除旧版温度别名。 */
    public static boolean isTemperatureKey(String key) {
        return key != null && List.of("Temp", "Temp1", "Temp2", "Temp3", "Temp4").contains(key);
    }


    /** 参与设备数、正常设备数和异常设备数。 */
    public record StatusCount(int participating, int normal, int abnormal) { }

    /**
     * 统计文本状态设备。
     *
     * <p>参与设备数按照“最新结果中存在状态键”计算，而不是只识别某几个中文状态，
     * 这样可以保证“存在对应状态键的设备总数”不因未知文本或空白字符被漏掉。
     * 0 或 0.0 表示正常，1 或 1.0 表示异常；此判断只用于统计，不改写保存的遥测值。</p>
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
                if (!isNormalStatusValue(value)) {
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

    /** 判断状态遥测是否为协议约定的正常值，不对原始遥测值进行转换。 */
    private static boolean isNormalStatusValue(String value) {
        String status = value.trim();
        return "0".equals(status) || "0.0".equals(status);
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

    /** 劣化：有效 SPD_RUL 大于等于 20% 为正常。 */
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
            if (Double.isFinite(value) && value >= MINIMUM_NORMAL_LIFE_PERCENT) {
                normal++;
            } else {
                abnormal++;
            }
        }
        return new StatusCount(participating, normal, abnormal);
    }

    /**
     * 计算累计计数器在统计窗口内的增量。
     *
     * <p>有窗口前基线时，从基线开始计算相邻差值；没有基线且窗口只有一条记录时，
     * 无法区分历史累计与本周期增量，返回 null。没有基线但有多条记录时，
     * 第一条只作为窗口内基准，后续按差值累计。计数器回退按设备重启处理，当前值计入新段。</p>
     */
    public static Long counterDelta(Double baseline, List<Double> samples) {
        CounterAccumulator counter = new CounterAccumulator(true);
        for (Double sample : samples) {
            counter.add(sample);
        }
        return counter.result(baseline);
    }

    private static boolean validCounter(Double value) { return value != null && Double.isFinite(value) && value >= 0D; }

    /** 跨页累计差值状态；只保存首尾读数、增量及样本是否达到两条，内存不随历史长度增长。 */
    public static final class CounterAccumulator {
        private final boolean ascending;
        private Double first;
        private Double previous;
        private int sampleCount;
        private long delta;

        /** ascending表示历史页及页内样本按时间升序，否则按降序消费。 */
        public CounterAccumulator(boolean ascending) {
            this.ascending = ascending;
        }

        /** 消费下一条有效累计值，跨页仍保留相邻关系；回退按计数器归零处理。 */
        public void add(Double value) {
            if (!validCounter(value)) {
                return;
            }
            if (first == null) {
                first = value;
            } else {
                double older = ascending ? previous : value;
                double newer = ascending ? value : previous;
                delta += Math.round(newer >= older ? newer - older : newer);
            }
            previous = value;
            sampleCount = Math.min(2, sampleCount + 1);
        }

        /** 与周期前基线合并；没有基线时仅报告首条以后已观测到的增量。 */
        public Long result(Double baseline) {
            if (sampleCount == 0) {
                return 0L;
            }
            if (!validCounter(baseline)) {
                return sampleCount == 1 ? null : delta;
            }
            double earliest = ascending ? first : previous;
            return delta + Math.round(earliest >= baseline ? earliest - baseline : earliest);
        }
    }
}
