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
package org.thingsboard.server.common.data.translation;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

/**
 * CE port of PE's custom translation record: a set of i18n key overrides layered on top of the
 * shipped locale file at a given owner level. Owner is encoded the same way as
 * {@link org.thingsboard.server.common.data.wl.WhiteLabeling}: tenantId=SYS_TENANT_ID +
 * customerId=null for SYSTEM level, real tenantId + customerId=null for TENANT level, both real
 * for CUSTOMER level.
 */
@Data
public class CustomTranslation implements Serializable {

    private static final long serialVersionUID = 1L;

    private TenantId tenantId;
    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "localeCode", max = 10)
    @Schema(description = "Locale code, e.g. 'zh_CN' or 'en_US'", example = "zh_CN")
    private String localeCode;

    @NoXss
    @Length(fieldName = "value", max = 1000000)
    @Schema(description = "JSON object of translation key overrides, mirroring the shape of the shipped locale.constant-*.json files")
    private JsonNode value;

}
