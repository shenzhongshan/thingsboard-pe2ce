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

/**
 * Running aggregate for one (entity, interval) pair. Implementations keep only the accumulator they
 * need, so a long interval costs a few fields rather than the whole sample set — except
 * COUNT_UNIQUE, which must remember the distinct values.
 * <p>
 * Two dirty flags are tracked separately: "report" drives the outgoing message, "persist" drives
 * writing the state back as telemetry. They are cleared independently because the two happen on
 * different schedules.
 */
public interface TbIntervalState {

    void update(JsonElement value);

    boolean hasChangesToPersist();

    void clearChangesToPersist();

    boolean hasChangesToReport();

    void clearChangesToReport();

    /** The aggregate itself, as the payload of an outgoing message. */
    String toValueJson(Gson gson, String outputValueKey);

    /** The accumulator, in the shape the matching constructor can read back. */
    String toStateJson(Gson gson);

}
