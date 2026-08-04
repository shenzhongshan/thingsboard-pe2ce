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
package org.thingsboard.server.dao.secret;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbSecretDeleteResult;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.validator.SecretDataValidator;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretServiceImpl extends AbstractEntityService implements SecretService {

    private static final String INCORRECT_SECRET_ID = "Incorrect secretId ";

    private final SecretDao secretDao;
    private final SecretInfoDao secretInfoDao;
    private final SecretDataValidator secretValidator;
    private final EncryptionService encryptionService;

    @Override
    public Secret saveSecret(TenantId tenantId, Secret secret) {
        log.trace("Executing saveSecret [{}]", secret);
        try {
            Secret old = secretValidator.validate(secret, SecretInfo::getTenantId);
            if (secret.getValue() != null) {
                byte[] encrypted = encryptionService.encrypt(tenantId, secret.getType(), secret.getValue().getBytes());
                secret.setEncryptedValue(encrypted);
            } else if (old != null) {
                secret.setEncryptedValue(old.getEncryptedValue());
            }
            boolean created = secret.getId() == null;
            Secret savedSecret = secretDao.save(tenantId, secret);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedSecret.getId())
                    .entity(savedSecret).created(created).build());
            return savedSecret;
        } catch (Exception e) {
            checkConstraintViolation(e, "secret_unq_key", "Secret with such name already exists!");
            throw e;
        }
    }

    @Override
    public TbSecretDeleteResult deleteSecret(TenantId tenantId, SecretInfo secretInfo) {
        SecretId secretId = secretInfo.getId();
        log.trace("Executing deleteSecret [{}]", secretId);
        secretDao.removeById(tenantId, secretId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(secretInfo.getId()).build());
        return TbSecretDeleteResult.builder().success(true).build();
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        secretDao.removeById(tenantId, id.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteSecretsByTenantId, tenantId [{}]", tenantId);
        secretDao.deleteByTenantId(tenantId);
    }

    @Override
    public Secret findSecretById(TenantId tenantId, SecretId secretId) {
        return secretDao.findById(tenantId, secretId.getId());
    }

    @Override
    public SecretInfo findSecretInfoById(TenantId tenantId, SecretId secretId) {
        return secretInfoDao.findById(tenantId, secretId.getId());
    }

    @Override
    public Secret findSecretByName(TenantId tenantId, String name) {
        return secretDao.findByName(tenantId, name);
    }

    @Override
    public SecretInfo findSecretInfoByName(TenantId tenantId, String name) {
        return secretInfoDao.findByName(tenantId, name);
    }

    @Override
    public List<String> findSecretNamesByTenantId(TenantId tenantId) {
        return secretInfoDao.findAllNamesByTenantId(tenantId);
    }

    @Override
    public PageData<SecretInfo> findSecretInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        return secretInfoDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findSecretById(tenantId, new SecretId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(secretDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
