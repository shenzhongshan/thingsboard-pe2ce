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
package org.thingsboard.server.dao.sql.integration;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.IntegrationFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.IntegrationEntity;

import java.util.List;
import java.util.UUID;

public interface IntegrationRepository extends JpaRepository<IntegrationEntity, UUID>,
        ExportableEntityRepository<IntegrationEntity> {

    IntegrationEntity findByRoutingKey(String routingKey);

    List<IntegrationEntity> findByTenantId(UUID tenantId);

    /**
     * 启动时要真正拉起来的集成。**必须排除 edge 模板** —— 模板只存不跑，它的连接配置里
     * 通常还带着 ${{attr}} 占位符，真去连只会一路报错；而且那是 edge 侧该跑的东西，
     * 不该在云上占一份连接。
     */
    @Query("SELECT i FROM IntegrationEntity i WHERE i.enabled = true AND i.edgeTemplate = false")
    List<IntegrationEntity> findAllEnabledNonTemplate();

    // 模板与普通实体分居两个命名空间：列表页按这个位切分，互不可见。
    @Query("SELECT i FROM IntegrationEntity i WHERE i.tenantId = :tenantId AND i.edgeTemplate = :isEdgeTemplate " +
            "AND (:textSearch IS NULL OR ilike(i.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<IntegrationEntity> findByTenantIdAndIsEdgeTemplate(@Param("tenantId") UUID tenantId,
                                                @Param("isEdgeTemplate") boolean isEdgeTemplate,
                                                @Param("textSearch") String textSearch,
                                                Pageable pageable);

    // 普通列表必须排除模板，对齐 PE 的 findCoreIntegrationsByTenantId。
    @Query("SELECT i FROM IntegrationEntity i WHERE i.tenantId = :tenantId AND i.edgeTemplate = false " +
            "AND (:searchText IS NULL OR ilike(i.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                           @Param("searchText") String searchText,
                                           Pageable pageable);

    List<IntegrationEntity> findByTenantIdAndConverterId(UUID tenantId, UUID converterId);

    List<IntegrationEntity> findByTenantIdAndDownlinkConverterId(UUID tenantId, UUID downlinkConverterId);

    IntegrationEntity findByTenantIdAndName(UUID tenantId, String name);

    // 分配给某台 edge 的集成。关系方向 edge -> integration，类型 "Contains"，
    // 关系组 EDGE —— 与规则链一致，区别于分组用的 EDGE_ENTITY_GROUP_<TYPE>。
    @Query("SELECT ie FROM IntegrationEntity ie, RelationEntity re WHERE ie.tenantId = :tenantId " +
            "AND ie.id = re.toId AND re.toType = 'INTEGRATION' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(ie.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                    @Param("edgeId") UUID edgeId,
                                                    @Param("searchText") String searchText,
                                                    Pageable pageable);

    @Query("SELECT externalId FROM IntegrationEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.IntegrationFields(i.id, i.createdTime, i.tenantId, i.name, i.version, i.type, i.additionalInfo) FROM IntegrationEntity i WHERE i.id > :id ORDER BY i.id")
    List<IntegrationFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
