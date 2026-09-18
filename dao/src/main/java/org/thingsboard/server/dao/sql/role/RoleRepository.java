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
package org.thingsboard.server.dao.sql.role;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.RoleFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.RoleEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>, ExportableEntityRepository<RoleEntity> {

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(r.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<RoleEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("textSearch") String textSearch,
                                    Pageable pageable);

    Optional<RoleEntity> findByTenantIdAndName(UUID tenantId, String name);

    Optional<RoleEntity> findByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId AND r.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(r.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<RoleEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                 @Param("customerId") UUID customerId,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId " +
            "AND (r.customerId = :customerId OR r.customerId IS NULL OR r.customerId = :tenantLevelId) " +
            "AND (:textSearch IS NULL OR ilike(r.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<RoleEntity> findByTenantIdAndCustomerIdIncludingTenantLevel(@Param("tenantId") UUID tenantId,
                                                                    @Param("customerId") UUID customerId,
                                                                    @Param("tenantLevelId") UUID tenantLevelId,
                                                                    @Param("textSearch") String textSearch,
                                                                    Pageable pageable);

    List<RoleEntity> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<RoleEntity> findAllByTenantId(UUID tenantId);

    @Query("SELECT externalId FROM RoleEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.RoleFields(r.id, r.createdTime, r.tenantId, r.customerId, r.name, r.version, r.type, r.additionalInfo) FROM RoleEntity r WHERE r.id > :id ORDER BY r.id")
    List<RoleFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
