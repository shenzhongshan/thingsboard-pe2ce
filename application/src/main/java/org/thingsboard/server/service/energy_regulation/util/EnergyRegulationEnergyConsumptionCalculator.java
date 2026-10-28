/** 文件职责：将标准累计电能遥测转换为日、周、月用电量及趋势。 */
package org.thingsboard.server.service.energy_regulation.util;

import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationEnergyConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPeriodConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationValueSeriesDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** 按亚洲上海自然日统计，电表值按总正向有功电能的累计表底处理。 */
public final class EnergyRegulationEnergyConsumptionCalculator {

    /** 日、周、月切分使用的业务时区。 */
    public static final ZoneId TIME_ZONE = ZoneId.of("Asia/Shanghai");
    /** 小时趋势的时间桶长度。 */
    private static final long HOUR_MILLIS = 60L * 60 * 1000;
    /** 百分比变化率的乘数。 */
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    /** 变化率保留的小数位数。 */
    private static final int CHANGE_PERCENT_SCALE = 4;

    /** 纯计算工具类不创建实例。 */
    private EnergyRegulationEnergyConsumptionCalculator() {
    }

    /** 为本次看板查询确定历史起点，覆盖上一个自然月及用于计算起点表底的历史区间。
     * @param now 当前上海时区时间
     * @return 上一个自然月月初的毫秒时间戳
     */
    public static long findReadStartTs(ZonedDateTime now) {
        // 先定位当前月月初，再回退一个自然月作为最早读取日。
        LocalDate currentMonthStart = now.toLocalDate().withDayOfMonth(1);
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        // 将上海自然日边界转换为 UTC 毫秒时间戳，供时序查询使用。
        return previousMonthStart.atStartOfDay(TIME_ZONE).toInstant().toEpochMilli();
    }

