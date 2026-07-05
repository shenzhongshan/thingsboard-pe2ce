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
 * CE port of the PE Custom Menu feature: multiple named menus per scope (SYSTEM/TENANT), each
 * optionally assigned to everyone (ALL = the scope's default menu), to specific customers, or to
 * specific users. CE simplifications: no CUSTOMER scope, no USER_GROUPS assignee type, no edge
 * sync, no ETag caching. Management endpoints are gated by the WHITE_LABELING permission (same
 * as PE); the effective-menu endpoint {@code GET /api/customMenu} keeps its original contract
 * and is consumed by the UI to build the navigation menu.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
public class CustomMenuController extends BaseController {

    @Autowired
    private CustomMenuService customMenuService;

    @ApiOperation(value = "Get Custom Menus (getCustomMenuInfos)",
            notes = "Returns a page of custom menus owned by the current scope: system menus for SYS_ADMIN, tenant menus for TENANT_ADMIN. Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = {"/customMenu/infos"})
    public PageData<CustomMenuInfo> getCustomMenuInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional scope filter", schema = @Schema(allowableValues = {"SYSTEM", "TENANT"}))
            @RequestParam(required = false) String scope,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "scope", "assigneeType"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        CMScope scopeFilter = scope != null ? CMScope.valueOf(scope) : null;
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return customMenuService.findCustomMenuInfos(getCurrentUser().getTenantId(), scopeFilter, pageLink);
    }

    @ApiOperation(value = "Create Custom Menu (createCustomMenu)",
            notes = "Creates a new custom menu with the given name, scope and assignee type, and an empty item tree. " +
                    "Only SYS_ADMIN may create SYSTEM scope menus. If assigneeType is ALL and a default menu already " +
                    "exists for the scope, the request fails unless force=true (which demotes the existing default to NO_ASSIGN). " +
                    "Optional assignToIds sets the initial assignees for CUSTOMERS/USERS assignee types. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = {"/customMenu"})
    public CustomMenu createCustomMenu(
            @Parameter(description = "A JSON value representing the custom menu metadata (name, scope, assigneeType)", required = true)
            @RequestBody CustomMenuInfo customMenuInfo,
            @Parameter(description = "A list of customer or user ids to assign the menu to (for CUSTOMERS/USERS assignee types)")
            @RequestParam(required = false) String[] assignToIds,
            @Parameter(description = "Replace the existing default menu of the same scope, if any")
            @RequestParam(required = false, defaultValue = "false") boolean force) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        SecurityUser user = getCurrentUser();
        if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
            customMenuInfo.setScope(CMScope.TENANT);
        }
        List<EntityId> assignToList = toAssigneeIds(customMenuInfo.getAssigneeType(), assignToIds);
        return customMenuService.createCustomMenu(user.getTenantId(), customMenuInfo, assignToList, force);
    }

    @ApiOperation(value = "Get Custom Menu info (getCustomMenuInfoById)",
            notes = "Returns the metadata (name, scope, assignee type) of the custom menu. Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = {"/customMenu/{customMenuId}/info"})
    public CustomMenuInfo getCustomMenuInfoById(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        return customMenuService.findCustomMenuInfoById(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)));
    }

    @ApiOperation(value = "Get Custom Menu configuration (getCustomMenuConfigById)",
            notes = "Returns the item tree of the custom menu, for the drag-and-drop editor. Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = {"/customMenu/{customMenuId}/config"})
    public CustomMenuConfig getCustomMenuConfigById(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        CustomMenu menu = customMenuService.findCustomMenuById(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)));
        return menu.getConfig() != null ? menu.getConfig() : new CustomMenuConfig();
    }

    @ApiOperation(value = "Update Custom Menu configuration (updateCustomMenuConfig)",
            notes = "Replaces the item tree of the custom menu. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(value = {"/customMenu/{customMenuId}/config"})
    public CustomMenu updateCustomMenuConfig(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "A JSON value representing the custom menu configuration", required = true)
            @RequestBody CustomMenuConfig config) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        return customMenuService.updateCustomMenuConfig(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)), config);
    }

    @ApiOperation(value = "Update Custom Menu name (updateCustomMenuName)",
            notes = "Renames the custom menu. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(value = {"/customMenu/{customMenuId}/name"})
    public CustomMenu updateCustomMenuName(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "The new name of the custom menu", required = true)
            @RequestParam String name) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        return customMenuService.updateCustomMenuName(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)), name);
    }

    @ApiOperation(value = "Assign Custom Menu (updateCustomMenuAssigneeList)",
            notes = "Changes who the custom menu applies to. Body: array of customer or user ids (required for CUSTOMERS/USERS, " +
                    "ignored otherwise). Previous assignees are always cleared first. Switching to ALL fails if another default " +
                    "menu exists for the scope, unless force=true. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(value = {"/customMenu/{customMenuId}/assign/{assigneeType}"})
    public void updateCustomMenuAssigneeList(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "The new assignee type", required = true, schema = @Schema(allowableValues = {"NO_ASSIGN", "ALL", "CUSTOMERS", "USERS"}))
            @PathVariable("assigneeType") String strAssigneeType,
            @Parameter(description = "A JSON array of customer or user ids to assign the menu to")
            @RequestBody(required = false) String[] assignToIds,
            @Parameter(description = "Replace the existing default menu of the same scope, if any")
            @RequestParam(required = false, defaultValue = "false") boolean force) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        checkParameter("assigneeType", strAssigneeType);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        CMAssigneeType assigneeType = CMAssigneeType.valueOf(strAssigneeType);
        List<EntityId> assignToList = toAssigneeIds(assigneeType, assignToIds);
        customMenuService.updateAssigneeList(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)), assigneeType, assignToList, force);
    }

    @ApiOperation(value = "Get Custom Menu assignees (getCustomMenuAssigneeList)",
            notes = "Returns the customers or users the custom menu is currently assigned to (empty for NO_ASSIGN/ALL). Requires WHITE_LABELING READ permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = {"/customMenu/{customMenuId}/assigneeList"})
    public List<EntityInfo> getCustomMenuAssigneeList(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.READ);
        return customMenuService.findCustomMenuAssigneeList(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)));
    }

    @ApiOperation(value = "Delete Custom Menu (deleteCustomMenu)",
            notes = "Deletes the custom menu. If the menu is still assigned to customers or users, the deletion is blocked " +
                    "(success=false, the assignees are returned) unless force=true, in which case the assignments are cleared " +
                    "and the menu deleted. Requires WHITE_LABELING WRITE permission.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = {"/customMenu/{customMenuId}"})
    public CustomMenuDeleteResult deleteCustomMenu(
            @Parameter(description = "A string value representing the custom menu id", required = true)
            @PathVariable("customMenuId") String strCustomMenuId,
            @Parameter(description = "Delete even if the menu is still assigned, clearing the assignments")
            @RequestParam(required = false, defaultValue = "false") boolean force) throws ThingsboardException {
        checkParameter("customMenuId", strCustomMenuId);
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE);
        return customMenuService.deleteCustomMenu(getCurrentUser().getTenantId(), new CustomMenuId(toUUID(strCustomMenuId)), force);
    }

    @ApiOperation(value = "Get effective Custom Menu (getCustomMenu)",
            notes = "Returns the Custom Menu configuration that applies to the current user, resolved as: the user's own " +
                    "assigned menu, then (for customer users) their customer's assigned menu, then the tenant default menu, " +
                    "then the system-provided tenant default, then empty. Consumed by the UI to build the navigation menu.")
    @PreAuthorize(value = "hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/customMenu"})
    public CustomMenuConfig getCustomMenu() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
            return customMenuService.resolveSystemAdminMenu();
        }
        CustomerId customerId = Authority.CUSTOMER_USER.equals(user.getAuthority()) ? user.getCustomerId() : null;
        return customMenuService.resolveUserMenu(user.getTenantId(), customerId, user.getId());
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
