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
package org.thingsboard.server.dao.sql.blob;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.dao.blob.BlobEntityDao;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaBlobEntityDao extends JpaPartitionedAbstractDao<BlobEntityEntity, BlobEntity> implements BlobEntityDao {

    private static final String TABLE_NAME = "blob_entity";

    private final BlobEntityRepository blobEntityRepository;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.blob_entities.partition_size:168}")
    private int partitionSizeInHours;

    @Override
    protected Class<BlobEntityEntity> getEntityClass() {
        return BlobEntityEntity.class;
    }

    @Override
    protected JpaRepository<BlobEntityEntity, UUID> getRepository() {
        return blobEntityRepository;
    }

    @Override
    public void createPartition(BlobEntityEntity entity) {
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, entity.getCreatedTime(),
                TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.BLOB_ENTITY;
    }

}
