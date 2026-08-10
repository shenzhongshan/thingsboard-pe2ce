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

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/** Runs a different strategy inside declared clock windows, e.g. irrigation at 06:00-08:00. */
public class ScheduleValueStrategyGenerator extends TelemetryGenerator {

    private final TimeZone timeZone;
    private final TelemetryGenerator defaultGenerator;
    private final Map<ValueStrategySchedule, TelemetryGenerator> scheduleGenerators = new LinkedHashMap<>();
    private TelemetryGenerator previousGenerator;

    public ScheduleValueStrategyGenerator(TelemetryProfile profile) {
        super(profile);
        ScheduleValueStrategyDefinition definition = (ScheduleValueStrategyDefinition) profile.getValueStrategy();
        this.timeZone = TimeZone.getTimeZone(definition.getTimeZone());
        this.defaultGenerator = TelemetryGeneratorFactory.create(
                new TelemetryProfile(profile.getKey(), definition.getDefaultDefinition()));
        for (ValueStrategySchedule window : definition.getSchedule()) {
            scheduleGenerators.put(window, TelemetryGeneratorFactory.create(
                    new TelemetryProfile(profile.getKey(), window.getDefinition())));
        }
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        int hour = GeneratorTools.getHour(timeZone, ts);
        int minute = GeneratorTools.getMinute(timeZone, ts);
        TelemetryGenerator generator = scheduleGenerators.entrySet().stream()
                .filter(entry -> covers(entry.getKey(), hour, minute))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaultGenerator);
        if (previousGenerator != null && previousGenerator != generator) {
            // Hand the level over so the series steps continuously across a window boundary.
            handOver(previousGenerator, generator);
        }
        generator.addValue(ts, values);
        previousGenerator = generator;
    }

    private static void handOver(TelemetryGenerator from, TelemetryGenerator to) {
        try {
            to.setValue(from.getValue());
        } catch (UnsupportedOperationException e) {
            // One side is non-numeric (e.g. constant); the new window simply starts fresh.
        }
    }

    private static boolean covers(ValueStrategySchedule window, int hour, int minute) {
        if (hour < window.getStartHour() || hour > window.getEndHour()) {
            return false;
        }
        if (hour == window.getStartHour() && minute < window.getStartMinute()) {
            return false;
        }
        return hour != window.getEndHour() || minute <= window.getEndMinute();
    }

}
