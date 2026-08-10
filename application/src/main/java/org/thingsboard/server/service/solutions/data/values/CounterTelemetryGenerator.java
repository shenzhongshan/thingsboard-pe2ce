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

/** A meter reading: rises steadily, wraps back to the start once it tops out. */
public class CounterTelemetryGenerator extends TelemetryGenerator {

    private final CounterValueStrategyDefinition strategy;
    private double value;

    public CounterTelemetryGenerator(TelemetryProfile profile) {
        super(profile);
        this.strategy = (CounterValueStrategyDefinition) profile.getValueStrategy();
        this.value = randomStartValue();
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        double step = GeneratorTools.randomDouble(strategy.getMinIncrement(), strategy.getMaxIncrement());
        double multiplier = GeneratorTools.getMultiplier(ts, strategy.getHolidayMultiplier(),
                strategy.getWorkHoursMultiplier(), strategy.getNightHoursMultiplier());
        value += step * multiplier;
        if (value > randomEndValue()) {
            value = randomStartValue();
        }
        put(values, value, strategy.getPrecision());
    }

    private double randomStartValue() {
        return GeneratorTools.randomDouble(strategy.getMinStartValue(), strategy.getMaxStartValue());
    }

    private double randomEndValue() {
        return GeneratorTools.randomDouble(strategy.getMinEndValue(), strategy.getMaxEndValue());
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public void setValue(double value) {
        this.value = value;
    }

}
