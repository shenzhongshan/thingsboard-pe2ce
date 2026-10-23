package org.thingsboard.server.service.energy_regulation.util;

import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPowerTrendDTO;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 对同一上海自然小时的电表平均功率求和；禁止相加不同小时峰值或用电量冒充功率。 */
public final class EnergyRegulationPowerTrendCalculator {
    private static final long HOUR_MILLIS = 3_600_000L;
    private static final int HOURS_PER_DAY = 24;

    private EnergyRegulationPowerTrendCalculator() {
    }

    /** 查询结果已按每表每小时AVG聚合；只有全部电表都有数值的小时才能形成总功率。 */
    public static EnergyRegulationPowerTrendDTO calculate(List<List<TsKvEntry>> meters, ZonedDateTime now) {
        Map<Long, BigDecimal> hourly = aggregateCompleteHours(meters);
        long start = now.toLocalDate().minusDays(1).atStartOfDay(now.getZone()).toInstant().toEpochMilli();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> today = new ArrayList<>();
        List<BigDecimal> yesterday = new ArrayList<>();
        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            labels.add(String.format("%02d:00", hour));
            yesterday.add(hourly.get(start + hour * HOUR_MILLIS));
            today.add(hour <= now.getHour() ? hourly.get(start + (hour + HOURS_PER_DAY) * HOUR_MILLIS) : null);
        }
        return EnergyRegulationPowerTrendDTO.builder().labels(labels).today(today).yesterday(yesterday)
                .todayPeakKw(maximum(today)).yesterdayPeakKw(maximum(yesterday))
                .periodPeaks(periodPeaks(hourly, now)).build();
    }

    /** 每表每小时只计一次，缺数/查询失败的设备仍计入应到数量，避免把部分功率标为总功率。 */
    private static Map<Long, BigDecimal> aggregateCompleteHours(List<List<TsKvEntry>> meters) {
        Map<Long, BigDecimal> sums = new HashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        for (List<TsKvEntry> entries : meters) {
            Set<Long> seen = new HashSet<>();
            for (TsKvEntry entry : entries) {
                if (entry == null || entry.getValueAsString() == null
                        || !EnergyRegulationEnergyMetricDefinitions.TOTAL_ACTIVE_POWER_KEY.equals(entry.getKey())) {
                    continue;
                }
                long hour = entry.getTs() / HOUR_MILLIS * HOUR_MILLIS;
                if (seen.contains(hour)) {
                    continue;
                }
                try {
                    BigDecimal value = new BigDecimal(entry.getValueAsString());
                    sums.merge(hour, value, BigDecimal::add);
                    counts.merge(hour, 1, Integer::sum);
                    seen.add(hour);
                } catch (NumberFormatException exception) {
                    // 非数值记录不参与功率计算，缺数小时保持未知，不改变原始遥测。
                }
            }
        }
        sums.entrySet().removeIf(entry -> counts.get(entry.getKey()) != meters.size());
        return sums;
    }

    /** 同一小时总功率先求和，再分别取本日、本周、本月最大值。 */
    private static Map<String, BigDecimal> periodPeaks(Map<Long, BigDecimal> hourly, ZonedDateTime now) {
        Map<String, Long> starts = new LinkedHashMap<>();
        starts.put("day", now.toLocalDate().atStartOfDay(now.getZone()).toInstant().toEpochMilli());
        starts.put("week", now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1L)
                .atStartOfDay(now.getZone()).toInstant().toEpochMilli());
        starts.put("month", now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone()).toInstant().toEpochMilli());
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> period : starts.entrySet()) {
            BigDecimal peak = null;
            for (Map.Entry<Long, BigDecimal> hour : hourly.entrySet()) {
                if (hour.getKey() >= period.getValue() && hour.getKey() <= now.toInstant().toEpochMilli()
                        && (peak == null || hour.getValue().compareTo(peak) > 0)) {
                    peak = hour.getValue();
                }
            }
            result.put(period.getKey(), peak);
        }
        return result;
    }

    /** 只对有效的小时平均功率取最大值。 */
    private static BigDecimal maximum(List<BigDecimal> values) {
        BigDecimal result = null;
        for (BigDecimal value : values) {
            if (value != null && (result == null || value.compareTo(result) > 0)) {
                result = value;
            }
        }
        return result;
    }
}
