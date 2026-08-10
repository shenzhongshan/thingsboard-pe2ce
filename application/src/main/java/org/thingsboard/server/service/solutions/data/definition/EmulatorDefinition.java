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
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A named telemetry generator that devices and assets opt into by name. Definitions form a shallow
 * inheritance chain via {@code extendz} so a template can declare, say, a base "WristBand" and then
 * a "Panic WristBand" that only overrides the keys that differ.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmulatorDefinition {

    private String name;
    /** Name of the definition to inherit unset fields and telemetry keys from. */
    private String extendz;
    /** Fully qualified emulator class; empty means the plain key-by-key generator. */
    private String clazz;
    private int publishPeriodInDays;
    private int publishFrequencyInSeconds;
    private int publishPauseInMillis;
    /** Written to the device as {@code inactivityTimeout} so it does not go stale after install. */
    private long activityPeriodInMillis;
    private List<TelemetryProfile> telemetryProfiles = Collections.emptyList();

    /** Fills in everything this definition left unset from its parent. Own keys always win. */
    public void enrich(EmulatorDefinition parent) {
        if (clazz == null || clazz.isEmpty()) {
            clazz = parent.getClazz();
        }
        if (publishPeriodInDays == 0) {
            publishPeriodInDays = parent.getPublishPeriodInDays();
        }
        if (publishFrequencyInSeconds == 0) {
            publishFrequencyInSeconds = parent.getPublishFrequencyInSeconds();
        }
        if (publishPauseInMillis == 0) {
            publishPauseInMillis = parent.getPublishPauseInMillis();
        }
        if (activityPeriodInMillis == 0L) {
            activityPeriodInMillis = parent.getActivityPeriodInMillis();
        }
        Map<String, TelemetryProfile> merged = new LinkedHashMap<>();
        telemetryProfiles.forEach(profile -> merged.put(profile.getKey(), profile));
        parent.getTelemetryProfiles().forEach(profile -> merged.putIfAbsent(profile.getKey(), profile));
        telemetryProfiles = new ArrayList<>(merged.values());
    }

    /** Timestamp of the first backfilled sample, i.e. how far into the past history reaches. */
    public long getOldestTs(long startTs) {
        return startTs - TimeUnit.DAYS.toMillis(publishPeriodInDays) - publishFrequencyInSeconds;
    }

}
