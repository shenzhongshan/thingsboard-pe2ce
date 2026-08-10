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

/** A mostly-quiet signal, e.g. a smoke sensor that fires once in a while. */
public class EventTelemetryGenerator extends TelemetryGenerator {

    private final EventValueStrategyDefinition strategy;
    /** The draw that counts as an anomaly, fixed per generator so each device differs. */
    private final long anomalyDraw;

    public EventTelemetryGenerator(TelemetryProfile profile) {
        super(profile);
        this.strategy = (EventValueStrategyDefinition) profile.getValueStrategy();
        this.anomalyDraw = GeneratorTools.randomLong(0, strategy.getAnomalyChance());
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        boolean anomaly = GeneratorTools.randomLong(0, strategy.getAnomalyChance()) == anomalyDraw;
        Object value = anomaly ? strategy.getAnomalyValue() : strategy.getNormalValue();
        if (value instanceof Boolean booleanValue) {
            values.put(key, booleanValue);
        } else if (value instanceof Double doubleValue) {
            values.put(key, doubleValue);
        } else if (value instanceof Integer intValue) {
            values.put(key, intValue);
        } else if (value instanceof Number number) {
            values.put(key, number.doubleValue());
        } else {
            values.put(key, String.valueOf(value));
        }
    }

}
