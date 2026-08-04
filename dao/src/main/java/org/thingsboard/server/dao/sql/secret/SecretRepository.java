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
package org.thingsboard.server.dao.sql.secret;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.SecretEntity;

import java.util.UUID;

@Repository
public interface SecretRepository extends JpaRepository<SecretEntity, UUID> {

    @Query("SELECT d FROM SecretEntity d WHERE d.tenantId = :tenantId AND (:searchText IS NULL OR " +
           "ilike(d.name, concat('%', :searchText, '%')) = true OR ilike(d.description, concat('%', :searchText, '%')) = true)")
    Page<SecretEntity> findByTenantId(@Param("tenantId") UUID tenantId, @Param("searchText") String searchText, Pageable pageable);

    SecretEntity findByTenantIdAndName(UUID tenantId, String name);

    @Transactional
    @Modifying
    @Query("DELETE FROM SecretEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}
