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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;
import org.thingsboard.server.dao.model.sql.CustomTranslationEntity;
import org.thingsboard.server.dao.sql.translation.CustomTranslationRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseCustomTranslationService implements CustomTranslationService {

    private static final Pattern LOCALE_CODE_PATTERN = Pattern.compile("^[a-z]{2}(_[A-Z]{2})?$");

    private final CustomTranslationRepository customTranslationRepository;

    @Override
    public CustomTranslation getCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        UUID ownerCustomerId = ownerCustomerId(customerId);
        return customTranslationRepository.findById(new CustomTranslationCompositeKey(tenantId.getId(), ownerCustomerId, localeCode))
                .map(CustomTranslationEntity::toData).orElse(null);
    }

    @Override
    public List<String> getCustomizedLocales(TenantId tenantId, CustomerId customerId) {
        UUID ownerCustomerId = ownerCustomerId(customerId);
        return customTranslationRepository.findAllByTenantIdAndCustomerId(tenantId.getId(), ownerCustomerId).stream()
                .map(CustomTranslationEntity::getLocaleCode).toList();
    }

    @Override
    public JsonNode getMergedCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode result = JacksonUtil.newObjectNode();
        CustomTranslation system = getCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode);
        result = deepMerge(result, system != null ? system.getValue() : null);
        if (!tenantId.isSysTenantId()) {
            CustomTranslation tenant = getCustomTranslation(tenantId, null, localeCode);
            result = deepMerge(result, tenant != null ? tenant.getValue() : null);
        }
        if (customerId != null) {
            CustomTranslation customer = getCustomTranslation(tenantId, customerId, localeCode);
            result = deepMerge(result, customer != null ? customer.getValue() : null);
        }
        return result;
    }

    @Override
    public CustomTranslation saveCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode value) {
        validateLocaleCode(localeCode);
        CustomTranslation customTranslation = new CustomTranslation();
        customTranslation.setTenantId(tenantId);
        customTranslation.setCustomerId(customerId);
        customTranslation.setLocaleCode(localeCode);
        customTranslation.setValue(value != null ? value : JacksonUtil.newObjectNode());
        return customTranslationRepository.save(new CustomTranslationEntity(customTranslation)).toData();
    }

    @Override
    public CustomTranslation patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode patch) {
        CustomTranslation existing = getCustomTranslation(tenantId, customerId, localeCode);
        JsonNode merged = deepMerge(existing != null ? existing.getValue() : JacksonUtil.newObjectNode(), patch);
        return saveCustomTranslation(tenantId, customerId, localeCode, merged);
    }

    @Override
    public void deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String keyPath) {
        CustomTranslation existing = getCustomTranslation(tenantId, customerId, localeCode);
        if (existing == null || existing.getValue() == null) {
            return;
        }
        JsonNode value = existing.getValue().deepCopy();
        deleteKeyPath(value, keyPath);
        saveCustomTranslation(tenantId, customerId, localeCode, value);
    }

    @Override
    public void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        UUID ownerCustomerId = ownerCustomerId(customerId);
        customTranslationRepository.deleteById(new CustomTranslationCompositeKey(tenantId.getId(), ownerCustomerId, localeCode));
    }

    private UUID ownerCustomerId(CustomerId customerId) {
        return customerId != null ? customerId.getId() : EntityId.NULL_UUID;
    }

    private void validateLocaleCode(String localeCode) {
        if (StringUtils.isBlank(localeCode) || !LOCALE_CODE_PATTERN.matcher(localeCode).matches()) {
            throw new DataValidationException("Invalid locale code! Expected format: 'xx' or 'xx_XX'.");
        }
    }

    /** Later-wins deep merge of two JSON trees; non-object values are replaced outright. */
    private JsonNode deepMerge(JsonNode base, JsonNode override) {
        if (override == null || override.isNull()) {
            return base != null ? base : JacksonUtil.newObjectNode();
        }
        if (base == null || base.isNull() || !base.isObject() || !override.isObject()) {
            return override;
        }
        ObjectNode result = ((ObjectNode) base).deepCopy();
        Iterator<String> fieldNames = override.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            result.set(field, deepMerge(result.get(field), override.get(field)));
        }
        return result;
    }

    private void deleteKeyPath(JsonNode root, String keyPath) {
        String[] parts = keyPath.split("\\.");
        JsonNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (current == null || !current.isObject()) {
                return;
            }
            current = current.get(parts[i]);
        }
        if (current != null && current.isObject()) {
            ((ObjectNode) current).remove(parts[parts.length - 1]);
        }
    }

}
