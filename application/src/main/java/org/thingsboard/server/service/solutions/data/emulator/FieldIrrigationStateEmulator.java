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
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Backfills a week of completed irrigation runs for the smart irrigation template, one per day at
 * 06:00 local time. The figures are fixed rather than generated: the dashboard shows them against
 * duration and consumption thresholds, so the story only reads correctly with known values.
 */
public class FieldIrrigationStateEmulator implements CustomEmulator {

    private static final int START_HOUR = 6;
    private static final String TIME_ZONE = "America/New_York";
    private static final long HALF_HOUR_MS = 1_800_000L;

    private final List<Pair<Long, ObjectNode>> data = new ArrayList<>();
    private int index;

    @Override
    public void init(EmulatorDefinition definition) {
        index = 0;
        data.clear();
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone(TIME_ZONE));
        day.setTimeInMillis(System.currentTimeMillis());
        // Step back a week, landing on today's run only once 06:00 has already passed.
        day.add(Calendar.DAY_OF_MONTH, day.get(Calendar.HOUR_OF_DAY) < START_HOUR ? -7 : -6);
        day.set(Calendar.HOUR_OF_DAY, START_HOUR);
        day.set(Calendar.MINUTE, 0);

        addRun(day, task(day).put("durationThreshold", HALF_HOUR_MS).put("consumption", 423).put("duration", HALF_HOUR_MS));
        addRun(day, task(day).put("durationThreshold", HALF_HOUR_MS).put("consumption", 407).put("duration", HALF_HOUR_MS));
        addRun(day, task(day).put("consumptionThreshold", 1000).put("consumption", 1000).put("duration", 3_780_000L));
        addRun(day, task(day).put("consumptionThreshold", 500).put("consumption", 500).put("duration", 2_160_000L));
        addRun(day, task(day).put("consumptionThreshold", 1000).put("consumption", 1000).put("duration", 3_780_000L));
        addRun(day, task(day).put("durationThreshold", HALF_HOUR_MS).put("duration", HALF_HOUR_MS).put("consumption", 452));
        addRun(day, task(day).put("durationThreshold", HALF_HOUR_MS).put("duration", HALF_HOUR_MS).put("consumption", 447));
    }

    private static ObjectNode task(Calendar day) {
        return JacksonUtil.newObjectNode().put("startTs", day.getTimeInMillis());
    }

    private void addRun(Calendar day, ObjectNode irrigationTask) {
        ObjectNode message = JacksonUtil.newObjectNode();
        message.put("irrigationState", "DONE");
        message.set("irrigationTask", irrigationTask);
        data.add(Pair.of(day.getTimeInMillis(), message));
        day.add(Calendar.DAY_OF_MONTH, 1);
    }

    @Override
    public Pair<Long, ObjectNode> getNextValue() {
        return index < data.size() ? data.get(index++) : null;
    }

}
