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
package org.thingsboard.rule.engine.analytics.incoming;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * Length of one aggregation interval. MONTH and YEAR are nominal (30/365 days) — the node buckets
 * messages by {@code ts / interval}, so calendar-exact months are not attempted.
 */
@Getter
public enum AggIntervalType {

    HOUR(TimeUnit.HOURS),
    DAY(TimeUnit.DAYS),
    WEEK(DAY.getInterval() * 7L),
    WEEK_SUN_SAT(WEEK.getInterval()),
    MONTH(DAY.getInterval() * 30L),
    YEAR(DAY.getInterval() * 365L),
    /** Interval length comes from the node configuration instead. */
    CUSTOM(0L);

    private final long interval;

    AggIntervalType(long interval) {
        this.interval = interval;
    }

    AggIntervalType(TimeUnit timeUnit) {
        this.interval = timeUnit.toMillis(1L);
    }

}
