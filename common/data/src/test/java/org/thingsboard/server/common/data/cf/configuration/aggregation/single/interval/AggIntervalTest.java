/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import org.thingsboard.server.common.data.util.TbPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AggIntervalTest {

    private static final String TZ = "Europe/Kiev";

    @Test
    void validateShouldThrowWhenInvalidTimZone() {
        AggInterval interval = new HourInterval("TimeZone", null);

        assertThatThrownBy(interval::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid timezone in interval: ");
    }

    @Test
    void validateShouldThrowWhenOffsetIsNegative() {
        AggInterval interval = new CustomInterval(TZ, -100L, TimeUnit.HOURS.toSeconds(2));

        assertThatThrownBy(interval::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Offset cannot be negative.");
    }

    @Test
    void validateShouldThrowWhenOffsetGreaterThanIntervalDuration() {
        AggInterval interval = new CustomInterval(TZ, TimeUnit.HOURS.toSeconds(2), TimeUnit.HOURS.toSeconds(2));

        assertThatThrownBy(interval::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Offset must be greater than interval duration.");
    }

    @ParameterizedTest
    @MethodSource("intervals")
    void testGetStartAndEndWithoutOffset(LongFunction<AggInterval> intervalCreator, long expectedDuration) {
        AggInterval interval = intervalCreator.apply(0L);

        ZonedDateTime dateTime = ZonedDateTime.of(
                // 2025.11.11 00:00:00
                2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)
        );
        long startTs = interval.getDateTimeIntervalStartTs(dateTime);
        long endTs = interval.getDateTimeIntervalEndTs(dateTime);

        assertThat(endTs).isGreaterThan(startTs);
        assertThat(endTs - startTs).isEqualTo(expectedDuration);
    }

    @ParameterizedTest
    @MethodSource("intervals")
    void testApplyOffset(LongFunction<AggInterval> intervalCreator) {
        long offsetSec = TimeUnit.MINUTES.toSeconds(15);
        AggInterval intervalWithOffset = intervalCreator.apply(offsetSec);
        AggInterval intervalNoOffset = intervalCreator.apply(0L);

        ZonedDateTime dateTime = ZonedDateTime.of(
                // 2025.11.11 11:20:00 - chosen so 15m offset shifts into a new interval
                2025, 6, 6, 6, 20, 0, 0, ZoneId.of(TZ)
        );

        long startWithOffsetTs = intervalWithOffset.getDateTimeIntervalStartTs(dateTime);
        long startNoOffsetTs = intervalNoOffset.getDateTimeIntervalStartTs(dateTime);

        ZonedDateTime startWithOffset = Instant.ofEpochMilli(startWithOffsetTs).atZone(intervalWithOffset.getZoneId());
        ZonedDateTime startNoOffset = Instant.ofEpochMilli(startNoOffsetTs).atZone(intervalNoOffset.getZoneId());

        long actualOffset = Duration.between(startNoOffset, startWithOffset).toSeconds();
        assertThat(actualOffset).isEqualTo(offsetSec);
    }

    private static Stream<Arguments> intervals() {
        return Stream.of(
                Arguments.of((LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset), TimeUnit.HOURS.toMillis(1)),
                Arguments.of((LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset), TimeUnit.DAYS.toMillis(1)),
                Arguments.of((LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset), TimeUnit.DAYS.toMillis(7)),
                Arguments.of((LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset), TimeUnit.DAYS.toMillis(7)),
                Arguments.of((LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset), TimeUnit.DAYS.toMillis(30)),
                Arguments.of((LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset), TimeUnit.DAYS.toMillis(92) + TimeUnit.HOURS.toMillis(1)),// Includes DST fallback (2025-10-26), so duration = 92 days + 1 hour(expected for Europe/Kyiv timezone).
                Arguments.of((LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset), TimeUnit.DAYS.toMillis(365)),
                Arguments.of((LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)), TimeUnit.HOURS.toMillis(4))
        );
    }

    @ParameterizedTest
    @MethodSource("nextIntervalFromExactDate")
    void testNextIntervalFromExactDate(LongFunction<AggInterval> intervalCreator, Function<ZonedDateTime, ZonedDateTime> expectedDateTimeFunction) {
        AggInterval interval = intervalCreator.apply(0L);

        ZonedDateTime currentStart = ZonedDateTime.of(
                2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)
        );

        ZonedDateTime nextStart = interval.getNextIntervalStart(currentStart);

        assertThat(nextStart).isEqualTo(expectedDateTimeFunction.apply(currentStart));
    }

    private static Stream<Arguments> nextIntervalFromExactDate() {
        return Stream.of(
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusHours(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusDays(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusWeeks(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusWeeks(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusMonths(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusMonths(3)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusYears(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusHours(4)
                )
        );
    }

    @Test
    void getIntervalsBetweenShouldSplitWindowIntoAlignedIntervals() {
        AggInterval interval = new HourInterval(TZ, null);
        long startTs = ts("2026-01-01T00:30:00");
        long endTs = ts("2026-01-01T03:30:00");

        List<TbPair<Long, Long>> intervals = interval.getIntervalsBetween(startTs, endTs);

        assertThat(intervals).containsExactly(
                new TbPair<>(ts("2026-01-01T00:00:00"), ts("2026-01-01T01:00:00")),
                new TbPair<>(ts("2026-01-01T01:00:00"), ts("2026-01-01T02:00:00")),
                new TbPair<>(ts("2026-01-01T02:00:00"), ts("2026-01-01T03:00:00")));
    }

    @Test
    void getIntervalsBetweenShouldStartFromTheIntervalContainingStartTs() {
        AggInterval interval = new HourInterval(TZ, null);

        // both bounds inside the same hour -> the interval is not complete, nothing to aggregate
        assertThat(interval.getIntervalsBetween(ts("2026-01-01T00:10:00"), ts("2026-01-01T00:50:00"))).isEmpty();
    }

    @Test
    void getIntervalsBetweenShouldExcludeTheIntervalContainingEndTs() {
        AggInterval interval = new HourInterval(TZ, null);

        // 02:00-03:00 contains endTs and is still open, so it must not be reported
        assertThat(interval.getIntervalsBetween(ts("2026-01-01T00:00:00"), ts("2026-01-01T02:30:00")))
                .containsExactly(
                        new TbPair<>(ts("2026-01-01T00:00:00"), ts("2026-01-01T01:00:00")),
                        new TbPair<>(ts("2026-01-01T01:00:00"), ts("2026-01-01T02:00:00")));
    }

    @Test
    void getIntervalsBetweenShouldHonorOffset() {
        AggInterval interval = new HourInterval(TZ, TimeUnit.MINUTES.toSeconds(15));

        assertThat(interval.getIntervalsBetween(ts("2026-01-01T00:20:00"), ts("2026-01-01T02:20:00")))
                .containsExactly(
                        new TbPair<>(ts("2026-01-01T00:15:00"), ts("2026-01-01T01:15:00")),
                        new TbPair<>(ts("2026-01-01T01:15:00"), ts("2026-01-01T02:15:00")));
    }

    @Test
    void getIntervalsBetweenShouldBeContiguous() {
        AggInterval interval = new DayInterval(TZ, null);
        List<TbPair<Long, Long>> intervals = interval.getIntervalsBetween(ts("2026-03-01T05:00:00"), ts("2026-04-05T05:00:00"));

        assertThat(intervals).hasSize(35);
        for (int i = 1; i < intervals.size(); i++) {
            // no gaps and no overlaps across the DST switch
            assertThat(intervals.get(i).getFirst()).isEqualTo(intervals.get(i - 1).getSecond());
        }
    }

    private static long ts(String localDateTime) {
        return java.time.LocalDateTime.parse(localDateTime).atZone(ZoneId.of(TZ)).toInstant().toEpochMilli();
    }

}
