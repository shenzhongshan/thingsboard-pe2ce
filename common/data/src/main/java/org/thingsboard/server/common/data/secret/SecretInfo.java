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
package org.thingsboard.server.common.data.secret;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema
public class SecretInfo extends BaseData<SecretId> implements HasTenantId, HasName {

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the secret cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @NotBlank
    @Length(fieldName = "name")
    @Schema(description = "Secret name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Secret")
    private String name;

    @NotNull
    @Schema(description = "Secret type.", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT")
    private SecretType type;

    @NoXss
    @Length(fieldName = "description")
    @Schema(description = "Secret description.", example = "Secret description")
    private String description;

    public SecretInfo(SecretId id) {
        super(id);
    }

    public SecretInfo(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId();
        this.name = secretInfo.getName();
        this.type = secretInfo.getType();
        this.description = secretInfo.getDescription();
    }

    @Schema(description = "JSON object with the Secret Id. Specify this field to update the Secret. Referencing non-existing Secret Id will cause error. Omit this field to create new Secret.")
    @Override
    public SecretId getId() {
        return super.getId();
    }

    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
