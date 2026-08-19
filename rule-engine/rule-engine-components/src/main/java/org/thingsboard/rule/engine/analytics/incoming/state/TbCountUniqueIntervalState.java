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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The only state that grows with the sample count — it must remember every distinct value seen in
 * the interval. Keep that in mind when choosing a long interval on a high-cardinality key.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TbCountUniqueIntervalState extends TbBaseIntervalState {

    private Set<String> items = ConcurrentHashMap.newKeySet();

    public TbCountUniqueIntervalState(JsonElement stateJson) {
        for (JsonElement jsonElement : stateJson.getAsJsonArray()) {
            String elementAsString = jsonElement.getAsString();
            try {
                // Canonicalise JSON-valued items so two spellings of the same object do not count
                // twice. PE additionally ran an unescapeJson pass to repair values double-escaped by
                // its own older releases; CE never shipped this node, so there is no such legacy
                // state to repair and the extra pass (plus a commons-text dependency) is dropped.
                JsonNode jsonNode = JacksonUtil.toJsonNode(elementAsString);
                items.add(JacksonUtil.writeValueAsString(jsonNode));
            } catch (Exception e) {
                items.add(elementAsString); // not JSON — keep the raw value
            }
        }
    }

    @Override
    protected boolean doUpdate(JsonElement data) {
        return items.add(data.getAsString()); // false when already seen, so nothing changed
    }

    @Override
    public String toValueJson(Gson gson, String outputValueKey) {
        JsonObject json = new JsonObject();
        json.addProperty(outputValueKey, items.size());
        return gson.toJson(json);
    }

    @Override
    public String toStateJson(Gson gson) {
        JsonArray array = new JsonArray();
        items.forEach(item -> array.add(new JsonPrimitive(item)));
        return gson.toJson(array);
    }

}
