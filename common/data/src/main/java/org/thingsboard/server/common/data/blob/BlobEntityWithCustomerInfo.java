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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Blob metadata enriched with the owning customer's title, for list views. */
@Schema
@EqualsAndHashCode(callSuper = true)
public class BlobEntityWithCustomerInfo extends BlobEntityInfo {

    private static final long serialVersionUID = 4771264926280948453L;

    @Getter @Setter
    @Schema(description = "Title of the customer that owns the blob entity", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;

    @Getter @Setter
    @Schema(description = "Whether the owning customer is public", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    public BlobEntityWithCustomerInfo() {
        super();
    }

    public BlobEntityWithCustomerInfo(BlobEntityInfo blobEntityInfo) {
        super(blobEntityInfo);
    }

}
