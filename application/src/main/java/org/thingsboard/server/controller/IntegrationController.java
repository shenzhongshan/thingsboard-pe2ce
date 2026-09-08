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

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.thingsboard.common.util.JacksonUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final EventService eventService;

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
        ActionType actionType = integration.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        if (integration.getId() != null) {
            checkAccess(checkNotNull(integrationService.findIntegrationById(getTenantId(), integration.getId())));
        }
        try {
            Integration saved = integrationService.saveIntegration(integration);
            platformIntegrationService.onIntegrationUpdated(saved);
            logEntityActionService.logEntityAction(getTenantId(), saved.getId(), saved, actionType, getCurrentUser());
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION), integration,
                    actionType, getCurrentUser(), e);
            throw e;
        }
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

    @ApiOperation(value = "Get Integration Infos (getIntegrationInfos)",
            notes = "Same page of integrations as getIntegrations, plus each one's current runtime " +
                    "status and its per-hour message counts over the last 24 hours. Lets the " +
                    "integration list render status and the activity sparkline in a single request.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/integrationInfos"}, params = {"pageSize", "page"})
    public PageData<IntegrationInfo> getIntegrationInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on integration name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "enabled", "remote"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.READ);
        TenantId tenantId = getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        PageData<Integration> pageData = integrationService.findTenantIntegrations(tenantId, pageLink);
        List<IntegrationInfo> infos = pageData.getData().stream().map(IntegrationInfo::new).toList();
        // One aggregate query for the whole page rather than one per row.
        Map<UUID, ArrayNode> statsByEntityId = infos.isEmpty()
                ? Map.of()
                : eventService.findAggregatedDailyStats(tenantId, infos.stream().map(i -> i.getId().getId()).toList());
        for (IntegrationInfo info : infos) {
            info.setStatus(platformIntegrationService.getIntegrationInfoStatus(info));
            info.setStats(statsByEntityId.getOrDefault(info.getId().getId(), JacksonUtil.newArrayNode()));
        }
        return new PageData<>(infos, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    @ApiOperation(value = "Check integration connectivity (checkIntegrationConnection)",
            notes = "Opens a connection using the posted configuration and closes it again. Returns " +
                    "200 when the connection succeeded, or 400 with the underlying client's error " +
                    "message when it did not. The integration does not have to exist yet.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/integration/check"})
    public void checkIntegrationConnection(
            @Parameter(description = "A JSON value representing the integration", required = true)
            @RequestBody Integration integration) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, Operation.WRITE);
        checkNotNull(integration);
        integration.setTenantId(getTenantId());
        try {
            platformIntegrationService.checkConnection(integration);
        } catch (Exception e) {
            // The client's own message is the whole value of this endpoint ("Failed to connect to
            // MQTT broker at host:port ..."), so surface it rather than a generic 500.
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new ThingsboardException(message, ThingsboardErrorCode.GENERAL);
        }
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
        try {
            integrationService.deleteIntegration(getTenantId(), integrationId);
            platformIntegrationService.onIntegrationDeleted(integration);
            logEntityActionService.logEntityAction(getTenantId(), integrationId, integration, ActionType.DELETED,
                    getCurrentUser(), strIntegrationId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.INTEGRATION), ActionType.DELETED,
                    getCurrentUser(), e, strIntegrationId);
            throw e;
        }
    }

    private Integration checkAccess(Integration integration) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(integration.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return integration;
    }

}
