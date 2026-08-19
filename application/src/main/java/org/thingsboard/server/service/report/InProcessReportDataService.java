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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Supplies the report engine with platform data.
 * <p>
 * PE ships only a REST-backed implementation of this interface, because its report engine runs in a
 * separate {@code tb-report} process. Running the same engine inside the platform means the calls
 * can go straight to the services — no HTTP hop, no access token to mint and expire.
 * <p>
 * The report still executes with the <em>report user's</em> permissions rather than unrestricted:
 * every query that supports it is issued through {@link EntityQueryService} with a
 * {@link SecurityUser} rebuilt from {@link TbReportCtx#getUserId()}, so a customer user's scheduled
 * report cannot read entities they could not read interactively.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class InProcessReportDataService implements ReportDataService {

    private static final long QUERY_TIMEOUT_SECONDS = 60;

    private final ReportTemplateService reportTemplateService;
    private final ReportService reportService;
    private final EntityQueryService entityQueryService;
    private final AlarmService alarmService;
    private final TimeseriesService timeseriesService;
    private final ImageService imageService;
    private final UserService userService;

    @Override
    public ReportTemplate findReportTemplate(ReportTemplateId reportTemplateId, TbReportCtx ctx) throws ThingsboardException {
        ReportTemplate template = reportTemplateService.findReportTemplateById(ctx.getTenantId(), reportTemplateId);
        if (template == null) {
            throw new ThingsboardException("Report template not found: " + reportTemplateId,
                    ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return template;
    }

    @Override
    public byte[] downloadImage(String type, String key, TbReportCtx ctx) throws ThingsboardException {
        // "system" images belong to the system tenant, "tenant" ones to the report's tenant.
        TenantId tenantId = "system".equals(type) ? TenantId.SYS_TENANT_ID : ctx.getTenantId();
        TbResourceInfo imageInfo = imageService.getImageInfoByTenantIdAndKey(tenantId, key);
        if (imageInfo == null) {
            throw new ThingsboardException("Image not found: " + type + "/" + key, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
    }

    @Override
    public byte[] downloadPublicImage(String publicResourceKey, TbReportCtx ctx) throws ThingsboardException {
        TbResourceInfo imageInfo = imageService.getPublicImageInfoByKey(publicResourceKey);
        if (imageInfo == null) {
            throw new ThingsboardException("Public image not found: " + publicResourceKey,
                    ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, TbReportCtx ctx) {
        return entityQueryService.findEntityDataByQuery(securityUser(ctx), query);
    }

    @Override
    public Long countEntitiesByQuery(EntityCountQuery query, TbReportCtx ctx) {
        return entityQueryService.countEntitiesByQuery(securityUser(ctx), query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, TbReportCtx ctx) {
        return entityQueryService.findAlarmDataByQuery(securityUser(ctx), query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(AlarmDataQuery query, Collection<EntityId> entityIds, TbReportCtx ctx) {
        // The entity ids were already produced by a permission-checked entity query above, so this
        // one is scoped by that list rather than re-resolved through the query service.
        return alarmService.findAlarmDataByQueryForEntities(ctx.getTenantId(), query, entityIds);
    }

    @Override
    public Long countAlarmsByQuery(AlarmCountQuery query, TbReportCtx ctx) {
        return entityQueryService.countAlarmsByQuery(securityUser(ctx), query);
    }

    @Override
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs,
                                         Interval interval, String timezone, Aggregation aggregation,
                                         SortOrder.Direction orderBy, Integer limit, boolean useStrictDataTypes,
                                         TbReportCtx ctx) {
        long intervalMs = interval != null ? interval.getInterval() : 0L;
        int resultLimit = limit != null ? limit : 1000;
        String direction = orderBy != null ? orderBy.name() : SortOrder.Direction.ASC.name();
        List<ReadTsKvQuery> queries = keys.stream()
                .map(key -> (ReadTsKvQuery) new BaseReadTsKvQuery(key, startTs, endTs, intervalMs, resultLimit,
                        aggregation != null ? aggregation : Aggregation.NONE, direction))
                .toList();
        return get(entityId, queries);
    }

    @Override
    public List<ReadTsKvQueryResult> findTimeseriesByQueries(EntityId entityId, List<BaseReadTsKvQuery> queries, TbReportCtx ctx) {
        try {
            return timeseriesService.findAllByQueries(ctx.getTenantId(), entityId, List.copyOf(queries))
                    .get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read timeseries for " + entityId, e);
        }
    }

    @Override
    public Report createReport(Report report, byte[] data, TbReportCtx ctx) throws ThingsboardException {
        return reportService.createReport(ctx.getTenantId(), report, data);
    }

    private List<TsKvEntry> get(EntityId entityId, List<ReadTsKvQuery> queries) {
        try {
            return timeseriesService.findAll(TenantId.SYS_TENANT_ID, entityId, queries)
                    .get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read timeseries for " + entityId, e);
        }
    }

    /**
     * Rebuilds the report user so queries run with their permissions. Reports are generated
     * asynchronously (scheduler, job) with no request-bound security context to borrow.
     */
    private SecurityUser securityUser(TbReportCtx ctx) {
        UserId userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("Report task carries no user id; cannot resolve permissions");
        }
        User user = userService.findUserById(ctx.getTenantId(), userId);
        if (user == null) {
            throw new IllegalStateException("Report user " + userId + " no longer exists");
        }
        return new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail()));
    }

}
