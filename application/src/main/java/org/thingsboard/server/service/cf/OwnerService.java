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
package org.thingsboard.server.service.cf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.user.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final DeviceService deviceService;
    private final AssetService assetService;
    private final CustomerService customerService;
    private final EntityViewService entityViewService;
    private final DashboardService dashboardService;
    private final UserService userService;
    private final RoleService roleService;
    private final EdgeService edgeService;

    public EntityId getOwner(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case DEVICE -> deviceService.findDeviceById(tenantId, (DeviceId) entityId).getOwnerId();
            case ASSET -> assetService.findAssetById(tenantId, (AssetId) entityId).getOwnerId();
            case EDGE -> edgeService.findEdgeById(tenantId, (EdgeId) entityId).getOwnerId();
            case ENTITY_VIEW -> ownerOf(entityViewService.findEntityViewById(tenantId, (EntityViewId) entityId).getCustomerId(), tenantId);
            // dashboard.customer_id is the owner. Deliberately NOT assignedCustomers: that is the
            // share list, a dashboard can be shared with several customers, and picking "the first"
            // of an unordered set gave a different answer than the SQL query engine's resolveOwnerId.
            case DASHBOARD -> ownerOf(dashboardService.findDashboardById(tenantId, (DashboardId) entityId).getCustomerId(), tenantId);
            case ROLE -> ownerOf(roleService.findRoleById(tenantId, (RoleId) entityId).getCustomerId(), tenantId);
            case USER -> ownerOf(userService.findUserById(tenantId, (UserId) entityId).getCustomerId(), tenantId);
            case CUSTOMER -> ownerOf(customerService.findCustomerById(tenantId, (CustomerId) entityId).getParentCustomerId(), tenantId);
            default -> throw new UnsupportedOperationException("Unsupported entity type for owner lookup: " + entityId.getEntityType());
        };
    }

    private EntityId ownerOf(CustomerId customerId, TenantId tenantId) {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    /**
     * The full owner chain of an entity: its direct owner, that owner's ancestors, and finally
     * the tenant. Used by EntityGroupInfo.ownerIds and by scope checks. PE calls this
     * OwnersCacheService#getOwners.
     */
    public Set<EntityId> getOwners(TenantId tenantId, EntityId entityId) {
        return getOwnerHierarchy(tenantId, getOwner(tenantId, entityId));
    }

    /**
     * The owner chain of an owner itself (tenant or customer), rather than of an owned entity.
     * Ordered nearest-first: the owner, then each parent in turn, ending at the tenant.
     */
    public Set<EntityId> getOwnerHierarchy(TenantId tenantId, EntityId ownerId) {
        Set<EntityId> owners = new LinkedHashSet<>();
        if (ownerId instanceof CustomerId customerId && !customerId.isNullUid()) {
            owners.add(customerId);
            // findCustomerAncestors returns root-first; reverse it so the chain stays
            // monotonic (nearest parent first) all the way up to the tenant.
            List<Customer> ancestors = new ArrayList<>(customerService.findCustomerAncestors(tenantId, customerId));
            Collections.reverse(ancestors);
            ancestors.forEach(ancestor -> owners.add(ancestor.getId()));
        }
        owners.add(tenantId);
        return owners;
    }

    /** All owners below the given one. Empty for a leaf customer; all customers for the tenant. */
    public Set<EntityId> getChildOwners(TenantId tenantId, EntityId ownerId) {
        if (ownerId instanceof CustomerId customerId && !customerId.isNullUid()) {
            return new LinkedHashSet<>(customerService.findCustomerDescendantIds(tenantId, customerId));
        }
        Set<EntityId> result = new LinkedHashSet<>();
        new PageDataIterable<>(pageLink -> customerService.findCustomersByTenantId(tenantId, pageLink), 1000)
                .forEach(customer -> result.add(customer.getId()));
        return result;
    }

    /**
     * True if {@code candidate} is at or below {@code ancestor} in the owner hierarchy.
     * A tenant is considered to be above every customer in that tenant.
     */
    public boolean isOwnedBy(TenantId tenantId, EntityId ancestor, EntityId candidate) {
        if (ancestor.equals(candidate)) {
            return true;
        }
        return getOwnerHierarchy(tenantId, candidate).contains(ancestor);
    }

    /**
     * Moves an entity between owners (tenant or customer). Delegates to the regular
     * assign/unassign paths, so the entity's "All" group relations move with it
     * (see EntityGroupService#moveAllGroupMembership).
     */
    public void changeOwner(TenantId tenantId, EntityId newOwnerId, EntityId entityId) {
        boolean toCustomer = EntityType.CUSTOMER.equals(newOwnerId.getEntityType());
        switch (entityId.getEntityType()) {
            case DEVICE -> {
                if (toCustomer) {
                    deviceService.assignDeviceToCustomer(tenantId, (DeviceId) entityId, (CustomerId) newOwnerId);
                } else {
                    deviceService.unassignDeviceFromCustomer(tenantId, (DeviceId) entityId);
                }
            }
            case ASSET -> {
                if (toCustomer) {
                    assetService.assignAssetToCustomer(tenantId, (AssetId) entityId, (CustomerId) newOwnerId);
                } else {
                    assetService.unassignAssetFromCustomer(tenantId, (AssetId) entityId);
                }
            }
            case EDGE -> {
                if (toCustomer) {
                    edgeService.assignEdgeToCustomer(tenantId, (EdgeId) entityId, (CustomerId) newOwnerId);
                } else {
                    edgeService.unassignEdgeFromCustomer(tenantId, (EdgeId) entityId);
                }
            }
            case ENTITY_VIEW -> {
                if (toCustomer) {
                    entityViewService.assignEntityViewToCustomer(tenantId, (EntityViewId) entityId, (CustomerId) newOwnerId);
                } else {
                    entityViewService.unassignEntityViewFromCustomer(tenantId, (EntityViewId) entityId);
                }
            }
            case DASHBOARD -> {
                // Ownership now lives in dashboard.customer_id, like every other entity. The
                // assignedCustomers set is deliberately left alone: it is the SHARE list, and a
                // dashboard may stay shared with customers other than its owner (PE keeps the same
                // split). Assigning to the new owner as well keeps it visible to them, since the
                // customer-user permission clause is still assignment-based.
                Dashboard dashboard = dashboardService.findDashboardById(tenantId, (DashboardId) entityId);
                dashboard.setCustomerId(toCustomer ? (CustomerId) newOwnerId : null);
                dashboardService.saveDashboard(dashboard);
                if (toCustomer && !dashboard.isAssignedToCustomer((CustomerId) newOwnerId)) {
                    dashboardService.assignDashboardToCustomer(tenantId, (DashboardId) entityId, (CustomerId) newOwnerId);
                }
            }
            case ROLE -> {
                Role role = roleService.findRoleById(tenantId, (RoleId) entityId);
                // NULL_UUID rather than null: tenant-level is a sentinel here, see Role#customerId.
                role.setCustomerId(toCustomer ? (CustomerId) newOwnerId : new CustomerId(CustomerId.NULL_UUID));
                roleService.saveRole(tenantId, role);
            }
            case USER -> {
                User user = userService.findUserById(tenantId, (UserId) entityId);
                user.setCustomerId(toCustomer ? (CustomerId) newOwnerId : new CustomerId(CustomerId.NULL_UUID));
                // doValidate=false: UserDataValidator forbids customerId changes on the normal
                // save path; an explicit owner change is exactly the sanctioned exception (PE
                // uses a dedicated userService.changeOwner for the same reason).
                userService.saveUser(tenantId, user, false);
            }
            case CUSTOMER -> {
                Customer customer = customerService.findCustomerById(tenantId, (CustomerId) entityId);
                if (customer.isPublic()) {
                    throw new IllegalArgumentException("Public customer owner can't be changed!");
                }
                if (toCustomer && isOwnedBy(tenantId, entityId, newOwnerId)) {
                    throw new IllegalArgumentException("Owner of the Customer can't be changed to its Sub-Customer!");
                }
                customer.setParentCustomerId(toCustomer ? (CustomerId) newOwnerId : null);
                customerService.saveCustomer(customer);
            }
            default -> throw new UnsupportedOperationException("Unsupported entity type for owner change: " + entityId.getEntityType());
        }
    }


    public Set<EntityId> getOwnedEntities(TenantId tenantId, EntityId ownerId) {
        Set<EntityId> ownedEntities = new HashSet<>();
        if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
            PageDataIterable<DeviceInfo> deviceIdInfos = new PageDataIterable<>(pageLink -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).customerId((CustomerId) ownerId).build(), pageLink), 1000);
            deviceIdInfos.forEach(deviceInfo -> ownedEntities.add(deviceInfo.getId()));

            PageDataIterable<Asset> assets = new PageDataIterable<>(pageLink -> assetService.findAssetsByTenantIdAndCustomerId(tenantId, (CustomerId) ownerId, pageLink), 1000);
            assets.forEach(asset -> ownedEntities.add(asset.getId()));
        } else if (EntityType.TENANT.equals(ownerId.getEntityType())) {
            PageDataIterable<DeviceInfo> deviceIdInfos = new PageDataIterable<>(pageLink -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId((TenantId) ownerId).customerId(new CustomerId(CustomerId.NULL_UUID)).build(), pageLink), 1000);
            deviceIdInfos.forEach(deviceInfo -> ownedEntities.add(deviceInfo.getId()));

            PageDataIterable<Asset> assets = new PageDataIterable<>(pageLink -> assetService.findAssetsByTenantIdAndCustomerId((TenantId) ownerId, new CustomerId(CustomerId.NULL_UUID), pageLink), 1000);
            assets.forEach(asset -> ownedEntities.add(asset.getId()));

            PageDataIterable<Customer> customers = new PageDataIterable<>(pageLink -> customerService.findCustomersByTenantId((TenantId) ownerId, pageLink), 1000);
            customers.forEach(customer -> ownedEntities.add(customer.getId()));
        }
        return ownedEntities;
    }

}
