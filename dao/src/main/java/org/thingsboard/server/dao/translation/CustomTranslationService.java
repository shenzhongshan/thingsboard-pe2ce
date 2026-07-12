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
package org.thingsboard.server.dao.translation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;

import java.util.List;

/**
 * CE port of PE's custom translation service. Owner level is encoded the same way as
 * {@link org.thingsboard.server.dao.wl.WhiteLabelingService}: pass customerId=null for the
 * TENANT level, tenantId=TenantId.SYS_TENANT_ID (customerId=null) for the SYSTEM level.
 * Resolution order (later overrides earlier): SYSTEM -> TENANT -> CUSTOMER.
 */
public interface CustomTranslationService {

    /** Raw record at exactly this owner level, or null if not customized here. */
    CustomTranslation getCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    /** Locale codes customized at exactly this owner level. */
    List<String> getCustomizedLocales(TenantId tenantId, CustomerId customerId);

    /** Deep-merged effective override JSON for this owner level (walks parent levels), never null (empty object if none). */
    JsonNode getMergedCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    /** Full replace of the override value at this owner level. */
    CustomTranslation saveCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode value);

    /** Deep-merges the patch into the existing override value at this owner level. */
    CustomTranslation patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode patch);

    /** Removes a single key (dot path, e.g. "access.access-forbidden") from the override at this owner level. */
    void deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String keyPath);

    /** Deletes the whole override record at this owner level. */
    void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

}
