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
package org.thingsboard.server.dao.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.sql.grouppermission.GroupPermissionRepository;
import org.thingsboard.server.dao.sql.role.RoleRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseRoleService implements RoleService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final RoleRepository roleRepository;
    private final CustomerDao customerDao;
    private final GroupPermissionRepository groupPermissionRepository;

    @Override
    public Role saveRole(TenantId tenantId, Role role) {
        if (StringUtils.isBlank(role.getName())) {
            throw new DataValidationException("Role name should be specified!");
        }
        if (role.getType() == null) {
            throw new DataValidationException("Role type should be specified!");
        }
        role.setTenantId(tenantId);
        validateCustomerId(tenantId, role);
        if (role.getId() == null) {
            role.setCreatedTime(System.currentTimeMillis());
        }
        boolean created = role.getId() == null;
        Role saved = roleRepository.save(new RoleEntity(role)).toData();
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId())
                .entity(saved).created(created).broadcastEvent(true).build());
        return saved;
    }

    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        return roleRepository.findById(roleId.getId()).map(RoleEntity::toData).orElse(null);
    }

    @Override
    public PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink) {
        ensureDefaultRoles(tenantId);
        return DaoUtil.toPageData(roleRepository.findByTenantId(
                tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        ensureDefaultRoles(tenantId);
        return DaoUtil.toPageData(roleRepository.findByTenantIdAndCustomerIdIncludingTenantLevel(
                tenantId.getId(), customerId.getId(), EntityId.NULL_UUID,
                pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Role> findRolesByCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        return DaoUtil.toPageData(roleRepository.findByTenantIdAndCustomerId(
                tenantId.getId(), customerId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void deleteRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        List<RoleEntity> entities = roleRepository.findAllByTenantIdAndCustomerId(tenantId.getId(), customerId.getId());
        if (entities.isEmpty()) {
            return;
        }
        List<Role> roles = entities.stream().map(RoleEntity::toData).toList();
        roleRepository.deleteAll(entities);
        roles.forEach(role -> eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId).entityId(role.getId()).entity(role).build()));
    }

    /**
     * A role may only be owned by a customer of its own tenant. Without this a tenant admin could
     * POST an arbitrary customerId and create a role that {@code resolveOwnerId} would then report
     * as belonging to another tenant's customer.
     */
    private void validateCustomerId(TenantId tenantId, Role role) {
        CustomerId customerId = role.getCustomerId();
        if (customerId == null || customerId.isNullUid()) {
            return;
        }
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Role is referencing to non-existent customer!");
        }
        if (!customer.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Role can't be assigned to customer from different tenant!");
        }
    }

    @Override
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        if (!groupPermissionRepository.findByTenantIdAndRoleId(tenantId.getId(), roleId.getId()).isEmpty()) {
            throw new DataValidationException("The role is referenced by group permissions and cannot be deleted!");
        }
        roleRepository.deleteById(roleId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(roleId).build());
    }

    @Override
    @Transactional
    public void deleteRolesByTenantId(TenantId tenantId) {
        List<RoleEntity> entities = roleRepository.findAllByTenantId(tenantId.getId());
        if (entities.isEmpty()) {
            return;
        }
        List<Role> roles = entities.stream().map(RoleEntity::toData).toList();
        roleRepository.deleteAll(entities);
        // Published like deleteRole does, so EDQS drops the roles too - otherwise an
        // entityType=ROLE query would keep returning rows SQL no longer has.
        roles.forEach(role -> eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId).entityId(role.getId()).entity(role).build()));
    }

    /** PE parity: names of the canonical share roles, matching PE's RoleServiceImpl. */
    private static final String READ_ONLY_ENTITY_GROUP_ROLE = "Entity Group Read-only User";
    private static final String WRITE_ENTITY_GROUP_ROLE = "Entity Group Write User";
    private static final String PUBLIC_USER_ENTITY_GROUP_ROLE = "Entity Group Public User";

    @Override
    public Role findOrCreateReadOnlyEntityGroupRole(TenantId tenantId) {
        // Mirrors PE's GroupPermission.READ_ONLY_GROUP_PERMISSIONS.
        return findOrCreateGroupRole(tenantId, READ_ONLY_ENTITY_GROUP_ROLE,
                "[\"READ\",\"RPC_CALL\",\"READ_CREDENTIALS\",\"READ_ATTRIBUTES\",\"READ_TELEMETRY\"]",
                "Autogenerated group role with read-only permissions.");
    }

    @Override
    public Role findOrCreateWriteEntityGroupRole(TenantId tenantId) {
        // Mirrors PE's GroupPermission.WRITE_GROUP_PERMISSIONS.
        return findOrCreateGroupRole(tenantId, WRITE_ENTITY_GROUP_ROLE, "[\"ALL\"]",
                "Autogenerated group role with write permissions.");
    }

    @Override
    public Role findOrCreatePublicUsersEntityGroupRole(TenantId tenantId) {
        // Mirrors PE's GroupPermission.PUBLIC_USER_ENTITY_GROUP_PERMISSIONS.
        return findOrCreateGroupRole(tenantId, PUBLIC_USER_ENTITY_GROUP_ROLE,
                "[\"READ\",\"RPC_CALL\",\"READ_ATTRIBUTES\",\"READ_TELEMETRY\"]",
                "Autogenerated Public User group role with read-only permissions.");
    }

    @Override
    public Role findOrCreateTenantAdminRole(TenantId tenantId) {
        return findOrCreateGenericRole(tenantId, TENANT_ADMIN_ROLE, "{\"ALL\":[\"ALL\"]}",
                "Autogenerated Tenant Administrator role with all permissions.");
    }

    @Override
    public Role findOrCreateCustomerUserRole(TenantId tenantId) {
        return findOrCreateGenericRole(tenantId, CUSTOMER_USER_ROLE,
                "{\"ALL\":[\"READ\",\"READ_ATTRIBUTES\",\"READ_TELEMETRY\",\"READ_CREDENTIALS\"]}",
                "Autogenerated Customer User role with read-only permissions.");
    }

    private Role findOrCreateGenericRole(TenantId tenantId, String name, String permissionsJson, String description) {
        ensureRole(tenantId, name, RoleType.GENERIC, permissionsJson, description);
        return findTenantLevelRole(tenantId, name)
                .map(RoleEntity::toData)
                .orElseThrow(() -> new DataValidationException("Failed to create role: " + name));
    }

    private Role findOrCreateGroupRole(TenantId tenantId, String name, String permissionsJson, String description) {
        ensureRole(tenantId, name, RoleType.GROUP, permissionsJson, description);
        return findTenantLevelRole(tenantId, name)
                .map(RoleEntity::toData)
                .orElseThrow(() -> new DataValidationException("Failed to create role: " + name));
    }

    public static final String TENANT_ADMIN_ROLE = "Tenant Administrator";
    public static final String CUSTOMER_USER_ROLE = "Customer User";

    // PE ships with three autogenerated roles per tenant; CE seeds them lazily on first list access.
    private void ensureDefaultRoles(TenantId tenantId) {
        ensureRole(tenantId, "Customer Administrator", RoleType.GENERIC,
                "{\"ALL\":[\"ALL\"]}",
                "Autogenerated Customer Administrator role with all permissions.");
        ensureRole(tenantId, CUSTOMER_USER_ROLE, RoleType.GENERIC,
                "{\"ALL\":[\"READ\",\"READ_ATTRIBUTES\",\"READ_TELEMETRY\",\"READ_CREDENTIALS\"]}",
                "Autogenerated Customer User role with read-only permissions.");
        ensureRole(tenantId, "Read Only", RoleType.GROUP,
                "[\"READ\",\"READ_ATTRIBUTES\",\"READ_TELEMETRY\"]", null);
    }

    /**
     * Looks up a role by name at tenant level only. Deliberately not findByTenantIdAndName: once a
     * customer can own a role, a customer-scoped role sharing a default's name would otherwise be
     * mistaken for the default and the tenant-level one would never be created.
     */
    private Optional<RoleEntity> findTenantLevelRole(TenantId tenantId, String name) {
        return roleRepository.findByTenantIdAndCustomerIdAndName(tenantId.getId(), EntityId.NULL_UUID, name);
    }

    private void ensureRole(TenantId tenantId, String name, RoleType type, String permissionsJson, String description) {
        if (findTenantLevelRole(tenantId, name).isEmpty()) {
            Role role = new Role();
            role.setTenantId(tenantId);
            role.setName(name);
            role.setType(type);
            role.setPermissions(JacksonUtil.toJsonNode(permissionsJson));
            if (description != null) {
                role.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
            }
            role.setCreatedTime(System.currentTimeMillis());
            try {
                // Must publish like saveRole does: writing straight through the repository would
                // leave EDQS unaware of the role, so an entityType=ROLE query would return fewer
                // rows from EDQS than from SQL.
                Role saved = roleRepository.save(new RoleEntity(role)).toData();
                eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId())
                        .entity(saved).created(true).broadcastEvent(true).build());
            } catch (Exception e) {
                log.debug("[{}] Concurrent default role creation for [{}], ignoring", tenantId, name);
            }
        }
    }

}
