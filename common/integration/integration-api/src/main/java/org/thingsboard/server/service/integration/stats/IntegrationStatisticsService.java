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
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

/**
 * Counts what integrations actually do, per integration type: how many started or failed to start,
 * and how many messages went up and down. Without this there is no way to answer "is this
 * integration working" other than reading logs.
 * <p>
 * Implementations are per host — the platform records into the metrics registry; a remote
 * integration process reports its numbers back over gRPC.
 */
public interface IntegrationStatisticsService {

    void onIntegrationStateUpdate(IntegrationType integrationType, ComponentLifecycleEvent state, boolean success);

    /** Current population of integrations of this type, split into started and failed. */
    void onIntegrationsCountUpdate(IntegrationType integrationType, int started, int failed);

    void onUplinkMsg(IntegrationType integrationType, boolean success);

    void onDownlinkMsg(IntegrationType integrationType, boolean success);

    /** Dumps the accumulated counters to the log; called on a schedule by the host. */
    void printStats();

    void reset();

}
