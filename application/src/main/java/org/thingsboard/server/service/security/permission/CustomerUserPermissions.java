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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.EntityGroupService;
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
 */
@Component
public class CustomerUserPermissions extends AbstractPermissions {

    private final EntityGroupService entityGroupService;

    public CustomerUserPermissions(EntityGroupService entityGroupService) {
        super();
        this.entityGroupService = entityGroupService;
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
        put(Resource.MOBILE_APP_SETTINGS, new PermissionChecker.GenericPermissionChecker(Operation.READ));
        put(Resource.API_KEY, apiKeysPermissionChecker);
    }

    /** True if the user's MergedUserPermissions (generic, or group-scoped via the entity's groups) cover this operation. */
    private boolean hasGroupOrGenericPermission(SecurityUser user, Resource resource, Operation operation, EntityId entityId) {
        MergedUserPermissions perms = user.getUserPermissions();
        if (perms == null) {
            return false;
        }
        if (perms.hasGenericPermission(resource, operation)) {
            return true;
        }
        if (!operation.isAllowedForGroupRole()) {
            return false;
        }
        for (EntityGroupId groupId : entityGroupService.findEntityGroupsForEntity(user.getTenantId(), entityId, user.getCustomerId())) {
            if (perms.hasGroupPermissions(groupId, operation)) {
                return true;
            }
        }
        return false;
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
            return hasGroupOrGenericPermission(user, Resource.ALARM, operation, entityId);
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
                    boolean baseline = super.hasPermission(user, operation, entityId, entity)
                            && (operation.equals(Operation.CLAIM_DEVICES) || user.getCustomerId().equals(((HasCustomerId) entity).getCustomerId()));
                    if (baseline) {
                        return true;
                    }
                    return hasGroupOrGenericPermission(user, Resource.of(entityId.getEntityType()), operation, entityId);
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
                    return hasGroupOrGenericPermission(user, Resource.CUSTOMER, operation, entityId);
                }

            };

    private final PermissionChecker customerResourcePermissionChecker =
            new PermissionChecker<TbResourceId, TbResourceInfo>() {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, TbResourceId resourceId, TbResourceInfo resource) {
                    if (operation != Operation.READ) {
                        return false;
                    }
                    if (resource.getResourceType() == null || !resource.getResourceType().isCustomerAccess()) {
                        return false;
                    }
                    if (resource.getTenantId() == null || resource.getTenantId().isNullUid()) {
                        return true;
                    }
                    return user.getTenantId().equals(resource.getTenantId());
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
                    return hasGroupOrGenericPermission(user, Resource.DASHBOARD, operation, dashboardId);
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

            return hasGroupOrGenericPermission(user, Resource.USER, operation, userId);
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
