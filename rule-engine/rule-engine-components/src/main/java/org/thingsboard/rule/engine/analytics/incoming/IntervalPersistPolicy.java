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

/** When the node emits the aggregated result of an interval downstream. */
public enum IntervalPersistPolicy {

    /** On every periodic check tick, whether or not the interval has ended. */
    ON_EACH_CHECK,
    /** On the first check tick after the interval's end, i.e. once per interval. */
    ON_EACH_CHECK_AFTER_INTERVAL_END,
    /** Immediately for every incoming message. */
    ON_EACH_MESSAGE

}
