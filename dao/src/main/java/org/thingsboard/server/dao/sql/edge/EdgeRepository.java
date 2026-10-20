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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.EdgeFields;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.model.sql.EdgeInfoEntity;

import java.util.List;
import java.util.UUID;

public interface EdgeRepository extends JpaRepository<EdgeEntity, UUID> {

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(d, c.title, c.additionalInfo) " +
            "FROM EdgeEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "WHERE d.id = :edgeId")
    EdgeInfoEntity findEdgeInfoById(@Param("edgeId") UUID edgeId);

    @Query(value = "SELECT * FROM edge_active_attribute_view edge_active",
            countQuery = "SELECT count(*) FROM edge_active_attribute_view",
            nativeQuery = true)
    Page<EdgeEntity> findActiveEdges(Pageable pageable);

    @Query("SELECT d.id FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId,
                                 @Param("textSearch") String textSearch,
                                 Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("textSearch") String textSearch,
                                    Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(d, c.title, c.additionalInfo) " +
            "FROM EdgeEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeInfoEntity> findEdgeInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    /**
     * 只要租户自己持有（未分配给任何客户）的 edge。未分配有两种表示：列名为 SQL NULL，
     * 或是 NULL_UUID 哨兵值 —— 两种都要排除，只判其一会漏。
     */
    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(d, c.title, c.additionalInfo) " +
            "FROM EdgeEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "WHERE d.tenantId = :tenantId " +
            "AND (d.customerId IS NULL OR d.customerId = :nullCustomerId) " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeInfoEntity> findTenantEdgeInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                       @Param("nullCustomerId") UUID nullCustomerId,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(d, c.title, c.additionalInfo) " +
            "FROM EdgeEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (d.customerId IS NULL OR d.customerId = :nullCustomerId) " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeInfoEntity> findTenantEdgeInfosByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                              @Param("type") String type,
                                                              @Param("nullCustomerId") UUID nullCustomerId,
                                                              @Param("textSearch") String textSearch,
                                                              Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                           @Param("type") String type,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(d, c.title, c.additionalInfo) " +
            "FROM EdgeEntity d " +
            "LEFT JOIN CustomerEntity c on c.id = d.customerId " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeInfoEntity> findEdgeInfosByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("type") String type,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT d FROM EdgeEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("type") String type,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(a, c.title, c.additionalInfo) " +
            "FROM EdgeEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeInfoEntity> findEdgeInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                              @Param("customerId") UUID customerId,
                                                              @Param("textSearch") String textSearch,
                                                              Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.EdgeInfoEntity(a, c.title, c.additionalInfo) " +
            "FROM EdgeEntity a " +
            "LEFT JOIN CustomerEntity c on c.id = a.customerId " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND a.type = :type " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeInfoEntity> findEdgeInfosByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                                     @Param("customerId") UUID customerId,
                                                                     @Param("type") String type,
                                                                     @Param("textSearch") String textSearch,
                                                                     Pageable pageable);

    @Query("SELECT ee FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.toId = :entityId AND re.toType = :entityType " +
            "AND (:textSearch IS NULL OR ilike(ee.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeEntity> findByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                               @Param("entityId") UUID entityId,
                                               @Param("entityType") String entityType,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.toId = :entityId AND re.toType = :entityType " +
            "AND (:textSearch IS NULL OR ilike(ee.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<UUID> findIdsByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                            @Param("entityId") UUID entityId,
                                            @Param("entityType") String entityType,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);

    /**
     * 多实体版：一个转换器可被多个集成引用，要取这些集成所在的全部 edge 的并集。
     * DISTINCT 是必须的 —— 同一台 edge 上挂着两个引用同一转换器的集成时会出现重复行。
     */
    @Query("SELECT DISTINCT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.toId IN (:entityIds) AND re.toType = :entityType")
    Page<UUID> findIdsByTenantIdAndEntityIds(@Param("tenantId") UUID tenantId,
                                             @Param("entityIds") List<UUID> entityIds,
                                             @Param("entityType") String entityType,
                                             Pageable pageable);

