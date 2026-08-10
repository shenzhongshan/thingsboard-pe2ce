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

/** Picks a sub-strategy per sample: office lights busy by day, idle at night and weekends. */
public class CompositeValueStrategyGenerator extends TelemetryGenerator {

    private final TelemetryGenerator defaultGenerator;
    private final TelemetryGenerator workHoursGenerator;
    private final TelemetryGenerator nightHoursGenerator;
    private final TelemetryGenerator holidayGenerator;

    public CompositeValueStrategyGenerator(TelemetryProfile profile) {
        super(profile);
        CompositeValueStrategyDefinition definition = (CompositeValueStrategyDefinition) profile.getValueStrategy();
        this.defaultGenerator = create(profile, definition.getDefaultHours());
        this.workHoursGenerator = create(profile, definition.getWorkHours());
        this.nightHoursGenerator = create(profile, definition.getNightHours());
        this.holidayGenerator = create(profile, definition.getHolidayHours());
    }

    private static TelemetryGenerator create(TelemetryProfile profile, ValueStrategyDefinition definition) {
        return definition != null ? TelemetryGeneratorFactory.create(new TelemetryProfile(profile.getKey(), definition)) : null;
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        // Holiday wins over night, night over work hours — same precedence as PE.
        if (holidayGenerator != null && GeneratorTools.isHoliday(ts)) {
            holidayGenerator.addValue(ts, values);
        } else if (nightHoursGenerator != null && GeneratorTools.isNightHour(ts)) {
            nightHoursGenerator.addValue(ts, values);
        } else if (workHoursGenerator != null && GeneratorTools.isWorkHour(ts)) {
            workHoursGenerator.addValue(ts, values);
        } else {
            defaultGenerator.addValue(ts, values);
        }
    }

}
