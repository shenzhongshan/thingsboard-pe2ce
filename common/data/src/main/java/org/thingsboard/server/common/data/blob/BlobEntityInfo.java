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
package org.thingsboard.server.common.data.blob;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Metadata of a stored binary blob (generated report files and similar), without the payload.
 * The payload lives on {@link BlobEntity}, so listings never load blob contents.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class BlobEntityInfo extends BaseData<BlobEntityId> implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 2807223040519549363L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @Schema(description = "JSON object with Customer Id", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @Getter @Setter
    @NoXss
    @Schema(description = "Blob entity name (used as the download file name)", example = "report.pdf")
    private String name;

    @Getter @Setter
    @NoXss
    @Schema(description = "Blob entity type", example = "report")
    private String type;

    @Getter @Setter
    @NoXss
    @Schema(description = "Content type of the stored data", example = "application/pdf")
    private String contentType;

    @Getter @Setter
    @Schema(description = "Additional parameters of the blob entity", implementation = JsonNode.class)
    private JsonNode additionalInfo;

    public BlobEntityInfo() {
        super();
    }

    public BlobEntityInfo(BlobEntityId id) {
        super(id);
    }

    public BlobEntityInfo(BlobEntityInfo blobEntityInfo) {
        super(blobEntityInfo);
        this.tenantId = blobEntityInfo.getTenantId();
        this.customerId = blobEntityInfo.getCustomerId();
        this.name = blobEntityInfo.getName();
        this.type = blobEntityInfo.getType();
        this.contentType = blobEntityInfo.getContentType();
        this.additionalInfo = blobEntityInfo.getAdditionalInfo();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "BlobEntityInfo [name=" + name + ", type=" + type + ", contentType=" + contentType + "]";
    }

}
