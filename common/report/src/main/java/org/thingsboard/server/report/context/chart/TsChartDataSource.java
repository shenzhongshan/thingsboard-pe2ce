/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.common.util.JacksonUtil
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.kv.Aggregation
 *  org.thingsboard.server.common.data.kv.TsKvEntry
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.query.EntityKeyType
 *  org.thingsboard.server.common.data.query.TsValue
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration
 *  org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings
 *  org.thingsboard.server.common.data.report.configuration.timewindow.Interval
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 */
package org.thingsboard.server.report.context.chart;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Generated;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.report.context.chart.DataPostProcessFunction;
import org.thingsboard.server.report.context.chart.TsChartSeriesData;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;

public class TsChartDataSource {
    private final boolean generated;
    private final boolean comparison;
    private final EntityId entityId;
    private final EntityData entityData;
    private String entityName;
    private String entityLabel;
    private final List<DataKey> dataKeys;
    private final List<TsChartSeriesData> data;
    private final int index;
    private final Map<String, Object> variables;

    public TsChartDataSource(DataSource dataSource, EntityData entityData, List<TsKvEntry> tsKvEntries, TimeIntervalCalculator.TimeRange timeWindow, Interval aggInterval, Aggregation aggregation, ZoneId zoneId, boolean comparison, ComparisonDuration timeForComparison, DataPostProcessFunction postProcessFunction, int index, int startDataIndex) {
        ArrayList<DataKey> newDataKeys = new ArrayList<DataKey>();
        for (DataKey dataKey : dataSource.getDataKeys()) {
            if (comparison && !dataKey.isComparisonKey()) continue;
            newDataKeys.add((DataKey)JacksonUtil.clone((Object)dataKey));
        }
        this.dataKeys = newDataKeys;
        this.index = index;
        this.generated = index > 0;
        this.comparison = comparison;
        this.entityId = entityData.getEntityId();
        this.entityData = entityData;
        this.entityName = "";
        this.entityLabel = "";
        this.variables = new HashMap<String, Object>();
        if (entityData.getLatest() != null && entityData.getLatest().get(EntityKeyType.ENTITY_FIELD) != null) {
            TsValue labelValue;
            Map entityFields = (Map)entityData.getLatest().get(EntityKeyType.ENTITY_FIELD);
            TsValue nameValue = (TsValue)entityFields.get("name");
            if (nameValue != null) {
                this.entityName = nameValue.getValue();
            }
            if ((labelValue = (TsValue)entityFields.get("label")) != null) {
                this.entityLabel = labelValue.getValue();
            }
        }
        this.data = new ArrayList<TsChartSeriesData>(this.getDataKeys().size());
        int dataIndex = startDataIndex;
        for (int keyIndex = 0; keyIndex < this.dataKeys.size(); ++keyIndex) {
            DataKey key = this.dataKeys.get(keyIndex);
            if (comparison) {
                TimeSeriesChartKeySettings timeSeriesChartKeySettings = (TimeSeriesChartKeySettings)key.getSettings();
                DataKeyComparisonSettings comparisonSettings = timeSeriesChartKeySettings.getComparisonSettings();
                if (StringUtils.isNotBlank((String)comparisonSettings.getComparisonValuesLabel())) {
                    key.setLabel(comparisonSettings.getComparisonValuesLabel());
                } else {
                    Object label = key.getLabel();
                    label = (String)label + " " + this.labelSuffixForComparisonUnit(timeForComparison);
                    key.setLabel((String)label);
                }
            }
            TsChartSeriesData seriesData = new TsChartSeriesData();
            seriesData.setDataSource(this);
            seriesData.setDataKey(key);
            List<TsChartSeriesEntry> keyValues = tsKvEntries.stream().filter(entry -> entry.getKey().equals(key.getName())).map(entry -> {
                String value;
                TimeIntervalCalculator.TimeRange interval = TimeIntervalCalculator.getAggTimeRange((TimeIntervalCalculator.TimeRange)timeWindow, (Interval)aggInterval, (Aggregation)aggregation, (ZoneId)zoneId, (long)entry.getTs());
                long ts = interval.startTs + (long)Math.floor((double)(interval.endTs - interval.startTs) / 2.0);
                Object processed = postProcessFunction.apply(key, ts, value = entry.getValueAsString());
                value = processed != null ? processed.toString() : null;
                Double doubleValue = null;
                if (value != null) {
                    try {
                        doubleValue = Double.parseDouble(value);
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                }
                return new TsChartSeriesEntry(ts, interval, value, doubleValue);
            }).filter(entry -> entry.getValue() != null).sorted(Comparator.comparing(TsChartSeriesEntry::getTs)).toList();
            seriesData.setData(keyValues);
            seriesData.setNumericData(keyValues.stream().map(TsChartSeriesEntry::getDoubleValue).filter(Objects::nonNull).toList());
            seriesData.setIndex(dataIndex);
            seriesData.setKeyIndex(keyIndex);
            this.data.add(seriesData);
            ++dataIndex;
        }
        this.variables.put("entityName", this.entityName);
        this.variables.put("entityLabel", this.entityLabel);
    }

    private String labelSuffixForComparisonUnit(ComparisonDuration timeUnit) {
        switch (timeUnit) {
            case previousInterval: {
                return "(previous interval)";
            }
            case days: {
                return "(day ago)";
            }
            case weeks: {
                return "(week ago)";
            }
            case months: {
                return "(month ago)";
            }
            case years: {
                return "(year ago)";
            }
            case customInterval: {
                return "(custom interval)";
            }
        }
        return "(unknown)";
    }

    @Generated
    public boolean isGenerated() {
        return this.generated;
    }

    @Generated
    public boolean isComparison() {
        return this.comparison;
    }

    @Generated
    public EntityId getEntityId() {
        return this.entityId;
    }

    @Generated
    public EntityData getEntityData() {
        return this.entityData;
    }

    @Generated
    public String getEntityName() {
        return this.entityName;
    }

    @Generated
    public String getEntityLabel() {
        return this.entityLabel;
    }

    @Generated
    public List<DataKey> getDataKeys() {
        return this.dataKeys;
    }

    @Generated
    public List<TsChartSeriesData> getData() {
        return this.data;
    }

    @Generated
    public int getIndex() {
        return this.index;
    }

    @Generated
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    @Generated
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    @Generated
    public void setEntityLabel(String entityLabel) {
        this.entityLabel = entityLabel;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TsChartDataSource)) {
            return false;
        }
        TsChartDataSource other = (TsChartDataSource)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isGenerated() != other.isGenerated()) {
            return false;
        }
        if (this.isComparison() != other.isComparison()) {
            return false;
        }
        if (this.getIndex() != other.getIndex()) {
            return false;
        }
        EntityId this$entityId = this.getEntityId();
        EntityId other$entityId = other.getEntityId();
        if (this$entityId == null ? other$entityId != null : !this$entityId.equals(other$entityId)) {
            return false;
        }
        EntityData this$entityData = this.getEntityData();
        EntityData other$entityData = other.getEntityData();
        if (this$entityData == null ? other$entityData != null : !this$entityData.equals(other$entityData)) {
            return false;
        }
        String this$entityName = this.getEntityName();
        String other$entityName = other.getEntityName();
        if (this$entityName == null ? other$entityName != null : !this$entityName.equals(other$entityName)) {
            return false;
        }
        String this$entityLabel = this.getEntityLabel();
        String other$entityLabel = other.getEntityLabel();
        if (this$entityLabel == null ? other$entityLabel != null : !this$entityLabel.equals(other$entityLabel)) {
            return false;
        }
        List<DataKey> this$dataKeys = this.getDataKeys();
        List<DataKey> other$dataKeys = other.getDataKeys();
        if (this$dataKeys == null ? other$dataKeys != null : !((Object)this$dataKeys).equals(other$dataKeys)) {
            return false;
        }
        List<TsChartSeriesData> this$data = this.getData();
        List<TsChartSeriesData> other$data = other.getData();
        if (this$data == null ? other$data != null : !((Object)this$data).equals(other$data)) {
            return false;
        }
        Map<String, Object> this$variables = this.getVariables();
        Map<String, Object> other$variables = other.getVariables();
        return !(this$variables == null ? other$variables != null : !((Object)this$variables).equals(other$variables));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TsChartDataSource;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isGenerated() ? 79 : 97);
        result = result * 59 + (this.isComparison() ? 79 : 97);
        result = result * 59 + this.getIndex();
        EntityId $entityId = this.getEntityId();
        result = result * 59 + ($entityId == null ? 43 : $entityId.hashCode());
        EntityData $entityData = this.getEntityData();
        result = result * 59 + ($entityData == null ? 43 : $entityData.hashCode());
        String $entityName = this.getEntityName();
        result = result * 59 + ($entityName == null ? 43 : $entityName.hashCode());
        String $entityLabel = this.getEntityLabel();
        result = result * 59 + ($entityLabel == null ? 43 : $entityLabel.hashCode());
        List<DataKey> $dataKeys = this.getDataKeys();
        result = result * 59 + ($dataKeys == null ? 43 : ((Object)$dataKeys).hashCode());
        List<TsChartSeriesData> $data = this.getData();
        result = result * 59 + ($data == null ? 43 : ((Object)$data).hashCode());
        Map<String, Object> $variables = this.getVariables();
        result = result * 59 + ($variables == null ? 43 : ((Object)$variables).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TsChartDataSource(generated=" + this.isGenerated() + ", comparison=" + this.isComparison() + ", entityId=" + String.valueOf(this.getEntityId()) + ", entityData=" + String.valueOf(this.getEntityData()) + ", entityName=" + this.getEntityName() + ", entityLabel=" + this.getEntityLabel() + ", dataKeys=" + String.valueOf(this.getDataKeys()) + ", data=" + String.valueOf(this.getData()) + ", index=" + this.getIndex() + ", variables=" + String.valueOf(this.getVariables()) + ")";
    }
}

