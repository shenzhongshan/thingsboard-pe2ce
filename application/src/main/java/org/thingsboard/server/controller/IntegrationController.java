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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's integration API. Dropped vs PE: remote integration endpoints, edge assignment,
 * integration infos with stats.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class IntegrationController extends BaseController {

    public static final String INTEGRATION_ID = "integrationId";

    private final IntegrationService integrationService;
    private final PlatformIntegrationService platformIntegrationService;

    @ApiOperation(value = "Get Integration (getIntegrationById)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/integration/{integrationId}"})
    public Integration getIntegrationById(
            @Parameter(description = "A string value representing the integration id", required = true)
            @PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        return checkAccess(checkNotNull(integrationService.findIntegrationById(
                getTenantId(), new IntegrationId(toUUID(strIntegrationId)))));
    }

    @ApiOperation(value = "Save Integration (saveIntegration)",
            notes = "Creates or updates an integration; the runtime is started/restarted accordingly.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/integration"})
    public Integration saveIntegration(
            @Parameter(description = "A JSON value representing the integration", required = true)
            @RequestBody Integration integration) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.WRITE);
        integration.setTenantId(getTenantId());
        if (integration.getId() != null) {
            checkAccess(checkNotNull(integrationService.findIntegrationById(getTenantId(), integration.getId())));
        }
        Integration saved = integrationService.saveIntegration(integration);
        platformIntegrationService.onIntegrationUpdated(saved);
        return saved;
    }

    @ApiOperation(value = "Get Integrations (getIntegrations)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/integrations"}, params = {"pageSize", "page"})
    public PageData<Integration> getIntegrations(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on integration name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return integrationService.findTenantIntegrations(getTenantId(), pageLink);
    }

    @ApiOperation(value = "Get Integration status (getIntegrationStatus)",
            notes = "Returns the runtime status of the integration: {status: 'ACTIVE'|'FAILED'|'STOPPED', error?}.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/integration/{integrationId}/status"})
    public com.fasterxml.jackson.databind.JsonNode getIntegrationStatus(
            @Parameter(description = "A string value representing the integration id", required = true)
            @PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        checkAccess(checkNotNull(integrationService.findIntegrationById(getTenantId(), integrationId)));
        return platformIntegrationService.getIntegrationStatus(integrationId);
    }

    @ApiOperation(value = "Delete Integration (deleteIntegration)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/integration/{integrationId}"})
    public void deleteIntegration(
            @Parameter(description = "A string value representing the integration id", required = true)
            @PathVariable(INTEGRATION_ID) String strIntegrationId) throws ThingsboardException {
        checkParameter(INTEGRATION_ID, strIntegrationId);
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.DELETE);
        IntegrationId integrationId = new IntegrationId(toUUID(strIntegrationId));
        Integration integration = checkAccess(checkNotNull(integrationService.findIntegrationById(getTenantId(), integrationId)));
        integrationService.deleteIntegration(getTenantId(), integrationId);
        platformIntegrationService.onIntegrationDeleted(integration);
    }

    private Integration checkAccess(Integration integration) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(integration.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return integration;
    }

}
