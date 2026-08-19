/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;

public class TimeSeriesChartBarWidthSettings {
    private Double barGap;
    private Double intervalGap;

    public TimeSeriesChartBarWidthSettings() {
    }

    public TimeSeriesChartBarWidthSettings(TimeSeriesChartBarWidthSettings input) {
        if (input == null) {
            input = new TimeSeriesChartBarWidthSettings();
        }
        this.barGap = input.getBarGap() != null ? input.getBarGap() : 0.3;
        this.intervalGap = input.getIntervalGap() != null ? input.getIntervalGap() : 0.6;
    }

    @Generated
    public Double getBarGap() {
        return this.barGap;
    }

    @Generated
    public Double getIntervalGap() {
        return this.intervalGap;
    }

    @Generated
    public void setBarGap(Double barGap) {
        this.barGap = barGap;
    }

    @Generated
    public void setIntervalGap(Double intervalGap) {
        this.intervalGap = intervalGap;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartBarWidthSettings)) {
            return false;
        }
        TimeSeriesChartBarWidthSettings other = (TimeSeriesChartBarWidthSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$barGap = this.getBarGap();
        Double other$barGap = other.getBarGap();
        if (this$barGap == null ? other$barGap != null : !((Object)this$barGap).equals(other$barGap)) {
            return false;
        }
        Double this$intervalGap = this.getIntervalGap();
        Double other$intervalGap = other.getIntervalGap();
        return !(this$intervalGap == null ? other$intervalGap != null : !((Object)this$intervalGap).equals(other$intervalGap));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartBarWidthSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $barGap = this.getBarGap();
        result = result * 59 + ($barGap == null ? 43 : ((Object)$barGap).hashCode());
        Double $intervalGap = this.getIntervalGap();
        result = result * 59 + ($intervalGap == null ? 43 : ((Object)$intervalGap).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartBarWidthSettings(barGap=" + this.getBarGap() + ", intervalGap=" + this.getIntervalGap() + ")";
    }
}

