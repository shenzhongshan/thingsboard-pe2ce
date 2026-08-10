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
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;
import org.thingsboard.server.dao.sql.blob.BlobEntityInfoRepository;
import org.thingsboard.server.dao.sql.blob.BlobEntityRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("BlobEntityDaoService")
@RequiredArgsConstructor
public class BaseBlobEntityService implements BlobEntityService {

    private final BlobEntityDao blobEntityDao;
    private final BlobEntityRepository blobEntityRepository;
    private final BlobEntityInfoRepository blobEntityInfoRepository;
    private final CustomerService customerService;

    @Override
    public BlobEntity saveBlobEntity(BlobEntity blobEntity) {
        if (blobEntity.getTenantId() == null) {
            throw new DataValidationException("Blob entity should be assigned to tenant!");
        }
        if (StringUtils.isBlank(blobEntity.getName())) {
            throw new DataValidationException("Blob entity name should be specified!");
        }
        if (StringUtils.isBlank(blobEntity.getType())) {
            throw new DataValidationException("Blob entity type should be specified!");
        }
        if (blobEntity.getData() == null) {
            throw new DataValidationException("Blob entity data should be specified!");
        }
        if (blobEntity.getCreatedTime() == 0) {
            // The table is partitioned by created_time, so it must be set before the insert
            // picks/creates a partition.
            blobEntity.setCreatedTime(System.currentTimeMillis());
        }
        return blobEntityDao.save(blobEntity.getTenantId(), blobEntity);
    }

    @Override
    public BlobEntity findBlobEntityById(TenantId tenantId, BlobEntityId blobEntityId) {
        return blobEntityDao.findById(tenantId, blobEntityId.getId());
    }

    @Override
    public BlobEntityInfo findBlobEntityInfoById(TenantId tenantId, BlobEntityId blobEntityId) {
        return blobEntityInfoRepository.findById(blobEntityId.getId()).map(e -> e.toData()).orElse(null);
    }

    @Override
    public BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(TenantId tenantId, BlobEntityId blobEntityId) {
        BlobEntityInfo info = findBlobEntityInfoById(tenantId, blobEntityId);
        return info != null ? withCustomerInfo(info) : null;
    }

    @Override
    public List<BlobEntityInfo> findBlobEntityInfoByIds(TenantId tenantId, List<BlobEntityId> blobEntityIds) {
        List<UUID> ids = blobEntityIds.stream().map(BlobEntityId::getId).toList();
        return blobEntityInfoRepository.findByIdIn(ids).stream()
                .filter(e -> tenantId.getId().equals(e.getTenantId()))
                .map(e -> (BlobEntityInfo) e.toData())
                .toList();
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(TenantId tenantId, CustomerId customerId,
                                                                           String type, TimePageLink pageLink) {
        PageData<BlobEntityInfo> page = DaoUtil.toPageData(blobEntityInfoRepository.findByTenantId(
                tenantId.getId(),
                customerId != null && !customerId.isNullUid() ? customerId.getId() : null,
                StringUtils.isNotBlank(type) ? type : null,
                pageLink.getStartTime(),
                pageLink.getEndTime(),
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
        return page.mapData(this::withCustomerInfo);
    }

    private BlobEntityWithCustomerInfo withCustomerInfo(BlobEntityInfo info) {
        BlobEntityWithCustomerInfo result = new BlobEntityWithCustomerInfo(info);
        if (info.getCustomerId() != null && !info.getCustomerId().isNullUid()) {
            Customer customer = customerService.findCustomerById(info.getTenantId(), info.getCustomerId());
            if (customer != null) {
                result.setCustomerTitle(customer.getTitle());
                result.setCustomerIsPublic(customer.isPublic());
            }
        }
        return result;
    }

    @Override
    public void deleteBlobEntity(TenantId tenantId, BlobEntityId blobEntityId) {
        blobEntityRepository.deleteById(blobEntityId.getId());
    }

    @Override
    public void deleteBlobEntitiesByTenantId(TenantId tenantId) {
        for (UUID id : blobEntityRepository.findIdsByTenantId(tenantId.getId())) {
            blobEntityRepository.deleteById(id);
        }
    }

    @Override
    public void deleteBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        for (UUID id : blobEntityRepository.findIdsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId())) {
            blobEntityRepository.deleteById(id);
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findBlobEntityInfoById(tenantId, new BlobEntityId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(Futures.immediateFuture(findEntity(tenantId, entityId)));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteBlobEntity(tenantId, new BlobEntityId(id.getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteBlobEntitiesByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.BLOB_ENTITY;
    }

}
