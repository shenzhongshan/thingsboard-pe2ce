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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cf.OwnerService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's entity group API. Dropped vs PE: customer-owned groups/hierarchy, sharing,
 * group view configuration, edge groups. Group permissions are managed data only (no RBAC
 * enforcement).
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class EntityGroupController extends BaseController {

    public static final String ENTITY_GROUP_ID = "entityGroupId";

    private final EntityGroupService entityGroupService;
    private final UserPermissionsService userPermissionsService;
    private final CustomerService customerService;
    private final OwnerService ownerService;
    private final RoleService roleService;
    private final GroupPermissionService groupPermissionService;

    @ApiOperation(value = "Get Entity Group (getEntityGroupById)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroup/{entityGroupId}"})
    public EntityGroup getEntityGroupById(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        return checkAccess(checkNotNull(entityGroupService.findEntityGroupById(
                getTenantId(), new EntityGroupId(toUUID(strEntityGroupId)))));
    }

    @ApiOperation(value = "Save Entity Group (saveEntityGroup)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/entityGroup"})
    public EntityGroup saveEntityGroup(
            @Parameter(description = "A JSON value representing the entity group", required = true)
            @RequestBody EntityGroup entityGroup) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.WRITE);
        if (entityGroup.getId() != null) {
            checkAccess(checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), entityGroup.getId())));
        }
        return entityGroupService.saveEntityGroup(getTenantId(), entityGroup);
    }

    @ApiOperation(value = "Get Entity Groups by type (getEntityGroupsByType)",
            notes = "Lists the tenant's entity groups of the given type (USER, CUSTOMER, DEVICE, ASSET, ENTITY_VIEW or DASHBOARD). The 'All' group is created automatically.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroups/{groupType}"}, params = {"pageSize", "page"})
    public PageData<EntityGroup> getEntityGroupsByType(
            @Parameter(description = "Entity group type", required = true, schema = @Schema(allowableValues = {"USER", "CUSTOMER", "DEVICE", "ASSET", "ENTITY_VIEW", "DASHBOARD"}))
            @PathVariable("groupType") String strGroupType,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on group name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Optional customer id — lists the customer's own groups of the type instead of the tenant's")
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        checkParameter("groupType", strGroupType);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        CustomerId ownerId = StringUtils.isNotBlank(customerId) ? new CustomerId(toUUID(customerId)) : null;
        return entityGroupService.findEntityGroupsByType(getTenantId(), ownerId, EntityType.valueOf(strGroupType), pageLink);
    }

    @ApiOperation(value = "Get Entity Group Info (getEntityGroupInfoById)",
            notes = "Same as getEntityGroupById, additionally resolving the group's owner chain (ownerIds).")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/entityGroupInfo/{entityGroupId}"})
    public EntityGroupInfo getEntityGroupInfoById(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        EntityGroup entityGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(
                getTenantId(), new EntityGroupId(toUUID(strEntityGroupId)))));
        return toEntityGroupInfo(getTenantId(), entityGroup);
    }

    @ApiOperation(value = "Get Entity Group Infos by type (getEntityGroupInfosByType)",
            notes = "Same as getEntityGroupsByType, additionally resolving each group's owner chain (ownerIds).")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/entityGroupInfos/{groupType}"}, params = {"pageSize", "page"})
    public PageData<EntityGroupInfo> getEntityGroupInfosByType(
            @Parameter(description = "Entity group type", required = true, schema = @Schema(allowableValues = {"USER", "CUSTOMER", "DEVICE", "ASSET", "ENTITY_VIEW", "DASHBOARD"}))
            @PathVariable("groupType") String strGroupType,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on group name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Optional customer id — lists the customer's own groups of the type instead of the tenant's")
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        PageData<EntityGroup> groups = getEntityGroupsByType(strGroupType, pageSize, page, textSearch, sortProperty, sortOrder, customerId);
        TenantId tenantId = getTenantId();
        return groups.mapData(group -> toEntityGroupInfo(tenantId, group));
    }

    @ApiOperation(value = "Get Entity Group Infos for entity (getEntityGroupInfosForEntity)",
            notes = "Returns the groups (with owner chains) that the given entity belongs to, including implicit 'All' groups.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/entityGroupInfos/forEntity/{entityType}/{entityId}"})
    public List<EntityGroupInfo> getEntityGroupInfosForEntity(
            @Parameter(description = "Entity type", required = true)
            @PathVariable("entityType") String strEntityType,
            @Parameter(description = "Entity id", required = true)
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        List<EntityGroupId> groupIds = getEntityGroupsForEntity(strEntityType, strEntityId);
        TenantId tenantId = getTenantId();
        List<EntityGroupInfo> result = new ArrayList<>();
        for (EntityGroupId groupId : groupIds) {
            EntityGroup group = entityGroupService.findEntityGroupById(tenantId, groupId);
            if (group != null) {
                result.add(toEntityGroupInfo(tenantId, group));
            }
        }
        return result;
    }

    @ApiOperation(value = "Get Owner Info (getOwnerInfo)",
            notes = "Resolves an owner id (tenant or customer) to a name/id info object.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/ownerInfo/{ownerType}/{ownerId}"})
    public EntityInfo getOwnerInfo(
            @Parameter(description = "Owner type", required = true, schema = @Schema(allowableValues = {"TENANT", "CUSTOMER"}))
            @PathVariable("ownerType") String strOwnerType,
            @Parameter(description = "Owner id", required = true)
            @PathVariable("ownerId") String strOwnerId) throws ThingsboardException {
        checkParameter("ownerType", strOwnerType);
        checkParameter("ownerId", strOwnerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        if (EntityType.TENANT.name().equals(strOwnerType)) {
            TenantId ownerTenantId = TenantId.fromUUID(toUUID(strOwnerId));
            if (!getTenantId().equals(ownerTenantId)) {
                throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            Tenant tenant = checkNotNull(tenantService.findTenantById(ownerTenantId));
            return new EntityInfo(tenant.getId(), tenant.getName());
        }
        Customer customer = checkNotNull(customerService.findCustomerById(getTenantId(), new CustomerId(toUUID(strOwnerId))));
        return new EntityInfo(customer.getId(), customer.getName());
    }

    @ApiOperation(value = "Share Entity Group (shareEntityGroup)",
            notes = "Shares the entity group with a user group by creating GroupPermission(s). " +
                    "Target either a specific user group, or an owner's 'All' user group via " +
                    "'allUserGroup' + 'ownerId'. When 'roleIds' is omitted a canonical read-only or " +
                    "read-write group role is created on demand (see 'readElseWrite').")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = {"/entityGroup/{entityGroupId}/share"})
    @ResponseStatus(HttpStatus.OK)
    public void shareEntityGroup(
            @Parameter(description = "A string value representing the entity group id to share", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "Share request", required = true)
            @RequestBody ShareGroupRequest shareGroupRequest) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.CREATE);
        TenantId tenantId = getTenantId();
        EntityGroup entityGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(
                tenantId, new EntityGroupId(toUUID(strEntityGroupId)))));

        EntityGroup userGroup = resolveTargetUserGroup(tenantId, shareGroupRequest);
        List<RoleId> roleIds = resolveShareRoleIds(tenantId, shareGroupRequest);
        for (RoleId roleId : roleIds) {
            createGroupPermission(tenantId, entityGroup, userGroup.getId(), roleId);
        }
    }

    @ApiOperation(value = "Share Entity Group with user group and role (shareEntityGroupToUserGroup)",
            notes = "Path-parameter form of shareEntityGroup for a single explicit user group + role.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = {"/entityGroup/{entityGroupId}/{userGroupId}/{roleId}/share"})
    @ResponseStatus(HttpStatus.OK)
    public void shareEntityGroupToUserGroup(
            @Parameter(description = "A string value representing the entity group id to share", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "A string value representing the user group id to share with", required = true)
            @PathVariable("userGroupId") String strUserGroupId,
            @Parameter(description = "A string value representing the group role id to share with", required = true)
            @PathVariable("roleId") String strRoleId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkParameter("userGroupId", strUserGroupId);
        checkParameter("roleId", strRoleId);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.CREATE);
        TenantId tenantId = getTenantId();
        EntityGroup entityGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(
                tenantId, new EntityGroupId(toUUID(strEntityGroupId)))));
        EntityGroup userGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(
                tenantId, new EntityGroupId(toUUID(strUserGroupId)))));
        if (userGroup.getType() != EntityType.USER) {
            throw new ThingsboardException("Target group is not a user group!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        RoleId roleId = new RoleId(toUUID(strRoleId));
        checkNotNull(roleService.findRoleById(tenantId, roleId));
        createGroupPermission(tenantId, entityGroup, userGroup.getId(), roleId);
    }

    @ApiOperation(value = "Get shared Entity Groups by type (getSharedEntityGroupsByType)",
            notes = "Lists entity groups of the given type that have been shared with the current user " +
                    "via a group permission on one of the user groups the user belongs to. " +
                    "Groups the user already owns are excluded.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/entityGroups/{groupType}/shared"})
    public List<EntityGroupInfo> getSharedEntityGroupsByType(
            @Parameter(description = "Entity group type", required = true, schema = @Schema(allowableValues = {"USER", "CUSTOMER", "DEVICE", "ASSET", "ENTITY_VIEW", "DASHBOARD"}))
            @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        checkParameter("groupType", strGroupType);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        EntityType groupType = EntityType.valueOf(strGroupType);
        TenantId tenantId = getTenantId();
        EntityId userOwnerId = getCurrentUser().getOwnerId();

        // The user groups this user belongs to determine which shares apply to them.
        List<EntityGroupId> myUserGroupIds = entityGroupService.findEntityGroupsForEntity(
                tenantId, getCurrentUser().getId(), getCurrentUser().getCustomerId());

        Set<EntityGroupId> sharedGroupIds = new LinkedHashSet<>();
        for (EntityGroupId userGroupId : myUserGroupIds) {
            for (GroupPermission permission : groupPermissionService.findGroupPermissionsByTenantIdAndUserGroupId(tenantId, userGroupId)) {
                if (permission.getEntityGroupId() != null && groupType.equals(permission.getEntityGroupType())) {
                    sharedGroupIds.add(permission.getEntityGroupId());
                }
            }
        }

        List<EntityGroupInfo> result = new ArrayList<>();
        for (EntityGroupId groupId : sharedGroupIds) {
            EntityGroup group = entityGroupService.findEntityGroupById(tenantId, groupId);
            // Only groups owned by somebody else count as "shared with me".
            if (group != null && group.getOwnerId() != null && !group.getOwnerId().equals(userOwnerId)) {
                result.add(toEntityGroupInfo(tenantId, group));
            }
        }
        return result;
    }

    /** Resolves the user group a share targets: either explicit, or the owner's "All" user group. */
    private EntityGroup resolveTargetUserGroup(TenantId tenantId, ShareGroupRequest request) throws ThingsboardException {
        if (request.isAllUserGroup()) {
            EntityId ownerId = request.getOwnerId() != null ? request.getOwnerId() : tenantId;
            CustomerId customerOwnerId = ownerId instanceof CustomerId customerId && !customerId.isNullUid() ? customerId : null;
            EntityGroup allUserGroup = entityGroupService.findEntityGroupByTypeAndName(
                    tenantId, customerOwnerId, EntityType.USER, EntityGroup.GROUP_ALL_NAME);
            return checkNotNull(allUserGroup);
        }
        if (request.getUserGroupId() == null) {
            throw new ThingsboardException("Either 'allUserGroup' or 'userGroupId' must be specified!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        EntityGroup userGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(tenantId, request.getUserGroupId())));
        if (userGroup.getType() != EntityType.USER) {
            throw new ThingsboardException("Target group is not a user group!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return userGroup;
    }

    /** Explicit roleIds when given, otherwise the canonical read-only / write share role. */
    private List<RoleId> resolveShareRoleIds(TenantId tenantId, ShareGroupRequest request) {
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            return request.getRoleIds();
        }
        Role role = request.isReadElseWrite()
                ? roleService.findOrCreateReadOnlyEntityGroupRole(tenantId)
                : roleService.findOrCreateWriteEntityGroupRole(tenantId);
        return List.of(role.getId());
    }

    private void createGroupPermission(TenantId tenantId, EntityGroup entityGroup, EntityGroupId userGroupId, RoleId roleId) {
        // Idempotent: re-sharing the same group to the same user group with the same role is a
        // no-op rather than a duplicate row. (PE creates a duplicate here; identical permissions
        // grant identical access, so the extra rows are pure noise in the permissions UI.)
        boolean alreadyShared = groupPermissionService.findGroupPermissionsByTenantIdAndUserGroupId(tenantId, userGroupId)
                .stream()
                .anyMatch(existing -> roleId.equals(existing.getRoleId())
                        && entityGroup.getId().equals(existing.getEntityGroupId()));
        if (alreadyShared) {
            return;
        }
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setEntityGroupId(entityGroup.getId());
        groupPermission.setEntityGroupType(entityGroup.getType());
        groupPermission.setRoleId(roleId);
        groupPermission.setUserGroupId(userGroupId);
        GroupPermission saved = groupPermissionService.saveGroupPermission(tenantId, groupPermission);
        userPermissionsService.onGroupPermissionUpdated(saved);
    }

    /**
     * Resolves the group's owner chain so the UI can render ownership without extra calls.
     * Takes tenantId explicitly so this can be used from a lambda (getTenantId() is checked).
     */
    private EntityGroupInfo toEntityGroupInfo(TenantId tenantId, EntityGroup entityGroup) {
        EntityId ownerId = entityGroup.getOwnerId() != null ? entityGroup.getOwnerId() : tenantId;
        return new EntityGroupInfo(entityGroup, ownerService.getOwnerHierarchy(tenantId, ownerId));
    }

    @ApiOperation(value = "Delete Entity Group (deleteEntityGroup)",
            notes = "Deletes the entity group and its membership relations. The 'All' group cannot be deleted.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/entityGroup/{entityGroupId}"})
    public void deleteEntityGroup(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.DELETE);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkAccess(checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), entityGroupId)));
        entityGroupService.deleteEntityGroup(getTenantId(), entityGroupId);
    }

    @ApiOperation(value = "Add entities to Entity Group (addEntitiesToEntityGroup)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/entityGroup/{entityGroupId}/addEntities"})
    public void addEntitiesToEntityGroup(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "A JSON array of entity ids (strings)", required = true)
            @RequestBody String[] strEntityIds) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.WRITE);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), entityGroupId)));
        entityGroupService.addEntitiesToEntityGroup(getTenantId(), entityGroupId, toEntityIds(entityGroup, strEntityIds));
        if (entityGroup.getType() == EntityType.USER) {
            userPermissionsService.onUserGroupMembershipChanged(getTenantId(), entityGroupId);
        }
    }

    @ApiOperation(value = "Remove entities from Entity Group (removeEntitiesFromEntityGroup)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/entityGroup/{entityGroupId}/deleteEntities"})
    public void removeEntitiesFromEntityGroup(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "A JSON array of entity ids (strings)", required = true)
            @RequestBody String[] strEntityIds) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.WRITE);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkAccess(checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), entityGroupId)));
        if (entityGroup.getType() == EntityType.USER) {
            // Evict before removal — membership lookup after removal would miss the departing members.
            userPermissionsService.onUserGroupMembershipChanged(getTenantId(), entityGroupId);
        }
        entityGroupService.removeEntitiesFromEntityGroup(getTenantId(), entityGroupId, toEntityIds(entityGroup, strEntityIds));
    }

    @ApiOperation(value = "Get Entity Group entities (getEntityGroupEntities)",
            notes = "Lists members of the entity group as full entity objects (DeviceInfo, AssetInfo, Customer or User). " +
                    "For the 'All' group this is the full tenant entity list of the group's type.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroup/{entityGroupId}/entities"}, params = {"pageSize", "page"})
    public PageData<? extends HasName> getEntityGroupEntities(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on entity name")
            @RequestParam(required = false) String textSearch) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkAccess(checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), entityGroupId)));
        PageLink pageLink = createPageLink(pageSize, page, textSearch, null, null);
        return entityGroupService.findGroupEntities(getTenantId(), entityGroupId, pageLink);
    }

    @ApiOperation(value = "Make Entity Group public or private (makeEntityGroupPublic)",
            notes = "Stores the public flag on the group. CE simplification: cosmetic only — no public-customer permission machinery.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/entityGroup/{entityGroupId}/makePublic"})
    public EntityGroup makeEntityGroupPublic(
            @Parameter(description = "A string value representing the entity group id", required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @Parameter(description = "true to make public, false to make private")
            @RequestParam(defaultValue = "true") boolean isPublic) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.WRITE);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        checkAccess(checkNotNull(entityGroupService.findEntityGroupById(getTenantId(), entityGroupId)));
        return entityGroupService.makePublic(getTenantId(), entityGroupId, isPublic);
    }

    @ApiOperation(value = "Get entity group owners (getEntityGroupOwners)",
            notes = "Searches the tenant itself plus its customers by name, for use as an entity-group-owner target " +
                    "(PE parity: scheduler '实体组所有者' targeting).")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroup/owners"}, params = {"pageSize", "page"})
    public PageData<EntityInfo> getEntityGroupOwners(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on owner name")
            @RequestParam(required = false) String textSearch) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        List<EntityInfo> owners = new ArrayList<>();
        Tenant tenant = tenantService.findTenantById(getTenantId());
        if (tenant != null && (StringUtils.isBlank(textSearch)
                || tenant.getName().toLowerCase().contains(textSearch.toLowerCase()))) {
            owners.add(new EntityInfo(getTenantId(), tenant.getName()));
        }
        PageLink pageLink = createPageLink(pageSize, page, textSearch, "title", "ASC");
        owners.addAll(customerService.findCustomersByTenantId(getTenantId(), pageLink).getData().stream()
                .map(c -> new EntityInfo(c.getId(), c.getName()))
                .collect(Collectors.toList()));
        owners.sort(Comparator.comparing(EntityInfo::getName, String.CASE_INSENSITIVE_ORDER));
        int total = owners.size();
        int from = Math.min(page * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<EntityInfo> pageData = owners.subList(from, to);
        return new PageData<>(pageData, (total + pageSize - 1) / Math.max(pageSize, 1), total, to < total);
    }

    @ApiOperation(value = "Get or create the 'All' group id for an owner (getAllGroupIdForOwner)",
            notes = "Resolves the given owner's 'All' group of the specified type, creating it if needed. Omit " +
                    "ownerId (or pass the tenant id) for the tenant-level 'All' group. PE parity: scheduler " +
                    "'实体组所有者' targeting resolves to this group at save time.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroup/allGroupId/{groupType}"})
    public EntityGroupId getAllGroupIdForOwner(
            @Parameter(description = "Entity group type", required = true, schema = @Schema(allowableValues = {"USER", "CUSTOMER", "DEVICE", "ASSET"}))
            @PathVariable("groupType") String strGroupType,
            @Parameter(description = "Optional customer id — the customer's 'All' group instead of the tenant's")
            @RequestParam(required = false) String ownerId) throws ThingsboardException {
        checkParameter("groupType", strGroupType);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        CustomerId customerId = StringUtils.isNotBlank(ownerId) && !getTenantId().getId().toString().equals(ownerId) ?
                new CustomerId(toUUID(ownerId)) : null;
        return checkNotNull(entityGroupService.findOrCreateAllGroupId(getTenantId(), customerId, EntityType.valueOf(strGroupType)));
    }

    @ApiOperation(value = "Get entity groups for entity (getEntityGroupsForEntity)",
            notes = "Returns the ids of all Entity Groups (including implicit 'All' groups) the given entity belongs to.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroups/forEntity/{entityType}/{entityId}"})
    public List<EntityGroupId> getEntityGroupsForEntity(
            @Parameter(description = "Entity type", required = true)
            @PathVariable("entityType") String strEntityType,
            @Parameter(description = "A string value representing the entity id", required = true)
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter("entityType", strEntityType);
        checkParameter("entityId", strEntityId);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(strEntityType, toUUID(strEntityId));
        CustomerId customerId = getCurrentUser().getCustomerId();
        return entityGroupService.findEntityGroupsForEntity(getTenantId(), entityId, customerId);
    }

    private List<EntityId> toEntityIds(EntityGroup entityGroup, String[] strEntityIds) throws ThingsboardException {
        List<EntityId> entityIds = new ArrayList<>();
        for (String strId : strEntityIds) {
            entityIds.add(EntityIdFactory.getByTypeAndUuid(entityGroup.getType(), toUUID(strId)));
        }
        return entityIds;
    }

    private EntityGroup checkAccess(EntityGroup entityGroup) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(entityGroup.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return entityGroup;
    }

}
