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
package org.thingsboard.server.service.secret;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${secret:<name>;type:<type>}} placeholders anywhere in a config JSON tree (or
 * a single string) into the tenant's decrypted secret value. The {@code type} token is part of
 * the placeholder syntax but is not used to select behavior here - it exists so the UI/user can
 * see at a glance what kind of secret a placeholder refers to.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSecretConfigurationService implements SecretConfigurationService {

    private static final Pattern SECRET_PATTERN = Pattern.compile("\\$\\{secret:([^;{}]+);type:([^;{}]+)}");

    private final SecretService secretService;
    private final EncryptionService encryptionService;

    @Override
    public void replaceSecretUsages(TenantId tenantId, JsonNode config) {
        replaceAllSecretUsages(tenantId, config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T replaceSecretUsages(TenantId tenantId, T entity, Class<T> clazz) {
        JsonNode config = JacksonUtil.valueToTree(entity);
        JsonNode replaced = replaceAllSecretUsages(tenantId, config);
        return JacksonUtil.treeToValue(replaced, clazz);
    }

    @Override
    public String replaceSecretUsage(TenantId tenantId, String value) {
        Matcher matcher = SECRET_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            Secret secret = secretService.findSecretByName(tenantId, name);
            String replacement = "";
            if (secret != null) {
                replacement = encryptionService.decryptToString(tenantId, secret.getType(), secret.getEncryptedValue());
            } else {
                log.debug("[{}] Secret placeholder references unknown secret '{}'", tenantId, name);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private JsonNode replaceAllSecretUsages(TenantId tenantId, JsonNode config) {
        JacksonUtil.replaceAll(config, "", (path, value) -> replaceSecretUsage(tenantId, value));
        return config;
    }

}
