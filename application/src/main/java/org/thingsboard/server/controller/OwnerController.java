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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cf.OwnerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CE port of PE's owner-change API. Moves an entity between owners (tenant / customer).
 * Excludes PE's EDGE support, consistent with the rest of this port.
 */
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class OwnerController extends BaseController {

    public static final String OWNER_ID = "ownerId";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";

    private final OwnerService ownerService;
    private final EntityGroupService entityGroupService;

    @ApiOperation(value = "Change owner to tenant (changeOwnerToTenant)",
            notes = "Tenant changes Owner from Customer or sub-Customer back to the Tenant.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/owner/TENANT/{ownerId}/{entityType}/{entityId}")
    @ResponseStatus(HttpStatus.OK)
    public void changeOwnerToTenant(
            @Parameter(description = "A string value representing the tenant id.")
            @PathVariable(OWNER_ID) String ownerIdStr,
            @Parameter(description = "A string value representing the entity type. For example, 'DEVICE'")
            @PathVariable(ENTITY_TYPE) String entityType,
            @Parameter(description = "A string value representing the entity id.")
            @PathVariable(ENTITY_ID) String entityIdStr,
            @Parameter(description = "An optional list of additional entity group ids to add the entity to")
            @RequestBody(required = false) String[] strEntityGroupIds) throws ThingsboardException {
        checkParameter(OWNER_ID, ownerIdStr);
        checkParameter(ENTITY_TYPE, entityType);
        checkParameter(ENTITY_ID, entityIdStr);

        TenantId targetOwnerId = TenantId.fromUUID(UUID.fromString(ownerIdStr));
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);

        if (!getCurrentUser().getTenantId().equals(targetOwnerId)) {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        doChangeOwner(entityId, targetOwnerId, strEntityGroupIds);
    }

    @ApiOperation(value = "Change owner to customer (changeOwnerToCustomer)",
            notes = "Tenant/Customer changes Owner to a Customer or sub-Customer. " +
                    "A customer user may only target its own owner scope or a sub-customer of it.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/owner/CUSTOMER/{ownerId}/{entityType}/{entityId}")
    @ResponseStatus(HttpStatus.OK)
    public void changeOwnerToCustomer(
            @Parameter(description = "A string value representing the customer id.")
            @PathVariable(OWNER_ID) String ownerIdStr,
            @Parameter(description = "A string value representing the entity type. For example, 'DEVICE'")
            @PathVariable(ENTITY_TYPE) String entityType,
            @Parameter(description = "A string value representing the entity id.")
            @PathVariable(ENTITY_ID) String entityIdStr,
            @Parameter(description = "An optional list of additional entity group ids to add the entity to")
            @RequestBody(required = false) String[] strEntityGroupIds) throws ThingsboardException {
        checkParameter(OWNER_ID, ownerIdStr);
        checkParameter(ENTITY_TYPE, entityType);
        checkParameter(ENTITY_ID, entityIdStr);

        SecurityUser currentUser = getCurrentUser();
        CustomerId targetOwnerId = new CustomerId(UUID.fromString(ownerIdStr));
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);

        Customer targetOwner = customerService.findCustomerById(currentUser.getTenantId(), targetOwnerId);
        if (targetOwner == null) {
            throw new ThingsboardException("Target owner customer not found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        // A customer user may only move entities into its own scope or below it; a tenant admin
        // (whose ownerId is the tenant) sits above every customer in the tenant.
        if (!ownerService.isOwnedBy(currentUser.getTenantId(), currentUser.getOwnerId(), targetOwnerId)) {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        doChangeOwner(entityId, targetOwnerId, strEntityGroupIds);
    }

    private void doChangeOwner(EntityId entityId, EntityId targetOwnerId, String[] strEntityGroupIds) throws ThingsboardException {
        checkEntityId(entityId, Operation.CHANGE_OWNER);
        TenantId tenantId = getCurrentUser().getTenantId();
        List<EntityGroup> entityGroups = validateEntityGroupIds(strEntityGroupIds, entityId, targetOwnerId);
        try {
            ownerService.changeOwner(tenantId, targetOwnerId, entityId);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        for (EntityGroup entityGroup : entityGroups) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), entityId);
        }
        logEntityActionService.logEntityAction(tenantId, entityId, null, null, ActionType.UPDATED, getCurrentUser());
    }

    /** Resolves the optional group ids, checking each exists, matches the entity type and belongs to the target owner. */
    private List<EntityGroup> validateEntityGroupIds(String[] strEntityGroupIds, EntityId entityId, EntityId targetOwnerId) throws ThingsboardException {
        List<EntityGroup> result = new ArrayList<>();
        if (strEntityGroupIds == null || strEntityGroupIds.length == 0) {
            return result;
        }
        TenantId tenantId = getCurrentUser().getTenantId();
        for (String strEntityGroupId : strEntityGroupIds) {
            EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, new EntityGroupId(UUID.fromString(strEntityGroupId)));
            if (entityGroup == null) {
                throw new ThingsboardException("Entity group not found: " + strEntityGroupId, ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            if (entityGroup.getType() != entityId.getEntityType()) {
                throw new ThingsboardException("Entity group type doesn't match the entity type!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            if (!targetOwnerId.equals(entityGroup.getOwnerId())) {
                throw new ThingsboardException("Entity group is not owned by the target owner!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            result.add(entityGroup);
        }
        return result;
    }

}
