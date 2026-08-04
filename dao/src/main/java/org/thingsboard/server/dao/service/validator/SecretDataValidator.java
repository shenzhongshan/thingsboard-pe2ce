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
package org.thingsboard.server.dao.service.validator;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.regex.Pattern;

@Component
public class SecretDataValidator extends DataValidator<Secret> {

    // '{', '}' and ';' are reserved for the ${secret:<name>;type:<type>} placeholder syntax.
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[^{};\\p{Cntrl}]+$");
    private static final int MAX_FILE_SIZE_BYTES = 524288;
    private static final int MAX_TEXT_SIZE_LENGTH = 2048;

    @Lazy
    private final SecretService secretService;

    public SecretDataValidator(@Lazy SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Secret secret) {
        if (!VALID_NAME_PATTERN.matcher(secret.getName()).matches()) {
            throw new DataValidationException("Secret name contains unsupported characters. It must not include '{', '}', ';' or control characters.");
        }
        String value = secret.getValue();
        if (value != null) {
            if (SecretType.TEXT.equals(secret.getType())) {
                if (value.length() > MAX_TEXT_SIZE_LENGTH) {
                    throw new DataValidationException(String.format("Secret value is %d characters; exceeds maximum of %d characters", value.length(), MAX_TEXT_SIZE_LENGTH));
                }
            } else if (value.length() > MAX_FILE_SIZE_BYTES) {
                throw new DataValidationException(String.format("Secret file size is %d bytes; exceeds the maximum of %d bytes", value.length(), MAX_FILE_SIZE_BYTES));
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, Secret secret) {
        if (secret.getValue() == null) {
            throw new DataValidationException("Secret must contain value");
        }
    }

    @Override
    protected Secret validateUpdate(TenantId tenantId, Secret secret) {
        Secret old = secretService.findSecretById(tenantId, secret.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing secret!");
        }
        if (!old.getName().equals(secret.getName())) {
            throw new DataValidationException("Can't update secret name!");
        }
        if (!old.getType().equals(secret.getType())) {
            throw new DataValidationException("Can't update secret type!");
        }
        return old;
    }

}
