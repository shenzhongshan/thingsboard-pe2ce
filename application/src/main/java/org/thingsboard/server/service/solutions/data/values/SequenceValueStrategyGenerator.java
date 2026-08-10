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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Replays a scripted series — used where the demo needs an exact, repeatable story. */
public class SequenceValueStrategyGenerator extends TelemetryGenerator {

    private final SequenceValueStrategyDefinition strategy;
    private final int length;
    private int index;

    public SequenceValueStrategyGenerator(TelemetryProfile profile) {
        super(profile);
        this.strategy = (SequenceValueStrategyDefinition) profile.getValueStrategy();
        // Every key holds an array of the same length; the first one sets the cycle.
        this.length = strategy.getTelemetry().fields().next().getValue().size();
        this.index = strategy.isRandom() && length > 0 ? ThreadLocalRandom.current().nextInt(length) : 0;
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        if (length == 0) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : strategy.getTelemetry().properties()) {
            JsonNode sequence = entry.getValue();
            values.set(entry.getKey(), sequence.get(index % sequence.size()));
        }
        index = (index + 1) % length;
    }

}
