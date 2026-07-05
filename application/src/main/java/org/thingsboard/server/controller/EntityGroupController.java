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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;

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
            notes = "Lists the tenant's entity groups of the given type (USER, CUSTOMER, DEVICE or ASSET). The 'All' group is created automatically.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/entityGroups/{groupType}"}, params = {"pageSize", "page"})
    public PageData<EntityGroup> getEntityGroupsByType(
            @Parameter(description = "Entity group type", required = true, schema = @Schema(allowableValues = {"USER", "CUSTOMER", "DEVICE", "ASSET"}))
            @PathVariable("groupType") String strGroupType,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on group name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("groupType", strGroupType);
        accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_GROUP, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return entityGroupService.findEntityGroupsByType(getTenantId(), EntityType.valueOf(strGroupType), pageLink);
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
