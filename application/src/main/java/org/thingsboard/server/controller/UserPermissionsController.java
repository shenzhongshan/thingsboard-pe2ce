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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AllowedPermissionsInfo;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class UserPermissionsController extends BaseController {

    @ApiOperation(value = "Get Permissions (getAllowedPermissions)",
            notes = "Returns a complex object that describes:\n\n" +
                    " * all possible (both granted and not granted) permissions for the authority of the user (Tenant or Customer);\n" +
                    " * all granted permissions for the user;\n\n" +
                    "The result impacts UI behavior and hides certain UI elements if the user has no permissions to invoke " +
                    "the related operations. Nevertheless, all API calls check the permissions each time they are executed " +
                    "on the server side.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/permissions/allowedPermissions")
    public AllowedPermissionsInfo getAllowedPermissions() throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        Set<Resource> allowedResources = accessControlService.getAllowedResources(currentUser.getAuthority());
        Map<Resource, Set<Operation>> operationsByResource = new HashMap<>();
        allowedResources.forEach(resource -> operationsByResource.put(resource, Resource.operationsForResource(resource)));
        return new AllowedPermissionsInfo(
                operationsByResource,
                Operation.allowedForGroupRoleOperations,
                Operation.allowedForGroupOwnerOnlyOperations,
                Operation.allowedForGroupOwnerOnlyGroupOperations,
                allowedResources,
                currentUser.getUserPermissions(),
                currentUser.getOwnerId());
    }

    @ApiOperation(value = "Check permission for specified entity (hasEntityPermission)",
            notes = "Returns true if the user has permission to perform the operation, and false otherwise.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/permission/{entityType}/{entityId}/{operation}")
    public boolean hasEntityPermission(
            @Parameter(description = "A string value representing the entity type. For example, 'DEVICE'", required = true,
                    schema = @Schema(defaultValue = "DEVICE"))
            @PathVariable("entityType") EntityType entityType,
            @Parameter(description = "A string value representing the entity id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'", required = true)
            @PathVariable("entityId") String strEntityId,
            @Parameter(description = "A string value representing permission operation. For example, 'READ', 'WRITE', 'DELETE', 'CREATE', 'ALL'", required = true)
            @PathVariable("operation") Operation operation) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType.name(), strEntityId);
        try {
            checkEntityId(entityId, operation);
        } catch (ThingsboardException e) {
            return false;
        }
        return true;
    }

}
