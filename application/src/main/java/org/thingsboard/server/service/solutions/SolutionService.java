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
package org.thingsboard.server.service.solutions;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.util.List;

/**
 * CE port of PE's solution templates service. Templates are read from
 * {@code data/json/solutions}; per-tenant install state is kept in tenant server-scope
 * attributes ({@code <id>_status}, {@code <id>_instructions}, {@code <id>_entities}),
 * exactly as in PE.
 * <p>
 * Dropped vs PE: edge provisioning (no edge support in this port), the subscription-level gate,
 * and the telemetry emulators (a later stage — dashboards install fine without generated data).
 */
public interface SolutionService {

    /** All shipped templates, each flagged with whether this tenant installed it. */
    List<TenantSolutionTemplateInfo> getSolutionInfos(TenantId tenantId) throws ThingsboardException;

    /** Details (screenshots + highlights/description markdown) of one template. */
    TenantSolutionTemplateDetails getSolutionDetails(TenantId tenantId, String solutionId) throws ThingsboardException;

    /** Post-install instructions previously stored for this tenant, if the template is installed. */
    TenantSolutionTemplateInstructions getSolutionInstructions(TenantId tenantId, String solutionId) throws ThingsboardException;

    /**
     * Creates every entity the template declares. On any failure the partially created entities
     * are rolled back, so a failed install leaves nothing behind.
     */
    SolutionInstallResponse installSolution(User user, TenantId tenantId, String solutionId) throws ThingsboardException;

    /** Deletes exactly the entities this tenant's install created, then clears the install state. */
    void uninstallSolution(TenantId tenantId, String solutionId) throws ThingsboardException;

}
