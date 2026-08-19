/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.common.util.JacksonUtil
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.kv.Aggregation
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.query.EntityKeyType
 *  org.thingsboard.server.common.data.query.TsValue
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 */
package org.thingsboard.server.report.context.chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.report.context.chart.DataPostProcessFunction;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;
import org.thingsboard.server.report.util.ThymeleafUtil;

public class LatestChartDataSource {
    private final boolean generated;
    private final EntityId entityId;
    private final EntityData entityData;
    private String entityName;
    private String entityLabel;
    private final List<DataKey> dataKeys;
    private final List<LatestChartDataItem> items;
    private final int index;
    private final Map<String, Object> variables;

    public LatestChartDataSource(DataSource dataSource, EntityData entityData, DataPostProcessFunction postProcessFunction, int index) {
        ArrayList<DataKey> newDataKeys = new ArrayList<DataKey>();
        for (DataKey dataKey : dataSource.getDataKeys()) {
            newDataKeys.add((DataKey)JacksonUtil.clone((Object)dataKey));
        }
        this.dataKeys = newDataKeys;
        this.index = index;
        this.generated = index > 0;
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
        this.variables.put("entityName", this.entityName);
        this.variables.put("entityLabel", this.entityLabel);
        this.items = new ArrayList<LatestChartDataItem>(this.getDataKeys().size());
        int dataIndex = this.index * this.dataKeys.size();
        for (int keyIndex = 0; keyIndex < this.dataKeys.size(); ++keyIndex) {
            DataKey key = this.dataKeys.get(keyIndex);
            LatestChartDataItem dataItem = new LatestChartDataItem();
            dataItem.setDataSource(this);
            dataItem.setDataKey(key);
            this.putDataForDataKey(key, dataItem, postProcessFunction);
            dataItem.setIndex(dataIndex);
            dataItem.setKeyIndex(keyIndex);
            String label = ThymeleafUtil.renderFromTextString(key.getLabel(), this.variables);
            dataItem.setLabel(label);
            this.items.add(dataItem);
            ++dataIndex;
        }
    }

    private void putDataForDataKey(DataKey dataKey, LatestChartDataItem dataItem, DataPostProcessFunction postProcessFunction) {
        dataItem.setHasValue(false);
        dataItem.setValue(0.0);
        TsValue tsValue = this.extractTsValueForDataKey(dataKey);
        if (tsValue != null) {
            dataItem.setTs(tsValue.getTs());
            String value = tsValue.getValue();
            Object processed = postProcessFunction.apply(dataKey, dataItem.getTs(), value);
            String string = value = processed != null ? processed.toString() : null;
            if (value != null) {
                try {
                    double doubleValue = Double.parseDouble(value);
                    dataItem.setHasValue(true);
                    dataItem.setValue(doubleValue);
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
        }
    }

    private TsValue extractTsValueForDataKey(DataKey dataKey) {
        Map latest;
        Map keyValueMap;
        if (dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE) {
            Map timeseries;
            TsValue[] values;
            if (this.entityData.getTimeseries() != null && (values = (TsValue[])(timeseries = this.entityData.getTimeseries()).get(dataKey.getLabel())) != null && values.length > 0) {
                return values[0];
            }
        } else if (this.entityData.getLatest() != null && (keyValueMap = (Map)(latest = this.entityData.getLatest()).get(EntityKeyType.fromName((String)dataKey.getType()))) != null) {
            return (TsValue)keyValueMap.get(dataKey.getName());
        }
        return null;
    }

    @Generated
    public boolean isGenerated() {
        return this.generated;
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
    public List<LatestChartDataItem> getItems() {
        return this.items;
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
        if (!(o instanceof LatestChartDataSource)) {
            return false;
        }
        LatestChartDataSource other = (LatestChartDataSource)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isGenerated() != other.isGenerated()) {
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
        List<LatestChartDataItem> this$items = this.getItems();
        List<LatestChartDataItem> other$items = other.getItems();
        if (this$items == null ? other$items != null : !((Object)this$items).equals(other$items)) {
            return false;
        }
        Map<String, Object> this$variables = this.getVariables();
        Map<String, Object> other$variables = other.getVariables();
        return !(this$variables == null ? other$variables != null : !((Object)this$variables).equals(other$variables));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LatestChartDataSource;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isGenerated() ? 79 : 97);
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
        List<LatestChartDataItem> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        Map<String, Object> $variables = this.getVariables();
        result = result * 59 + ($variables == null ? 43 : ((Object)$variables).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "LatestChartDataSource(generated=" + this.isGenerated() + ", entityId=" + String.valueOf(this.getEntityId()) + ", entityData=" + String.valueOf(this.getEntityData()) + ", entityName=" + this.getEntityName() + ", entityLabel=" + this.getEntityLabel() + ", dataKeys=" + String.valueOf(this.getDataKeys()) + ", items=" + String.valueOf(this.getItems()) + ", index=" + this.getIndex() + ", variables=" + String.valueOf(this.getVariables()) + ")";
    }
}

