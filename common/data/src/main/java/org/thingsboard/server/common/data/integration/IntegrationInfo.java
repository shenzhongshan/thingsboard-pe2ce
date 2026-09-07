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
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * An {@link Integration} plus the two pieces of runtime information the integration list needs but
 * that are not stored on the entity: the current runtime {@code status} and the {@code stats} series
 * behind the "daily activity" sparkline.
 *
 * <p>Wire format matches PE's {@code IntegrationInfo} so the same frontend table config works:
 * {@code status} is {@code {success: boolean, error?: string}} (absent entirely when the integration
 * has not reported yet - the UI renders that as "pending"), and {@code stats} is a flat array of
 * per-hour message counts over the last 24h.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class IntegrationInfo extends Integration {

    private static final long serialVersionUID = 4934987577236873728L;

    @Getter
    @Setter
    @Schema(description = "Current runtime status: {success: boolean, error?: string}. Absent when the " +
            "integration has not reported a status yet.")
    private transient ObjectNode status;

    @Getter
    @Setter
    @Schema(description = "Per-hour message counts over the last 24 hours, oldest first.")
    private transient ArrayNode stats;

    public IntegrationInfo() {
        super();
    }

    /**
     * The copy constructor carries the id and createdTime through {@link Integration}'s.
     *
     * <p>Strips the fields the list view never renders but that {@link Integration} would otherwise
     * serialize: {@code configuration} holds broker passwords, cloud access keys and client private
     * keys, and {@code secret}/{@code routingKey} together authenticate a remote integration. PE's
     * IntegrationInfo extends a base class that simply does not declare them; this build's extends
     * Integration, so they have to be cleared explicitly. Callers that genuinely need them use
     * GET /api/integration/{id}.
     */
    public IntegrationInfo(Integration integration) {
        super(integration);
        setConfiguration(null);
        setSecret(null);
        setRoutingKey(null);
    }

}
