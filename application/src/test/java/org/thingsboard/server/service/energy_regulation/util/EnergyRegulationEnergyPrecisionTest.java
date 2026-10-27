package org.thingsboard.server.service.energy_regulation.util;

import org.junit.Test;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 电表原始表底、差值与环比不应被页面展示精度提前四舍五入。 */
public class EnergyRegulationEnergyPrecisionTest {
    @Test
    public void shouldPreserveMeterDecimalsAndChangePercentBeyondFourPlaces() {
        List<TsKvEntry> readings = List.of(
                reading("2026-10-24T23:59:59+08:00", "0"),
                reading("2026-10-25T23:59:59+08:00", "1"),
                reading("2026-10-26T12:00:00+08:00", "2.123456789"));
        var result = EnergyRegulationEnergyConsumptionCalculator.calculate(List.of(readings), 0,
                ZonedDateTime.parse("2026-10-26T16:00:00+08:00"));
        var day = result.getEnergyConsumptionByPeriod().get("day");
        assertEquals(0, new BigDecimal("2.123456789").compareTo(result.getTotalKwh()));
        assertEquals(0, new BigDecimal("1.123456789").compareTo(day.getValueKwh()));
        assertEquals(0, new BigDecimal("12.3456789").compareTo(day.getChangePercent()));
    }

    /** 使用文本型数值避免测试准备阶段先通过double丢失原始十进制精度。 */
    private static TsKvEntry reading(String time, String value) {
        return new BasicTsKvEntry(ZonedDateTime.parse(time).toInstant().toEpochMilli(),
                new StringDataEntry("FwdActET", value));
    }
}
