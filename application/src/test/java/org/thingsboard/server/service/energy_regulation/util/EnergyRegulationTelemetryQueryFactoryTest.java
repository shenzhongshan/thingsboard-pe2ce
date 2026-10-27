package org.thingsboard.server.service.energy_regulation.util;

import org.junit.Test;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationEnergyConsumptionDTO;
import org.thingsboard.server.service.energy_regulation.dto.EnergyRegulationPowerTrendDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 无Spring和数据库的回归测试，验证查询预算、真实边界表底及分段后的统计口径。 */
public class EnergyRegulationTelemetryQueryFactoryTest {

    private static final long HOUR_MILLIS = 3_600_000L;
    private static final String ENERGY_KEY = "FwdActET";
    private static final String POWER_KEY = "ActPwrTot";

    @Test
    public void shouldReadOnlyUniqueRawBoundariesInsteadOf1334HourlyIntervals() {
        ZonedDateTime now = time("2026-10-26T14:30:00+08:00");
        List<ReadTsKvQuery> queries = EnergyRegulationTelemetryQueryFactory.createConsumptionQueries(now);
        Set<String> identities = new HashSet<>();
        for (ReadTsKvQuery query : queries) {
            assertEquals(Aggregation.NONE, query.getAggregation());
            assertEquals(1, query.getLimit());
            assertEquals("DESC", query.getOrder());
            assertEquals(0, query.getStartTs());
            assertTrue(query.getEndTs() <= now.toInstant().toEpochMilli());
            assertTrue(identities.add(query.getKey() + ":" + query.getEndTs()));
        }
        assertTrue(queries.size() < 150);
        List<Long> boundaries = EnergyRegulationEnergyConsumptionCalculator.findReadBoundaryTimestamps(now);
        assertEquals(boundaries.size() * 3, queries.size());
        assertTrue(boundaries.contains(timestamp("2026-09-01T00:00:00+08:00")));
        assertTrue(boundaries.contains(timestamp("2026-10-01T00:00:00+08:00")));
        assertFalse(boundaries.contains(timestamp("2026-09-15T12:00:00+08:00")));
    }

    @Test
    public void shouldPreserveCurrentAndPreviousDayWeekMonthConsumptionWithSparseReadings() {
        ZonedDateTime now = time("2026-10-26T14:30:00+08:00");
        List<TsKvEntry> raw = List.of(
                energy("2026-08-31T23:59:59+08:00", 100),
                energy("2026-09-30T23:59:59+08:00", 200),
                energy("2026-10-18T23:59:59+08:00", 300),
                energy("2026-10-24T23:59:59+08:00", 400),
                energy("2026-10-25T23:59:59+08:00", 450),
                energy("2026-10-26T14:29:59+08:00", 480));
        List<TsKvEntry> sampled = readRawBoundaries(raw, now);
        EnergyRegulationEnergyConsumptionDTO result = EnergyRegulationEnergyConsumptionCalculator.calculate(
                List.of(sampled), 0, now);
        assertNumber("480", result.getTotalKwh());
        assertNumber("30", result.getEnergyConsumptionByPeriod().get("day").getValueKwh());
        assertNumber("50", result.getEnergyConsumptionByPeriod().get("day").getPreviousValueKwh());
        assertNumber("30", result.getEnergyConsumptionByPeriod().get("week").getValueKwh());
        assertNumber("150", result.getEnergyConsumptionByPeriod().get("week").getPreviousValueKwh());
        assertNumber("280", result.getEnergyConsumptionByPeriod().get("month").getValueKwh());
        assertNumber("100", result.getEnergyConsumptionByPeriod().get("month").getPreviousValueKwh());
        // 重复命中的同一原始采样按实际时间戳去重，不把多个边界命中误当作多块表。
        assertEquals(1, result.getMeterCount());
    }

    @Test
    public void shouldRespectExclusiveBoundaryAndKeepLatestPartialHour() {
        ZonedDateTime now = time("2026-10-26T02:30:00+08:00");
        List<TsKvEntry> raw = List.of(
                energy("2026-10-25T23:59:59+08:00", 100),
                energy("2026-10-26T00:59:59+08:00", 102),
                energy("2026-10-26T01:00:00+08:00", 103),
                energy("2026-10-26T01:59:59+08:00", 105),
                energy("2026-10-26T02:29:59+08:00", 106),
                energy("2026-10-26T02:30:00+08:00", 999));
        EnergyRegulationEnergyConsumptionDTO result = EnergyRegulationEnergyConsumptionCalculator.calculate(
                List.of(readRawBoundaries(raw, now)), 0, now);
        List<BigDecimal> day = result.getEnergySeries().get("day").getValues();
        assertNumber("2", day.get(0));
        assertNumber("3", day.get(1));
        assertNumber("1", day.get(2));
        assertNull(day.get(3));
        assertNumber("6", result.getEnergyConsumptionByPeriod().get("day").getValueKwh());
    }

    @Test
    public void shouldKeepMissingBaselineAsUnknownAndCounterRollbackAsInvalid() {
        ZonedDateTime now = time("2026-10-26T14:30:00+08:00");
        EnergyRegulationEnergyConsumptionDTO missing = EnergyRegulationEnergyConsumptionCalculator.calculate(
                List.of(readRawBoundaries(List.of(energy("2026-10-26T14:00:00+08:00", 100)), now)), 1, now);
        assertNull(missing.getEnergyConsumptionByPeriod().get("day").getValueKwh());
        assertEquals(1, missing.getReadFailureCount());
        EnergyRegulationEnergyConsumptionDTO rollback = EnergyRegulationEnergyConsumptionCalculator.calculate(
                List.of(readRawBoundaries(List.of(
                        energy("2026-10-25T23:59:59+08:00", 100),
                        energy("2026-10-26T14:00:00+08:00", 90)), now)), 0, now);
        assertNull(rollback.getEnergyConsumptionByPeriod().get("day").getValueKwh());
        assertEquals(1, rollback.getInvalidCounterCount());
    }

