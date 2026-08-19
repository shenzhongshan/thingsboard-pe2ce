/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

@Schema
public class DataKey {
    private String name;
    private String type;
    private String label;
    private String color;
    private Integer decimals;
    private String units;
    private Aggregation aggregationType;
    private TimeWindowConfiguration timewindow;
    private boolean usePostProcessing;
    private String postFuncBody;
    private DataKeySettings settings;

    public DataKey(String name, String type, String label) {
        this.name = name;
        this.type = type;
        this.label = label;
    }

    @JsonIgnore
    public boolean isComparisonKey() {
        TimeSeriesChartKeySettings timeSeriesChartKeySettings;
        DataKeyComparisonSettings comparisonSettings;
        if (this.settings != null && this.settings.getType() == DataKeySettingsType.TIME_SERIES_CHART && (comparisonSettings = (timeSeriesChartKeySettings = (TimeSeriesChartKeySettings)this.settings).getComparisonSettings()) != null) {
            return comparisonSettings.getShowValuesForComparison() != null ? comparisonSettings.getShowValuesForComparison() : false;
        }
        return false;
    }

    @Generated
    public static DataKeyBuilder builder() {
        return new DataKeyBuilder();
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getType() {
        return this.type;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public Integer getDecimals() {
        return this.decimals;
    }

    @Generated
    public String getUnits() {
        return this.units;
    }

    @Generated
    public Aggregation getAggregationType() {
        return this.aggregationType;
    }

    @Generated
    public TimeWindowConfiguration getTimewindow() {
        return this.timewindow;
    }

    @Generated
    public boolean isUsePostProcessing() {
        return this.usePostProcessing;
    }

    @Generated
    public String getPostFuncBody() {
        return this.postFuncBody;
    }

    @Generated
    public DataKeySettings getSettings() {
        return this.settings;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setType(String type) {
        this.type = type;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    @Generated
    public void setUnits(String units) {
        this.units = units;
    }

    @Generated
    public void setAggregationType(Aggregation aggregationType) {
        this.aggregationType = aggregationType;
    }

    @Generated
    public void setTimewindow(TimeWindowConfiguration timewindow) {
        this.timewindow = timewindow;
    }

    @Generated
    public void setUsePostProcessing(boolean usePostProcessing) {
        this.usePostProcessing = usePostProcessing;
    }

    @Generated
    public void setPostFuncBody(String postFuncBody) {
        this.postFuncBody = postFuncBody;
    }

    @Generated
    public void setSettings(DataKeySettings settings) {
        this.settings = settings;
    }

    @Generated
    public String toString() {
        return "DataKey(name=" + this.getName() + ", type=" + this.getType() + ", label=" + this.getLabel() + ", color=" + this.getColor() + ", decimals=" + this.getDecimals() + ", units=" + this.getUnits() + ", aggregationType=" + String.valueOf((Object)this.getAggregationType()) + ", timewindow=" + String.valueOf(this.getTimewindow()) + ", usePostProcessing=" + this.isUsePostProcessing() + ", postFuncBody=" + this.getPostFuncBody() + ", settings=" + String.valueOf(this.getSettings()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DataKey)) {
            return false;
        }
        DataKey other = (DataKey)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isUsePostProcessing() != other.isUsePostProcessing()) {
            return false;
        }
        Integer this$decimals = this.getDecimals();
        Integer other$decimals = other.getDecimals();
        if (this$decimals == null ? other$decimals != null : !((Object)this$decimals).equals(other$decimals)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        String this$units = this.getUnits();
        String other$units = other.getUnits();
        if (this$units == null ? other$units != null : !this$units.equals(other$units)) {
            return false;
        }
        Aggregation this$aggregationType = this.getAggregationType();
        Aggregation other$aggregationType = other.getAggregationType();
        if (this$aggregationType == null ? other$aggregationType != null : !((Object)((Object)this$aggregationType)).equals((Object)other$aggregationType)) {
            return false;
        }
        TimeWindowConfiguration this$timewindow = this.getTimewindow();
        TimeWindowConfiguration other$timewindow = other.getTimewindow();
        if (this$timewindow == null ? other$timewindow != null : !((Object)this$timewindow).equals(other$timewindow)) {
            return false;
        }
        String this$postFuncBody = this.getPostFuncBody();
        String other$postFuncBody = other.getPostFuncBody();
        if (this$postFuncBody == null ? other$postFuncBody != null : !this$postFuncBody.equals(other$postFuncBody)) {
            return false;
        }
        DataKeySettings this$settings = this.getSettings();
        DataKeySettings other$settings = other.getSettings();
        return !(this$settings == null ? other$settings != null : !this$settings.equals(other$settings));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DataKey;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isUsePostProcessing() ? 79 : 97);
        Integer $decimals = this.getDecimals();
        result = result * 59 + ($decimals == null ? 43 : ((Object)$decimals).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        String $units = this.getUnits();
        result = result * 59 + ($units == null ? 43 : $units.hashCode());
        Aggregation $aggregationType = this.getAggregationType();
        result = result * 59 + ($aggregationType == null ? 43 : ((Object)((Object)$aggregationType)).hashCode());
        TimeWindowConfiguration $timewindow = this.getTimewindow();
        result = result * 59 + ($timewindow == null ? 43 : ((Object)$timewindow).hashCode());
        String $postFuncBody = this.getPostFuncBody();
        result = result * 59 + ($postFuncBody == null ? 43 : $postFuncBody.hashCode());
        DataKeySettings $settings = this.getSettings();
        result = result * 59 + ($settings == null ? 43 : $settings.hashCode());
        return result;
    }

    @Generated
    public DataKey() {
    }

    @ConstructorProperties(value={"name", "type", "label", "color", "decimals", "units", "aggregationType", "timewindow", "usePostProcessing", "postFuncBody", "settings"})
    @Generated
    public DataKey(String name, String type, String label, String color, Integer decimals, String units, Aggregation aggregationType, TimeWindowConfiguration timewindow, boolean usePostProcessing, String postFuncBody, DataKeySettings settings) {
        this.name = name;
        this.type = type;
        this.label = label;
        this.color = color;
        this.decimals = decimals;
        this.units = units;
        this.aggregationType = aggregationType;
        this.timewindow = timewindow;
        this.usePostProcessing = usePostProcessing;
        this.postFuncBody = postFuncBody;
        this.settings = settings;
    }

    @Generated
    public static class DataKeyBuilder {
        @Generated
        private String name;
        @Generated
        private String type;
        @Generated
        private String label;
        @Generated
        private String color;
        @Generated
        private Integer decimals;
        @Generated
        private String units;
        @Generated
        private Aggregation aggregationType;
        @Generated
        private TimeWindowConfiguration timewindow;
        @Generated
        private boolean usePostProcessing;
        @Generated
        private String postFuncBody;
        @Generated
        private DataKeySettings settings;

        @Generated
        DataKeyBuilder() {
        }

        @Generated
        public DataKeyBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Generated
        public DataKeyBuilder type(String type) {
            this.type = type;
            return this;
        }

        @Generated
        public DataKeyBuilder label(String label) {
            this.label = label;
            return this;
        }

        @Generated
        public DataKeyBuilder color(String color) {
            this.color = color;
            return this;
        }

        @Generated
        public DataKeyBuilder decimals(Integer decimals) {
            this.decimals = decimals;
            return this;
        }

        @Generated
        public DataKeyBuilder units(String units) {
            this.units = units;
            return this;
        }

        @Generated
        public DataKeyBuilder aggregationType(Aggregation aggregationType) {
            this.aggregationType = aggregationType;
            return this;
        }

        @Generated
        public DataKeyBuilder timewindow(TimeWindowConfiguration timewindow) {
            this.timewindow = timewindow;
            return this;
        }

        @Generated
        public DataKeyBuilder usePostProcessing(boolean usePostProcessing) {
            this.usePostProcessing = usePostProcessing;
            return this;
        }

        @Generated
        public DataKeyBuilder postFuncBody(String postFuncBody) {
            this.postFuncBody = postFuncBody;
            return this;
        }

        @Generated
        public DataKeyBuilder settings(DataKeySettings settings) {
            this.settings = settings;
            return this;
        }

        @Generated
        public DataKey build() {
            return new DataKey(this.name, this.type, this.label, this.color, this.decimals, this.units, this.aggregationType, this.timewindow, this.usePostProcessing, this.postFuncBody, this.settings);
        }

        @Generated
        public String toString() {
            return "DataKey.DataKeyBuilder(name=" + this.name + ", type=" + this.type + ", label=" + this.label + ", color=" + this.color + ", decimals=" + this.decimals + ", units=" + this.units + ", aggregationType=" + String.valueOf((Object)this.aggregationType) + ", timewindow=" + String.valueOf(this.timewindow) + ", usePostProcessing=" + this.usePostProcessing + ", postFuncBody=" + this.postFuncBody + ", settings=" + String.valueOf(this.settings) + ")";
        }
    }
}

