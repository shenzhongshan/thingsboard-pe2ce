package org.thingsboard.server.service.monitoring.util;

import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 单台设备的流式遥测聚合器，只保留每个查询的极值、最新值及计数状态，不保留历史页。 */
public final class MonitoringTelemetryAccumulator {
    private final Set<String> counterKeys;
    // 周期查询与最新值查询必须分开保存，防止周期外的最新值覆盖周期极值。
    private final Map<ReadTsKvQuery, MetricSamples> metrics = new LinkedHashMap<>();
    private final Map<String, SpdMonitoringCalculator.CounterAccumulator> counters = new HashMap<>();
    private final Map<String, Double> baselines = new HashMap<>();

    /** counterKeys为本次调用中需要按累计计数器处理的规范键；空集合表示只计算极值和最新状态。 */
    public MonitoringTelemetryAccumulator(Set<String> counterKeys) {
        this.counterKeys = Set.copyOf(counterKeys);
    }

    /** 消费当前查询的一页记录；query始终传入原查询，翻页窗口由读取层管理。 */
    public void accept(ReadTsKvQuery query, List<TsKvEntry> page) {
        MetricSamples samples = metrics.computeIfAbsent(query,
                ignored -> new MetricSamples("ASC".equalsIgnoreCase(query.getOrder())));
        boolean counter = counterKeys.contains(query.getKey());
        List<TsKvEntry> ordered = page;
        if (counter && query.getLimit() > 1) {
            // 仅排序当前页，保证不同存储实现返回顺序不影响跨页累计差值。
            ordered = new ArrayList<>(page);
            Comparator<TsKvEntry> comparator = Comparator.comparingLong(TsKvEntry::getTs);
            ordered.sort("ASC".equalsIgnoreCase(query.getOrder()) ? comparator : comparator.reversed());
        }
        for (TsKvEntry entry : ordered) {
            samples.accept(entry);
            if (!counter) {
                continue;
            }
            Double value = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
            if (query.getLimit() > 1) {
                counters.computeIfAbsent(query.getKey(), ignored ->
                        new SpdMonitoringCalculator.CounterAccumulator("ASC".equalsIgnoreCase(query.getOrder())))
                        .add(value);
            } else if (value != null && value >= 0D) {
                // 调用方为累计键追加的单条查询，是周期开始前最近的基线。
                baselines.put(query.getKey(), value);
            }
        }
    }

    /** 返回供既有极值及最新状态组装使用的少量真实样本，不伪造遥测值或时间。 */
    public List<TsKvEntry> representativeEntries() {
        List<TsKvEntry> result = new ArrayList<>();
        for (MetricSamples samples : metrics.values()) {
            samples.appendTo(result);
        }
        return result;
    }

    /** 返回已观测增量；无基线单样本为null，无有效周期样本为0。 */
    public Long counterDelta(String key) {
        SpdMonitoringCalculator.CounterAccumulator counter = counters.get(key);
        if (counter == null) {
            return 0L;
        }
        // 避免基本类型与Long三元表达式自动拆箱，把合法的未知null变成空指针异常。
        return counter.result(baselines.get(key));
    }

    /** 每个查询最多保留五条真实记录，同时支持原值最值、绝对值最值和最新状态。 */
    private static final class MetricSamples {
        private final boolean ascending;
        private TsKvEntry latest;
        private TsKvEntry minimum;
        private TsKvEntry maximum;
        private TsKvEntry minimumAbsolute;
        private TsKvEntry maximumAbsolute;
        private Double minValue;
        private Double maxValue;
        private Double minAbsolute;
        private Double maxAbsolute;

        private MetricSamples(boolean ascending) {
            this.ascending = ascending;
        }

        private void accept(TsKvEntry entry) {
            if (latest == null || entry.getTs() > latest.getTs()) {
                latest = entry;
            }
            Double value = MonitoringValueParser.parseFiniteDouble(entry.getValueAsString());
            if (value == null) {
                return;
            }
            if (minValue == null || value < minValue) {
                minValue = value;
                minimum = entry;
            }
            if (maxValue == null || value > maxValue) {
                maxValue = value;
                maximum = entry;
            }
            double absolute = Math.abs(value);
            if (minAbsolute == null || absolute < minAbsolute) {
                minAbsolute = absolute;
                minimumAbsolute = entry;
            }
            if (maxAbsolute == null || absolute > maxAbsolute) {
                maxAbsolute = absolute;
                maximumAbsolute = entry;
            }
        }

        private void appendTo(List<TsKvEntry> target) {
            // 单个样本可能同时代表多个最值，避免重复添加同一对象。
            List<TsKvEntry> unique = new ArrayList<>();
            for (TsKvEntry entry : new TsKvEntry[]{latest, minimum, maximum, minimumAbsolute, maximumAbsolute}) {
                if (entry != null && !unique.contains(entry)) {
                    unique.add(entry);
                }
            }
            // 保留原查询的时间顺序，数值相同时仍选原先先遇到的来源时间。
            Comparator<TsKvEntry> comparator = Comparator.comparingLong(TsKvEntry::getTs);
            unique.sort(ascending ? comparator : comparator.reversed());
            target.addAll(unique);
        }
    }
}
