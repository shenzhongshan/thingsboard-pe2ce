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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.List;

/**
 * CE port of PE's group permission API.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class GroupPermissionController extends BaseController {

    public static final String GROUP_PERMISSION_ID = "groupPermissionId";

    private final GroupPermissionService groupPermissionService;
    private final UserPermissionsService userPermissionsService;
    private final EntityGroupService entityGroupService;
    private final RoleService roleService;

    @ApiOperation(value = "Save Group Permission (saveGroupPermission)",
            notes = "A customer user may only wire together a role, user group and entity group it " +
                    "owns (or a tenant-level role); checkGroupAccess/checkRoleAccess enforce that on " +
                    "every referenced id, not just the permission row's own tenant.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = {"/groupPermission"})
    public GroupPermission saveGroupPermission(
            @Parameter(description = "A JSON value representing the group permission", required = true)
            @RequestBody GroupPermission groupPermission) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.WRITE);
        if (groupPermission.getId() != null) {
            checkAccess(checkNotNull(groupPermissionService.findGroupPermissionById(getTenantId(), groupPermission.getId())));
        }
        checkRoleAccess(groupPermission.getRoleId(), Operation.READ);
        checkGroupAccess(groupPermission.getUserGroupId(), Operation.WRITE);
        if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
            checkGroupAccess(groupPermission.getEntityGroupId(), Operation.WRITE);
        }
        GroupPermission saved = groupPermissionService.saveGroupPermission(getTenantId(), groupPermission);
        userPermissionsService.onGroupPermissionUpdated(saved);
        return saved;
    }

    @ApiOperation(value = "Get user group permissions (getUserGroupPermissions)",
            notes = "Lists the permissions of a USER entity group, enriched with role name/type and target entity group name.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/userGroup/{userGroupId}/groupPermissions"})
    public List<JsonNode> getUserGroupPermissions(
            @Parameter(description = "A string value representing the user entity group id", required = true)
            @PathVariable("userGroupId") String strUserGroupId) throws ThingsboardException {
        checkParameter("userGroupId", strUserGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        EntityGroupId userGroupId = new EntityGroupId(toUUID(strUserGroupId));
        checkGroupAccess(userGroupId, Operation.READ);
        return groupPermissionService.findGroupPermissionInfosByUserGroupId(getTenantId(), userGroupId);
    }

    @ApiOperation(value = "Get entity group permissions (getEntityGroupPermissions)",
            notes = "Lists the permissions granted on a target entity group, enriched with role name/type and granting user group name/owner.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/entityGroup/{entityGroupId}/groupPermissions"})
    public List<JsonNode> getEntityGroupPermissions(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkGroupAccess(entityGroupId, Operation.READ);
        return groupPermissionService.findGroupPermissionInfosByEntityGroupId(getTenantId(), entityGroupId);
    }

    @ApiOperation(value = "Delete Group Permission (deleteGroupPermission)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = {"/groupPermission/{groupPermissionId}"})
    public void deleteGroupPermission(
            @Parameter(description = "A string value representing the group permission id", required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.DELETE);
        GroupPermissionId groupPermissionId = new GroupPermissionId(toUUID(strGroupPermissionId));
        GroupPermission existing = checkAccess(checkNotNull(groupPermissionService.findGroupPermissionById(getTenantId(), groupPermissionId)));
        checkRoleAccess(existing.getRoleId(), Operation.READ);
        checkGroupAccess(existing.getUserGroupId(), Operation.WRITE);
        if (existing.getEntityGroupId() != null && !existing.getEntityGroupId().isNullUid()) {
            checkGroupAccess(existing.getEntityGroupId(), Operation.WRITE);
        }
        groupPermissionService.deleteGroupPermission(getTenantId(), groupPermissionId);
        userPermissionsService.onGroupPermissionDeleted(existing);
    }

    private GroupPermission checkAccess(GroupPermission groupPermission) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(groupPermission.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return groupPermission;
    }

    /**
     * Tenant check plus the per-group permission check, same as {@code EntityGroupController}'s own
     * helper (duplicated rather than shared, matching this codebase's one-checkAccess-per-controller
     * style). Without this, a customer user could reach any group in the tenant — including a
     * sibling customer's — by id alone, since the coarse {@code Resource.GROUP_PERMISSION} gate at
     * the top of each endpoint only confirms the caller may use group permissions in general.
     */
    private EntityGroup checkGroupAccess(EntityGroupId groupId, Operation operation) throws ThingsboardException {
        EntityGroup group = checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), groupId));
        if (!getCurrentUser().getTenantId().equals(group.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        accessControlService.checkPermission(getCurrentUser(),
                Resource.groupResourceFromGroupType(group.getType()), operation, group.getId(), group);
        return group;
    }

    /** Mirrors {@code RoleController}'s own checkAccess: a customer user may reference a tenant-level
     * role (usable anywhere) or one it owns, never a sibling customer's. */
    private Role checkRoleAccess(RoleId roleId, Operation operation) throws ThingsboardException {
        Role role = checkNotNull(roleService.findRoleById(getTenantId(), roleId));
        SecurityUser user = getCurrentUser();
        if (!user.getTenantId().equals(role.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (user.isCustomerUser()) {
            boolean owned = user.getCustomerId().equals(role.getCustomerId());
            boolean readableTenantLevel = Operation.READ.equals(operation) && role.isTenantLevel();
            if (!owned && !readableTenantLevel) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
        return role;
    }

}
