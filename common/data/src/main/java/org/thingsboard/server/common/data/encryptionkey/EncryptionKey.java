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
package org.thingsboard.server.common.data.encryptionkey;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EncryptionKeyId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * Per-tenant symmetric key material used to encrypt {@link org.thingsboard.server.common.data.secret.Secret}
 * values at rest. Never exposed via REST API.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EncryptionKey extends BaseData<EncryptionKeyId> implements HasTenantId {

    private TenantId tenantId;
    private String password;
    private String salt;

    public EncryptionKey(EncryptionKeyId id) {
        super(id);
    }

}