    /** 按每台设备选取一个总正向有功电能键并生成周期统计，避免多个候选键重复计入。
     * @param entriesByDevice 按设备顺序排列的候选遥测记录列表
     * @param readFailureCount 时序查询失败的设备数量
     * @param now 当前上海时区时间
     * @return 含周期用电汇总、趋势序列和读取状态的统计 DTO
     */
    public static EnergyRegulationEnergyConsumptionDTO calculate(
            List<List<TsKvEntry>> entriesByDevice,
            int readFailureCount,
            ZonedDateTime now) {
        // 固定本次统计截止时间，并预先生成日、周、月三个周期边界。
        long nowTs = now.toInstant().toEpochMilli();
        Map<String, PeriodWindow> periods = createPeriods(now.toLocalDate(), nowTs);
        List<MeterSeries> meters = new ArrayList<>();
        BigDecimal totalKwh = BigDecimal.ZERO;
        int invalidCounterCount = 0;

        // 每台设备只选一个最可信的候选累计键，防止兼容键并存时重复计量。
        for (List<TsKvEntry> entries : entriesByDevice) {
            MeterSeries meter = chooseMeterSeries(entries);
            if (meter == null || meter.values.isEmpty()) {
                // 无可用表底的设备不计入有数据电表数和累计值。
                continue;
            }
            // 保存本设备选定的完整序列，后续汇总和趋势都复用该序列。
            meters.add(meter);
            // 累计值使用当前窗口内最新表底，汇总各电表的当前表底。
            totalKwh = totalKwh.add(meter.values.lastEntry().getValue());
        }

        // 对每个周期分别汇总当前值、上一周期值及有效参与电表数量。
        Map<String, EnergyRegulationPeriodConsumptionDTO> summaries = new LinkedHashMap<>();
        // 使用设备索引记录回退表底，确保同一设备多个周期异常时只计一次。
        boolean[] invalidMeters = new boolean[meters.size()];
        for (Map.Entry<String, PeriodWindow> periodEntry : periods.entrySet()) {
            SummaryAccumulator accumulator = new SummaryAccumulator();
            PeriodWindow window = periodEntry.getValue();
            for (int meterIndex = 0; meterIndex < meters.size(); meterIndex++) {
                MeterSeries meter = meters.get(meterIndex);
                // 通过区间两端最近表底的差值计算当前周期和上一周期用电量。
                BigDecimal current = calculateDelta(meter.values, window.currentStartTs, window.endTs);
                BigDecimal previous = calculateDelta(meter.values, window.previousStartTs, window.currentStartTs);
                // 缺少任一边界读数时保留缺数，不把它折算成零。
                if (current != null) {
                    accumulator.addCurrent(current);
                }
                if (previous != null) {
                    accumulator.addPrevious(previous);
                }
                // 记录负差值表计，汇总器会跳过该差值但响应仍报告异常表计数。
                if ((current != null && current.signum() < 0) || (previous != null && previous.signum() < 0)) {
                    invalidMeters[meterIndex] = true;
                }
            }
            // 当前周期结果和上一周期对比值一起保存，供接口按周期读取。
            summaries.put(periodEntry.getKey(), accumulator.toDTO());
        }
        // 归并各周期异常标记，避免同一块电表被重复计数。
        for (boolean invalidMeter : invalidMeters) {
            if (invalidMeter) {
                invalidCounterCount++;
            }
        }

        // 先为三个趋势粒度建立固定标签和空数据桶。
        Map<String, EnergyRegulationValueSeriesDTO> series = new LinkedHashMap<>();
        SeriesAccumulator daySeries = new SeriesAccumulator(createDayLabels());
        SeriesAccumulator weekSeries = new SeriesAccumulator(createWeekLabels());
        SeriesAccumulator monthSeries = new SeriesAccumulator(createMonthLabels(now.toLocalDate()));
        // 将每台电表的区间差值累加到对应粒度的趋势桶中。
        for (MeterSeries meter : meters) {
            accumulateDaySeries(meter.values, now, daySeries);
            accumulateWeekSeries(meter.values, now, weekSeries);
            accumulateMonthSeries(meter.values, now, monthSeries);
        }
        // 使用稳定的 day、week、month 键组织趋势响应。
        series.put("day", daySeries.toDTO());
        series.put("week", weekSeries.toDTO());
        series.put("month", monthSeries.toDTO());

        // 汇总数据、失败数和异常表计数构成最终服务响应。
        return EnergyRegulationEnergyConsumptionDTO.builder()
                .meterCount(meters.size())
                .readFailureCount(readFailureCount)
                .invalidCounterCount(invalidCounterCount)
                .totalKwh(meters.isEmpty() ? null : totalKwh)
                .energyConsumptionByPeriod(Collections.unmodifiableMap(summaries))
                .energySeries(Collections.unmodifiableMap(series))
                .build();
    }

    /** 建立自然日、ISO 周和自然月的当前与上一周期边界。
     * @param currentDate 当前上海自然日
     * @param nowTs 本次查询结束时间戳
     * @return 按 day、week、month 键保存的周期边界
     */
    private static Map<String, PeriodWindow> createPeriods(LocalDate currentDate, long nowTs) {
        Map<String, PeriodWindow> periods = new LinkedHashMap<>();
        // 当前日作为日统计起点，上一自然日作为对比周期起点。
        LocalDate dayStart = currentDate;
        // ISO 周从周一开始，上一完整周用于环比对照。
        LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // 自然月从每月一日开始，上一自然月作为月统计对照。
        LocalDate monthStart = currentDate.withDayOfMonth(1);
        // 三种周期都以同一查询截止时间结束，避免未来时间段进入统计。
        periods.put("day", new PeriodWindow(startOf(dayStart), startOf(dayStart.minusDays(1)), nowTs));
        periods.put("week", new PeriodWindow(startOf(weekStart), startOf(weekStart.minusWeeks(1)), nowTs));
        periods.put("month", new PeriodWindow(startOf(monthStart), startOf(monthStart.minusMonths(1)), nowTs));
        return periods;
    }

