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
package org.thingsboard.server.dao.grouppermission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.eventsourcing.GroupPermissionsEvictEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.sql.grouppermission.GroupPermissionRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseGroupPermissionService implements GroupPermissionService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final GroupPermissionRepository groupPermissionRepository;
    private final RoleService roleService;
    private final EntityGroupService entityGroupService;

    @Override
    public GroupPermission saveGroupPermission(TenantId tenantId, GroupPermission groupPermission) {
        if (groupPermission.getUserGroupId() == null) {
            throw new DataValidationException("Group permission user group should be specified!");
        }
        if (groupPermission.getRoleId() == null) {
            throw new DataValidationException("Group permission role should be specified!");
        }
        Role role = roleService.findRoleById(tenantId, groupPermission.getRoleId());
        if (role == null) {
            throw new DataValidationException("Group permission is referencing a non-existent role!");
        }
        if (RoleType.GROUP.equals(role.getType()) && groupPermission.getEntityGroupId() == null) {
            throw new DataValidationException("Entity group should be specified for a GROUP-type role!");
        }
        groupPermission.setTenantId(tenantId);
        if (groupPermission.getId() == null) {
            groupPermission.setCreatedTime(System.currentTimeMillis());
        }
        boolean created = groupPermission.getId() == null;
        GroupPermission saved = groupPermissionRepository.save(new GroupPermissionEntity(groupPermission)).toData();
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId())
                .entity(saved).created(created).broadcastEvent(true).build());
        return saved;
    }

    @Override
    public GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId) {
        return groupPermissionRepository.findById(groupPermissionId.getId()).map(GroupPermissionEntity::toData).orElse(null);
    }

    @Override
    public List<JsonNode> findGroupPermissionInfosByUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        List<JsonNode> result = new ArrayList<>();
        for (GroupPermissionEntity entity : groupPermissionRepository.findByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId())) {
            GroupPermission gp = entity.toData();
            ObjectNode info = (ObjectNode) JacksonUtil.valueToTree(gp);
            Role role = roleService.findRoleById(tenantId, gp.getRoleId());
            if (role != null) {
                info.put("roleName", role.getName());
                info.put("roleType", role.getType().name());
            }
            if (gp.getEntityGroupId() != null) {
                EntityGroup group = entityGroupService.findEntityGroupById(tenantId, gp.getEntityGroupId());
                if (group != null) {
                    info.put("entityGroupName", group.getName());
                }
            }
            result.add(info);
        }
        return result;
    }

    @Override
    public List<JsonNode> findGroupPermissionInfosByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        List<JsonNode> result = new ArrayList<>();
        for (GroupPermissionEntity entity : groupPermissionRepository.findByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId())) {
            GroupPermission gp = entity.toData();
            ObjectNode info = (ObjectNode) JacksonUtil.valueToTree(gp);
            Role role = roleService.findRoleById(tenantId, gp.getRoleId());
            if (role != null) {
                info.put("roleName", role.getName());
                info.put("roleType", role.getType().name());
            }
            if (gp.getUserGroupId() != null) {
                EntityGroup userGroup = entityGroupService.findEntityGroupById(tenantId, gp.getUserGroupId());
                if (userGroup != null) {
                    info.put("userGroupName", userGroup.getName());
                    info.put("userGroupOwnerName", userGroup.getOwnerId() instanceof CustomerId ? "customer" : "tenant");
                }
            }
            result.add(info);
        }
        return result;
    }

    @Override
    public void deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId) {
        groupPermissionRepository.deleteById(groupPermissionId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(groupPermissionId).build());
    }

    @Override
    @Transactional
    public void deleteGroupPermissionsByUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        deleteAll(tenantId, groupPermissionRepository.findByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId()));
    }

    @Override
    @Transactional
    public void deleteGroupPermissionsByEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        deleteAll(tenantId, groupPermissionRepository.findByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId()));
    }

    private void deleteAll(TenantId tenantId, List<GroupPermissionEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        List<GroupPermission> permissions = entities.stream().map(GroupPermissionEntity::toData).toList();
        // Evict first: the caller deletes the group's membership relations right after this, and the
        // eviction has to enumerate the group's users while they are still resolvable.
        eventPublisher.publishEvent(GroupPermissionsEvictEvent.builder()
                .tenantId(tenantId)
                .userGroupIds(permissions.stream().map(GroupPermission::getUserGroupId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()))
                .build());
        groupPermissionRepository.deleteAll(entities);
        permissions.forEach(permission -> eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId).entityId(permission.getId()).entity(permission).build()));
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        return groupPermissionRepository.findByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId())
                .stream().map(GroupPermissionEntity::toData).toList();
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, org.thingsboard.server.common.data.id.RoleId roleId) {
        return groupPermissionRepository.findByTenantIdAndRoleId(tenantId.getId(), roleId.getId())
                .stream().map(GroupPermissionEntity::toData).toList();
    }

}
