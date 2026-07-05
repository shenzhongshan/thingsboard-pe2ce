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
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's role API. Roles are a managed data model only — CE's authority-based
 * security is not rewired to enforce them.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class RoleController extends BaseController {

    public static final String ROLE_ID = "roleId";

    private final RoleService roleService;

    @ApiOperation(value = "Get Role (getRoleById)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/role/{roleId}"})
    public Role getRoleById(
            @Parameter(description = "A string value representing the role id", required = true)
            @PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.READ);
        return checkAccess(checkNotNull(roleService.findRoleById(getTenantId(), new RoleId(toUUID(strRoleId)))));
    }

    @ApiOperation(value = "Save Role (saveRole)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/role"})
    public Role saveRole(
            @Parameter(description = "A JSON value representing the role", required = true)
            @RequestBody Role role) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.WRITE);
        if (role.getId() != null) {
            checkAccess(checkNotNull(roleService.findRoleById(getTenantId(), role.getId())));
        }
        return roleService.saveRole(getTenantId(), role);
    }

    @ApiOperation(value = "Get Roles (getRoles)",
            notes = "Lists the tenant's roles. The three PE default roles are created automatically.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/roles"}, params = {"pageSize", "page"})
    public PageData<Role> getRoles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on role name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return roleService.findRolesByTenantId(getTenantId(), pageLink);
    }

    @ApiOperation(value = "Delete Role (deleteRole)",
            notes = "Deletion fails if the role is referenced by group permissions.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/role/{roleId}"})
    public void deleteRole(
            @Parameter(description = "A string value representing the role id", required = true)
            @PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.DELETE);
        RoleId roleId = new RoleId(toUUID(strRoleId));
        checkAccess(checkNotNull(roleService.findRoleById(getTenantId(), roleId)));
        roleService.deleteRole(getTenantId(), roleId);
    }

    private Role checkAccess(Role role) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(role.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return role;
    }

}
