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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Stored binary blob. The payload column is varchar holding base64 (matching PE), not bytea,
 * so blobs stay portable across the SQL/Timescale variants.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "blob_entity")
@NoArgsConstructor
public class BlobEntityEntity extends BaseSqlEntity<BlobEntity> implements BaseEntity<BlobEntity> {

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
    @Column(name = "data")
    private String data;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_info")
    private JsonNode additionalInfo;

    public BlobEntityEntity(BlobEntity blobEntity) {
        if (blobEntity.getId() != null) {
            this.setUuid(blobEntity.getId().getId());
        }
        this.setCreatedTime(blobEntity.getCreatedTime());
        if (blobEntity.getTenantId() != null) {
            this.tenantId = blobEntity.getTenantId().getId();
        }
        if (blobEntity.getCustomerId() != null) {
            this.customerId = blobEntity.getCustomerId().getId();
        }
        this.name = blobEntity.getName();
        this.type = blobEntity.getType();
        this.contentType = blobEntity.getContentType();
        this.additionalInfo = blobEntity.getAdditionalInfo();
        if (blobEntity.getData() != null) {
            this.data = Base64.getEncoder().encodeToString(blobEntity.getData().array());
        }
    }

    @Override
    public BlobEntity toData() {
        BlobEntity blobEntity = new BlobEntity(new BlobEntityId(getUuid()));
        blobEntity.setCreatedTime(createdTime);
        if (tenantId != null) {
            blobEntity.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            blobEntity.setCustomerId(new CustomerId(customerId));
        }
        blobEntity.setName(name);
        blobEntity.setType(type);
        blobEntity.setContentType(contentType);
        blobEntity.setAdditionalInfo(additionalInfo);
        if (data != null) {
            blobEntity.setData(ByteBuffer.wrap(Base64.getDecoder().decode(data)));
        }
        return blobEntity;
    }

}