    /** 从同一设备的标准完整键、简称和 ESA 原生键中选出历史点最多且最新的一个。
     * @param entries 当前设备返回的候选键遥测记录
     * @return 该设备唯一选中的累计表底序列；无可解析读数时为 null
     */
    private static MeterSeries chooseMeterSeries(List<TsKvEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        // 为每个兼容键准备独立的有序表底序列。
        Map<String, NavigableMap<Long, BigDecimal>> valuesByKey = new LinkedHashMap<>();
        for (String key : EnergyRegulationEnergyMetricDefinitions.TOTAL_FORWARD_ACTIVE_ENERGY_KEYS) {
            valuesByKey.put(key, new TreeMap<>());
        }
        // 扫描遥测，只收集候选键且可转为十进制数值的读数。
        for (TsKvEntry entry : entries) {
            NavigableMap<Long, BigDecimal> values = valuesByKey.get(entry.getKey());
            BigDecimal value = parseNumericValue(entry);
            if (values != null && value != null) {
                values.put(entry.getTs(), value);
            }
        }

        // 选择点数最多的候选序列；点数相同时选择最新读数时间更晚的序列。
        NavigableMap<Long, BigDecimal> selectedValues = null;
        for (String key : EnergyRegulationEnergyMetricDefinitions.TOTAL_FORWARD_ACTIVE_ENERGY_KEYS) {
            NavigableMap<Long, BigDecimal> values = valuesByKey.get(key);
            if (values.isEmpty()) {
                continue;
            }
            if (selectedValues == null
                    || values.size() > selectedValues.size()
                    || (values.size() == selectedValues.size()
                        && values.lastKey() > selectedValues.lastKey())) {
                selectedValues = values;
            }
        }
        if (selectedValues == null) {
            return null;
        }
        // 返回单一选定序列，确保同设备不会把不同键的表底相加。
        return new MeterSeries(selectedValues);
    }

    /** 兼容数值型遥测和可解析为数值的字符串遥测。
     * @param entry ThingsBoard 时序记录
     * @return 十进制表底；记录缺失或格式非法时为 null
     */
    private static BigDecimal parseNumericValue(TsKvEntry entry) {
        if (entry == null || entry.getValueAsString() == null || entry.getValueAsString().isBlank()) {
            return null;
        }
        try {
            // 使用十进制解析保留累计表底精度。
            return new BigDecimal(entry.getValueAsString());
        } catch (NumberFormatException e) {
            // 非数值遥测不参与累计量计算。
            return null;
        }
    }

    /** 计算左闭右开时间段的表底差；任一边界缺数时不把缺数伪装成零。
     * @param values 按时间戳排列的累计表底序列
     * @param startTs 区间起始时间戳
     * @param endTs 区间结束时间戳
     * @return 区间用电量差值；任一边界无读数时为 null
     */
    private static BigDecimal calculateDelta(NavigableMap<Long, BigDecimal> values, long startTs, long endTs) {
        // 分别取得起点和终点之前的最近表底，边界点本身不纳入左闭右开区间。
        Map.Entry<Long, BigDecimal> startEntry = values.lowerEntry(startTs);
        Map.Entry<Long, BigDecimal> endEntry = values.lowerEntry(endTs);
        if (startEntry == null || endEntry == null) {
            // 任一侧没有可用表底时无法可靠计算差值，保留 null 表示缺数。
            return null;
        }
        // 累计表底相减得到区间用电量；负值由上层作为表计回退异常处理。
        return endEntry.getValue().subtract(startEntry.getValue());
    }

    /** 按当前自然日每小时的表底差构造日趋势。
     * @param values 当前电表累计表底序列
     * @param now 当前上海时区时间
     * @param accumulator 接收每小时差值的趋势累加器
     */
    private static void accumulateDaySeries(
            NavigableMap<Long, BigDecimal> values,
            ZonedDateTime now,
            SeriesAccumulator accumulator) {
        LocalDate today = now.toLocalDate();
        long dayStartTs = startOf(today);
        // 依次计算当天已经结束或正在进行的小时桶，未来桶维持空值。
        for (int hour = 0; hour < 24; hour++) {
            long startTs = dayStartTs + hour * HOUR_MILLIS;
            if (startTs >= now.toInstant().toEpochMilli()) {
                break;
            }
            // 当前小时只统计到查询时刻，未结束的小时不会读取未来数据。
            long endTs = Math.min(startTs + HOUR_MILLIS, now.toInstant().toEpochMilli());
            BigDecimal delta = calculateDelta(values, startTs, endTs);
            accumulator.add(hour, delta);
        }
    }

