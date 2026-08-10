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

import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

/**
 * CE port of PE's blob entity service — storage for generated binary artifacts (report files
 * and similar). Dropped vs PE: async lookups.
 */
public interface BlobEntityService extends EntityDaoService {

    BlobEntity saveBlobEntity(BlobEntity blobEntity);

    /** Full blob including the payload. Use the info variants for listings. */
    BlobEntity findBlobEntityById(TenantId tenantId, BlobEntityId blobEntityId);

    BlobEntityInfo findBlobEntityInfoById(TenantId tenantId, BlobEntityId blobEntityId);

    BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(TenantId tenantId, BlobEntityId blobEntityId);

    List<BlobEntityInfo> findBlobEntityInfoByIds(TenantId tenantId, List<BlobEntityId> blobEntityIds);

    /** customerId and type are optional filters; either may be null. */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(TenantId tenantId, CustomerId customerId,
                                                                    String type, TimePageLink pageLink);

    void deleteBlobEntity(TenantId tenantId, BlobEntityId blobEntityId);

    void deleteBlobEntitiesByTenantId(TenantId tenantId);

    void deleteBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

}