    /**
     * relationType 作为参数传入的版本。实体分组挂到 edge 用的不是 'Contains'，而是
     * {@code EDGE_ENTITY_GROUP_<分组类型>}（见 BaseEntityGroupService#assignEntityGroupToEdge），
     * 所以不能复用上面那些把 'Contains' 写死的查询。
     */
    @Query("SELECT ee.id FROM EdgeEntity ee, RelationEntity re WHERE ee.tenantId = :tenantId " +
            "AND ee.id = re.fromId AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = :relationType AND re.toId = :entityId AND re.toType = :entityType")
    Page<UUID> findIdsByTenantIdAndEntityIdAndRelationType(@Param("tenantId") UUID tenantId,
                                                           @Param("entityId") UUID entityId,
                                                           @Param("entityType") String entityType,
                                                           @Param("relationType") String relationType,
                                                           Pageable pageable);

    /**
     * 实体到达 edge 的**两条**途径的并集：
     *   ① 直连分配  edge --Contains--> 实体
     *   ② 经分组    edge --EDGE_ENTITY_GROUP_X--> 分组 --Contains--> 实体
     *
     * <p>PE 对 DEVICE/ASSET/ENTITY_VIEW/DASHBOARD/USER 只查 ②，因为 PE 里实体只能经分组
     * 到达 edge。CE 两套机制都有（且直连那套是现成能用的功能），所以这里取并集而不是
     * 照搬 PE 的替换 —— 直接换成 ② 会打断 CE 的直连分配。
     *
     * <p>内层子查询的 FROM_ENTITY_GROUP / 'Contains' 与 BaseEntityGroupService#addRelation
     * 写入的关系完全对应（CE 与 PE 在这一点上存法一致）。
     */
    @Query("SELECT DISTINCT ee.id FROM EdgeEntity ee, RelationEntity re " +
            "WHERE ee.tenantId = :tenantId AND ee.id = re.fromId " +
            "AND re.fromType = 'EDGE' AND re.relationTypeGroup = 'EDGE' " +
            "AND ( " +
            "  (re.relationType = 'Contains' AND re.toId = :entityId AND re.toType = :entityType) " +
            "  OR " +
            "  (re.relationType = :groupRelationType AND re.toType = 'ENTITY_GROUP' AND re.toId IN ( " +
            "     SELECT r2.fromId FROM RelationEntity r2 " +
            "     WHERE r2.toId = :entityId AND r2.toType = :entityType " +
            "     AND r2.fromType = 'ENTITY_GROUP' AND r2.relationType = 'Contains' " +
            "     AND r2.relationTypeGroup = 'FROM_ENTITY_GROUP')) " +
            ")")
    Page<UUID> findIdsByTenantIdAndEntityIdIncludingGroups(@Param("tenantId") UUID tenantId,
                                                           @Param("entityId") UUID entityId,
                                                           @Param("entityType") String entityType,
                                                           @Param("groupRelationType") String groupRelationType,
                                                           Pageable pageable);

    @Query("SELECT ee FROM EdgeEntity ee, TenantEntity te WHERE ee.tenantId = te.id AND te.tenantProfileId = :tenantProfileId ")
    Page<EdgeEntity> findByTenantProfileId(@Param("tenantProfileId") UUID tenantProfileId,
                                           Pageable pageable);

    @Query("SELECT DISTINCT d.type FROM EdgeEntity d WHERE d.tenantId = :tenantId")
    List<String> findTenantEdgeTypes(@Param("tenantId") UUID tenantId);

    @Query("SELECT count(*) FROM EdgeEntity e WHERE e.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    EdgeEntity findByTenantIdAndName(UUID tenantId, String name);

    List<EdgeEntity> findEdgesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> edgeIds);

    List<EdgeEntity> findEdgesByTenantIdAndIdIn(UUID tenantId, List<UUID> edgeIds);

    EdgeEntity findByRoutingKey(String routingKey);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.EdgeFields(e.id, e.createdTime, e.tenantId, e.customerId," +
            "e.name, e.version, e.type, e.label, e.additionalInfo) FROM EdgeEntity e WHERE e.id > :id ORDER BY e.id")
    List<EdgeFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
