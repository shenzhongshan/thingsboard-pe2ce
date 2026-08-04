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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractSecretInfoEntity<T extends SecretInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SecretType type;

    @Column(name = "description")
    private String description;

    public AbstractSecretInfoEntity(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId().getId();
        this.name = secretInfo.getName();
        this.type = secretInfo.getType();
        this.description = secretInfo.getDescription();
    }

    protected SecretInfo toSecretInfo() {
        SecretInfo secretInfo = new SecretInfo(new SecretId(this.getUuid()));
        secretInfo.setCreatedTime(createdTime);
        secretInfo.setTenantId(TenantId.fromUUID(tenantId));
        secretInfo.setName(name);
        secretInfo.setType(type);
        secretInfo.setDescription(description);
        return secretInfo;
    }

}
