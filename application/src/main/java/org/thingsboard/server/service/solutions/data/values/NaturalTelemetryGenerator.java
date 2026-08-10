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

/** The workhorse: a temperature-like signal drifting up and down between moving bounds. */
public class NaturalTelemetryGenerator extends TelemetryGenerator {

    private final NaturalValueStrategyDefinition strategy;
    private double value;
    private boolean increment;
    private double lowValue;
    private double highValue;

    public NaturalTelemetryGenerator(TelemetryProfile profile) {
        super(profile);
        this.strategy = (NaturalValueStrategyDefinition) profile.getValueStrategy();
        this.value = GeneratorTools.randomDouble(strategy.getMinStartValue(), strategy.getMaxStartValue());
        this.lowValue = randomLowValue();
        this.highValue = randomHighValue();
        this.increment = !strategy.isDecrementOnStart();
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        double multiplier = GeneratorTools.getMultiplier(ts, strategy.getHolidayMultiplier(),
                strategy.getWorkHoursMultiplier(), strategy.getNightHoursMultiplier());
        if (increment) {
            value += GeneratorTools.randomDouble(strategy.getMinIncrement(), strategy.getMaxIncrement()) * multiplier;
            if (value > highValue) {
                // Turn around, and pick a fresh ceiling so successive peaks differ.
                value = highValue;
                highValue = randomHighValue();
                increment = false;
            }
        } else {
            value -= GeneratorTools.randomDouble(strategy.getMinDecrement(), strategy.getMaxDecrement()) * multiplier;
            if (value < lowValue) {
                value = lowValue;
                lowValue = randomLowValue();
                increment = true;
            }
        }
        put(values, value, strategy.getPrecision());
    }

    private double randomLowValue() {
        return GeneratorTools.randomDouble(strategy.getMinLowValue(), strategy.getMaxLowValue());
    }

    private double randomHighValue() {
        return GeneratorTools.randomDouble(strategy.getMinHighValue(), strategy.getMaxHighValue());
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
