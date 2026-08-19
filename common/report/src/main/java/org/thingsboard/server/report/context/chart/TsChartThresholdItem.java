/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold
 */
package org.thingsboard.server.report.context.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;

public class TsChartThresholdItem {
    private final TimeSeriesChartThreshold settings;
    private final Double value;

    public TsChartThresholdItem(TimeSeriesChartThreshold settings, Double value) {
        this.settings = new TimeSeriesChartThreshold(settings);
        this.value = value;
    }

    @Generated
    public TimeSeriesChartThreshold getSettings() {
        return this.settings;
    }

    @Generated
    public Double getValue() {
        return this.value;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TsChartThresholdItem)) {
            return false;
        }
        TsChartThresholdItem other = (TsChartThresholdItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$value = this.getValue();
        Double other$value = other.getValue();
        if (this$value == null ? other$value != null : !((Object)this$value).equals(other$value)) {
            return false;
        }
        TimeSeriesChartThreshold this$settings = this.getSettings();
        TimeSeriesChartThreshold other$settings = other.getSettings();
        return !(this$settings == null ? other$settings != null : !this$settings.equals(other$settings));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TsChartThresholdItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : ((Object)$value).hashCode());
        TimeSeriesChartThreshold $settings = this.getSettings();
        result = result * 59 + ($settings == null ? 43 : $settings.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TsChartThresholdItem(settings=" + String.valueOf(this.getSettings()) + ", value=" + this.getValue() + ")";
    }
}

