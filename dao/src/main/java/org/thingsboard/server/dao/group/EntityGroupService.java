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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

/**
 * CE port of PE's entity group service. Groups are tenant- or customer-owned; membership is
 * stored as relations (FROM group TO entity, type "Contains", group FROM_ENTITY_GROUP) — this
 * includes the "All" group, matching PE: it is auto-seeded per owner per type and its membership
 * is maintained via the same relations as any named group (see ensureAllGroupMembership /
 * moveAllGroupMembership), not computed on the fly.
 */
public interface EntityGroupService {

    String GROUP_RELATION_TYPE = "Contains";

    EntityGroup saveEntityGroup(TenantId tenantId, EntityGroup entityGroup);

    EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityType groupType, PageLink pageLink);

    /** ownerId = null → tenant-owned groups; otherwise the customer's groups of that type. */
    PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, CustomerId ownerId, EntityType groupType, PageLink pageLink);

    /**
     * Groups of the given type held by any of the owners, in one page. Used to present an owner
     * together with its ancestors, which is how a sub-customer sees the groups it inherits.
     *
     * @param ownerIds tenant ids and/or customer ids; an empty list yields an empty page
     */
    PageData<EntityGroup> findEntityGroupsByOwnersAndType(TenantId tenantId, List<EntityId> ownerIds, EntityType groupType, PageLink pageLink);

    void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId);

    /** Removes every group the owner holds, including its "All" groups. Call when deleting the owner. */
    void deleteEntityGroupsByOwner(TenantId tenantId, EntityId ownerId);

    /**
     * Removes every group the tenant owns - its own and its customers' - together with the
     * FROM_ENTITY_GROUP membership relations pointing at them. Called when deleting the tenant:
     * customer-owned groups are swept by {@link #deleteEntityGroupsByOwner} as each customer goes,
     * but the tenant's own groups have no such hook and would otherwise outlive the tenant.
     */
    void deleteEntityGroupsByTenantId(TenantId tenantId);

    void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds);

    PageData<? extends HasName> findGroupEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    /**
     * Entities of {@code groupType} a user may see: the union of their owner scope (when
     * {@code includeOwnerScope}, i.e. they hold a generic grant) and the entity groups granted to
     * them through GROUP roles, which may belong to other owners.
     * <p>
     * Takes plain ids rather than the permission model, because that lives in the application module
     * and is not visible from here; callers resolve it with
     * {@code MergedUserPermissions.getGroupPermissionsByEntityTypeAndOperation}.
     *
     * @param ownerId           owner whose scope to include, or null for the whole tenant
     * @param groupIds          additional groups granted to the user; may be empty
     * @param includeOwnerScope whether the owner scope itself is reachable
     */
    PageData<? extends HasName> findUserGroupEntities(TenantId tenantId, EntityType groupType, CustomerId ownerId,
                                                      List<EntityGroupId> groupIds, boolean includeOwnerScope,
                                                      PageLink pageLink);

    EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId);

    EntityGroup makePrivate(TenantId tenantId, EntityGroupId entityGroupId);

    /** Ensures + returns the "All" group id for the given owner+type (tenant-owned when ownerId is null). */
    EntityGroupId findOrCreateAllGroupId(TenantId tenantId, CustomerId ownerId, EntityType groupType);

    /** Ensures + returns the "Public Users" USER group owned by the given customer (PE parity). */
    EntityGroup findOrCreatePublicUsersGroup(TenantId tenantId, CustomerId customerId);

    /**
     * Ensures + returns a named USER group under the given owner, idempotently (PE parity: used by
     * self-registration to provision its "Self Registration Users" group). The description is stored
     * in the group's {@code additionalInfo.description}.
     */
    EntityGroup findOrCreateUserGroup(TenantId tenantId, CustomerId ownerId, String name, String description);

    /**
     * All entity-group ids the given entity belongs to: its tenant-level (and, if customerId is
     * provided, its customer-level) implicit "All" group, plus any explicit group memberships
     * (relations). Used by permission enforcement to resolve GROUP-role scoping.
     */
    List<EntityGroupId> findEntityGroupsForEntity(TenantId tenantId, EntityId entityId, CustomerId customerId);

    /** Member user ids of a USER-type entity group (including the virtual "All" group). Used for permission-cache eviction. */
    List<org.thingsboard.server.common.data.id.UserId> findGroupUserIds(TenantId tenantId, EntityGroupId entityGroupId);

    /** Sync lookup by owner+type+name (ownerId null = tenant-owned). Returns null if not found. */
    EntityGroup findEntityGroupByTypeAndName(TenantId tenantId, CustomerId ownerId, EntityType groupType, String name);

    void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId);

    /** Raw member entity ids of a group (including the virtual "All" group). */
    List<EntityId> findAllEntityIds(TenantId tenantId, EntityGroupId entityGroupId);

    /** 分页版，供 edge 全量同步逐页取组成员。 */
    PageData<EntityId> findEntityIds(TenantId tenantId, EntityType groupType, EntityGroupId entityGroupId, PageLink pageLink);

    /**
     * Adds entityId as a member of the "All" group for groupType, owned by ownerId (null =
     * tenant-owned) — creating the group row first if needed. Idempotent: safe to call for an
     * entity that's already a member. Call at entity creation, and again whenever an entity's
     * customer assignment changes (in addition to moveAllGroupMembership, which also detaches
     * the old owner's relation).
     */
    void ensureAllGroupMembership(TenantId tenantId, CustomerId ownerId, EntityType groupType, EntityId entityId);

    /**
     * Puts a newly created user into the default user group matching their authority, and makes sure
     * that group is bound to the corresponding default role. PE does this in
     * findOrCreateTenantAdminsGroup/findOrCreateCustomerUsersGroup; without it a user has no role
     * grant at all, which is harmless while permissions are additive but would lock them out the
     * moment enforcement becomes deny-by-default.
     */
    void ensureDefaultUserRoleMembership(TenantId tenantId, CustomerId ownerId, UserId userId, boolean tenantAdmin);

    /**
     * Pre-creates the tenant's default user groups ("Tenant Administrators"/"Tenant Users") and
     * binds each to its default role. PE parity: these exist from the moment the tenant is created,
     * not only once its first user appears.
     */
    void createDefaultTenantEntityGroups(TenantId tenantId);

    /**
     * Pre-creates a customer's default user groups and binds each to its default role. A public
     * customer gets only the "Public Users" group instead — PE parity.
     */
    void createDefaultCustomerEntityGroups(TenantId tenantId, CustomerId customerId, boolean isPublic);

    /**
     * Moves entityId's "All"-group membership from oldOwnerId's group to newOwnerId's group for
     * groupType (either may be null for tenant-owned). No-op if oldOwnerId equals newOwnerId.
     */
    void moveAllGroupMembership(TenantId tenantId, CustomerId oldOwnerId, CustomerId newOwnerId, EntityType groupType, EntityId entityId);

    /**
     * 把实体分组分配给一台 edge。PE 的 edge 同步以分组为单位：分配之后该组内的实体才会被
     * 下发到这台 edge，实体消息上的 entityGroupIdMSB/LSB 也由此而来。
     *
     * 关系存在 RelationTypeGroup.EDGE 下，类型 EDGE_ENTITY_GROUP_&lt;GROUP_TYPE&gt;，方向 edge -&gt; group。
     * 与成员关系（FROM_ENTITY_GROUP / "Contains"，方向 group -&gt; entity）是两套，互不干扰。
     */
    EntityGroup assignEntityGroupToEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType);

    EntityGroup unassignEntityGroupFromEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType);

    PageData<EntityGroup> findEdgeEntityGroupsByType(TenantId tenantId, EdgeId edgeId, EntityType groupType, PageLink pageLink);

    PageData<EntityGroup> findEdgeEntityGroupsByOwnerIdAndType(TenantId tenantId, EdgeId edgeId, EntityId ownerId, EntityType groupType, PageLink pageLink);

    ListenableFuture<Boolean> checkEntityGroupAssignedToEdgeAsync(TenantId tenantId, EdgeId edgeId, EntityGroupId entityGroupId, EntityType groupType);

    /**
     * 找到或创建这台 edge 的镜像分组 "[Edge] &lt;edgeName&gt; All"（客户所有时为
     * "[Edge][&lt;customerName&gt;] &lt;edgeName&gt; All"），并保证它已分配给该 edge。
     * 用于上行：edge 上新建的实体推到云端后需要一个归属分组。幂等。
     */
    EntityGroup findOrCreateEdgeAllGroup(TenantId tenantId, Edge edge, EntityType groupType);

    /**
     * edge 或其所属客户改名后，同步改掉镜像分组的名字。不做的话
     * {@link #findOrCreateEdgeAllGroup} 会按新名字找不到旧组而再建一个，
     * 同一台 edge 就出现两个镜像分组。
     *
     * <p>PE 把这个方法放在 EdgeService 上；这里放在分组服务，因为组名/组查询的逻辑都在这边，
     * 且本服务已持有 edgeService，反过来给 EdgeServiceImpl 注入 entityGroupService 会多一条环。
     */
    /**
     * 新建 edge 时把租户级用户组（"All" 与 "Tenant Administrators"）分配给它。
     * 用户随后经 EntityGroupEdgeEventFetcher(USER) 同步过去；没有这一步，按分组同步的
     * edge 会一个用户都收不到。幂等。
     */
    void assignTenantAdministratorsAndUsersGroupToEdge(TenantId tenantId, EdgeId edgeId);

    void renameEdgeAllGroups(TenantId tenantId, Edge edge, String oldEdgeName, String oldCustomerName, String newCustomerName);

}
