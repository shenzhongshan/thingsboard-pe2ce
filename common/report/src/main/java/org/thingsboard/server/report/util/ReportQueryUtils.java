/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.EntityType
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.id.DeviceId
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.id.EntityIdFactory
 *  org.thingsboard.server.common.data.id.TenantId
 *  org.thingsboard.server.common.data.id.UserId
 *  org.thingsboard.server.common.data.kv.Aggregation
 *  org.thingsboard.server.common.data.page.PageLink
 *  org.thingsboard.server.common.data.query.AlarmCountQuery
 *  org.thingsboard.server.common.data.query.AlarmDataPageLink
 *  org.thingsboard.server.common.data.query.AlarmDataQuery
 *  org.thingsboard.server.common.data.query.AliasEntityId
 *  org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter
 *  org.thingsboard.server.common.data.query.EntityCountQuery
 *  org.thingsboard.server.common.data.query.EntityDataPageLink
 *  org.thingsboard.server.common.data.query.EntityDataQuery
 *  org.thingsboard.server.common.data.query.EntityDataSortOrder
 *  org.thingsboard.server.common.data.query.EntityDataSortOrder$Direction
 *  org.thingsboard.server.common.data.query.EntityFilter
 *  org.thingsboard.server.common.data.query.EntityGroupFilter
 *  org.thingsboard.server.common.data.query.EntityKey
 *  org.thingsboard.server.common.data.query.EntityKeyType
 *  org.thingsboard.server.common.data.query.EntitySearchQueryFilter
 *  org.thingsboard.server.common.data.query.KeyFilter
 *  org.thingsboard.server.common.data.query.RelationsQueryFilter
 *  org.thingsboard.server.common.data.query.SchedulerEventFilter
 *  org.thingsboard.server.common.data.query.SingleEntityFilter
 *  org.thingsboard.server.common.data.query.StateEntityFilter
 *  org.thingsboard.server.common.data.query.StateEntityOwnerFilter
 *  org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.DataSourceType
 *  org.thingsboard.server.common.data.report.configuration.EntityAlias
 *  org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig
 *  org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration
 *  org.thingsboard.server.common.data.util.DataSourceUtils
 */
package org.thingsboard.server.report.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntitySearchQueryFilter;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.common.data.util.DataSourceUtils;
import org.thingsboard.server.report.context.TbReportCtx;

