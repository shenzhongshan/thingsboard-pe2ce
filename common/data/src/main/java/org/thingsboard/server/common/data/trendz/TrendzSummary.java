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

import java.io.Serializable;
import java.util.List;

/** Opaque to ThingsBoard: the items are rendered by the Trendz UI, so they stay untyped. */
public record TrendzSummary(@JsonProperty("metricSummaryItems") List<Object> metricSummaryItems,
                           @JsonProperty("anomalyModelSummaryItems") List<Object> anomalyModelSummaryItems,
                           @JsonProperty("calculationFieldSummaryItems") List<Object> calculationFieldSummaryItems,
                           @JsonProperty("predictionModelSummaryItems") List<Object> predictionModelSummaryItems,
                           @JsonProperty("viewSummaryItems") List<Object> viewSummaryItems,
                           @JsonProperty("aiSummaryItems") List<Object> aiSummaryItems) implements Serializable {
}
