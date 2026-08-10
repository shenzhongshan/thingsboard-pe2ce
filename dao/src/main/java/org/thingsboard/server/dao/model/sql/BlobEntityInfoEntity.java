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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

/**
 * Blob metadata without the payload column, so listings never pull blob contents into memory.
 * Read-only: writes go through {@link BlobEntityEntity}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = "blob_entity")
@NoArgsConstructor
public class BlobEntityInfoEntity extends BaseSqlEntity<BlobEntityInfo> implements BaseEntity<BlobEntityInfo> {

    @Column(name = "tenant_id")
    private UUID tenantId;
    @Column(name = "customer_id")
    private UUID customerId;
    @Column(name = "name")
    private String name;
    @Column(name = "type")
    private String type;
    @Column(name = "content_type")
    private String contentType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    @Override
    public BlobEntityInfo toData() {
        BlobEntityInfo info = new BlobEntityInfo(new BlobEntityId(getUuid()));
        info.setCreatedTime(createdTime);
        if (tenantId != null) {
            info.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            info.setCustomerId(new CustomerId(customerId));
        }
        info.setName(name);
        info.setType(type);
        info.setContentType(contentType);
        info.setAdditionalInfo(additionalInfo);
        return info;
    }

}