public class ReportQueryUtils {
    public static final EntityDataSortOrder DEFAULT_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "id"), EntityDataSortOrder.Direction.ASC);
    public static final EntityDataSortOrder DEFAULT_TS_CHART_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC);
    public static final EntityDataSortOrder DEFAULT_ALARM_SORT_ORDER = new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC);

    public static EntityCountQuery toEntityCountQuery(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        EntityFilter entityFilter = ReportQueryUtils.buildEntityFilter(dataSource, ctx, stateEntityId);
        List<KeyFilter> keyFilters = ReportQueryUtils.findKeyFilters(dataSource, ctx.getConfiguration());
        return new EntityCountQuery(entityFilter, keyFilters);
    }

    public static AlarmCountQuery toAlarmCountQuery(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        EntityFilter entityFilter = ReportQueryUtils.buildEntityFilter(dataSource, ctx, stateEntityId);
        List<KeyFilter> keyFilters = ReportQueryUtils.findKeyFilters(dataSource, ctx.getConfiguration());
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(entityFilter, keyFilters);
        AlarmFilterConfig alarmFilterConfig = dataSource.getAlarmFilterConfig();
        if (alarmFilterConfig != null) {
            alarmCountQuery.setStatusList(alarmFilterConfig.getStatusList());
            alarmCountQuery.setSeverityList(alarmFilterConfig.getSeverityList());
            alarmCountQuery.setTypeList(alarmFilterConfig.getTypeList());
            alarmCountQuery.setAssigneeId(alarmFilterConfig.getAssigneeId());
        }
        return alarmCountQuery;
    }

    public static AlarmDataQuery toAlarmDataQuery(AlarmTableComponent component, TbReportCtx ctx, EntityId stateEntityId, PageLink pageLink) {
        DataSource alarmSource = component.getAlarmSource();
        EntityFilter entityFilter = ReportQueryUtils.buildEntityFilter(alarmSource, ctx, stateEntityId);
        List<KeyFilter> keyFilters = ReportQueryUtils.findKeyFilters(alarmSource, ctx.getConfiguration());
        List<EntityKey> alarmFields = alarmSource.getDataKeys().stream().filter(dataKey -> "alarm".equals(dataKey.getType())).map(dataKey -> new EntityKey(EntityKeyType.ALARM_FIELD, dataKey.getName())).toList();
        List entityFields = alarmSource.getDataKeys().stream().filter(dataKey -> "entityField".equals(dataKey.getType())).map(dataKey -> new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getName())).toList();
        entityFields = DataSourceUtils.setEntityKeyIfNotExists(entityFields, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"name");
        entityFields = DataSourceUtils.setEntityKeyIfNotExists((List)entityFields, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"label");
        List<EntityKey> attrFields = alarmSource.getDataKeys().stream().filter(dataKey -> "attribute".equals(dataKey.getType())).map(dataKey -> new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getName())).toList();
        List<EntityKey> tsFields = alarmSource.getDataKeys().stream().filter(dataKey -> "timeseries".equals(dataKey.getType())).map(dataKey -> new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getName())).toList();
        List latestValues = Stream.concat(attrFields.stream(), tsFields.stream()).toList();
        AlarmFilterConfig alarmFilterConfig = alarmSource.getAlarmFilterConfig();
        AlarmDataPageLink alarmDataPageLink = new AlarmDataPageLink();
        alarmDataPageLink.setPage(pageLink.getPage());
        alarmDataPageLink.setPageSize(pageLink.getPageSize());
        alarmDataPageLink.setSortOrder(DEFAULT_ALARM_SORT_ORDER);
        String targetTimezone = StringUtils.isNotBlank((String)component.getTimewindow().getTimezone()) ? component.getTimewindow().getTimezone() : ctx.getTimeZone();
        TimeIntervalCalculator.TimeRange timeRange = TimeIntervalCalculator.getTimeRange((TimeWindowConfiguration)component.getTimewindow(), (String)targetTimezone);
        alarmDataPageLink.setStartTs(timeRange.startTs);
        alarmDataPageLink.setEndTs(timeRange.endTs);
        alarmDataPageLink.setSearchPropagatedAlarms(alarmFilterConfig.isSearchPropagatedAlarms());
        alarmDataPageLink.setSeverityList(alarmFilterConfig.getSeverityList());
        alarmDataPageLink.setStatusList(alarmFilterConfig.getStatusList());
        alarmDataPageLink.setTypeList(alarmFilterConfig.getTypeList());
        alarmDataPageLink.setAssigneeId(alarmFilterConfig.getAssigneeId());
        return new AlarmDataQuery(entityFilter, alarmDataPageLink, entityFields, latestValues, keyFilters, alarmFields);
    }

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, TbReportCtx ctx, EntityFilter filter, PageLink pageLink) {
        return ReportQueryUtils.toEntityDataQuery(dataSource, ctx, filter, pageLink, DEFAULT_SORT_ORDER);
    }

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, TbReportCtx ctx, EntityFilter filter, PageLink pageLink, EntityDataSortOrder sortOrder) {
        EntityDataPageLink entityDataPageLink = new EntityDataPageLink(pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder);
        List<KeyFilter> keyFilters = ReportQueryUtils.findKeyFilters(dataSource, ctx.getConfiguration());
        List<EntityKey> entityFields = new ArrayList();
        ArrayList<EntityKey> latestValues = new ArrayList<EntityKey>();
        if (dataSource.getDataKeys() != null) {
            for (DataKey dataKey : dataSource.getDataKeys()) {
                switch (dataKey.getType()) {
                    case "attribute": {
                        latestValues.add(new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getName()));
                        break;
                    }
                    case "timeseries": {
                        if (dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE) break;
                        latestValues.add(new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getName()));
                        break;
                    }
                    case "entityField": {
                        entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getName()));
                    }
                }
            }
        }
        entityFields = DataSourceUtils.setEntityKeyIfNotExists(entityFields, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"name");
        entityFields = DataSourceUtils.setEntityKeyIfNotExists(entityFields, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"label");
        return new EntityDataQuery(filter, entityDataPageLink, entityFields, latestValues, keyFilters);
    }

    public static EntityFilter buildEntityFilter(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        if (dataSource.getType() == DataSourceType.DEVICE) {
            return ReportQueryUtils.buildSingleEntityFilter((EntityId)DeviceId.fromString((String)dataSource.getDeviceId()));
        }
        return ReportQueryUtils.buildAliasBasedFilter(dataSource, ctx, stateEntityId);
    }

    public static Optional<String> resolveAliasId(TbReportCtx ctx, String aliasName) {
        return ctx.getConfiguration().getEntityAliases().stream().filter(alias -> alias.getAlias().equals(aliasName)).findFirst().map(EntityAlias::getId);
    }

    private static EntityFilter buildSingleEntityFilter(EntityId entityId) {
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId((EntityId)entityId));
        return filter;
    }

    private static EntityFilter buildAliasBasedFilter(DataSource dataSource, TbReportCtx ctx, EntityId stateEntityId) {
        EntitiesByGroupNameFilter entitiesByGroupNameFilter;
        EntityGroupFilter entityGroupFilter;
        SchedulerEventFilter queryFilter;
        EntitySearchQueryFilter queryFilter2;
        RelationsQueryFilter queryFilter3;
        EntityFilter filter = ctx.getConfiguration().getEntityAliases().stream().filter(alias -> alias.getId().equals(dataSource.getEntityAliasId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getEntityAliasId())).getFilter();
        AliasEntityId resolvedEntity = ReportQueryUtils.resolveStateEntityId(stateEntityId, filter, ctx);
        if (filter instanceof StateEntityFilter) {
            return ReportQueryUtils.buildSingleEntityFilter((EntityId)resolvedEntity);
        }
        if (filter instanceof StateEntityOwnerFilter) {
            StateEntityOwnerFilter ownerFilter = (StateEntityOwnerFilter)filter;
            ownerFilter.setSingleEntity(resolvedEntity);
            return ownerFilter;
        }
        if (filter instanceof RelationsQueryFilter && (queryFilter3 = (RelationsQueryFilter)filter).isRootStateEntity()) {
            queryFilter3.setRootEntity(resolvedEntity);
        } else if (filter instanceof EntitySearchQueryFilter && (queryFilter2 = (EntitySearchQueryFilter)filter).isRootStateEntity()) {
            queryFilter2.setRootEntity(resolvedEntity);
        } else if (filter instanceof SchedulerEventFilter && (queryFilter = (SchedulerEventFilter)filter).isOriginatorStateEntity()) {
            queryFilter.setOriginator(resolvedEntity);
        } else if (filter instanceof EntityGroupFilter && (entityGroupFilter = (EntityGroupFilter)filter).isGroupStateEntity()) {
            if (resolvedEntity != null) {
                entityGroupFilter.setGroupType(resolvedEntity.getEntityType());
                entityGroupFilter.setEntityGroup(resolvedEntity.getId().toString());
            }
        } else if (filter instanceof EntitiesByGroupNameFilter && (entitiesByGroupNameFilter = (EntitiesByGroupNameFilter)filter).isGroupStateEntity()) {
            entitiesByGroupNameFilter.setOwnerId((EntityId)resolvedEntity);
        }
        EntityFilter.resolveEntityFilter((EntityFilter)filter, (TenantId)ctx.getTenantId(), (UserId)ctx.getUserId(), (EntityId)ctx.getUserOwnerId());
        return filter;
    }

    private static AliasEntityId resolveStateEntityId(EntityId stateEntityId, EntityFilter filter, TbReportCtx ctx) {
        EntityGroupFilter entityGroupFilter;
        if (stateEntityId != null) {
            return AliasEntityId.fromEntityId((EntityId)stateEntityId);
        }
        if (filter instanceof StateEntityFilter) {
            StateEntityFilter stateFilter = (StateEntityFilter)filter;
            return AliasEntityId.resolveAliasEntityId((AliasEntityId)stateFilter.getDefaultStateEntity(), (TenantId)ctx.getTenantId(), (UserId)ctx.getUserId(), (EntityId)ctx.getUserOwnerId());
        }
        if (filter instanceof StateEntityOwnerFilter) {
            StateEntityOwnerFilter ownerFilter = (StateEntityOwnerFilter)filter;
            return AliasEntityId.resolveAliasEntityId((AliasEntityId)ownerFilter.getDefaultStateEntity(), (TenantId)ctx.getTenantId(), (UserId)ctx.getUserId(), (EntityId)ctx.getUserOwnerId());
        }
        if (filter instanceof RelationsQueryFilter) {
            RelationsQueryFilter queryFilter = (RelationsQueryFilter)filter;
            return AliasEntityId.resolveAliasEntityId((AliasEntityId)queryFilter.getDefaultStateEntity(), (TenantId)ctx.getTenantId(), (UserId)ctx.getUserId(), (EntityId)ctx.getUserOwnerId());
        }
        if (filter instanceof EntitySearchQueryFilter) {
            EntitySearchQueryFilter queryFilter = (EntitySearchQueryFilter)filter;
            return AliasEntityId.resolveAliasEntityId((AliasEntityId)queryFilter.getDefaultStateEntity(), (TenantId)ctx.getTenantId(), (UserId)ctx.getUserId(), (EntityId)ctx.getUserOwnerId());
        }
        if (filter instanceof SchedulerEventFilter) {
            SchedulerEventFilter queryFilter = (SchedulerEventFilter)filter;
            return AliasEntityId.resolveAliasEntityId((AliasEntityId)queryFilter.getDefaultStateEntity(), (TenantId)ctx.getTenantId(), (UserId)ctx.getUserId(), (EntityId)ctx.getUserOwnerId());
        }
        if (filter instanceof EntityGroupFilter && (entityGroupFilter = (EntityGroupFilter)filter).getDefaultStateGroupType() != null && entityGroupFilter.getDefaultStateEntityGroup() != null) {
            return AliasEntityId.fromEntityId((EntityId)EntityIdFactory.getByTypeAndId((EntityType)entityGroupFilter.getDefaultStateGroupType(), (String)entityGroupFilter.getDefaultStateEntityGroup()));
        }
        return null;
    }

    private static List<KeyFilter> findKeyFilters(DataSource dataSource, ReportTemplateConfig reportTemplateConfig) {
        if (dataSource.getFilterId() != null) {
            return reportTemplateConfig.getFilters().stream().filter(filter -> filter.getId().equals(dataSource.getFilterId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Entity filter not found: " + dataSource.getFilterId())).getKeyFilters();
        }
        return null;
    }
}

