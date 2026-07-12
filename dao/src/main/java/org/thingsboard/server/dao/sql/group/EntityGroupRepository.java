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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityGroupRepository extends JpaRepository<EntityGroupEntity, UUID> {

    // owner_id = tenant_id for tenant-owned groups, a customer id for customer-owned groups
    @Query("SELECT g FROM EntityGroupEntity g WHERE g.ownerId = :ownerId AND g.type = :type " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EntityGroupEntity> findByOwnerIdAndType(@Param("ownerId") UUID ownerId,
                                                 @Param("type") EntityType type,
                                                 @Param("textSearch") String textSearch,
                                                 Pageable pageable);

    Optional<EntityGroupEntity> findByOwnerIdAndTypeAndName(UUID ownerId, EntityType type, String name);

    List<EntityGroupEntity> findAllByTenantId(UUID tenantId);

}
