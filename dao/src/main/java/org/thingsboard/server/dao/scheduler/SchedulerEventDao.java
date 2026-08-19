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
package org.thingsboard.server.dao.scheduler;

import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;
import java.util.UUID;

/**
 * Also a {@link Dao}/{@link ExportableEntityDao} so the version-control export/import framework —
 * which scans Dao&lt;?&gt; beans by getEntityType() — can resolve scheduler events. The bespoke
 * methods below stay; the generic Dao contract is served by JpaAbstractDao.
 */
public interface SchedulerEventDao extends Dao<SchedulerEvent>, ExportableEntityDao<SchedulerEventId, SchedulerEvent> {

    SchedulerEvent save(TenantId tenantId, SchedulerEvent schedulerEvent);

    SchedulerEvent findById(TenantId tenantId, SchedulerEventId id);

    SchedulerEventInfo findInfoById(TenantId tenantId, SchedulerEventId id);

    SchedulerEventWithCustomerInfo findWithCustomerInfoById(TenantId tenantId, SchedulerEventId id);

    List<SchedulerEventInfo> findInfosByTenantId(TenantId tenantId);

    List<SchedulerEventInfo> findInfosByTenantIdAndEnabled(TenantId tenantId, boolean enabled);

    PageData<SchedulerEventWithCustomerInfo> findByTenantIdAndFilter(TenantId tenantId, SchedulerEventFilter filter, PageLink pageLink);

    List<UUID> findIdsByTenantId(TenantId tenantId);

    List<UUID> findIdsByTenantIdAndCustomerId(TenantId tenantId, UUID customerId);

    void removeById(TenantId tenantId, SchedulerEventId id);

}
