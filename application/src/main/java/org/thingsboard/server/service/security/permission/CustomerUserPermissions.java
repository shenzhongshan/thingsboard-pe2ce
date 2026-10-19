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
package org.thingsboard.server.service.security.permission;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.service.cf.OwnerService;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * CE's CUSTOMER_USER checkers are augmented (on top of their existing PE-independent CE baseline
 * access, unchanged, see below) with real entity-group/role enforcement: a customer_user's default
 * access remains "anything owned by their own customer" (CE's original design), and GENERIC or
 * GROUP role permissions from {@link MergedUserPermissions} — resolved through the user's entity
 * -group memberships — can ADDITIONALLY grant operations/entities the baseline wouldn't otherwise
 * cover (e.g. an entity outside their own customer, explicitly shared via an entity group).
 * <p>
 * This is a deliberately additive enforcement model, not PE's literal deny-by-default algorithm:
 * PE's checker returns false when no generic/group permission matches at all, which only works
 * because PE auto-provisions a default tenant/customer role grant for every user. CE has no such
 * bootstrap step, and returning false-by-default here would silently lock every existing
 * tenant-admin/customer-user out the moment this port is deployed. Additive enforcement makes the
 * already-built Role/GroupPermission UI have real effect without that regression risk.
 * <p>
 * One limit on "additive": a GENERIC grant is confined to the user's own owner scope (their
 * customer and its sub-customers), matching PE. Only a GROUP grant may reach across owners, since
 * sharing an entity group is precisely how access is meant to be extended beyond one's own scope.
 * Without that distinction the stock "Customer User" role — which grants {@code {"ALL":["READ"]}}
 * — would let any customer user read every other customer's data in the tenant.
 */
@Component
public class CustomerUserPermissions extends AbstractPermissions {

    private final EntityGroupService entityGroupService;
    private final OwnerService ownerService;

    public CustomerUserPermissions(EntityGroupService entityGroupService, @Lazy OwnerService ownerService) {
        super();
        this.entityGroupService = entityGroupService;
        this.ownerService = ownerService;
        put(Resource.WHITE_LABELING, PermissionChecker.allowAllPermissionChecker);
        put(Resource.SCHEDULER_EVENT, PermissionChecker.allowAllPermissionChecker);
        put(Resource.ALARM, customerAlarmPermissionChecker);
        put(Resource.ASSET, customerEntityPermissionChecker);
        put(Resource.DEVICE, customerEntityPermissionChecker);
        put(Resource.CUSTOMER, customerPermissionChecker);
        put(Resource.DASHBOARD, customerDashboardPermissionChecker);
        put(Resource.ENTITY_VIEW, customerEntityPermissionChecker);
        put(Resource.USER, userPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.EDGE, customerEntityPermissionChecker);
        put(Resource.RPC, rpcPermissionChecker);
        put(Resource.DEVICE_PROFILE, profilePermissionChecker);
        put(Resource.ASSET_PROFILE, profilePermissionChecker);
        put(Resource.TB_RESOURCE, customerResourcePermissionChecker);
        put(Resource.ROLE, customerRolePermissionChecker);
        put(Resource.MOBILE_APP_SETTINGS, new PermissionChecker.GenericPermissionChecker(Operation.READ));
        put(Resource.API_KEY, apiKeysPermissionChecker);
        // Blobs are tenant/customer-scoped like devices and assets, so the same checker applies.
        put(Resource.BLOB_ENTITY, customerEntityPermissionChecker);
        // Entity groups were previously unreachable for customer users (no checker registered at
        // all), so every EntityGroupController call 403'd for them regardless of their roles.
        put(Resource.ENTITY_GROUP, entityGroupPermissionChecker);
        Resource.groupResources().forEach(groupResource -> put(groupResource, entityGroupPermissionChecker));
        put(Resource.PROFILE, PermissionChecker.allowAllPermissionChecker);
        // 报告由客户用户生成、查看和下载；删除是否允许由报告控制器继续校验“本人生成”。
        put(Resource.REPORT, new PermissionChecker.GenericPermissionChecker(
                Operation.READ, Operation.WRITE, Operation.DELETE));
        // 模板只开放读取，创建、修改和删除仍然由租户管理员负责。
        put(Resource.REPORT_TEMPLATE, new PermissionChecker.GenericPermissionChecker(Operation.READ));
    }