    /** 按本周周一到周日的自然日表底差构造周趋势。
     * @param values 当前电表累计表底序列
     * @param now 当前上海时区时间
     * @param accumulator 接收每日差值的趋势累加器
     */
    private static void accumulateWeekSeries(
            NavigableMap<Long, BigDecimal> values,
            ZonedDateTime now,
            SeriesAccumulator accumulator) {
        LocalDate weekStart = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long nowTs = now.toInstant().toEpochMilli();
        // 按周一至周日逐日切桶，超出当前日期的桶保持空值。
        for (int day = 0; day < 7; day++) {
            LocalDate date = weekStart.plusDays(day);
            long startTs = startOf(date);
            long endTs = Math.min(startOf(date.plusDays(1)), nowTs);
            if (startTs >= nowTs) {
                break;
            }
            // 当天趋势终点不晚于当前查询时刻。
            accumulator.add(day, calculateDelta(values, startTs, endTs));
        }
    }

    /** 按每月连续七天为一组构造月趋势，月末不足七天仍作为独立时间桶。
     * @param values 当前电表累计表底序列
     * @param now 当前上海时区时间
     * @param accumulator 接收每个七天区间差值的趋势累加器
     */
    private static void accumulateMonthSeries(
            NavigableMap<Long, BigDecimal> values,
            ZonedDateTime now,
            SeriesAccumulator accumulator) {
        LocalDate monthStart = now.toLocalDate().withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        long nowTs = now.toInstant().toEpochMilli();
        // 按每七天一个桶计算当月趋势，最后一桶按月末截断。
        for (int week = 0; week < accumulator.values.size(); week++) {
            LocalDate start = monthStart.plusDays(week * 7L);
            if (!start.isBefore(nextMonthStart) || !start.isBefore(now.toLocalDate().plusDays(1))) {
                break;
            }
            LocalDate end = start.plusDays(7);
            if (end.isAfter(nextMonthStart)) {
                end = nextMonthStart;
            }
            // 当前桶只计算至当前时刻，避免把未来日期计入用电量。
            long endTs = Math.min(startOf(end), nowTs);
            accumulator.add(week, calculateDelta(values, startOf(start), endTs));
        }
    }

    /** 生成日趋势的小时标签。
     * @return 从 00:00 到 23:00 的小时标签
     */
    private static List<String> createDayLabels() {
        List<String> labels = new ArrayList<>(24);
        // 创建与 24 个小时趋势桶一一对应的固定横轴标签。
        for (int hour = 0; hour < 24; hour++) {
            labels.add(String.format("%02d:00", hour));
        }
        return labels;
    }

    /** 生成 ISO 周趋势标签。
     * @return 周一至周日的中文标签
     */
    private static List<String> createWeekLabels() {
        return List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日");
    }

    /** 按本月天数计算四到五个连续七天趋势桶。
     * @param date 当前上海自然日
     * @return 覆盖本月天数的七天分段标签
     */
    private static List<String> createMonthLabels(LocalDate date) {
        // 月趋势每七天一桶，向上取整以覆盖月末不足七天的区间。
        int weekCount = (date.lengthOfMonth() + 6) / 7;
        List<String> labels = new ArrayList<>(weekCount);
        // 标签位置与月趋势桶索引保持一致。
        for (int week = 1; week <= weekCount; week++) {
            labels.add("第" + week + "周");
        }
        return labels;
    }

    /** 将上海时区的自然日零点转换成时序查询使用的毫秒时间戳。
     * @param date 需要转换的上海自然日
     * @return 该自然日零点对应的 UTC 毫秒时间戳
     */
    private static long startOf(LocalDate date) {
        // 使用业务时区计算自然日边界，避免直接按固定毫秒数换算日期边界。
        return date.atStartOfDay(TIME_ZONE).toInstant().toEpochMilli();
    }

