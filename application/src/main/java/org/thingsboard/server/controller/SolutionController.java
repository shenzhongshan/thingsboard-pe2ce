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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.solutions.SolutionService;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.util.List;

/**
 * CE port of PE's solution templates API — browse, install and uninstall. Unlike PE there is no
 * subscription-level gate (CE has no licence tiers) and no edge provisioning.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/solutions")
@RequiredArgsConstructor
@Slf4j
public class SolutionController extends BaseController {

    public static final String SOLUTION_TEMPLATE_ID = "solutionTemplateId";

    private final SolutionService solutionService;

    @ApiOperation(value = "Get solution template infos (getSolutionTemplateInfos)",
            notes = "Lists the shipped solution templates, each flagged with whether this tenant installed it.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/templates/infos")
    public List<TenantSolutionTemplateInfo> getSolutionTemplateInfos() throws ThingsboardException {
        return solutionService.getSolutionInfos(getTenantId());
    }

    @ApiOperation(value = "Get solution template details (getSolutionTemplateDetails)",
            notes = "Returns screenshots and the highlights/description markdown for one template.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/templates/details/{solutionTemplateId}")
    public TenantSolutionTemplateDetails getSolutionTemplateDetails(
            @Parameter(description = "Solution template id", required = true)
            @PathVariable(SOLUTION_TEMPLATE_ID) String solutionTemplateId) throws ThingsboardException {
        checkParameter(SOLUTION_TEMPLATE_ID, solutionTemplateId);
        return solutionService.getSolutionDetails(getTenantId(), solutionTemplateId);
    }

    @ApiOperation(value = "Get solution template instructions (getSolutionTemplateInstructions)",
            notes = "Returns the post-install instructions stored for this tenant. " +
                    "Fails with 404 if the template is not installed.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/templates/instructions/{solutionTemplateId}")
    public TenantSolutionTemplateInstructions getSolutionTemplateInstructions(
            @Parameter(description = "Solution template id", required = true)
            @PathVariable(SOLUTION_TEMPLATE_ID) String solutionTemplateId) throws ThingsboardException {
        checkParameter(SOLUTION_TEMPLATE_ID, solutionTemplateId);
        return solutionService.getSolutionInstructions(getTenantId(), solutionTemplateId);
    }

    @ApiOperation(value = "Install solution template (installSolutionTemplate)",
            notes = "Creates every entity the template declares — rule chains, profiles, customers, " +
                    "assets, devices, dashboards, users and their groups. A failure part-way through " +
                    "rolls back everything already created. Returns the post-install instructions.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/templates/install/{solutionTemplateId}")
    public SolutionInstallResponse installSolutionTemplate(
            @Parameter(description = "Solution template id", required = true)
            @PathVariable(SOLUTION_TEMPLATE_ID) String solutionTemplateId) throws ThingsboardException {
        checkParameter(SOLUTION_TEMPLATE_ID, solutionTemplateId);
        return solutionService.installSolution(getCurrentUser(), getTenantId(), solutionTemplateId);
    }

    @ApiOperation(value = "Uninstall solution template (uninstallSolutionTemplate)",
            notes = "Deletes exactly the entities this tenant's install created and clears the install state.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/templates/uninstall/{solutionTemplateId}")
    public void uninstallSolutionTemplate(
            @Parameter(description = "Solution template id", required = true)
            @PathVariable(SOLUTION_TEMPLATE_ID) String solutionTemplateId) throws ThingsboardException {
        checkParameter(SOLUTION_TEMPLATE_ID, solutionTemplateId);
        solutionService.uninstallSolution(getTenantId(), solutionTemplateId);
    }

}
