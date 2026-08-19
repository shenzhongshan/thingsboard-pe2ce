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
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The aggregation itself, plus the state round trip that makes it survive a restart.
 */
class TbIntervalStateTest {

    private static final Gson GSON = new Gson();
    private static final String KEY = "out";

    private static double value(TbIntervalState state) {
        return JsonParser.parseString(state.toValueJson(GSON, KEY)).getAsJsonObject().get(KEY).getAsDouble();
    }

    private static void feed(TbIntervalState state, double... values) {
        for (double v : values) {
            state.update(new JsonPrimitive(v));
        }
    }

    @Test
    void sumAddsUpAndRoundTrips() {
        TbSumIntervalState state = new TbSumIntervalState();
        feed(state, 1.5, 2.5, -1);
        assertThat(value(state)).isEqualTo(3.0);

        TbSumIntervalState restored = new TbSumIntervalState(JsonParser.parseString(state.toStateJson(GSON)));
        assertThat(value(restored)).isEqualTo(3.0);
        feed(restored, 7);
        assertThat(value(restored)).isEqualTo(10.0);
    }

    @Test
    void avgDividesBySampleCountIncludingZeroes() {
        TbAvgIntervalState state = new TbAvgIntervalState();
        feed(state, 10, 0, 20); // the zero still counts as a sample
        assertThat(value(state)).isEqualTo(10.0);

        TbAvgIntervalState restored = new TbAvgIntervalState(JsonParser.parseString(state.toStateJson(GSON)));
        assertThat(value(restored)).isEqualTo(10.0);
    }

    /** PE divides unguarded here and throws ArithmeticException. */
    @Test
    void avgWithoutSamplesReportsZeroRatherThanThrowing() {
        assertThat(value(new TbAvgIntervalState())).isZero();
    }

    @Test
    void minAndMaxTrackExtremes() {
        TbMinIntervalState min = new TbMinIntervalState();
        TbMaxIntervalState max = new TbMaxIntervalState();
        feed(min, 5, 2, 9);
        feed(max, 5, 2, 9);
        assertThat(value(min)).isEqualTo(2.0);
        assertThat(value(max)).isEqualTo(9.0);

        assertThat(value(new TbMinIntervalState(JsonParser.parseString(min.toStateJson(GSON))))).isEqualTo(2.0);
        assertThat(value(new TbMaxIntervalState(JsonParser.parseString(max.toStateJson(GSON))))).isEqualTo(9.0);
    }

    @Test
    void countCountsEverySampleAndUniqueCountsDistinctOnes() {
        TbCountIntervalState count = new TbCountIntervalState();
        feed(count, 1, 1, 1);
        assertThat(value(count)).isEqualTo(3.0);

        TbCountUniqueIntervalState unique = new TbCountUniqueIntervalState();
        feed(unique, 1, 1, 2);
        assertThat(value(unique)).isEqualTo(2.0);

        TbCountUniqueIntervalState restored =
                new TbCountUniqueIntervalState(JsonParser.parseString(unique.toStateJson(GSON)));
        assertThat(value(restored)).isEqualTo(2.0);
        feed(restored, 2); // already seen — must not inflate the count
        assertThat(value(restored)).isEqualTo(2.0);
    }

    /**
     * The report flag drives the outgoing message, so an update that cannot move the aggregate must
     * not raise it — otherwise every message re-emits an unchanged interval.
     */
    @Test
    void onlyMeaningfulUpdatesRaiseTheReportFlag() {
        TbMaxIntervalState max = new TbMaxIntervalState();
        feed(max, 10);
        max.clearChangesToReport();
        feed(max, 3); // below the running max
        assertThat(max.hasChangesToReport()).isFalse();
        feed(max, 42);
        assertThat(max.hasChangesToReport()).isTrue();

        TbSumIntervalState sum = new TbSumIntervalState();
        feed(sum, 5);
        sum.clearChangesToReport();
        feed(sum, 0); // adding zero changes nothing
        assertThat(sum.hasChangesToReport()).isFalse();

        TbCountUniqueIntervalState unique = new TbCountUniqueIntervalState();
        feed(unique, 7);
        unique.clearChangesToReport();
        feed(unique, 7); // duplicate
        assertThat(unique.hasChangesToReport()).isFalse();
    }

    /** Report and persist are cleared on different schedules, so they must not share a flag. */
    @Test
    void reportAndPersistFlagsAreIndependent() {
        TbCountIntervalState state = new TbCountIntervalState();
        feed(state, 1);
        state.clearChangesToReport();
        assertThat(state.hasChangesToReport()).isFalse();
        assertThat(state.hasChangesToPersist()).isTrue();

        state.clearChangesToPersist();
        assertThat(state.hasChangesToPersist()).isFalse();
    }

    /** A state restored from telemetry starts dirty so it is reported once even with no new data. */
    @Test
    void restoredStateStartsDirty() {
        TbCountIntervalState state = new TbCountIntervalState();
        feed(state, 1);
        TbCountIntervalState restored = new TbCountIntervalState(JsonParser.parseString(state.toStateJson(GSON)));
        assertThat(restored.hasChangesToReport()).isTrue();
        assertThat(restored.hasChangesToPersist()).isTrue();
    }

}
