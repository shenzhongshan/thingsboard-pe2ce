/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.databind.JsonNode
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartStateSourceType;

public class TimeSeriesChartStateSettings {
    private String label;
    private Double value;
    private TimeSeriesChartStateSourceType sourceType;
    @JsonProperty(value="sourceValue")
    private JsonNode sourceValue;
    private Double sourceRangeFrom;
    private Double sourceRangeTo;

    @JsonIgnore
    public boolean isValidState() {
        if (this.value == null || !Double.isFinite(this.value) || this.sourceType == null) {
            return false;
        }
        if (this.sourceType == TimeSeriesChartStateSourceType.constant) {
            return this.sourceValue != null && !this.sourceValue.isNull();
        }
        return true;
    }

    @JsonIgnore
    public String sourceValueAsString() {
        if (this.sourceValue != null) {
            return this.sourceValue.asText();
        }
        return null;
    }

    @Generated
    public TimeSeriesChartStateSettings() {
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public Double getValue() {
        return this.value;
    }

    @Generated
    public TimeSeriesChartStateSourceType getSourceType() {
        return this.sourceType;
    }

    @Generated
    public JsonNode getSourceValue() {
        return this.sourceValue;
    }

    @Generated
    public Double getSourceRangeFrom() {
        return this.sourceRangeFrom;
    }

    @Generated
    public Double getSourceRangeTo() {
        return this.sourceRangeTo;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setValue(Double value) {
        this.value = value;
    }

    @Generated
    public void setSourceType(TimeSeriesChartStateSourceType sourceType) {
        this.sourceType = sourceType;
    }

    @Generated
    public void setSourceValue(JsonNode sourceValue) {
        this.sourceValue = sourceValue;
    }

    @Generated
    public void setSourceRangeFrom(Double sourceRangeFrom) {
        this.sourceRangeFrom = sourceRangeFrom;
    }

    @Generated
    public void setSourceRangeTo(Double sourceRangeTo) {
        this.sourceRangeTo = sourceRangeTo;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartStateSettings)) {
            return false;
        }
        TimeSeriesChartStateSettings other = (TimeSeriesChartStateSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$value = this.getValue();
        Double other$value = other.getValue();
        if (this$value == null ? other$value != null : !((Object)this$value).equals(other$value)) {
            return false;
        }
        Double this$sourceRangeFrom = this.getSourceRangeFrom();
        Double other$sourceRangeFrom = other.getSourceRangeFrom();
        if (this$sourceRangeFrom == null ? other$sourceRangeFrom != null : !((Object)this$sourceRangeFrom).equals(other$sourceRangeFrom)) {
            return false;
        }
        Double this$sourceRangeTo = this.getSourceRangeTo();
        Double other$sourceRangeTo = other.getSourceRangeTo();
        if (this$sourceRangeTo == null ? other$sourceRangeTo != null : !((Object)this$sourceRangeTo).equals(other$sourceRangeTo)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
            return false;
        }
        TimeSeriesChartStateSourceType this$sourceType = this.getSourceType();
        TimeSeriesChartStateSourceType other$sourceType = other.getSourceType();
        if (this$sourceType == null ? other$sourceType != null : !((Object)((Object)this$sourceType)).equals((Object)other$sourceType)) {
            return false;
        }
        JsonNode this$sourceValue = this.getSourceValue();
        JsonNode other$sourceValue = other.getSourceValue();
        return !(this$sourceValue == null ? other$sourceValue != null : !this$sourceValue.equals(other$sourceValue));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartStateSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : ((Object)$value).hashCode());
        Double $sourceRangeFrom = this.getSourceRangeFrom();
        result = result * 59 + ($sourceRangeFrom == null ? 43 : ((Object)$sourceRangeFrom).hashCode());
        Double $sourceRangeTo = this.getSourceRangeTo();
        result = result * 59 + ($sourceRangeTo == null ? 43 : ((Object)$sourceRangeTo).hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        TimeSeriesChartStateSourceType $sourceType = this.getSourceType();
        result = result * 59 + ($sourceType == null ? 43 : ((Object)((Object)$sourceType)).hashCode());
        JsonNode $sourceValue = this.getSourceValue();
        result = result * 59 + ($sourceValue == null ? 43 : $sourceValue.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartStateSettings(label=" + this.getLabel() + ", value=" + this.getValue() + ", sourceType=" + String.valueOf((Object)this.getSourceType()) + ", sourceValue=" + String.valueOf(this.getSourceValue()) + ", sourceRangeFrom=" + this.getSourceRangeFrom() + ", sourceRangeTo=" + this.getSourceRangeTo() + ")";
    }
}

