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
package org.thingsboard.server.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Shared data-access helper for the report rendering engine. Reuses CE's existing
 * telemetry/attribute/alarm/entity-lookup services directly — this is a deliberate simplification
 * vs PE's generic "entity alias + filter" data source resolution: report components reference a
 * single target {@link EntityId} directly (chosen in the template editor), rather than resolving
 * through an alias/filter query engine.
 */
@Component
@RequiredArgsConstructor
public class ReportDataFetcher {

    private static final long QUERY_TIMEOUT_SECONDS = 30;

    private final TimeseriesService timeseriesService;
    private final AttributesService attributesService;
    private final AlarmService alarmService;
    private final ExportableEntitiesService entitiesService;

    public String entityName(TenantId tenantId, EntityId entityId) {
        try {
            Object entity = entitiesService.findEntityByTenantIdAndId(tenantId, entityId);
            return entity instanceof HasName hasName ? hasName.getName() : entityId.toString();
        } catch (Exception e) {
            return entityId.toString();
        }
    }

    public List<TsKvEntry> fetchTimeseries(TenantId tenantId, EntityId entityId, List<String> keys, long startTs, long endTs) throws Exception {
        List<ReadTsKvQuery> queries = keys.stream()
                .map(key -> (ReadTsKvQuery) new BaseReadTsKvQuery(key, startTs, endTs, 5000, "ASC"))
                .toList();
        return get(timeseriesService.findAll(tenantId, entityId, queries));
    }

    public List<TsKvEntry> fetchLatest(TenantId tenantId, EntityId entityId, List<String> keys) throws Exception {
        if (keys == null || keys.isEmpty()) {
            return get(timeseriesService.findAllLatest(tenantId, entityId));
        }
        return get(timeseriesService.findLatest(tenantId, entityId, keys));
    }

    public List<AttributeKvEntry> fetchAttributes(TenantId tenantId, EntityId entityId, List<String> keys) throws Exception {
        if (keys == null || keys.isEmpty()) {
            return get(attributesService.findAll(tenantId, entityId, AttributeScope.SERVER_SCOPE));
        }
        return get(attributesService.find(tenantId, entityId, AttributeScope.SERVER_SCOPE, keys));
    }

    public List<AlarmInfo> fetchAlarms(TenantId tenantId, EntityId entityId, long startTs, long endTs) {
        AlarmQuery query = AlarmQuery.builder()
                .affectedEntityId(entityId)
                .pageLink(new TimePageLink(100, 0, null, null, startTs, endTs))
                .searchStatus(AlarmSearchStatus.ANY)
                .build();
        PageData<AlarmInfo> page = alarmService.findAlarms(tenantId, query);
        return page.getData();
    }

    private <T> T get(com.google.common.util.concurrent.ListenableFuture<T> future) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

}
