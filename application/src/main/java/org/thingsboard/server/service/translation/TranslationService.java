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
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.TranslationInfo;

import java.util.List;
import java.util.Set;

/**
 * Resolves the effective end-user translation by layering the tenant/customer custom translation
 * (see {@link org.thingsboard.server.dao.translation.CustomTranslationService}) on top of the
 * locale JSON files bundled with the UI on the classpath under {@code public/assets/locale}.
 */
public interface TranslationService {

    /** Locale metadata (display names, translation progress, whether customized at this level) for every available locale. */
    List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId);

    /** All locale codes visible to this owner: bundled system locales plus locales customized at any parent level. */
    Set<String> getAvailableLocaleCodes(TenantId tenantId, CustomerId customerId);

    /** The subset of the translation needed to render the login page, resolved via the login white labeling of the domain. */
    JsonNode getLoginTranslation(String localeCode, String domainName);

    /** System locale translation deep-merged with the custom translation of this owner. */
    JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    /**
     * Same shape as the full translation, but every leaf is replaced with an info object:
     * {@code o} original (en_US) text, {@code p} parent-level translation, {@code t} effective translation,
     * {@code s} status - {@code C}ustom, {@code T}ranslated, {@code U}ntranslated, {@code A}dded (key exists only here).
     */
    JsonNode getTranslationForBasicEdit(TenantId tenantId, CustomerId customerId, String localeCode);

}