    /**
     * Entity groups are checked against the group itself, not against groups containing it, so this
     * cannot reuse {@link #hasGroupOrGenericPermission}: the GROUP branch consults the permission
     * granted directly on this group id, and the GENERIC branch uses the per-member-type resource
     * ({@link Resource#groupResourceFromGroupType}) so a role may grant device groups without also
     * granting user groups. Owner scoping matches every other entity — GENERIC only reaches the
     * user's own owner subtree, GROUP may cross owners because that is what sharing a group means.
     */
    private final PermissionChecker<EntityGroupId, EntityGroup> entityGroupPermissionChecker =
            new PermissionChecker<>() {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation) {
                    // No group in hand yet (list endpoints). Allow through if any group grant could
                    // apply; the per-group check below still runs once a group is resolved.
                    MergedUserPermissions perms = user.getUserPermissions();
                    if (perms == null) {
                        return false;
                    }
                    if (perms.hasGenericPermission(Resource.ENTITY_GROUP, operation)) {
                        return true;
                    }
                    for (Resource groupResource : Resource.groupResources()) {
                        if (perms.hasGenericPermission(groupResource, operation)) {
                            return true;
                        }
                    }
                    return operation.isAllowedForGroupRole() && !perms.getGroupPermissions().isEmpty();
                }

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, EntityGroupId entityGroupId, EntityGroup entityGroup) {
                    if (!user.getTenantId().equals(entityGroup.getTenantId())) {
                        return false;
                    }
                    MergedUserPermissions perms = user.getUserPermissions();
                    if (perms == null) {
                        return false;
                    }
                    if (operation.isAllowedForGroupRole() && perms.hasGroupPermissions(entityGroupId, operation)) {
                        return true;
                    }
                    EntityId ownerId = entityGroup.getOwnerId();
                    return ownerId != null
                            && ownerService.isOwnedBy(user.getTenantId(), user.getOwnerId(), ownerId)
                            && perms.hasGenericPermission(Resource.groupResourceFromGroupType(entityGroup.getType()), operation);
                }
            };

    /**
     * True if the user's MergedUserPermissions cover this operation, either generically or via a
     * group the entity belongs to.
     * <p>
     * {@code entityOwnerId} scopes the GENERIC branch: a generic grant only applies to entities at
     * or below the user's own owner in the customer hierarchy. Without that gate a customer user
     * holding a broad generic role (the stock "Customer User" role grants {@code {"ALL":["READ"]}})
     * could read every other customer's data in the tenant. PE applies the same rule — see its
     * customerStandaloneEntityPermissionChecker, which only consults hasGenericPermission when
     * {@code getOwners(entity).contains(user.getOwnerId())}.
     * <p>
     * The GROUP branch is intentionally NOT scoped: sharing an entity group across owners is
     * exactly what group permissions are for.
     *
     * @param entityOwnerId owner of the entity being checked, or null when it cannot be determined
     *                      (the generic branch is then skipped rather than assumed to apply)
     */
    private boolean hasGroupOrGenericPermission(SecurityUser user, Resource resource, Operation operation,
                                                EntityId entityId, EntityId entityOwnerId) {
        MergedUserPermissions perms = user.getUserPermissions();
        if (perms == null) {
            return false;
        }
        if (entityOwnerId != null
                && ownerService.isOwnedBy(user.getTenantId(), user.getOwnerId(), entityOwnerId)
                && perms.hasGenericPermission(resource, operation)) {
            return true;
        }
        if (!operation.isAllowedForGroupRole() || entityId == null) {
            // entityId is null for CREATE (the entity doesn't exist yet, so it can't already belong
            // to any group) - only the GENERIC branch above can grant permission in that case.
            return false;
        }
        for (EntityGroupId groupId : entityGroupService.findEntityGroupsForEntity(user.getTenantId(), entityId, user.getCustomerId())) {
            if (perms.hasGroupPermissions(groupId, operation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the {@link Resource} of an entity that has no id yet (a CREATE in flight, see
     * {@link #customerEntityPermissionChecker}) from its concrete class instead. Covers exactly the
     * entity types {@code customerEntityPermissionChecker} is registered for.
     */
    private Resource resourceOf(HasTenantId entity) {
        if (entity instanceof Device) {
            return Resource.DEVICE;
        } else if (entity instanceof Asset) {
            return Resource.ASSET;
        } else if (entity instanceof EntityView) {
            return Resource.ENTITY_VIEW;
        } else if (entity instanceof Edge) {
            return Resource.EDGE;
        } else if (entity instanceof BlobEntity) {
            return Resource.BLOB_ENTITY;
        }
        throw new IllegalArgumentException("Cannot resolve Resource for entity type: " + entity.getClass().getSimpleName());
    }

    /** The owner an entity belongs to: its customer when customer-assigned, else the tenant. */
    private EntityId ownerOf(SecurityUser user, Object entity) {
        if (entity instanceof HasCustomerId hasCustomerId) {
            CustomerId customerId = hasCustomerId.getCustomerId();
            return customerId != null && !customerId.isNullUid() ? customerId : user.getTenantId();
        }
        return null;
    }

    private final PermissionChecker customerAlarmPermissionChecker = new PermissionChecker() {
        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasCustomerId)) {
                return false;
            }
            if (user.getCustomerId().equals(((HasCustomerId) entity).getCustomerId())) {
                return true;
            }
            return hasGroupOrGenericPermission(user, Resource.ALARM, operation, entityId, ownerOf(user, entity));
        }
    };

    private final PermissionChecker customerEntityPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ, Operation.READ_CREDENTIALS,
                    Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.RPC_CALL, Operation.CLAIM_DEVICES,
                    Operation.WRITE, Operation.WRITE_ATTRIBUTES, Operation.WRITE_TELEMETRY) {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
                    if (!user.getTenantId().equals(entity.getTenantId())) {
                        return false;
                    }
                    if (!(entity instanceof HasCustomerId)) {
                        return false;
                    }
                    // Deny-by-default (PE semantics): owning the entity is no longer sufficient by
                    // itself. Access must come from a GENERIC role grant - which hasGroupOrGeneric
                    // Permission still confines to the user's own owner scope - or from a GROUP
                    // grant. Nobody starts with zero permissions: every user is placed in a default
                    // user group bound to a default role by
                    // EntityGroupService#ensureDefaultUserRoleMembership.
                    // entityId is null for CREATE (nothing persisted yet, see BaseController#checkEntity),
                    // so the entity type has to come from the entity instance itself instead.
                    Resource resource = entityId != null ? Resource.of(entityId.getEntityType()) : resourceOf(entity);
                    return hasGroupOrGenericPermission(user, resource, operation, entityId, ownerOf(user, entity));
                }
            };

    private final PermissionChecker customerPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
                    if (super.hasPermission(user, operation, entityId, entity) && user.getCustomerId().equals(entityId)) {
                        return true;
                    }
                    // A customer IS its own owner, so scoping resolves to "self or an ancestor of it".
                    return hasGroupOrGenericPermission(user, Resource.CUSTOMER, operation, entityId, entityId);
                }

            };

    /**
     * READ stays broad (any customer-accessible resource in the tenant, or a system one) so a
     * customer user can view images/resources referenced by dashboards shared with them, even when
     * those resources were uploaded by the tenant admin. CREATE/WRITE/DELETE are scoped to the
     * caller's own {@code customerId} — this is the customer-private image/resource library: a
     * customer user may only manage resources they themselves own, mirroring PE's per-customer
     * image scoping (see {@code ImageController#getImages} branching on {@code isCustomerUser()}).
     */
    /**
     * Mirrors PE's customerStandaloneEntityPermissionChecker for roles. The coarse "may this user
     * touch roles at all" answer comes from the generic grant, so a customer user with only the
     * default read-only Customer User role can list roles but not create them. The fine-grained
     * owner scoping (own roles, plus tenant-level ones for READ) is enforced here AND again in
     * RoleController#checkAccess - the controller alone is not enough, because an entity query can
     * reach roles without going through it.
     */
    private final PermissionChecker customerRolePermissionChecker =
            new PermissionChecker<RoleId, Role>() {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation) {
                    MergedUserPermissions perms = user.getUserPermissions();
                    return perms != null && perms.hasGenericPermission(Resource.ROLE, operation);
                }

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, RoleId roleId, Role role) {
                    if (!user.getTenantId().equals(role.getTenantId())) {
                        return false;
                    }
                    boolean owned = user.getCustomerId() != null && user.getCustomerId().equals(role.getCustomerId());
                    if (!owned && !(Operation.READ.equals(operation) && role.isTenantLevel())) {
                        return false;
                    }
                    return hasPermission(user, operation);
                }

            };

    private final PermissionChecker customerResourcePermissionChecker =
            new PermissionChecker<TbResourceId, TbResourceInfo>() {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, TbResourceId resourceId, TbResourceInfo resource) {
                    if (resource.getResourceType() == null || !resource.getResourceType().isCustomerAccess()) {
                        return false;
                    }
                    if (operation == Operation.READ) {
                        if (resource.getTenantId() == null || resource.getTenantId().isNullUid()) {
                            return true;
                        }
                        return user.getTenantId().equals(resource.getTenantId());
                    }
                    if (operation == Operation.CREATE || operation == Operation.WRITE || operation == Operation.DELETE) {
                        return resource.getTenantId() != null && user.getTenantId().equals(resource.getTenantId())
                                && resource.getCustomerId() != null && user.getCustomerId().equals(resource.getCustomerId());
                    }
                    return false;
                }

            };

    private final PermissionChecker customerDashboardPermissionChecker =
            new PermissionChecker.GenericPermissionChecker<DashboardId, DashboardInfo>(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, DashboardId dashboardId, DashboardInfo dashboard) {

                    if (!user.getTenantId().equals(dashboard.getTenantId())) {
                        return false;
                    }
                    if (super.hasPermission(user, operation, dashboardId, dashboard) && dashboard.isAssignedToCustomer(user.getCustomerId())) {
                        return true;
                    }
                    // A dashboard can be assigned to several customers; it counts as in-scope if any
                    // of them is at or below the user's owner. Unassigned (tenant-only) dashboards
                    // are out of scope for a customer user's generic grant.
                    EntityId dashboardOwnerId = null;
                    if (dashboard.getAssignedCustomers() != null) {
                        dashboardOwnerId = dashboard.getAssignedCustomers().stream()
                                .map(ShortCustomerInfo::getCustomerId)
                                .filter(customerId -> ownerService.isOwnedBy(user.getTenantId(), user.getOwnerId(), customerId))
                                .findFirst().orElse(null);
                    }
                    return hasGroupOrGenericPermission(user, Resource.DASHBOARD, operation, dashboardId, dashboardOwnerId);
                }

            };

    private final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            if (!Authority.CUSTOMER_USER.equals(userEntity.getAuthority())) {
                return false;
            }

            if (user.getCustomerId().equals(userEntity.getCustomerId())) {
                if (Operation.READ.equals(operation)) {
                    return true;
                }
                if (user.getId().equals(userId)) {
                    return true;
                }
            }

            return hasGroupOrGenericPermission(user, Resource.USER, operation, userId, ownerOf(user, userEntity));
        }

    };

    private final PermissionChecker widgetsPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }

    };

    private final PermissionChecker rpcPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }
    };

    private final PermissionChecker profilePermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }
    };

    private final PermissionChecker apiKeysPermissionChecker = new PermissionChecker<ApiKeyId, ApiKeyInfo>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation) {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, ApiKeyId entityId, ApiKeyInfo entity) {
            return user.getTenantId().equals(entity.getTenantId());
        }
    };

}
