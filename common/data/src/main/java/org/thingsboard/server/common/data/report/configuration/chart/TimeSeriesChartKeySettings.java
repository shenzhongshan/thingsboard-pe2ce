/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.BarWithLabelsSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;
import org.thingsboard.server.common.data.report.configuration.style.DataKeySettingsType;

public class TimeSeriesChartKeySettings
implements DataKeySettings {
    @JsonProperty(value="yAxisId")
    private String yAxisId;
    private Boolean showInLegend;
    private TimeSeriesChartSeriesType seriesType;
    private LineSeriesSettings lineSettings;
    private BarSeriesSettings barSettings;
    private DataKeyComparisonSettings comparisonSettings;

    public TimeSeriesChartKeySettings() {
    }

    public TimeSeriesChartKeySettings(TimeSeriesChartKeySettings input) {
        BarSeriesSettings barSeriesSettings;
        if (input == null) {
            input = new TimeSeriesChartKeySettings();
        }
        this.yAxisId = input.getYAxisId() != null ? input.getYAxisId() : "default";
        this.showInLegend = input.getShowInLegend() != null ? input.getShowInLegend() : Boolean.TRUE;
        this.seriesType = input.getSeriesType() != null ? input.getSeriesType() : TimeSeriesChartSeriesType.line;
        this.lineSettings = new LineSeriesSettings(input.getLineSettings());
        if (input.getBarSettings() != null && (barSeriesSettings = input.getBarSettings()) instanceof BarWithLabelsSeriesSettings) {
            BarWithLabelsSeriesSettings barWithLabelsSeriesSettings = (BarWithLabelsSeriesSettings)barSeriesSettings;
            this.barSettings = new BarWithLabelsSeriesSettings(barWithLabelsSeriesSettings);
        } else {
            this.barSettings = new BarSeriesSettings(input.getBarSettings());
        }
        this.comparisonSettings = new DataKeyComparisonSettings(input.getComparisonSettings());
    }

    @Override
    public DataKeySettingsType getType() {
        return DataKeySettingsType.TIME_SERIES_CHART;
    }

    @Generated
    public String getYAxisId() {
        return this.yAxisId;
    }

    @Generated
    public Boolean getShowInLegend() {
        return this.showInLegend;
    }

    @Generated
    public TimeSeriesChartSeriesType getSeriesType() {
        return this.seriesType;
    }

    @Generated
    public LineSeriesSettings getLineSettings() {
        return this.lineSettings;
    }

    @Generated
    public BarSeriesSettings getBarSettings() {
        return this.barSettings;
    }

    @Generated
    public DataKeyComparisonSettings getComparisonSettings() {
        return this.comparisonSettings;
    }

    @Generated
    public void setYAxisId(String yAxisId) {
        this.yAxisId = yAxisId;
    }

    @Generated
    public void setShowInLegend(Boolean showInLegend) {
        this.showInLegend = showInLegend;
    }

    @Generated
    public void setSeriesType(TimeSeriesChartSeriesType seriesType) {
        this.seriesType = seriesType;
    }

    @Generated
    public void setLineSettings(LineSeriesSettings lineSettings) {
        this.lineSettings = lineSettings;
    }

    @Generated
    public void setBarSettings(BarSeriesSettings barSettings) {
        this.barSettings = barSettings;
    }

    @Generated
    public void setComparisonSettings(DataKeyComparisonSettings comparisonSettings) {
        this.comparisonSettings = comparisonSettings;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartKeySettings)) {
            return false;
        }
        TimeSeriesChartKeySettings other = (TimeSeriesChartKeySettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showInLegend = this.getShowInLegend();
        Boolean other$showInLegend = other.getShowInLegend();
        if (this$showInLegend == null ? other$showInLegend != null : !((Object)this$showInLegend).equals(other$showInLegend)) {
            return false;
        }
        String this$yAxisId = this.getYAxisId();
        String other$yAxisId = other.getYAxisId();
        if (this$yAxisId == null ? other$yAxisId != null : !this$yAxisId.equals(other$yAxisId)) {
            return false;
        }
        TimeSeriesChartSeriesType this$seriesType = this.getSeriesType();
        TimeSeriesChartSeriesType other$seriesType = other.getSeriesType();
        if (this$seriesType == null ? other$seriesType != null : !((Object)((Object)this$seriesType)).equals((Object)other$seriesType)) {
            return false;
        }
        LineSeriesSettings this$lineSettings = this.getLineSettings();
        LineSeriesSettings other$lineSettings = other.getLineSettings();
        if (this$lineSettings == null ? other$lineSettings != null : !((Object)this$lineSettings).equals(other$lineSettings)) {
            return false;
        }
        BarSeriesSettings this$barSettings = this.getBarSettings();
        BarSeriesSettings other$barSettings = other.getBarSettings();
        if (this$barSettings == null ? other$barSettings != null : !((Object)this$barSettings).equals(other$barSettings)) {
            return false;
        }
        DataKeyComparisonSettings this$comparisonSettings = this.getComparisonSettings();
        DataKeyComparisonSettings other$comparisonSettings = other.getComparisonSettings();
        return !(this$comparisonSettings == null ? other$comparisonSettings != null : !((Object)this$comparisonSettings).equals(other$comparisonSettings));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartKeySettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showInLegend = this.getShowInLegend();
        result = result * 59 + ($showInLegend == null ? 43 : ((Object)$showInLegend).hashCode());
        String $yAxisId = this.getYAxisId();
        result = result * 59 + ($yAxisId == null ? 43 : $yAxisId.hashCode());
        TimeSeriesChartSeriesType $seriesType = this.getSeriesType();
        result = result * 59 + ($seriesType == null ? 43 : ((Object)((Object)$seriesType)).hashCode());
        LineSeriesSettings $lineSettings = this.getLineSettings();
        result = result * 59 + ($lineSettings == null ? 43 : ((Object)$lineSettings).hashCode());
        BarSeriesSettings $barSettings = this.getBarSettings();
        result = result * 59 + ($barSettings == null ? 43 : ((Object)$barSettings).hashCode());
        DataKeyComparisonSettings $comparisonSettings = this.getComparisonSettings();
        result = result * 59 + ($comparisonSettings == null ? 43 : ((Object)$comparisonSettings).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartKeySettings(yAxisId=" + this.getYAxisId() + ", showInLegend=" + this.getShowInLegend() + ", seriesType=" + String.valueOf((Object)this.getSeriesType()) + ", lineSettings=" + String.valueOf(this.getLineSettings()) + ", barSettings=" + String.valueOf(this.getBarSettings()) + ", comparisonSettings=" + String.valueOf(this.getComparisonSettings()) + ")";
    }
}

