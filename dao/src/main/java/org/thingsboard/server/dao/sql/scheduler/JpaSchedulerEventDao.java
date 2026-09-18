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
package org.thingsboard.server.dao.sql.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edqs.fields.SchedulerEventFields;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity;
import org.thingsboard.server.dao.scheduler.SchedulerEventDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaSchedulerEventDao extends JpaAbstractDao<SchedulerEventEntity, SchedulerEvent> implements SchedulerEventDao {

    private final SchedulerEventRepository schedulerEventRepository;
    private final SchedulerEventInfoRepository schedulerEventInfoRepository;

    @Override
    protected Class<SchedulerEventEntity> getEntityClass() {
        return SchedulerEventEntity.class;
    }

    @Override
    protected JpaRepository<SchedulerEventEntity, UUID> getRepository() {
        return schedulerEventRepository;
    }

    @Override
    public SchedulerEvent findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(schedulerEventRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public SchedulerEvent findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(schedulerEventRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<SchedulerEvent> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(schedulerEventRepository.findByTenantId(
                tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public SchedulerEventId getExternalIdByInternal(SchedulerEventId internalId) {
        return Optional.ofNullable(schedulerEventRepository.getExternalIdById(internalId.getId()))
                .map(SchedulerEventId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

    /**
     * Feeds {@code EdqsSyncService#syncTenantEntity} - required now that SCHEDULER_EVENT is one of
     * {@code ObjectType#edqsTenantTypes}, otherwise the initial sync throws UnsupportedOperationException.
     */
    @Override
    public List<SchedulerEventFields> findNextBatch(UUID id, int batchSize) {
        return schedulerEventRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public SchedulerEvent save(TenantId tenantId, SchedulerEvent schedulerEvent) {
        if (schedulerEvent.getId() == null) {
            schedulerEvent.setCreatedTime(System.currentTimeMillis());
        }
        return schedulerEventRepository.save(new SchedulerEventEntity(schedulerEvent)).toData();
    }

    @Override
    public SchedulerEvent findById(TenantId tenantId, SchedulerEventId id) {
        return DaoUtil.getData(schedulerEventRepository.findById(id.getId()));
    }

    @Override
    public SchedulerEventInfo findInfoById(TenantId tenantId, SchedulerEventId id) {
        return DaoUtil.getData(schedulerEventInfoRepository.findById(id.getId()));
    }

    @Override
    public SchedulerEventWithCustomerInfo findWithCustomerInfoById(TenantId tenantId, SchedulerEventId id) {
        SchedulerEventWithCustomerInfoEntity entity = schedulerEventInfoRepository.findSchedulerEventWithCustomerInfoById(id.getId());
        return entity != null ? entity.toData() : null;
    }

    @Override
    public List<SchedulerEventInfo> findInfosByTenantId(TenantId tenantId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository.findSchedulerEventInfoEntitiesByTenantId(tenantId.getId()));
    }

    @Override
    public List<SchedulerEventInfo> findInfosByTenantIdAndEnabled(TenantId tenantId, boolean enabled) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository.findSchedulerEventInfoEntitiesByTenantIdAndEnabled(tenantId.getId(), enabled));
    }

    @Override
    public PageData<SchedulerEventWithCustomerInfo> findByTenantIdAndFilter(TenantId tenantId, SchedulerEventFilter filter, PageLink pageLink) {
        UUID customerId = filter != null && filter.hasCustomerId() ? filter.getCustomerId().getId() : null;
        String type = filter != null ? filter.getType() : null;
        return DaoUtil.toPageData(schedulerEventInfoRepository.findByTenantIdAndCustomerIdAndTypeAndSearchText(
                tenantId.getId(), customerId, type, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<UUID> findIdsByTenantId(TenantId tenantId) {
        return schedulerEventInfoRepository.findIdsByTenantId(tenantId.getId());
    }

    @Override
    public List<UUID> findIdsByTenantIdAndCustomerId(TenantId tenantId, UUID customerId) {
        return schedulerEventInfoRepository.findIdsByTenantIdAndCustomerId(tenantId.getId(), customerId);
    }

    @Override
    public void removeById(TenantId tenantId, SchedulerEventId id) {
        schedulerEventRepository.deleteById(id.getId());
    }

}
