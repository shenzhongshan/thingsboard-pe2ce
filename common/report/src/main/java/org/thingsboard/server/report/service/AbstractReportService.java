/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  lombok.Generated
 *  org.apache.commons.collections4.CollectionUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Lazy
 *  org.thingsboard.common.util.JacksonUtil
 *  org.thingsboard.script.api.ScriptType
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.kv.Aggregation
 *  org.thingsboard.server.common.data.kv.BaseReadTsKvQuery
 *  org.thingsboard.server.common.data.kv.ReadTsKvQueryResult
 *  org.thingsboard.server.common.data.kv.TsKvEntry
 *  org.thingsboard.server.common.data.page.PageDataIterable
 *  org.thingsboard.server.common.data.page.PageLink
 *  org.thingsboard.server.common.data.page.SortOrder
 *  org.thingsboard.server.common.data.page.SortOrder$Direction
 *  org.thingsboard.server.common.data.query.AlarmData
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.query.EntityDataQuery
 *  org.thingsboard.server.common.data.query.EntityDataSortOrder
 *  org.thingsboard.server.common.data.query.EntityFilter
 *  org.thingsboard.server.common.data.query.EntityKeyType
 *  org.thingsboard.server.common.data.query.SingleEntityFilter
 *  org.thingsboard.server.common.data.query.StateEntityOwnerFilter
 *  org.thingsboard.server.common.data.query.TsValue
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.DataReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent
 *  org.thingsboard.server.common.data.report.configuration.timewindow.History
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration
 *  org.thingsboard.server.common.data.util.DataSourceUtils
 */
package org.thingsboard.server.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Generated;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.common.data.util.DataSourceUtils;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.service.ReportService;
import org.thingsboard.server.report.util.ReportQueryUtils;
import org.thingsboard.server.report.util.ReportUtils;

