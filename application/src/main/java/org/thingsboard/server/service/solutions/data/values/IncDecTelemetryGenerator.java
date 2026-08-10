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

/** Shared state for the one-way strategies: walk from a start value towards an end value. */
public abstract class IncDecTelemetryGenerator<T extends IncDecValueStrategyDefinition> extends TelemetryGenerator {

    protected final T strategy;
    protected double value;
    protected double endValue;

    @SuppressWarnings("unchecked")
    protected IncDecTelemetryGenerator(TelemetryProfile profile) {
        super(profile);
        this.strategy = (T) profile.getValueStrategy();
        this.value = GeneratorTools.randomDouble(strategy.getMinStartValue(), strategy.getMaxStartValue());
        this.endValue = randomEndValue();
    }

    protected double randomEndValue() {
        return GeneratorTools.randomDouble(strategy.getMinEndValue(), strategy.getMaxEndValue());
    }

    protected void put(ObjectNode values) {
        put(values, value, strategy.getPrecision());
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public void setValue(double value) {
        this.value = value;
        // A handover starts a new run, so re-roll where this one should stop.
        this.endValue = randomEndValue();
    }

}
