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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.EncryptionKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "encryption_key")
public class EncryptionKeyEntity extends BaseSqlEntity<EncryptionKey> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "password")
    private String password;

    @Column(name = "salt")
    private String salt;

    public EncryptionKeyEntity(EncryptionKey encryptionKey) {
        super(encryptionKey);
        this.tenantId = encryptionKey.getTenantId().getId();
        this.password = encryptionKey.getPassword();
        this.salt = encryptionKey.getSalt();
    }

    @Override
    public EncryptionKey toData() {
        EncryptionKey encryptionKey = new EncryptionKey(new EncryptionKeyId(id));
        encryptionKey.setCreatedTime(createdTime);
        encryptionKey.setTenantId(TenantId.fromUUID(tenantId));
        encryptionKey.setPassword(password);
        encryptionKey.setSalt(salt);
        return encryptionKey;
    }

}
