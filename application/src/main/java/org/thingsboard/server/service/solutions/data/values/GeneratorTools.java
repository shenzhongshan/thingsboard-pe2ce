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
package org.thingsboard.server.service.solutions.data.values;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared randomness and calendar helpers for the telemetry generators. The day/hour classification
 * is what gives generated data a believable weekday/weekend and day/night shape.
 */
public class GeneratorTools {

    private GeneratorTools() {
    }

    public static long randomLong(double min, double max) {
        return (long) randomDouble(min, max);
    }

    public static double randomDouble(double min, double max) {
        return min + random().nextDouble() * Math.abs(max - min);
    }

    /** Generators run in parallel across the emulator pool, so avoid a shared {@link Random}. */
    private static Random random() {
        return ThreadLocalRandom.current();
    }

    public static double getMultiplier(long ts, double holidayMultiplier, double workHoursMultiplier, double nightHoursMultiplier) {
        double multiplier = 1.0;
        if (isHoliday(ts)) {
            multiplier *= holidayMultiplier;
        }
        if (isWorkHour(ts)) {
            multiplier *= workHoursMultiplier;
        } else if (isNightHour(ts)) {
            multiplier *= nightHoursMultiplier;
        }
        return multiplier;
    }

    public static int getHour(TimeZone tz, long ts) {
        return field(tz, ts, Calendar.HOUR_OF_DAY);
    }

    public static int getMinute(TimeZone tz, long ts) {
        return field(tz, ts, Calendar.MINUTE);
    }

    public static boolean isHoliday(long ts) {
        int dayOfWeek = field(null, ts, Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY;
    }

    public static boolean isWorkHour(long ts) {
        int hour = field(null, ts, Calendar.HOUR_OF_DAY);
        return hour > 8 && hour < 18;
    }

    /**
     * Note the gap: hours 6-8 and 18-21 are neither work nor night, so they take the plain
     * multiplier. That shoulder is deliberate — it is what PE does and it smooths the transition.
     */
    public static boolean isNightHour(long ts) {
        int hour = field(null, ts, Calendar.HOUR_OF_DAY);
        return hour < 6 || hour >= 22;
    }

    private static int field(TimeZone tz, long ts, int field) {
        Calendar c = tz != null ? Calendar.getInstance(tz) : Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c.get(field);
    }

}
