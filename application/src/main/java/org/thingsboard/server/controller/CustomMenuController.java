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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * Custom Menu feature: multiple named menus per scope (SYSTEM/TENANT/CUSTOMER), each optionally
 * assigned to everyone (ALL = the scope's default menu), to specific customers, or to specific
 * users. A menu is owned by the (tenant, customer) pair of the user that created it, so each
 * actor only ever sees and edits its own: the system administrator manages the three seeded
 * per-scope defaults, a tenant administrator manages TENANT and CUSTOMER scope menus for its
 * tenant, and a customer user manages CUSTOMER scope menus for its own customer.
 *
 * <p>Management endpoints are gated by the WHITE_LABELING permission; the effective-menu endpoint
 * {@code GET /api/customMenu} is consumed by the UI to build the navigation menu.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
public class CustomMenuController extends BaseController {

    @Autowired
    private CustomMenuService customMenuService;

    @ApiOperation(value = "Get Custom Menus (getCustomMenuInfos)",
            notes = "Returns a page of the custom menus owned by the current user's scope: system menus for SYS_ADMIN, " +
                    "the tenant's own menus for TENANT_ADMIN, the customer's own menus for CUSTOMER_USER. " +
                    "Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/customMenu/infos"})
    public PageData<CustomMenuInfo> getCustomMenuInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional scope filter", schema = @Schema(allowableValues = {"SYSTEM", "TENANT", "CUSTOMER"}))
            @RequestParam(required = false) String scope,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "scope", "assigneeType"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        CMScope scopeFilter = scope != null ? CMScope.valueOf(scope) : null;
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return customMenuService.findCustomMenuInfos(user.getTenantId(), ownerCustomerId(user), scopeFilter, pageLink);
    }

    @ApiOperation(value = "Create Custom Menu (createCustomMenu)",
            notes = "Creates a new custom menu with the given name, scope and assignee type, and an empty item tree. " +
                    "Only SYS_ADMIN may create SYSTEM scope menus and only a tenant administrator may create TENANT " +
                    "scope ones; a customer user may only create CUSTOMER scope menus for its own customer. " +
                    "If assigneeType is ALL and a default menu already exists for the scope, the request fails unless " +
                    "force=true (which demotes the existing default to NO_ASSIGN). Optional assignToIds sets the " +
                    "initial assignees for CUSTOMERS/USERS assignee types. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = {"/customMenu"})
    public CustomMenu createCustomMenu(
            @Parameter(description = "A JSON value representing the custom menu metadata (name, scope, assigneeType)", required = true)
            @RequestBody CustomMenuInfo customMenuInfo,
            @Parameter(description = "A list of customer or user ids to assign the menu to (for CUSTOMERS/USERS assignee types)")
            @RequestParam(required = false) String[] assignToIds,
            @Parameter(description = "Replace the existing default menu of the same scope, if any")
            @RequestParam(required = false, defaultValue = "false") boolean force) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.WRITE);
        if (customMenuInfo.getId() != null) {
            throw new ThingsboardException("Update is unsupported.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
            // A customer only ever configures the menu its own users see.
            customMenuInfo.setScope(CMScope.CUSTOMER);
        }
        List<EntityId> assignToList = toAssigneeIds(customMenuInfo.getAssigneeType(), assignToIds);
        return customMenuService.createCustomMenu(user.getTenantId(), ownerCustomerId(user), customMenuInfo, assignToList, force);
    }

    @ApiOperation(value = "Get Custom Menu info (getCustomMenuInfoById)",
            notes = "Returns the metadata (name, scope, assignee type) of the custom menu. Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/customMenu/{customMenuId}/info"})
    public CustomMenuInfo getCustomMenuInfoById(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        return checkOwner(user, customMenuService.findCustomMenuInfoById(user.getTenantId(), new CustomMenuId(toUUID(strCustomMenuId))));
    }

    @ApiOperation(value = "Get Custom Menu configuration (getCustomMenuConfigById)",
            notes = "Returns the item tree of the custom menu, for the drag-and-drop editor. Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/customMenu/{customMenuId}/config"})
    public CustomMenuConfig getCustomMenuConfigById(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        CustomMenu menu = checkOwner(user, customMenuService.findCustomMenuById(user.getTenantId(), new CustomMenuId(toUUID(strCustomMenuId))));
        return menu.getConfig() != null ? menu.getConfig() : new CustomMenuConfig();
    }

    @ApiOperation(value = "Update Custom Menu configuration (updateCustomMenuConfig)",
            notes = "Replaces the item tree of the custom menu. An empty item list means \"use the built-in menu of " +
                    "this scope\", which is also what the editor saves when the configuration is reset to default. " +
                    "Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = {"/customMenu/{customMenuId}/config"})
    public CustomMenu updateCustomMenuConfig(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "A JSON value representing the custom menu configuration", required = true)
            @RequestBody CustomMenuConfig config) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.WRITE);
        CustomMenuId customMenuId = new CustomMenuId(toUUID(strCustomMenuId));
        checkOwner(user, customMenuService.findCustomMenuInfoById(user.getTenantId(), customMenuId));
        return customMenuService.updateCustomMenuConfig(user.getTenantId(), customMenuId, config);
    }

    @ApiOperation(value = "Update Custom Menu name (updateCustomMenuName)",
            notes = "Renames the custom menu. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = {"/customMenu/{customMenuId}/name"})
    public CustomMenu updateCustomMenuName(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "The new name of the custom menu", required = true)
            @RequestParam String name) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.WRITE);
        CustomMenuId customMenuId = new CustomMenuId(toUUID(strCustomMenuId));
        checkOwner(user, customMenuService.findCustomMenuInfoById(user.getTenantId(), customMenuId));
        return customMenuService.updateCustomMenuName(user.getTenantId(), customMenuId, name);
    }

    @ApiOperation(value = "Assign Custom Menu (updateCustomMenuAssigneeList)",
            notes = "Changes who the custom menu applies to. Body: for CUSTOMERS/USERS an array of entity ids; for " +
                    "USER_GROUPS an array of entity-group names (e.g. \"Customer Administrators\", or a tenant/customer " +
                    "custom group name) — a user is matched if they directly belong to any group on the list, resolved " +
                    "by name rather than id since the same name is reused across owners. Ignored for NO_ASSIGN/ALL. " +
                    "Previous assignees are always cleared first. Switching to ALL fails if another default menu exists " +
                    "for the scope, unless force=true. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = {"/customMenu/{customMenuId}/assign/{assigneeType}"})
    public void updateCustomMenuAssigneeList(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "The new assignee type", required = true, schema = @Schema(allowableValues = {"NO_ASSIGN", "ALL", "CUSTOMERS", "USERS", "USER_GROUPS"}))
            @PathVariable("assigneeType") String strAssigneeType,
            @Parameter(description = "Customer/user ids (CUSTOMERS/USERS) or entity-group names (USER_GROUPS) to assign the menu to")
            @RequestBody(required = false) String[] assignToIds,
            @Parameter(description = "Replace the existing default menu of the same scope, if any")
            @RequestParam(required = false, defaultValue = "false") boolean force) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        checkParameter("assigneeType", strAssigneeType);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.WRITE);
        CustomMenuId customMenuId = new CustomMenuId(toUUID(strCustomMenuId));
        checkOwner(user, customMenuService.findCustomMenuInfoById(user.getTenantId(), customMenuId));
        CMAssigneeType assigneeType = CMAssigneeType.valueOf(strAssigneeType);
        List<EntityId> assignToList = toAssigneeIds(assigneeType, assignToIds);
        String[] userGroupNames = assigneeType == CMAssigneeType.USER_GROUPS ? assignToIds : null;
        customMenuService.updateAssigneeList(user.getTenantId(), customMenuId, assigneeType, assignToList, userGroupNames, force);
    }

    @ApiOperation(value = "Get Custom Menu assignees (getCustomMenuAssigneeList)",
            notes = "Returns the customers or users the custom menu is currently assigned to (empty for NO_ASSIGN/ALL). Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/customMenu/{customMenuId}/assigneeList"})
    public List<EntityInfo> getCustomMenuAssigneeList(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        CustomMenuId customMenuId = new CustomMenuId(toUUID(strCustomMenuId));
        checkOwner(user, customMenuService.findCustomMenuInfoById(user.getTenantId(), customMenuId));
        return customMenuService.findCustomMenuAssigneeList(user.getTenantId(), customMenuId);
    }

    @ApiOperation(value = "Delete Custom Menu (deleteCustomMenu)",
            notes = "Deletes the custom menu. The three seeded system default menus cannot be deleted. If the menu is " +
                    "still assigned to customers or users, the deletion is blocked (success=false, the assignees are " +
                    "returned) unless force=true, in which case the assignments are cleared and the menu deleted. " +
                    "Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = {"/customMenu/{customMenuId}"})
    public CustomMenuDeleteResult deleteCustomMenu(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "Delete even if the menu is still assigned, clearing the assignments")
            @RequestParam(required = false, defaultValue = "false") boolean force) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.WRITE);
        CustomMenuId customMenuId = new CustomMenuId(toUUID(strCustomMenuId));
        checkOwner(user, customMenuService.findCustomMenuInfoById(user.getTenantId(), customMenuId));
        return customMenuService.deleteCustomMenu(user.getTenantId(), customMenuId, force);
    }

    @ApiOperation(value = "Get effective Custom Menu (getCustomMenu)",
            notes = "Returns the Custom Menu configuration that applies to the current user. Hidden items are stripped. " +
                    "An empty item list means the built-in menu of the user's authority should be used. Resolution order: " +
                    "the user's own assigned menu, then (for customer users) the customer hierarchy, then the own " +
                    "tenant's default menu of the matching scope, then the system-provided default of that scope.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/customMenu"})
    public CustomMenuConfig getCustomMenu() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        UserId userId = user.getId();
        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
            return customMenuService.resolveSystemAdminMenu();
        }
        if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
            return customMenuService.resolveCustomerUserMenu(user.getTenantId(), user.getCustomerId(), userId);
        }
        return customMenuService.resolveTenantUserMenu(user.getTenantId(), userId);
    }

    /**
     * The (tenant, customer) owner of everything the current user creates and may manage. Tenant
     * administrators and the system administrator own at the tenant level (null-uid customer);
     * customer users own at their own customer.
     */
    private static CustomerId ownerCustomerId(SecurityUser user) {
        if (Authority.CUSTOMER_USER.equals(user.getAuthority()) && user.getCustomerId() != null
                && !user.getCustomerId().isNullUid()) {
            return user.getCustomerId();
        }
        return new CustomerId(CustomerId.NULL_UUID);
    }

    /**
     * Tenant-level checks alone are not enough once customers own menus: without this a customer
     * user could address its tenant's (or a sibling customer's) menu by id, since those rows share
     * the same tenant_id.
     */
    private <T extends CustomMenuInfo> T checkOwner(SecurityUser user, T menu) throws ThingsboardException {
        CustomerId owner = ownerCustomerId(user);
        CustomerId menuOwner = menu.getCustomerId() == null ? new CustomerId(CustomerId.NULL_UUID) : menu.getCustomerId();
        if (!owner.equals(menuOwner)) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return menu;
    }

    private List<EntityId> toAssigneeIds(CMAssigneeType assigneeType, String[] assignToIds) throws ThingsboardException {
        List<EntityId> assignToList = new ArrayList<>();
        if (assignToIds == null || assigneeType == null) {
            return assignToList;
        }
        for (String strId : assignToIds) {
            if (assigneeType == CMAssigneeType.CUSTOMERS) {
                assignToList.add(new CustomerId(toUUID(strId)));
            } else if (assigneeType == CMAssigneeType.USERS) {
                assignToList.add(new UserId(toUUID(strId)));
            }
        }
        return assignToList;
    }

}
