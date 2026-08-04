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
package org.thingsboard.server.dao.encryptionkey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service("EncryptionDaoService")
@RequiredArgsConstructor
@Slf4j
public class EncryptionServiceImpl implements EncryptionService {

    private final Map<TenantId, BytesEncryptor> encryptorMap = new ConcurrentReferenceHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final EncryptionKeyDao encryptionKeyDao;

    @Override
    public byte[] encrypt(TenantId tenantId, SecretType type, byte[] value) {
        try {
            return getEncryptor(tenantId).encrypt(value);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decryptToString(TenantId tenantId, SecretType type, byte[] encryptedValue) {
        byte[] decrypted = doDecrypt(tenantId, encryptedValue);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private byte[] doDecrypt(TenantId tenantId, byte[] encryptedValue) {
        try {
            return getEncryptor(tenantId).decrypt(encryptedValue);
        } catch (Exception e) {
            log.error("[{}] Failed to process decryption", tenantId, e);
            throw new RuntimeException("Failed to process decryption", e);
        }
    }

    private BytesEncryptor getEncryptor(TenantId tenantId) {
        return encryptorMap.computeIfAbsent(tenantId, id -> {
            EncryptionKey key = encryptionKeyDao.findByTenantId(tenantId);
            if (key == null) {
                // Regular tenants get their key provisioned on creation (see TenantServiceImpl),
                // but TenantId.SYS_TENANT_ID has no such lifecycle event - provision lazily here.
                createEncryptionKey(tenantId);
                key = encryptionKeyDao.findByTenantId(tenantId);
            }
            return Encryptors.stronger(key.getPassword(), key.getSalt());
        });
    }

    @Override
    public void createEncryptionKey(TenantId tenantId) {
        if (encryptionKeyDao.findByTenantId(tenantId) != null) {
            return;
        }
        EncryptionKey key = new EncryptionKey();
        key.setTenantId(tenantId);
        key.setPassword(generateSecurePassword());
        key.setSalt(generateSalt());
        encryptionKeyDao.save(tenantId, key);
    }

    private String generateSecurePassword() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String generateSalt() {
        byte[] randomSalt = new byte[8];
        secureRandom.nextBytes(randomSalt);
        return HexFormat.of().formatHex(randomSalt);
    }

    @Override
    public void deleteEncryptionKeyByTenantId(TenantId tenantId) {
        encryptionKeyDao.deleteByTenantId(tenantId);
    }

    @Override
    public EncryptionKey findByTenantId(TenantId tenantId) {
        return encryptionKeyDao.findByTenantId(tenantId);
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (ComponentLifecycleEvent.DELETED.equals(event.getEvent())) {
            EntityId entityId = event.getEntityId();
            if (EntityType.TENANT.equals(entityId.getEntityType())) {
                encryptorMap.remove(TenantId.fromUUID(entityId.getId()));
            }
        }
    }

}
