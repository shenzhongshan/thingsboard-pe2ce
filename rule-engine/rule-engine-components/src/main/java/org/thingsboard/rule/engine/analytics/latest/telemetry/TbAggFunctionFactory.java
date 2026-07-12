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

import org.thingsboard.rule.engine.analytics.incoming.MathFunction;

public final class TbAggFunctionFactory {

    private TbAggFunctionFactory() {
    }

    public static TbAggFunction createAggFunction(MathFunction mathFunction) {
        return switch (mathFunction) {
            case MIN -> new TbMinAggFunction();
            case MAX -> new TbMaxAggFunction();
            case SUM -> new TbSumAggFunction();
            case AVG -> new TbAvgAggFunction();
            case COUNT -> new TbCountAggFunction();
            case COUNT_UNIQUE -> new TbCountUniqueAggFunction();
        };
    }

}