    @Test
    public void shouldIncludeCrossMonthWeekAndYearLeapDayBoundariesWithoutFutureReads() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        for (int day = 0; day < 366; day++) {
            ZonedDateTime now = date.plusDays(day).atTime(23, 59)
                    .atZone(EnergyRegulationEnergyConsumptionCalculator.TIME_ZONE);
            List<Long> boundaries = EnergyRegulationEnergyConsumptionCalculator.findReadBoundaryTimestamps(now);
            assertTrue(boundaries.size() <= 50);
            for (long boundary : boundaries) {
                assertTrue(boundary <= now.toInstant().toEpochMilli());
            }
            List<ReadTsKvQuery> power = EnergyRegulationTelemetryQueryFactory.createPowerQueries(now);
            for (ReadTsKvQuery query : power) {
                assertEquals(Aggregation.AVG, query.getAggregation());
                assertTrue((query.getEndTs() - query.getStartTs()) / query.getInterval() <= 24);
            }
        }
        List<Long> october = EnergyRegulationEnergyConsumptionCalculator.findReadBoundaryTimestamps(
                time("2026-10-01T00:00:00+08:00"));
        assertTrue(october.contains(timestamp("2026-09-28T00:00:00+08:00")));
        assertTrue(october.contains(timestamp("2026-09-21T00:00:00+08:00")));
    }

    @Test
    public void shouldPartitionPowerHoursContinuouslyThrough31DayMonth() {
        ZonedDateTime now = time("2026-10-31T23:59:00+08:00");
        List<ReadTsKvQuery> queries = EnergyRegulationTelemetryQueryFactory.createPowerQueries(now);
        assertEquals(31, queries.size());
        long end = timestamp("2026-10-01T00:00:00+08:00");
        for (ReadTsKvQuery query : queries) {
            assertEquals(end, query.getStartTs());
            assertEquals(24, query.getLimit());
            assertEquals(HOUR_MILLIS, query.getInterval());
            end = query.getEndTs();
        }
        assertEquals(now.toInstant().toEpochMilli(), end);
    }

    @Test
    public void shouldPreserveHourlyAveragesAndPeaksAcrossDailyPartitions() {
        ZonedDateTime now = time("2026-10-26T01:30:00+08:00");
        List<TsKvEntry> raw = List.of(
                power("2026-10-25T23:10:00+08:00", 8),
                power("2026-10-25T23:40:00+08:00", 12),
                power("2026-10-26T00:00:00+08:00", 4),
                power("2026-10-26T00:30:00+08:00", 8),
                power("2026-10-26T01:10:00+08:00", 0));
        List<TsKvEntry> hourly = new ArrayList<>();
        for (ReadTsKvQuery query : EnergyRegulationTelemetryQueryFactory.createPowerQueries(now)) {
            for (long start = query.getStartTs(); start < query.getEndTs(); start += HOUR_MILLIS) {
                long end = Math.min(start + HOUR_MILLIS, query.getEndTs());
                double total = 0;
                int count = 0;
                for (TsKvEntry entry : raw) {
                    if (entry.getTs() >= start && entry.getTs() < end) {
                        total += Double.parseDouble(entry.getValueAsString());
                        count++;
                    }
                }
                if (count > 0) {
                    hourly.add(new BasicTsKvEntry(start + (end - start) / 2,
                            new DoubleDataEntry(POWER_KEY, total / count)));
                }
            }
        }
        EnergyRegulationPowerTrendDTO result = EnergyRegulationPowerTrendCalculator.calculate(List.of(hourly), now);
        assertNumber("10", result.getYesterday().get(23));
        assertNumber("6", result.getToday().get(0));
        assertNumber("0", result.getToday().get(1));
        assertNull(result.getToday().get(2));
        assertNumber("6", result.getTodayPeakKw());
        assertNumber("10", result.getYesterdayPeakKw());
        assertNumber("6", result.getPeriodPeaks().get("week"));
        assertNumber("10", result.getPeriodPeaks().get("month"));
    }

    /** 模拟原生SQL的[start,end)及DESC LIMIT 1，只模拟读取、不复制统计公式。 */
    private List<TsKvEntry> readRawBoundaries(List<TsKvEntry> raw, ZonedDateTime now) {
        List<TsKvEntry> selected = new ArrayList<>();
        for (ReadTsKvQuery query : EnergyRegulationTelemetryQueryFactory.createConsumptionQueries(now)) {
            TsKvEntry latest = null;
            for (TsKvEntry entry : raw) {
                if (query.getKey().equals(entry.getKey()) && entry.getTs() >= query.getStartTs()
                        && entry.getTs() < query.getEndTs() && (latest == null || entry.getTs() > latest.getTs())) {
                    latest = entry;
                }
            }
            if (latest != null) {
                selected.add(latest);
            }
        }
        return selected;
    }

    private static TsKvEntry energy(String time, double value) {
        return new BasicTsKvEntry(timestamp(time), new DoubleDataEntry(ENERGY_KEY, value));
    }

    private static TsKvEntry power(String time, double value) {
        return new BasicTsKvEntry(timestamp(time), new DoubleDataEntry(POWER_KEY, value));
    }

    private static ZonedDateTime time(String value) {
        return ZonedDateTime.parse(value).withZoneSameInstant(EnergyRegulationEnergyConsumptionCalculator.TIME_ZONE);
    }

    private static long timestamp(String value) {
        return time(value).toInstant().toEpochMilli();
    }

    private static void assertNumber(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
