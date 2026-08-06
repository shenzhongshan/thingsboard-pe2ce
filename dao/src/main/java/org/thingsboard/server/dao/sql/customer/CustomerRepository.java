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
package org.thingsboard.server.dao.sql.customer;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.CustomerFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.CustomerEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, ExportableEntityRepository<CustomerEntity> {

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<CustomerEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                        @Param("textSearch") String textSearch,
                                        Pageable pageable);

    CustomerEntity findByTenantIdAndTitle(UUID tenantId, String title);

    List<CustomerEntity> findByTenantIdAndCustomMenuId(UUID tenantId, UUID customMenuId);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'CUSTOMER', a.title) " +
            "FROM CustomerEntity a WHERE a.tenantId = :tenantId AND a.title LIKE CONCAT(:prefix, '%')")
    List<EntityInfo> findEntityInfosByNamePrefix(UUID tenantId, String prefix);

    @Query(value = "SELECT * FROM customer c WHERE c.tenant_id = :tenantId " +
            "AND c.is_public IS TRUE ORDER BY c.id ASC LIMIT 1", nativeQuery = true)
    CustomerEntity findPublicCustomerByTenantId(@Param("tenantId") UUID tenantId);


    Long countByTenantId(UUID tenantId);

    @Query("SELECT externalId FROM CustomerEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query(value = "SELECT c.* FROM customer c " +
            "INNER JOIN (SELECT tenant_id, title FROM customer GROUP BY tenant_id, title HAVING COUNT(title) > 1) dc " +
            "ON c.tenant_id = dc.tenant_id AND c.title = dc.title " +
            "ORDER BY c.tenant_id, c.title, c.id",
            nativeQuery = true)
    Page<CustomerEntity> findCustomersWithTheSameTitle(Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.CustomerFields(c.id, c.createdTime, c.tenantId, " +
            "c.title, c.version, c.additionalInfo, c.country, c.state, c.city, c.address, c.address2, c.zip, c.phone, c.email) " +
            "FROM CustomerEntity c WHERE c.id > :id ORDER BY c.id")
    List<CustomerFields> findNextBatch(@Param("id") UUID id, Limit limit);

    List<CustomerEntity> findCustomersByTenantIdAndIdIn(UUID tenantId, List<UUID> customerIds);

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId AND c.parentCustomerId = :parentCustomerId " +
            "AND (:textSearch IS NULL OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<CustomerEntity> findByTenantIdAndParentCustomerId(@Param("tenantId") UUID tenantId,
                                                            @Param("parentCustomerId") UUID parentCustomerId,
                                                            @Param("textSearch") String textSearch,
                                                            Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId AND c.parentCustomerId IS NULL " +
            "AND (:textSearch IS NULL OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<CustomerEntity> findByTenantIdAndParentCustomerIdIsNull(@Param("tenantId") UUID tenantId,
                                                                  @Param("textSearch") String textSearch,
                                                                  Pageable pageable);

    /** All descendants (any depth) of the given customer, via a recursive walk down parent_customer_id. Excludes the root itself. */
    @Query(value = "WITH RECURSIVE sub_customers(id) AS (" +
            "  SELECT id FROM customer WHERE tenant_id = :tenantId AND id = :customerId " +
            "  UNION " +
            "  SELECT c.id FROM customer c, sub_customers sc WHERE c.tenant_id = :tenantId AND c.parent_customer_id = sc.id" +
            ") " +
            "SELECT c.* FROM customer c WHERE c.tenant_id = :tenantId AND c.parent_customer_id IN (SELECT id FROM sub_customers) " +
            "AND (CAST(:textSearch AS text) IS NULL OR c.title ILIKE CONCAT('%', CAST(:textSearch AS text), '%')) " +
            "ORDER BY c.title",
            countQuery = "WITH RECURSIVE sub_customers(id) AS (" +
            "  SELECT id FROM customer WHERE tenant_id = :tenantId AND id = :customerId " +
            "  UNION " +
            "  SELECT c.id FROM customer c, sub_customers sc WHERE c.tenant_id = :tenantId AND c.parent_customer_id = sc.id" +
            ") " +
            "SELECT count(*) FROM customer c WHERE c.tenant_id = :tenantId AND c.parent_customer_id IN (SELECT id FROM sub_customers) " +
            "AND (CAST(:textSearch AS text) IS NULL OR c.title ILIKE CONCAT('%', CAST(:textSearch AS text), '%'))",
            nativeQuery = true)
    Page<CustomerEntity> findByTenantIdAndCustomerIdIncludingSubCustomers(@Param("tenantId") UUID tenantId,
                                                                           @Param("customerId") UUID customerId,
                                                                           @Param("textSearch") String textSearch,
                                                                           Pageable pageable);

    /** Direct + transitive ancestor chain of the given customer (excluding itself), root-first. */
    @Query(value = "WITH RECURSIVE ancestors(id, parent_customer_id, depth) AS (" +
            "  SELECT id, parent_customer_id, 0 FROM customer WHERE tenant_id = :tenantId AND id = :customerId " +
            "  UNION ALL " +
            "  SELECT c.id, c.parent_customer_id, a.depth + 1 FROM customer c, ancestors a " +
            "  WHERE c.tenant_id = :tenantId AND c.id = a.parent_customer_id" +
            ") " +
            "SELECT c.* FROM customer c JOIN ancestors a ON c.id = a.id " +
            "WHERE a.depth > 0 ORDER BY a.depth DESC",
            nativeQuery = true)
    List<CustomerEntity> findAncestors(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

    @Query("SELECT r.toId, eg.name FROM RelationEntity r JOIN EntityGroupEntity eg ON eg.id = r.fromId " +
            "WHERE r.toId IN :ids AND r.toType = 'CUSTOMER' AND eg.type = org.thingsboard.server.common.data.EntityType.CUSTOMER " +
            "AND r.relationTypeGroup = 'FROM_ENTITY_GROUP' AND r.relationType = 'Contains'")
    List<Object[]> findGroupNamesByCustomerIds(@Param("ids") List<UUID> ids);

    @Query("SELECT c.id, p.title FROM CustomerEntity c JOIN CustomerEntity p ON p.id = c.parentCustomerId " +
            "WHERE c.id IN :ids AND c.parentCustomerId IS NOT NULL")
    List<Object[]> findParentCustomerTitlesByIds(@Param("ids") List<UUID> ids);
}
