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

package org.thingsboard.server.common.data.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrendzUsage(@JsonProperty("used") boolean used,
                         @JsonProperty("anomalyUsage") Entity anomalyUsage,
                         @JsonProperty("predictionUsage") Entity predictionUsage,
                         @JsonProperty("calculationUsage") Entity calculationUsage,
                         @JsonProperty("viewUsage") SimpleEntity viewUsage,
                         @JsonProperty("metricUsage") SimpleEntity metricUsage,
                         @JsonProperty("chatUsage") SimpleEntity chatUsage) {

    public record Entity(@JsonProperty("used") boolean used,
                         @JsonProperty("activeCount") long activeCount,
                         @JsonProperty("totalCount") long totalCount) {
    }

    public record SimpleEntity(@JsonProperty("used") boolean used,
                               @JsonProperty("totalCount") long totalCount) {
    }

}
