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
package org.thingsboard.server.dao.sql.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.ReportTemplateEntity;

import java.util.List;
import java.util.UUID;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplateEntity, UUID>,
        ExportableEntityRepository<ReportTemplateEntity> {

    ReportTemplateEntity findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT t FROM ReportTemplateEntity t WHERE t.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(t.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportTemplateEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT t FROM ReportTemplateEntity t WHERE t.tenantId = :tenantId AND t.customerId = :customerId " +
            "AND (:searchText IS NULL OR ilike(t.name, CONCAT('%', :searchText, '%')) = true)")
    Page<ReportTemplateEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                            @Param("customerId") UUID customerId,
                                                            @Param("searchText") String searchText,
                                                            Pageable pageable);

    @Query("SELECT t.id FROM ReportTemplateEntity t WHERE t.tenantId = :tenantId")
    List<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT externalId FROM ReportTemplateEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
