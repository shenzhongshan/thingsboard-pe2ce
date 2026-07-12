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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.Optional;

public abstract class TbBaseAggFunction implements TbAggFunction {

    private boolean hasResult = false;

    @Override
    public void update(Optional<KvEntry> entry, double defaultValue) {
        double value = extractDoubleValue(entry, defaultValue);
        doUpdate(value);
        hasResult = true;
    }

    @Override
    public Optional<JsonElement> result() {
        if (hasResult) {
            return Optional.of(new JsonPrimitive(prepareResult()));
        }
        return Optional.empty();
    }

    protected abstract void doUpdate(double value);

    protected abstract double prepareResult();

    private double extractDoubleValue(Optional<KvEntry> entry, double defaultValue) {
        double result = defaultValue;
        if (entry.isPresent() && entry.get().getValue() != null) {
            KvEntry kvEntry = entry.get();
            switch (kvEntry.getDataType()) {
                case LONG -> result = kvEntry.getLongValue().get();
                case DOUBLE -> result = kvEntry.getDoubleValue().get();
                case BOOLEAN -> result = Boolean.TRUE.equals(kvEntry.getBooleanValue().get()) ? 1.0 : 0.0;
                case STRING -> {
                    String str = kvEntry.getStrValue().get();
                    try {
                        result = Double.parseDouble(str);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Aggregation failed. Unable to parse value [" + str + "] of attribute [" + kvEntry.getKey() + "] to Double");
                    }
                }
                default -> { }
            }
        }
        return result;
    }

}
