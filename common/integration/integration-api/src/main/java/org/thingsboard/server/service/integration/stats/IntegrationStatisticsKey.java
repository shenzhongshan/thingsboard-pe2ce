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
package org.thingsboard.server.service.integration.stats;

import org.thingsboard.server.common.data.integration.IntegrationType;

/**
 * Identifies one metric series: a metric name, whether it counts successes or failures, and the
 * integration type it belongs to. Doubles as the map key of the counter/gauge registry and as the
 * tag set handed to the metrics backend.
 */
public record IntegrationStatisticsKey(IntegrationStatisticsMetricName metricName,
                                       boolean success,
                                       IntegrationType integrationType) {

    /** Tags in the flat key/value form the {@code StatsFactory} expects. */
    public String[] getTags() {
        return new String[]{
                "metric", metricName.getName(),
                "status", success ? "success" : "failure",
                "integrationType", integrationType.name()
        };
    }

    @Override
    public String toString() {
        return integrationType.name() + "." + metricName.getName() + "." + (success ? "success" : "failure");
    }

}
