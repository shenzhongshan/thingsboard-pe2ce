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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.eventsourcing.GroupPermissionsEvictEvent;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CE port of PE's DefaultUserPermissionsService: resolves a user's entity-group memberships to
 * the GroupPermission rows granted to those groups, merges the referenced Roles' permissions into
 * a single {@link MergedUserPermissions}, and caches the result.
 *
 * CE simplification: uses a plain in-memory {@link ConcurrentHashMap} keyed by user id instead of
 * PE's TbTransactionalCache (Caffeine/Redis dual-cache) — correct for this monolith deployment,
 * consistent with other simplifications made throughout this port (e.g. the Scheduler's in-JVM
 * cluster notification). Also omitted: PE's public/anonymous-user permission resolution (no public
 * dashboard viewer feature in this port) and the UI-hint precomputed maps on MergedUserPermissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserPermissionsService implements UserPermissionsService {

    private final RoleService roleService;
    private final GroupPermissionService groupPermissionService;
    private final EntityGroupService entityGroupService;

    private final ConcurrentMap<UserId, MergedUserPermissions> cache = new ConcurrentHashMap<>();

    @Override
    public MergedUserPermissions getMergedPermissions(SecurityUser user, boolean isPublic) {
        if (user.getAuthority() == Authority.SYS_ADMIN) {
            return MergedUserPermissions.ALL;
        }
        if (isPublic) {
            return computeMergedPermissions(user, true);
        }
        return cache.computeIfAbsent(user.getId(), id -> computeMergedPermissions(user, false));
    }

    private MergedUserPermissions computeMergedPermissions(SecurityUser user, boolean isPublic) {
        TenantId tenantId = user.getTenantId();
        List<EntityGroupId> userGroups;
        if (isPublic) {
            // PE parity: the anonymous/public user is treated as a member of the public customer's
            // "Public Users" group, so the public GroupPermission(s) granted to that group resolve.
            EntityGroup publicUsersGroup = entityGroupService.findEntityGroupByTypeAndName(
                    tenantId, user.getCustomerId(), EntityType.USER, "Public Users");
            userGroups = publicUsersGroup != null ? List.of(publicUsersGroup.getId()) : Collections.emptyList();
        } else {
            userGroups = entityGroupService.findEntityGroupsForEntity(tenantId, user.getId(), user.getCustomerId());
        }

        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        Map<EntityGroupId, MergedGroupPermissionInfo> groupSpecificPermissions = new HashMap<>();

        for (EntityGroupId userGroupId : userGroups) {
            List<GroupPermission> groupPermissions = groupPermissionService.findGroupPermissionsByTenantIdAndUserGroupId(tenantId, userGroupId);
            for (GroupPermission gp : groupPermissions) {
                Role role = roleService.findRoleById(tenantId, gp.getRoleId());
                if (role == null) {
                    continue;
                }
                if (role.getType() == RoleType.GENERIC) {
                    addGenericRolePermissions(role, genericPermissions);
                } else {
                    addGroupSpecificRolePermissions(role, groupSpecificPermissions, gp);
                }
            }
        }
        return new MergedUserPermissions(genericPermissions, groupSpecificPermissions);
    }

    private void addGenericRolePermissions(Role role, Map<Resource, Set<Operation>> target) {
        JsonNode permissions = role.getPermissions();
        if (permissions == null || !permissions.isObject()) {
            return;
        }
        permissions.fields().forEachRemaining(entry -> {
            Resource resource;
            try {
                resource = Resource.valueOf(entry.getKey());
            } catch (IllegalArgumentException e) {
                return;
            }
            Set<Operation> ops = target.computeIfAbsent(resource, r -> new HashSet<>());
            entry.getValue().forEach(opNode -> {
                try {
                    ops.add(Operation.valueOf(opNode.asText()));
                } catch (IllegalArgumentException ignored) {
                }
            });
        });
    }

    private void addGroupSpecificRolePermissions(Role role, Map<EntityGroupId, MergedGroupPermissionInfo> target, GroupPermission gp) {
        JsonNode permissions = role.getPermissions();
        if (permissions == null || !permissions.isArray() || gp.getEntityGroupId() == null) {
            return;
        }
        Set<Operation> ops = new HashSet<>();
        permissions.forEach(opNode -> {
            try {
                ops.add(Operation.valueOf(opNode.asText()));
            } catch (IllegalArgumentException ignored) {
            }
        });
        target.computeIfAbsent(gp.getEntityGroupId(), id -> new MergedGroupPermissionInfo(gp.getEntityGroupType(), new HashSet<>()))
                .getOperations().addAll(ops);
    }

    @Override
    public void onRoleUpdated(Role role) {
        for (GroupPermission gp : groupPermissionService.findGroupPermissionsByTenantIdAndRoleId(role.getTenantId(), role.getId())) {
            evictUserGroup(role.getTenantId(), gp.getUserGroupId());
        }
    }

    @Override
    public void onGroupPermissionUpdated(GroupPermission groupPermission) {
        evictUserGroup(groupPermission.getTenantId(), groupPermission.getUserGroupId());
    }

    @Override
    public void onGroupPermissionDeleted(GroupPermission groupPermission) {
        evictUserGroup(groupPermission.getTenantId(), groupPermission.getUserGroupId());
    }

    @Override
    public void onUserGroupMembershipChanged(TenantId tenantId, EntityGroupId userGroupId) {
        evictUserGroup(tenantId, userGroupId);
    }

    @Override
    public void onUserUpdatedOrRemoved(User user) {
        cache.remove(user.getId());
    }

    /**
     * Handles the permission rows removed as a side effect of deleting an entity group.
     *
     * <p>A plain {@code @EventListener}, not a transactional one: the publisher fires this
     * <i>before</i> it deletes the group's membership relations, precisely so
     * {@link #evictUserGroup} can still enumerate the affected users. Running after commit would
     * find the memberships already gone and evict nothing.
     */
    @EventListener
    public void onGroupPermissionsEvict(GroupPermissionsEvictEvent event) {
        event.getUserGroupIds().forEach(userGroupId -> evictUserGroup(event.getTenantId(), userGroupId));
    }

    private void evictUserGroup(TenantId tenantId, EntityGroupId userGroupId) {
        entityGroupService.findGroupUserIds(tenantId, userGroupId).forEach(cache::remove);
    }

}
