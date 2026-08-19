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
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TbAvgIntervalState extends TbBaseIntervalState {

    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;

    public TbAvgIntervalState(JsonElement stateJson) {
        JsonObject jsonObject = stateJson.getAsJsonObject();
        this.sum = new BigDecimal(jsonObject.get("sum").getAsString());
        this.count = jsonObject.get("count").getAsLong();
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        BigDecimal value = data.getAsBigDecimal();
        if (value.compareTo(BigDecimal.ZERO) != 0) {
            sum = sum.add(value);
        }
        // Zero still counts as a sample, so the average always moves — always report.
        count++;
        return true;
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        // count == 0 only for a freshly constructed state that never saw a sample; PE divides
        // unguarded and throws ArithmeticException there.
        double avg = count == 0 ? 0d : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue();
        json.addProperty(outputValueKey, avg);
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("sum", sum.toString());
        object.addProperty("count", Long.toString(count));
        return gson.toJson(object);
    }

}