    /** 根据当前值和上一周期值计算变化百分比；上一期缺数或为零时返回 null。
     * @param current 当前周期用电量
     * @param previous 上一周期用电量
     * @return 相对上一周期的百分比变化，无法比较时为 null
     */
    private static BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            // 缺少对比值或上一周期为零时百分比无定义，返回 null。
            return null;
        }
        // 以百分比形式计算变化率，并按约定精度四舍五入。
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, CHANGE_PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /** 当前周期和上一周期的左边界，以及本次统计的结束时间。
     * @param currentStartTs 当前周期起始时间戳
     * @param previousStartTs 上一周期起始时间戳
     * @param endTs 当前统计结束时间戳
     */
    private record PeriodWindow(long currentStartTs, long previousStartTs, long endTs) {
    }

    /** 单台电表选定一个标准键后的时间戳与累计表底索引。
     * @param values 按遥测时间戳升序排列的累计表底
     */
    private record MeterSeries(NavigableMap<Long, BigDecimal> values) {
    }

    /** 合并参与统计的电表表底差，并记录当前与上一周期有效表计数。 */
    private static final class SummaryAccumulator {
        /** 当前周期所有有效电表的用电量和。 */
        private BigDecimal current = BigDecimal.ZERO;
        /** 上一周期所有有效电表的用电量和。 */
        private BigDecimal previous = BigDecimal.ZERO;
        /** 当前周期成功参与合计的电表数。 */
        private int currentMeterCount;
        /** 上一周期成功参与合计的电表数。 */
        private int previousMeterCount;

        /** 将非负的当前周期表底差加入汇总。 */
        private void addCurrent(BigDecimal value) {
            if (value.signum() < 0) {
                // 回退表底不纳入用电量合计，异常设备数由外层单独记录。
                return;
            }
            // 只有有效非负差值才增加总量和参与统计的电表数。
            current = current.add(value);
            currentMeterCount++;
        }

        /** 将非负的上一周期表底差加入汇总。 */
        private void addPrevious(BigDecimal value) {
            if (value.signum() < 0) {
                // 上一周期负差值同样跳过，避免异常表底拉低总体对比值。
                return;
            }
            // 只统计边界完整且非负的上一周期差值。
            previous = previous.add(value);
            previousMeterCount++;
        }

        /** 生成汇总 DTO；没有有效电表时用 null 表示缺数。 */
        private EnergyRegulationPeriodConsumptionDTO toDTO() {
            // 零台有效表计代表缺数，输出 null 而不是具有误导性的零用电量。
            BigDecimal currentValue = currentMeterCount == 0 ? null : current;
            BigDecimal previousValue = previousMeterCount == 0 ? null : previous;
            // 汇总值、环比和两侧参与电表数量一起组成周期 DTO。
            return EnergyRegulationPeriodConsumptionDTO.builder()
                    .valueKwh(currentValue)
                    .previousValueKwh(previousValue)
                    .changePercent(calculateChangePercent(currentValue, previousValue))
                    .includedMeterCount(currentMeterCount)
                    .previousIncludedMeterCount(previousMeterCount)
                    .build();
        }
    }

    /** 按时间桶合并多台电表用电量；缺失桶保持 null。 */
    private static final class SeriesAccumulator {
        /** 当前图表各桶对应的横轴标签。 */
        private final List<String> labels;
        /** 按标签位置累计的电量，缺失桶保持 null。 */
        private final List<BigDecimal> values;

        /** 为每个标签初始化一个尚无有效遥测的空桶。 */
        private SeriesAccumulator(List<String> labels) {
            // 保存横轴标签，并为每个桶初始化 null，区分缺数据与零用电量。
            this.labels = labels;
            this.values = new ArrayList<>(Collections.nCopies(labels.size(), null));
        }

        /** 将单台电表的有效区间差加入对应时间桶。 */
        private void add(int index, BigDecimal value) {
            if (value == null || value.signum() < 0) {
                // 缺数和回退表底都不写入趋势桶，保留桶当前的 null 或有效累计值。
                return;
            }
            BigDecimal existing = values.get(index);
            // 首个有效读数直接填桶，后续设备读数累加到已有桶值。
            values.set(index, existing == null ? value : existing.add(value));
        }

        /** 转换为包含横轴标签和各桶电量的趋势 DTO。 */
        private EnergyRegulationValueSeriesDTO toDTO() {
            // 将固定标签及聚合后的桶值封装为趋势 DTO。
            return EnergyRegulationValueSeriesDTO.builder()
                    .labels(labels)
                    .values(values)
                    .build();
        }
    }
}
