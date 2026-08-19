/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidth;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;

public class TimeSeriesChartNoAggregationBarWidthSettings {
    private TimeSeriesChartNoAggregationBarWidthStrategy strategy;
    private TimeSeriesChartBarWidth groupWidth;
    private TimeSeriesChartBarWidth barWidth;

    public TimeSeriesChartNoAggregationBarWidthSettings() {
    }

    public TimeSeriesChartNoAggregationBarWidthSettings(TimeSeriesChartNoAggregationBarWidthSettings input) {
        if (input == null) {
            input = new TimeSeriesChartNoAggregationBarWidthSettings();
        }
        this.strategy = input.getStrategy() != null ? input.strategy : TimeSeriesChartNoAggregationBarWidthStrategy.group;
        this.groupWidth = new TimeSeriesChartBarWidth(input.getGroupWidth());
        this.barWidth = new TimeSeriesChartBarWidth(input.getBarWidth());
    }

    @Generated
    public TimeSeriesChartNoAggregationBarWidthStrategy getStrategy() {
        return this.strategy;
    }

    @Generated
    public TimeSeriesChartBarWidth getGroupWidth() {
        return this.groupWidth;
    }

    @Generated
    public TimeSeriesChartBarWidth getBarWidth() {
        return this.barWidth;
    }

    @Generated
    public void setStrategy(TimeSeriesChartNoAggregationBarWidthStrategy strategy) {
        this.strategy = strategy;
    }

    @Generated
    public void setGroupWidth(TimeSeriesChartBarWidth groupWidth) {
        this.groupWidth = groupWidth;
    }

    @Generated
    public void setBarWidth(TimeSeriesChartBarWidth barWidth) {
        this.barWidth = barWidth;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartNoAggregationBarWidthSettings)) {
            return false;
        }
        TimeSeriesChartNoAggregationBarWidthSettings other = (TimeSeriesChartNoAggregationBarWidthSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        TimeSeriesChartNoAggregationBarWidthStrategy this$strategy = this.getStrategy();
        TimeSeriesChartNoAggregationBarWidthStrategy other$strategy = other.getStrategy();
        if (this$strategy == null ? other$strategy != null : !((Object)((Object)this$strategy)).equals((Object)other$strategy)) {
            return false;
        }
        TimeSeriesChartBarWidth this$groupWidth = this.getGroupWidth();
        TimeSeriesChartBarWidth other$groupWidth = other.getGroupWidth();
        if (this$groupWidth == null ? other$groupWidth != null : !((Object)this$groupWidth).equals(other$groupWidth)) {
            return false;
        }
        TimeSeriesChartBarWidth this$barWidth = this.getBarWidth();
        TimeSeriesChartBarWidth other$barWidth = other.getBarWidth();
        return !(this$barWidth == null ? other$barWidth != null : !((Object)this$barWidth).equals(other$barWidth));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartNoAggregationBarWidthSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        TimeSeriesChartNoAggregationBarWidthStrategy $strategy = this.getStrategy();
        result = result * 59 + ($strategy == null ? 43 : ((Object)((Object)$strategy)).hashCode());
        TimeSeriesChartBarWidth $groupWidth = this.getGroupWidth();
        result = result * 59 + ($groupWidth == null ? 43 : ((Object)$groupWidth).hashCode());
        TimeSeriesChartBarWidth $barWidth = this.getBarWidth();
        result = result * 59 + ($barWidth == null ? 43 : ((Object)$barWidth).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartNoAggregationBarWidthSettings(strategy=" + String.valueOf((Object)this.getStrategy()) + ", groupWidth=" + String.valueOf(this.getGroupWidth()) + ", barWidth=" + String.valueOf(this.getBarWidth()) + ")";
    }
}

