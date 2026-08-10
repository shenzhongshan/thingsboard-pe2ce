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
import lombok.Getter;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Produces the value(s) for one telemetry key at a given timestamp. Instances are stateful — most
 * strategies walk from the previous value — so one generator belongs to exactly one emulator.
 */
@Getter
public abstract class TelemetryGenerator {

    protected final TelemetryProfile profile;
    protected final String key;

    protected TelemetryGenerator(TelemetryProfile profile) {
        this.profile = profile;
        this.key = profile.getKey();
    }

    public abstract void addValue(long ts, ObjectNode values);

    /**
     * Only the numeric strategies support handover; a schedule strategy uses it to carry the
     * current level across a window boundary instead of jumping.
     */
    public double getValue() {
        throw new UnsupportedOperationException("Value handover is not supported by " + getClass().getSimpleName());
    }

    public void setValue(double value) {
        throw new UnsupportedOperationException("Value handover is not supported by " + getClass().getSimpleName());
    }

    protected void put(ObjectNode values, double value, int precision) {
        if (precision == 0) {
            values.put(key, (int) value);
        } else {
            values.put(key, BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).doubleValue());
        }
    }

}
