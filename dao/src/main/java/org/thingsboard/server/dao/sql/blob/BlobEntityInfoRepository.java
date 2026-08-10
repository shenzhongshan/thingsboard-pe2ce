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
package org.thingsboard.server.dao.sql.blob;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.BlobEntityInfoEntity;

import java.util.List;
import java.util.UUID;

public interface BlobEntityInfoRepository extends JpaRepository<BlobEntityInfoEntity, UUID> {

    /**
     * Paged blob metadata for a tenant, optionally narrowed to a customer and/or type, within
     * the [startTime, endTime] window (created_time, matching the table's partition key).
     */
    @Query("SELECT b FROM BlobEntityInfoEntity b WHERE b.tenantId = :tenantId " +
            "AND (:customerId IS NULL OR b.customerId = :customerId) " +
            "AND (:type IS NULL OR b.type = :type) " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (:searchText IS NULL OR ilike(b.name, CONCAT('%', :searchText, '%')) = true)")
    Page<BlobEntityInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              @Param("type") String type,
                                              @Param("startTime") Long startTime,
                                              @Param("endTime") Long endTime,
                                              @Param("searchText") String searchText,
                                              Pageable pageable);

    List<BlobEntityInfoEntity> findByIdIn(List<UUID> ids);

}
