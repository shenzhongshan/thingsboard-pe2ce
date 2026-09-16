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
package org.thingsboard.server.dao.group;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;
import org.thingsboard.server.dao.user.UserService;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseEntityGroupService implements EntityGroupService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Field-injected and @Lazy on purpose: BaseGroupPermissionService takes EntityGroupService as a
    // constructor dependency (it resolves group names for the permissions UI), so making this one a
    // constructor dependency too would be an unresolvable cycle at startup.
    @Autowired
    @Lazy
    private GroupPermissionService groupPermissionService;

    // 同样字段注入 + @Lazy：edge 侧的分组分配会反过来调用本服务
    // （PE 的 assignTenantAdministratorsAndUsersGroupToEdge 就是这么用的），
    // 做成构造依赖会在启动时形成不可解的环。
    @Autowired
    @Lazy
    private EdgeService edgeService;

    private final EntityGroupRepository entityGroupRepository;
    private final RelationService relationService;
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final CustomerService customerService;
    private final UserService userService;
    private final EntityViewService entityViewService;
    private final DashboardService dashboardService;
    private final RoleService roleService;

    @Override
    public EntityGroup saveEntityGroup(TenantId tenantId, EntityGroup entityGroup) {
        if (StringUtils.isBlank(entityGroup.getName())) {
            throw new DataValidationException("Entity group name should be specified!");
        }
        if (entityGroup.getType() == null || !Arrays.asList(EntityGroup.groupTypes).contains(entityGroup.getType())) {
            throw new DataValidationException("Unsupported entity group type!");
        }
        if (entityGroup.getId() != null) {
            EntityGroup existing = findEntityGroupById(tenantId, entityGroup.getId());
            if (existing == null) {
                throw new DataValidationException("Entity group not found!");
            }
            if (existing.isGroupAll() && !EntityGroup.GROUP_ALL_NAME.equals(entityGroup.getName())) {
                throw new DataValidationException("Renaming of the 'All' entity group is prohibited!");
            }
            if (entityGroup.getOwnerId() == null) {
                entityGroup.setOwnerId(existing.getOwnerId());
            }
            if (entityGroup.getVersion() == null) {
                // save() below goes through a plain repository.save(), not the JpaAbstractDao
                // insert/update dance that manages @Version itself — a null version on an existing
                // row makes Hibernate treat it as transient and attempt an INSERT, colliding on the
                // primary key. Carry the existing row's version forward when the caller omits it.
                entityGroup.setVersion(existing.getVersion());
            }
        } else {
            if (entityGroup.getOwnerId() instanceof CustomerId customerOwnerId
                    && customerService.findCustomerById(tenantId, customerOwnerId) == null) {
                throw new DataValidationException("Group owner customer not found!");
            }
            UUID ownerUuid = entityGroup.getOwnerId() != null ? entityGroup.getOwnerId().getId() : tenantId.getId();
            if (EntityGroup.GROUP_ALL_NAME.equals(entityGroup.getName())
                    && entityGroupRepository.findByOwnerIdAndTypeAndName(ownerUuid, entityGroup.getType(), EntityGroup.GROUP_ALL_NAME).isPresent()) {
                throw new DataValidationException("Entity group 'All' already exists!");
            }
        }
        entityGroup.setTenantId(tenantId);
        if (entityGroup.getId() == null) {
            entityGroup.setCreatedTime(System.currentTimeMillis());
        }
        try {
            boolean created = entityGroup.getId() == null;
            EntityGroup saved = entityGroupRepository.save(new EntityGroupEntity(entityGroup)).toData();
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId())
                    .entity(saved).created(created).broadcastEvent(true).build());
            return saved;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("group_name_per_owner_unq_key")
                    || e.getCause() != null && String.valueOf(e.getCause().getMessage()).contains("group_name_per_owner_unq_key")) {
                throw new DataValidationException("Entity group with such name already exists!");
            }
            throw e;
        }
    }

    @Override
    public EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId) {
        return entityGroupRepository.findById(entityGroupId.getId()).map(EntityGroupEntity::toData).orElse(null);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityType groupType, PageLink pageLink) {
        return findEntityGroupsByType(tenantId, null, groupType, pageLink);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, CustomerId ownerId, EntityType groupType, PageLink pageLink) {
        ensureDefaultGroups(tenantId, ownerId, groupType);
        UUID ownerUuid = ownerId != null ? ownerId.getId() : tenantId.getId();
        return DaoUtil.toPageData(entityGroupRepository.findByOwnerIdAndType(
                ownerUuid, groupType, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByOwnersAndType(TenantId tenantId, List<EntityId> ownerIds, EntityType groupType, PageLink pageLink) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return PageData.emptyPageData();
        }
        List<UUID> ownerUuids = new ArrayList<>(ownerIds.size());
        for (EntityId ownerId : ownerIds) {
            // Same normalisation as the single-owner overload, which expects null for the tenant.
            CustomerId customerId = tenantId.getId().equals(ownerId.getId()) ? null : new CustomerId(ownerId.getId());
            ensureDefaultGroups(tenantId, customerId, groupType);
            ownerUuids.add(ownerId.getId());
        }
        return DaoUtil.toPageData(entityGroupRepository.findByOwnerIdsAndType(
                ownerUuids, groupType, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    private void ensureDefaultGroups(TenantId tenantId, CustomerId ownerId, EntityType groupType) {
        ensureGroup(tenantId, ownerId, groupType, EntityGroup.GROUP_ALL_NAME, null);
        if (ownerId == null && groupType == EntityType.USER) {
            // Tenant-level counterpart of the per-customer groups below. Needed as a distinct group
            // because the tenant's "All" user group also contains every customer's users, so it
            // cannot be used to tell administrators apart from customer users.
            ensureGroup(tenantId, null, groupType, TENANT_ADMINS_GROUP,
                    "Autogenerated Tenant Administrators group with all permissions.");
        }
        if (ownerId != null && groupType == EntityType.USER) {
            // PE parity: per-customer default user groups (management data only, no RBAC enforcement)
            ensureGroup(tenantId, ownerId, groupType, "Customer Administrators",
                    "Autogenerated Customer Administrators group with all permissions.");
            ensureGroup(tenantId, ownerId, groupType, "Customer Users",
                    "Autogenerated Customer Users group with read-only permissions.");
        }
    }

    private void ensureGroup(TenantId tenantId, CustomerId ownerId, EntityType groupType, String name, String description) {
        UUID ownerUuid = ownerId != null ? ownerId.getId() : tenantId.getId();
        if (entityGroupRepository.findByOwnerIdAndTypeAndName(ownerUuid, groupType, name).isEmpty()) {
            EntityGroup group = new EntityGroup();
            group.setTenantId(tenantId);
            group.setOwnerId(ownerId);
            group.setType(groupType);
            group.setName(name);
            group.setCreatedTime(System.currentTimeMillis());
            if (description != null) {
                ObjectNode info = JacksonUtil.newObjectNode();
                info.put("description", description);
                group.setAdditionalInfo(info);
            }
            try {
                // Must publish like saveEntityGroup does. Writing straight through the repository
                // leaves EDQS unaware of the group, so anything resolved through group membership
                // (dashboard ownership, group-name filters) would differ from the SQL engine.
                EntityGroup saved = entityGroupRepository.save(new EntityGroupEntity(group)).toData();
                eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId())
                        .entity(saved).created(true).broadcastEvent(true).build());
            } catch (Exception e) {
                log.debug("[{}][{}][{}] Concurrent default group creation, ignoring", tenantId, groupType, name, e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            return;
        }
        if (entityGroup.isGroupAll()) {
            throw new DataValidationException("Deletion of the 'All' entity group is prohibited!");
        }
        deleteGroupPermissions(tenantId, entityGroupId);
        relationService.deleteEntityRelations(tenantId, entityGroupId);
        entityGroupRepository.deleteById(entityGroupId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(entityGroupId).build());
    }

    /**
     * A group can be referenced by a permission in either direction - as the user group being
     * granted access, or as the entity group access is granted on - so both have to go. Must run
     * before the relations are deleted, so the permission layer can still enumerate the group's
     * users to evict their cached permissions.
     */
    private void deleteGroupPermissions(TenantId tenantId, EntityGroupId entityGroupId) {
        groupPermissionService.deleteGroupPermissionsByUserGroupId(tenantId, entityGroupId);
        groupPermissionService.deleteGroupPermissionsByEntityGroupId(tenantId, entityGroupId);
    }

    @Override
    @Transactional
    public void deleteEntityGroupsByOwner(TenantId tenantId, EntityId ownerId) {
        // Goes through the repository rather than deleteEntityGroup because the owner's "All"
        // groups have to go too, and those are deliberately undeletable one at a time.
        for (EntityGroupEntity entity : entityGroupRepository.findAllByOwnerId(ownerId.getId())) {
            EntityGroupId entityGroupId = new EntityGroupId(entity.getUuid());
            deleteGroupPermissions(tenantId, entityGroupId);
            relationService.deleteEntityRelations(tenantId, entityGroupId);
            entityGroupRepository.deleteById(entity.getUuid());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(entityGroupId).build());
        }
    }

    @Override
    @Transactional
    public void deleteEntityGroupsByTenantId(TenantId tenantId) {
        // Permissions first, in one sweep rather than two queries per group - the eviction they
        // publish needs the groups' users still resolvable, which is why it cannot come after.
        groupPermissionService.deleteGroupPermissionsByTenantId(tenantId);
        for (EntityGroupEntity entity : entityGroupRepository.findAllByTenantId(tenantId.getId())) {
            EntityGroupId entityGroupId = new EntityGroupId(entity.getUuid());
            relationService.deleteEntityRelations(tenantId, entityGroupId);
            entityGroupRepository.deleteById(entity.getUuid());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(entityGroupId).build());
        }
    }

    @Override
    public void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        EntityGroup entityGroup = getExistingNonAll(tenantId, entityGroupId, "add entities to");
        for (EntityId entityId : entityIds) {
            if (entityId.getEntityType() != entityGroup.getType()) {
                throw new DataValidationException("Entity type does not match the entity group type!");
            }
            addRelation(tenantId, entityGroupId, entityId);
        }
    }

    @Override
    public void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        getExistingNonAll(tenantId, entityGroupId, "remove entities from");
        for (EntityId entityId : entityIds) {
            removeRelation(tenantId, entityGroupId, entityId);
        }
    }

    // Internal — bypasses getExistingNonAll, since ensureAllGroupMembership/moveAllGroupMembership
    // are exactly the programmatic paths allowed to maintain "All" group relations. The public
    // add/removeEntitiesToEntityGroup (REST-driven, manual editing) stay guarded against "All".
    private void addRelation(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        relationService.saveRelation(tenantId,
                new EntityRelation(entityGroupId, entityId, GROUP_RELATION_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP));
    }

    private void removeRelation(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        relationService.deleteRelation(tenantId, entityGroupId, entityId, GROUP_RELATION_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    @Override
    public void ensureAllGroupMembership(TenantId tenantId, CustomerId ownerId, EntityType groupType, EntityId entityId) {
        EntityGroupId allGroupId = findOrCreateAllGroupId(tenantId, ownerId, groupType);
        if (allGroupId != null) {
            addRelation(tenantId, allGroupId, entityId);
        }
    }

    @Override
    public void moveAllGroupMembership(TenantId tenantId, CustomerId oldOwnerId, CustomerId newOwnerId, EntityType groupType, EntityId entityId) {
        if (Objects.equals(oldOwnerId, newOwnerId)) {
            return;
        }
        if (oldOwnerId != null) {
            EntityGroupId oldAllGroupId = findOrCreateAllGroupId(tenantId, oldOwnerId, groupType);
            if (oldAllGroupId != null) {
                removeRelation(tenantId, oldAllGroupId, entityId);
            }
        }
        if (newOwnerId != null) {
            ensureAllGroupMembership(tenantId, newOwnerId, groupType, entityId);
        }
    }

    private EntityGroup getExistingNonAll(TenantId tenantId, EntityGroupId entityGroupId, String action) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        if (entityGroup.isGroupAll()) {
            throw new DataValidationException("Can't " + action + " the 'All' entity group — it always contains all entities!");
        }
        return entityGroup;
    }

    @Override
    public PageData<? extends HasName> findGroupEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        if (entityGroup.isGroupAll()) {
            if (entityGroup.getOwnerId() instanceof CustomerId customerOwnerId) {
                return findAllCustomerEntities(tenantId, customerOwnerId, entityGroup.getType(), pageLink);
            }
            return findAllTenantEntities(tenantId, entityGroup.getType(), pageLink);
        }
        String textSearch = pageLink.getTextSearch() != null ? pageLink.getTextSearch().toLowerCase() : null;
        List<HasName> members = relationService.findByFrom(tenantId, entityGroupId, RelationTypeGroup.FROM_ENTITY_GROUP).stream()
                .map(EntityRelation::getTo)
                .map(to -> loadGroupEntity(tenantId, to))
                .filter(Objects::nonNull)  // deleted entities leave stale relations; drop them from the view
                .filter(e -> textSearch == null || e.getName().toLowerCase().contains(textSearch))
                .sorted(Comparator.comparing(HasName::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        // Group sizes are modest — paginate in memory.
        int total = members.size();
        int from = Math.min(pageLink.getPage() * pageLink.getPageSize(), total);
        int to = Math.min(from + pageLink.getPageSize(), total);
        int totalPages = (int) Math.ceil((double) total / pageLink.getPageSize());
        return new PageData<>(members.subList(from, to), totalPages, total, to < total);
    }

    private PageData<? extends HasName> findAllTenantEntities(TenantId tenantId, EntityType type, PageLink pageLink) {
        return switch (type) {
            case DEVICE -> deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
            case ASSET -> assetService.findAssetInfosByTenantId(tenantId, pageLink);
            case CUSTOMER -> customerService.findCustomersByTenantId(tenantId, pageLink);
            case USER -> userService.findUsersByTenantId(tenantId, pageLink);
            case ENTITY_VIEW -> entityViewService.findEntityViewInfosByTenantId(tenantId, pageLink);
            case DASHBOARD -> dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            case EDGE -> edgeService.findEdgeInfosByTenantId(tenantId, pageLink);
            default -> throw new DataValidationException("Unsupported entity group type!");
        };
    }

    /** The customer's "All" group lists entities assigned to that customer. */
    private PageData<? extends HasName> findAllCustomerEntities(TenantId tenantId, CustomerId customerId, EntityType type, PageLink pageLink) {
        return switch (type) {
            case DEVICE -> deviceService.findDeviceInfosByFilter(
                    DeviceInfoFilter.builder().tenantId(tenantId).customerId(customerId).build(), pageLink);
            case ASSET -> assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            case CUSTOMER -> customerService.findCustomersByTenantIdAndParentCustomerId(tenantId, customerId, pageLink);
            case USER -> userService.findCustomerUsers(tenantId, customerId, pageLink);
            case ENTITY_VIEW -> entityViewService.findEntityViewInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            case DASHBOARD -> dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            case EDGE -> edgeService.findEdgeInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            default -> throw new DataValidationException("Unsupported entity group type!");
        };
    }

    @Override
    public PageData<? extends HasName> findUserGroupEntities(TenantId tenantId, EntityType groupType, CustomerId ownerId,
                                                             List<EntityGroupId> groupIds, boolean includeOwnerScope,
                                                             PageLink pageLink) {
        boolean hasGroups = groupIds != null && !groupIds.isEmpty();
        if (!includeOwnerScope && !hasGroups) {
            return new PageData<>(List.of(), 0, 0, false);
        }
        if (includeOwnerScope && !hasGroups) {
            // Pure owner scope — stay on the DB-paginated path.
            return ownerId != null && !ownerId.isNullUid()
                    ? findAllCustomerEntities(tenantId, ownerId, groupType, pageLink)
                    : findAllTenantEntities(tenantId, groupType, pageLink);
        }
        // A union of owner scope and shared groups cannot be expressed as one paginated query here
        // (group membership lives in the relation table), so it is merged in memory — the same
        // trade-off findGroupEntities already makes. Owner scope is pulled through the "All" group
        // so the per-type switch is not duplicated.
        Set<EntityId> memberIds = new LinkedHashSet<>();
        if (includeOwnerScope) {
            EntityGroupId allGroupId = findOrCreateAllGroupId(tenantId, ownerId, groupType);
            if (allGroupId != null) {
                memberIds.addAll(findAllEntityIds(tenantId, allGroupId));
            }
        }
        for (EntityGroupId groupId : groupIds) {
            EntityGroup group = findEntityGroupById(tenantId, groupId);
            if (group == null || group.getType() != groupType) {
                continue; // stale grant, or a role pointing at a group of a different type
            }
            memberIds.addAll(findAllEntityIds(tenantId, groupId));
        }
        String textSearch = pageLink.getTextSearch() != null ? pageLink.getTextSearch().toLowerCase() : null;
        List<HasName> entities = memberIds.stream()
                .map(id -> loadGroupEntity(tenantId, id))
                .filter(Objects::nonNull) // deleted entities leave stale relations behind
                .filter(e -> textSearch == null || e.getName().toLowerCase().contains(textSearch))
                .sorted(Comparator.comparing(HasName::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int total = entities.size();
        int from = Math.min(pageLink.getPage() * pageLink.getPageSize(), total);
        int to = Math.min(from + pageLink.getPageSize(), total);
        int totalPages = (int) Math.ceil((double) total / pageLink.getPageSize());
        return new PageData<>(entities.subList(from, to), totalPages, total, to < total);
    }

    private HasName loadGroupEntity(TenantId tenantId, EntityId entityId) {
        try {
            return switch (entityId.getEntityType()) {
                case DEVICE -> deviceService.findDeviceInfoById(tenantId, new DeviceId(entityId.getId()));
                case ASSET -> assetService.findAssetInfoById(tenantId, new AssetId(entityId.getId()));
                case CUSTOMER -> customerService.findCustomerById(tenantId, new CustomerId(entityId.getId()));
                case USER -> userService.findUserById(tenantId, new UserId(entityId.getId()));
                case ENTITY_VIEW -> entityViewService.findEntityViewInfoById(tenantId, new EntityViewId(entityId.getId()));
                case DASHBOARD -> dashboardService.findDashboardInfoById(tenantId, new DashboardId(entityId.getId()));
                case EDGE -> edgeService.findEdgeInfoById(tenantId, new EdgeId(entityId.getId()));
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public EntityGroupId findOrCreateAllGroupId(TenantId tenantId, CustomerId ownerId, EntityType groupType) {
        ensureDefaultGroups(tenantId, ownerId, groupType);
        UUID ownerUuid = ownerId != null ? ownerId.getId() : tenantId.getId();
        return entityGroupRepository.findByOwnerIdAndTypeAndName(ownerUuid, groupType, EntityGroup.GROUP_ALL_NAME)
                .map(e -> new EntityGroupId(e.getId()))
                .orElse(null);
    }

    @Override
    public List<EntityGroupId> findEntityGroupsForEntity(TenantId tenantId, EntityId entityId, CustomerId customerId) {
        // LinkedHashSet, not List: "All" membership is now backed by a real relation, so the
        // relation scan below re-yields the same "All" groups the block above ensures. The
        // explicit block still matters for entities whose relations predate that change.
        Set<EntityGroupId> result = new LinkedHashSet<>();
        EntityType type = entityId.getEntityType();
        if (Arrays.asList(EntityGroup.groupTypes).contains(type)) {
            EntityGroupId tenantAll = findOrCreateAllGroupId(tenantId, null, type);
            if (tenantAll != null) {
                result.add(tenantAll);
            }
            if (customerId != null && !customerId.isNullUid()) {
                EntityGroupId customerAll = findOrCreateAllGroupId(tenantId, customerId, type);
                if (customerAll != null) {
                    result.add(customerAll);
                }
            }
        }
        relationService.findByTo(tenantId, entityId, RelationTypeGroup.FROM_ENTITY_GROUP).stream()
                .map(EntityRelation::getFrom)
                .map(from -> new EntityGroupId(from.getId()))
                .forEach(result::add);
        return new ArrayList<>(result);
    }

    @Override
    public List<org.thingsboard.server.common.data.id.UserId> findGroupUserIds(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup group = findEntityGroupById(tenantId, entityGroupId);
        if (group == null || group.getType() != EntityType.USER) {
            return List.of();
        }
        if (group.isGroupAll()) {
            PageLink pageLink = new PageLink(1000);
            PageData<? extends HasName> page = group.getOwnerId() instanceof CustomerId customerOwnerId
                    ? findAllCustomerEntities(tenantId, customerOwnerId, EntityType.USER, pageLink)
                    : findAllTenantEntities(tenantId, EntityType.USER, pageLink);
            return page.getData().stream()
                    .map(u -> ((org.thingsboard.server.common.data.User) u).getId())
                    .toList();
        }
        return relationService.findByFrom(tenantId, entityGroupId, RelationTypeGroup.FROM_ENTITY_GROUP).stream()
                .map(EntityRelation::getTo)
                .map(to -> new UserId(to.getId()))
                .toList();
    }

    @Override
    public EntityGroup findEntityGroupByTypeAndName(TenantId tenantId, CustomerId ownerId, EntityType groupType, String name) {
        UUID ownerUuid = ownerId != null ? ownerId.getId() : tenantId.getId();
        return entityGroupRepository.findByOwnerIdAndTypeAndName(ownerUuid, groupType, name).map(EntityGroupEntity::toData).orElse(null);
    }

    @Override
    public void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        addEntitiesToEntityGroup(tenantId, entityGroupId, List.of(entityId));
    }

    @Override
    public void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        removeEntitiesFromEntityGroup(tenantId, entityGroupId, List.of(entityId));
    }

    @Override
    public List<EntityId> findAllEntityIds(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        if (entityGroup.isGroupAll()) {
            PageLink pageLink = new PageLink(Integer.MAX_VALUE);
            PageData<? extends HasName> page = entityGroup.getOwnerId() instanceof CustomerId customerOwnerId
                    ? findAllCustomerEntities(tenantId, customerOwnerId, entityGroup.getType(), pageLink)
                    : findAllTenantEntities(tenantId, entityGroup.getType(), pageLink);
            return page.getData().stream()
                    .map(e -> (EntityId) ((org.thingsboard.server.common.data.id.HasId<?>) e).getId())
                    .toList();
        }
        return relationService.findByFrom(tenantId, entityGroupId, RelationTypeGroup.FROM_ENTITY_GROUP).stream()
                .map(EntityRelation::getTo)
                .toList();
    }

    @Override
    public EntityGroup findOrCreatePublicUsersGroup(TenantId tenantId, CustomerId customerId) {
        ensureGroup(tenantId, customerId, EntityType.USER, "Public Users",
                "Autogenerated Public Users group with read-only permissions.");
        return findEntityGroupByTypeAndName(tenantId, customerId, EntityType.USER, "Public Users");
    }

    @Override
    public EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        checkPublicEntityGroupType(entityGroup.getType());
        if (entityGroup.isPublic()) {
            throw new DataValidationException("Entity group is already public!");
        }
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        EntityGroup publicUsers = findOrCreatePublicUsersGroup(tenantId, publicCustomer.getId());
        Role publicUserEntityGroupRole = roleService.findOrCreatePublicUsersEntityGroupRole(tenantId);
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setPublic(true);
        groupPermission.setTenantId(tenantId);
        groupPermission.setRoleId(publicUserEntityGroupRole.getId());
        groupPermission.setUserGroupId(publicUsers.getId());
        groupPermission.setEntityGroupId(entityGroup.getId());
        groupPermission.setEntityGroupType(entityGroup.getType());
        groupPermissionService.saveGroupPermission(tenantId, groupPermission);

        ObjectNode additionalInfo = entityGroup.getAdditionalInfo() != null && entityGroup.getAdditionalInfo().isObject()
                ? (ObjectNode) entityGroup.getAdditionalInfo() : JacksonUtil.newObjectNode();
        additionalInfo.put("isPublic", true);
        additionalInfo.put("publicCustomerId", publicCustomer.getId().getId().toString());
        entityGroup.setAdditionalInfo(additionalInfo);
        return saveEntityGroup(tenantId, entityGroup);
    }

    @Override
    public EntityGroup makePrivate(TenantId tenantId, EntityGroupId entityGroupId) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Entity group not found!");
        }
        checkPublicEntityGroupType(entityGroup.getType());
        if (!entityGroup.isPublic()) {
            throw new DataValidationException("Entity group is not public!");
        }
        Optional<GroupPermission> publicPermission = groupPermissionService
                .findPublicGroupPermissionByTenantIdAndEntityGroupId(tenantId, entityGroup.getId());
        publicPermission.ifPresent(permission ->
                groupPermissionService.deleteGroupPermission(tenantId, permission.getId()));

        ObjectNode additionalInfo = entityGroup.getAdditionalInfo() != null && entityGroup.getAdditionalInfo().isObject()
                ? (ObjectNode) entityGroup.getAdditionalInfo() : JacksonUtil.newObjectNode();
        additionalInfo.put("isPublic", false);
        additionalInfo.put("publicCustomerId", "");
        entityGroup.setAdditionalInfo(additionalInfo);
        return saveEntityGroup(tenantId, entityGroup);
    }

    private EntityType checkPublicEntityGroupType(EntityType groupType) {
        if (groupType != EntityType.ASSET && groupType != EntityType.DEVICE && groupType != EntityType.ENTITY_VIEW
                && groupType != EntityType.EDGE && groupType != EntityType.DASHBOARD) {
            throw new DataValidationException("Invalid entity group type! Only entity groups of types " +
                    "'ASSET', 'DEVICE', 'ENTITY_VIEW', 'EDGE' or 'DASHBOARD' can be public.");
        }
        return groupType;
    }

    public static final String TENANT_ADMINS_GROUP = "Tenant Administrators";
    public static final String CUSTOMER_USERS_GROUP = "Customer Users";

    @Override
    public void ensureDefaultUserRoleMembership(TenantId tenantId, CustomerId ownerId, UserId userId, boolean tenantAdmin) {
        try {
            CustomerId groupOwner = tenantAdmin ? null : ownerId;
            String groupName = tenantAdmin ? TENANT_ADMINS_GROUP : CUSTOMER_USERS_GROUP;
            if (!tenantAdmin && groupOwner == null) {
                return; // a customer user without a customer cannot be placed; nothing to bind
            }
            ensureDefaultGroups(tenantId, groupOwner, EntityType.USER);
            EntityGroup group = findEntityGroupByTypeAndName(tenantId, groupOwner, EntityType.USER, groupName);
            if (group == null) {
                return;
            }
            Role role = tenantAdmin ? roleService.findOrCreateTenantAdminRole(tenantId)
                    : roleService.findOrCreateCustomerUserRole(tenantId);
            ensureGroupPermission(tenantId, group.getId(), role.getId());
            addEntityToEntityGroup(tenantId, group.getId(), userId);
        } catch (Exception e) {
            // Never fail user creation over the default grant; the backfill migration can repair it.
            log.warn("[{}][{}] Failed to ensure default user role membership", tenantId, userId, e);
        }
    }

    /** Idempotent: one default GroupPermission per (userGroup, role) pair. */
    private void ensureGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId) {
        boolean exists = groupPermissionService.findGroupPermissionsByTenantIdAndUserGroupId(tenantId, userGroupId)
                .stream().anyMatch(gp -> roleId.equals(gp.getRoleId()) && gp.getEntityGroupId() == null);
        if (exists) {
            return;
        }
        GroupPermission permission = new GroupPermission();
        permission.setTenantId(tenantId);
        permission.setUserGroupId(userGroupId);
        permission.setRoleId(roleId);
        groupPermissionService.saveGroupPermission(tenantId, permission);
    }


    // ---- edge 分组分配 ----
    // PE 的 edge 同步以分组为单位。这里建立的是 edge -> group 的 EDGE 关系，
    // 与成员关系（group -> entity，FROM_ENTITY_GROUP）完全独立。
    // 发布的 ActionEntityEvent 带上 entityGroup，EdgeEventSourcingListener 会把它
    // 变成 edge_event.entity_group_id，最终成为实体消息上的 entityGroupIdMSB/LSB。

    public static final String EDGE_ENTITY_GROUP_RELATION_PREFIX = "EDGE_ENTITY_GROUP_";

    @Override
    public EntityGroup assignEntityGroupToEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType) {
        log.trace("Executing assignEntityGroupToEdge, tenantId [{}], entityGroupId [{}], edgeId [{}]", tenantId, entityGroupId, edgeId);
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Can't assign non-existent entity group to edge!");
        }
        if (edgeService.findEdgeById(tenantId, edgeId) == null) {
            throw new DataValidationException("Can't assign entity group to non-existent edge!");
        }
        relationService.saveRelation(tenantId, new EntityRelation(edgeId, entityGroupId,
                EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name(), RelationTypeGroup.EDGE));
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId).edgeId(edgeId).entityId(entityGroupId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).entityGroup(entityGroup).build());
        return entityGroup;
    }

    @Override
    public EntityGroup unassignEntityGroupFromEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType) {
        log.trace("Executing unassignEntityGroupFromEdge, tenantId [{}], entityGroupId [{}], edgeId [{}]", tenantId, entityGroupId, edgeId);
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new DataValidationException("Can't unassign non-existent entity group from edge!");
        }
        if (edgeService.findEdgeById(tenantId, edgeId) == null) {
            throw new DataValidationException("Can't unassign entity group from non-existent edge!");
        }
        relationService.deleteRelation(tenantId, edgeId, entityGroupId,
                EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name(), RelationTypeGroup.EDGE);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId).edgeId(edgeId).entityId(entityGroupId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).entityGroup(entityGroup).build());
        return entityGroup;
    }

    @Override
    public PageData<EntityGroup> findEdgeEntityGroupsByType(TenantId tenantId, EdgeId edgeId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEdgeEntityGroupsByType, tenantId [{}], edgeId [{}], groupType [{}]", tenantId, edgeId, groupType);
        return DaoUtil.toPageData(entityGroupRepository.findEdgeEntityGroupsByType(
                edgeId.getId(), EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityGroup> findEdgeEntityGroupsByOwnerIdAndType(TenantId tenantId, EdgeId edgeId, EntityId ownerId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEdgeEntityGroupsByOwnerIdAndType, tenantId [{}], edgeId [{}], ownerId [{}]", tenantId, edgeId, ownerId);
        return DaoUtil.toPageData(entityGroupRepository.findEdgeEntityGroupsByOwnerIdAndType(
                edgeId.getId(), ownerId.getId(), EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<Boolean> checkEntityGroupAssignedToEdgeAsync(TenantId tenantId, EdgeId edgeId, EntityGroupId entityGroupId, EntityType groupType) {
        log.trace("Executing checkEntityGroupAssignedToEdgeAsync, tenantId [{}], edgeId [{}], entityGroupId [{}]", tenantId, edgeId, entityGroupId);
        return relationService.checkRelationAsync(tenantId, edgeId, entityGroupId,
                EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name(), RelationTypeGroup.EDGE);
    }

}