public abstract class AbstractReportService
implements ReportService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(AbstractReportService.class);
    public static final int DEFAULT_ENTITIES_PAGE_SIZE = 1024;
    private static final Map<String, String> ALARM_FIELD_ALIASES_MAP = Map.of("startTime", "startTs", "endTime", "endTs", "ackTime", "ackTs", "clearTime", "clearTs", "assignTime", "assignTs", "originator", "originatorName", "originatorType", "originator.entityType");
    @Lazy
    @Autowired
    protected ReportDataService dataService;

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        return this.fetchEntities(ctx, dataSource, stateEntityId, ReportQueryUtils.DEFAULT_SORT_ORDER);
    }

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId, EntityDataSortOrder sortOrder) {
        return this.fetchEntities(ctx, dataSource, stateEntityId, sortOrder, false);
    }

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId, EntityDataSortOrder sortOrder, boolean singleEntity) {
        StateEntityOwnerFilter stateEntityOwnerFilter;
        SingleEntityFilter singleEntityFilter;
        EntityFilter filter = ReportQueryUtils.buildEntityFilter(dataSource, ctx, stateEntityId);
        if (filter instanceof SingleEntityFilter && (singleEntityFilter = (SingleEntityFilter)filter).getSingleEntity() == null) {
            return Collections.emptyList();
        }
        if (filter instanceof StateEntityOwnerFilter && (stateEntityOwnerFilter = (StateEntityOwnerFilter)filter).getSingleEntity() == null) {
            return Collections.emptyList();
        }
        return this.fetchEntityDataByQuery(pageLink -> ReportQueryUtils.toEntityDataQuery(dataSource, ctx, filter, pageLink, sortOrder), dataSource, ctx, singleEntity);
    }

    private List<EntityData> fetchEntityDataByQuery(Function<PageLink, EntityDataQuery> querySupplier, DataSource dataSource, TbReportCtx ctx, boolean singleEntity) {
        Iterable<EntityData> entityDataIterable;
        List<DataKey> dataKeysWithAggr = this.getDataKeysWithAggr(dataSource);
        ArrayList<EntityData> data = new ArrayList<EntityData>();
        if (singleEntity) {
            PageLink singleEntityPageLink = new PageLink(1);
            PageData<EntityData> entityData = this.dataService.findEntityDataByQuery(querySupplier.apply(singleEntityPageLink), ctx);
            entityDataIterable = entityData.getData();
        } else {
            entityDataIterable = new PageDataIterable<EntityData>(link -> this.dataService.findEntityDataByQuery((EntityDataQuery)querySupplier.apply(link), ctx), 1024);
        }
        for (EntityData entityData : entityDataIterable) {
            this.updateWithAggregatedData(ctx, dataKeysWithAggr, entityData);
            data.add(entityData);
        }
        return data;
    }

    private List<DataKey> getDataKeysWithAggr(DataSource dataSource) {
        List<DataKey> dataKeys = dataSource.getDataKeys();
        if (dataKeys != null) {
            return dataKeys.stream().filter(dataKey -> dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE).toList();
        }
        return Collections.emptyList();
    }

    private void updateWithAggregatedData(TbReportCtx ctx, List<DataKey> dataKeys, EntityData entityData) {
        if (!dataKeys.isEmpty()) {
            List<BaseReadTsKvQuery> queries = this.buildReadTsKvQueries(ctx, dataKeys);
            List<ReadTsKvQueryResult> result = this.dataService.findTimeseriesByQueries(entityData.getEntityId(), queries, ctx);
            int i = 0;
            for (ReadTsKvQueryResult queryResult : result) {
                List queryResultData = queryResult.getData();
                if (CollectionUtils.isNotEmpty((Collection)queryResultData)) {
                    entityData.getTimeseries().put(dataKeys.get(i).getLabel(), queryResult.toTsValues());
                }
                ++i;
            }
        }
    }

    private List<BaseReadTsKvQuery> buildReadTsKvQueries(TbReportCtx ctx, List<DataKey> dataKeysWithAggregation) {
        ArrayList<BaseReadTsKvQuery> queries = new ArrayList<BaseReadTsKvQuery>();
        for (DataKey key : dataKeysWithAggregation) {
            TimeWindowConfiguration timeWindowConf = key.getTimewindow();
            String targetTimezone = StringUtils.isNotBlank((String)timeWindowConf.getTimezone()) ? timeWindowConf.getTimezone() : ctx.getTimeZone();
            TimeIntervalCalculator.TimeRange timeRange = TimeIntervalCalculator.getTimeRange((TimeWindowConfiguration)timeWindowConf, (String)targetTimezone);
            BaseReadTsKvQuery query = new BaseReadTsKvQuery(key.getName(), timeRange.startTs, timeRange.endTs, timeRange.endTs - timeRange.startTs, 1, key.getAggregationType());
            queries.add(query);
        }
        return queries;
    }

    protected List<Map<String, String>> collectEntityDatas(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        switch (dataSource.getType()) {
            case DEVICE: 
            case ENTITY: {
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown data source type: " + String.valueOf(dataSource.getType()));
            }
        }
        return this.fetchEntities(ctx, dataSource, stateEntityId).stream().map(entityData -> this.toStringMap((EntityData)entityData, (List<DataKey>)dataSource.getDataKeys(), ctx, null)).collect(Collectors.toList());
    }

    protected ComponentData buildTsComponentData(int usablePageWidthPx, TbReportCtx ctx, TimeseriesTableComponent component, EntityData entity) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        String targetTimezone = StringUtils.isNotBlank((String)timeWindowConf.getTimezone()) ? timeWindowConf.getTimezone() : ctx.getTimeZone();
        TimeIntervalCalculator.TimeRange timeRange = TimeIntervalCalculator.getTimeRange((TimeWindowConfiguration)timeWindowConf, (String)targetTimezone);
        Optional<DataSource> singleDataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
        if (singleDataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx);
        }
        List<DataKey> dataKeys = singleDataSource.get().getDataKeys();
        List<DataKey> latestDataKeys = singleDataSource.get().getLatestDataKeys();
        List<String> keys = dataKeys.stream().map(DataKey::getName).distinct().toList();
        List<TsKvEntry> result = this.dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs, timeRange.endTs, historyConf.getInterval(), timeWindowConf.getTimezone(), timeWindowConf.getAggregation().getType(), SortOrder.Direction.DESC, timeWindowConf.getAggregation().getLimit(), false, ctx);
        SortOrder sortOrder = SortOrder.of((String)"rawTs", (SortOrder.Direction)SortOrder.Direction.DESC);
        List<Map<String, String>> entityDatas = this.collectTsData(dataKeys, latestDataKeys, entity, result, component, sortOrder, timeWindowConf.getTimezone(), ctx);
        HashMap<String, Object> variables = new HashMap<String, Object>(this.toStringMap(entity, (List<DataKey>)dataKeys, ctx, timeWindowConf.getTimezone()));
        return new ComponentData(usablePageWidthPx, null, entityDatas, variables);
    }

    protected ComponentData buildAlarmComponentData(int usablePageWidthPx, TbReportCtx ctx, AlarmTableComponent component, EntityData stateEntity) {
        DataSource alarmSource = component.getAlarmSource();
        if (alarmSource == null) {
            return new ComponentData(usablePageWidthPx);
        }
        switch (alarmSource.getType()) {
            case DEVICE: {
                if (alarmSource.getDeviceId() != null) break;
                return new ComponentData(usablePageWidthPx);
            }
            case ENTITY: {
                if (alarmSource.getEntityAliasId() != null) break;
                return new ComponentData(usablePageWidthPx);
            }
            default: {
                return new ComponentData(usablePageWidthPx);
            }
        }
        List<DataKey> alarmDataKeys = alarmSource.getDataKeys().stream().filter(dataKey -> dataKey.getType().equals("alarm")).collect(Collectors.toList());
        List<DataKey> latestDataKeys = alarmSource.getDataKeys().stream().filter(dataKey -> !dataKey.getType().equals("alarm")).collect(Collectors.toList());
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        List<EntityData> entityDataList = this.fetchEntities(ctx, alarmSource, stateEntityId);
        Map entityDataMap = entityDataList.stream().collect(Collectors.toMap(EntityData::getEntityId, Function.identity()));
        ArrayList<Map<String, String>> entityDatas = new ArrayList<Map<String, String>>();
        if (entityDataMap.size() == 0) {
            return new ComponentData(usablePageWidthPx);
        }
        for (AlarmData alarmData : new PageDataIterable<AlarmData>(link -> this.dataService.findAlarmDataByQueryForEntities(ReportQueryUtils.toAlarmDataQuery(component, ctx, stateEntityId, link), entityDataMap.keySet(), ctx), 1024)) {
            Map<String, String> mergedData = this.toStringMap(alarmData, alarmDataKeys, ctx, component.getTimewindow().getTimezone());
            EntityData entityData = (EntityData)entityDataMap.get(alarmData.getEntityId());
            mergedData.putAll(this.toStringMap(entityData, latestDataKeys, ctx, component.getTimewindow().getTimezone()));
            entityDatas.add(mergedData);
        }
        HashMap<String, String> variables = new HashMap<String, String>();
        if (stateEntity != null) {
            this.putEntityInfoData(stateEntity, variables);
        } else {
            this.putEntityInfoData(entityDataList.get(0), variables);
        }
        return new ComponentData(usablePageWidthPx, null, entityDatas, new HashMap<String, Object>(variables));
    }

    protected Map<String, String> toStringMap(EntityData entityData, List<DataKey> dataKeys, TbReportCtx ctx, String timezone) {
        HashMap<String, String> data = new HashMap<String, String>();
        if (entityData != null) {
            this.putLatestValues(dataKeys, data, entityData.getLatest(), ctx, timezone);
            this.putAggregatedTsValues(dataKeys, data, entityData.getTimeseries(), ctx, timezone);
            this.putEntityInfoData(entityData, data);
        }
        return data;
    }

    protected void putEntityInfoData(EntityData entityData, Map<String, String> data) {
        Optional<String> entityName = DataSourceUtils.getEntityLatestValue((EntityData)entityData, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"name");
        Optional<String> entityLabel = DataSourceUtils.getEntityLatestValue((EntityData)entityData, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        data.put("id", entityData.getEntityId().toString());
    }

    protected Map<String, String> toStringMap(AlarmData alarmData, List<DataKey> alarmDataKeys, TbReportCtx ctx, String timezone) {
        HashMap<String, String> data = new HashMap<String, String>();
        JsonNode alarmDataJson = JacksonUtil.valueToTree((Object)alarmData);
        for (DataKey alarmKey : alarmDataKeys) {
            String targetKey = alarmKey.getName();
            String value = null;
            if (targetKey.equals("assignee")) {
                value = this.getAssigneeDisplayName(alarmData);
            } else {
                JsonNode jsonValue = JacksonUtil.getByKeyPath((JsonNode)alarmDataJson, (String)(targetKey = ALARM_FIELD_ALIASES_MAP.getOrDefault(targetKey, targetKey)));
                if (jsonValue != null) {
                    value = jsonValue.asText();
                }
            }
            if (value == null) continue;
            if (ReportUtils.ENTITY_TIME_FIELDS.contains(alarmKey.getName())) {
                data.put("rawTs_" + alarmKey.getLabel(), value);
            }
            data.put(alarmKey.getLabel(), this.formatValue(ctx, alarmKey, 0L, value, timezone));
        }
        Optional<String> entityName = DataSourceUtils.getAlarmLatestValue((AlarmData)alarmData, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"name");
        Optional<String> entityLabel = DataSourceUtils.getAlarmLatestValue((AlarmData)alarmData, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        return data;
    }

    protected List<EntityData> getSubReportEntities(TbReportCtx ctx, DataReportComponent component, EntityId stateEntityId) {
        List<EntityData> entities;
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            entities = new ArrayList<EntityData>();
            entities.add(null);
        } else {
            entities = this.fetchEntities(ctx, dataSource.get(), stateEntityId);
        }
        return entities;
    }

    protected void populateReportVars(ComponentData componentData, TbReportCtx ctx) {
        Map<String, Object> variables = componentData.getVariables();
        variables.put("reportCreatedTime", ctx.getReportCreatedTime());
    }

    private void putLatestValues(List<DataKey> dataKeys, Map<String, String> data, Map<EntityKeyType, Map<String, TsValue>> latest, TbReportCtx ctx, String timezone) {
        if (dataKeys == null) {
            return;
        }
        for (DataKey dataKey : dataKeys) {
            TsValue tsValue;
            Map<String, TsValue> keyValueMap;
            Aggregation agg = dataKey.getAggregationType();
            if (agg != null && agg != Aggregation.NONE || (keyValueMap = latest.get(EntityKeyType.fromName((String)dataKey.getType()))) == null || (tsValue = keyValueMap.get(dataKey.getName())) == null || tsValue.getValue() == null) continue;
            if (ReportUtils.ENTITY_TIME_FIELDS.contains(dataKey.getName())) {
                data.put("rawTs_" + dataKey.getLabel(), tsValue.getValue());
            }
            data.put(dataKey.getLabel(), this.formatValue(ctx, dataKey, tsValue.getTs(), tsValue.getValue(), timezone));
        }
    }

    private void putAggregatedTsValues(List<DataKey> dataKeys, HashMap<String, String> data, Map<String, TsValue[]> timeseries, TbReportCtx ctx, String timezone) {
        if (dataKeys == null || timeseries == null) {
            return;
        }
        for (DataKey dk : dataKeys) {
            TsValue[] series;
            Aggregation agg = dk.getAggregationType();
            if (agg == null || agg == Aggregation.NONE || (series = timeseries.get(dk.getLabel())) == null || series.length == 0) continue;
            TsValue latest = Arrays.stream(series).max(Comparator.comparingLong(TsValue::getTs)).get();
            data.put(dk.getLabel(), this.formatValue(ctx, dk, latest.getTs(), latest.getValue(), timezone));
        }
    }

    private String getAssigneeDisplayName(AlarmData alarmData) {
        if (alarmData.getAssignee() != null) {
            return alarmData.getAssignee().getTitle();
        }
        if (alarmData.getAssigneeId() != null) {
            return "User deleted";
        }
        return "Unassigned";
    }

    private List<Map<String, String>> collectTsData(List<DataKey> dataKeys, List<DataKey> latestDataKeys, EntityData entity, List<TsKvEntry> tsKvEntries, TimeseriesTableComponent component, SortOrder sortOrder, String timezone, TbReportCtx ctx) {
        Optional<String> entityName = DataSourceUtils.getEntityLatestValue((EntityData)entity, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"name");
        Optional<String> entityLabel = DataSourceUtils.getEntityLatestValue((EntityData)entity, (EntityKeyType)EntityKeyType.ENTITY_FIELD, (String)"label");
        ArrayList<Map<String, String>> tsData = new ArrayList<Map<String, String>>();
        Comparator<Long> tsComparator = sortOrder.getDirection() == SortOrder.Direction.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder();
        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream().collect(Collectors.groupingBy(TsKvEntry::getTs, () -> new TreeMap<Long, List<TsKvEntry>>(tsComparator), Collectors.toList()));
        groupedByTs.forEach((ts, entries) -> {
            LinkedHashMap<String, String> tsValues = new LinkedHashMap<String, String>();
            tsValues.put("rawTs", ts.toString());
            if (component.isShowTimestamp()) {
                tsValues.put(component.getTimestampLabel(), ReportUtils.formatTimestamp(ts, component.getTimestampPattern(), ctx, timezone));
            }
            for (DataKey dataKey : dataKeys) {
                entries.stream().filter(tsKvEntry -> tsKvEntry.getKey().equals(dataKey.getName())).findFirst().ifPresentOrElse(tsKvEntry -> tsValues.put(dataKey.getLabel(), this.formatValue(ctx, dataKey, tsKvEntry.getTs(), tsKvEntry.getValue(), false, timezone)), () -> tsValues.put(dataKey.getLabel(), this.formatValue(ctx, dataKey, 0L, null, false, timezone)));
            }
            this.putLatestValues(latestDataKeys, tsValues, entity.getLatest(), ctx, timezone);
            tsValues.put("entityName", entityName.orElse(""));
            tsValues.put("entityLabel", entityLabel.orElse(""));
            tsData.add(tsValues);
        });
        return tsData;
    }

    private String formatValue(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, String timeZone) {
        return this.formatValue(ctx, dataKey, timestamp, value, true, timeZone);
    }

    private String formatValue(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, boolean parseString, String timezone) {
        Object processed;
        if (dataKey == null) {
            return value != null ? value.toString() : null;
        }
        if (ReportUtils.ENTITY_TIME_FIELDS.contains(dataKey.getName()) && !dataKey.isUsePostProcessing() && value != null) {
            value = ReportUtils.formatTimestamp(value.toString(), ctx.getConfiguration().getTimeDataPattern(), ctx, timezone);
        }
        if ((processed = this.postProcess(ctx, dataKey, timestamp, value, parseString)) == null) {
            return null;
        }
        String formattedValue = processed.toString();
        if (dataKey.getDecimals() != null || dataKey.getUnits() != null) {
            formattedValue = ReportUtils.formatValueWithPrecisionAndUnits(formattedValue, dataKey);
        }
        return formattedValue;
    }

    protected Object postProcess(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, boolean parseString) {
        if (dataKey.isUsePostProcessing()) {
            Object input = parseString && value instanceof String ? ReportUtils.convertStringToTypedValue((String)value) : value;
            UUID scriptId = ctx.getScripts().computeIfAbsent(dataKey.getPostFuncBody(), s -> this.evalScript(ctx, (String)s));
            if (scriptId != null) {
                return this.evalData(ctx, timestamp, input, scriptId);
            }
        }
        return value;
    }

    private Object evalData(TbReportCtx ctx, long timestamp, Object value, UUID scriptId) {
        try {
            return ctx.getTbelInvokeService().invokeScript(ctx.getTenantId(), null, scriptId, new Object[]{timestamp, value}).get();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Failed to evaluate data: " + String.valueOf(value), e);
        }
        catch (ExecutionException e) {
            String error = "Failed to evaluate data: " + String.valueOf(value);
            log.error("[{}] {}", new Object[]{ctx.getTenantId(), error, e});
            return error;
        }
    }

    private UUID evalScript(TbReportCtx ctx, String script) {
        try {
            return (UUID)ctx.getTbelInvokeService().eval(ctx.getTenantId(), ScriptType.REPORT_DATA_KEY_SCRIPT, script, new String[]{"time", "value"}).get();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Failed to compile script: " + script, e);
        }
        catch (ExecutionException e) {
            log.error("[{}] Failed to compile script {} ", new Object[]{ctx.getTenantId(), script, e});
            return null;
        }
    }
}

