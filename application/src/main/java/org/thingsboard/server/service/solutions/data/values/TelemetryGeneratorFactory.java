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

import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

public class TelemetryGeneratorFactory {

    private TelemetryGeneratorFactory() {
    }

    public static TelemetryGenerator create(TelemetryProfile profile) {
        return switch (profile.getValueStrategy().getStrategyType()) {
            case COUNTER -> new CounterTelemetryGenerator(profile);
            case NATURAL -> new NaturalTelemetryGenerator(profile);
            case EVENT -> new EventTelemetryGenerator(profile);
            case CONSTANT -> new ConstantTelemetryGenerator(profile);
            case SEQUENCE -> new SequenceValueStrategyGenerator(profile);
            case COMPOSITE -> new CompositeValueStrategyGenerator(profile);
            case SCHEDULE -> new ScheduleValueStrategyGenerator(profile);
            case INCREMENT -> new IncrementTelemetryGenerator(profile);
            case DECREMENT -> new DecrementTelemetryGenerator(profile);
        };
    }

}
