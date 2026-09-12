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
package org.thingsboard.server.dao.sql.scheduler;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.SchedulerEventFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventRepository extends JpaRepository<SchedulerEventEntity, UUID>,
        ExportableEntityRepository<SchedulerEventEntity> {

    SchedulerEventEntity findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT s FROM SchedulerEventEntity s WHERE s.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(s.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<SchedulerEventEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                              @Param("textSearch") String textSearch,
                                              Pageable pageable);

    @Query("SELECT externalId FROM SchedulerEventEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.SchedulerEventFields(e.id, e.createdTime, " +
            "e.tenantId, e.customerId, e.name, e.version, e.type, e.schedule, e.configuration, e.additionalInfo, " +
            "e.originatorId, e.originatorType) FROM SchedulerEventEntity e WHERE e.id > :id ORDER BY e.id")
    List<SchedulerEventFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
