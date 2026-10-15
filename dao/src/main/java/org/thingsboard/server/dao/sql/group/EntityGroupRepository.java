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
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityGroupFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityGroupRepository extends JpaRepository<EntityGroupEntity, UUID>, ExportableEntityRepository<EntityGroupEntity> {

    // owner_id = tenant_id for tenant-owned groups, a customer id for customer-owned groups
    @Query("SELECT g FROM EntityGroupEntity g WHERE g.ownerId = :ownerId AND g.type = :type " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findByOwnerIdAndType(@Param("ownerId") UUID ownerId,
                                                 @Param("type") EntityType type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    // Same as above across a set of owners, used to list an owner's groups together with those of
    // its ancestors (see EntityGroupService.findEntityGroupsByOwnersAndType).
    @Query("SELECT g FROM EntityGroupEntity g WHERE g.ownerId IN :ownerIds AND g.type = :type " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findByOwnerIdsAndType(@Param("ownerIds") List<UUID> ownerIds,
                                                  @Param("type") EntityType type,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    Optional<EntityGroupEntity> findByOwnerIdAndTypeAndName(UUID ownerId, EntityType type, String name);

    // 分配给某个 edge 的实体分组。关系方向是 edge -> group，类型是
    // EDGE_ENTITY_GROUP_<GROUP_TYPE>，关系组是 EDGE（与实体成员关系的 FROM_ENTITY_GROUP 区分开）。
    @Query("SELECT e FROM EntityGroupEntity e, RelationEntity re WHERE e.id = re.toId " +
            "AND re.toType = 'ENTITY_GROUP' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.fromId = :edgeId AND re.fromType = 'EDGE'")
    Page<EntityGroupEntity> findEdgeEntityGroupsByType(@Param("edgeId") UUID edgeId,
                                                       @Param("relationType") String relationType,
                                                       Pageable pageable);

    @Query("SELECT e FROM EntityGroupEntity e, RelationEntity re WHERE e.id = re.toId " +
            "AND re.toType = 'ENTITY_GROUP' AND e.ownerId = :ownerId AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.fromId = :edgeId AND re.fromType = 'EDGE'")
    Page<EntityGroupEntity> findEdgeEntityGroupsByOwnerIdAndType(@Param("edgeId") UUID edgeId,
                                                                 @Param("ownerId") UUID ownerId,
                                                                 @Param("relationType") String relationType,
                                                                 Pageable pageable);

    List<EntityGroupEntity> findAllByTenantId(UUID tenantId);

    List<EntityGroupEntity> findAllByOwnerId(UUID ownerId);

    @Query("SELECT g FROM EntityGroupEntity g WHERE g.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    @Query("SELECT externalId FROM EntityGroupEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.EntityGroupFields(g.id, g.createdTime, " +
            "g.tenantId, g.name, g.version, g.type, g.additionalInfo, g.ownerId, g.ownerType) " +
            "FROM EntityGroupEntity g WHERE g.id > :id ORDER BY g.id")
    List<EntityGroupFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
