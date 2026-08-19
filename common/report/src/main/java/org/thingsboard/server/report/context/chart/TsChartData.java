/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 */
package org.thingsboard.server.report.context.chart;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.TimeZone;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;

public class TsChartData {
    private final TimeZone timeZone;
    private final TimeIntervalCalculator.TimeRange timeRange;
    private final boolean noAggregation;
    private final List<TsChartDataSource> chartData;
    private final List<TsChartThresholdItem> thresholdItems;
    private final List<TsChartRangeItem> rangeItems;
    private final boolean comparisonEnabled;
    private final TimeIntervalCalculator.TimeRange comparisonTimeRange;

    @Generated
    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    @Generated
    public TimeIntervalCalculator.TimeRange getTimeRange() {
        return this.timeRange;
    }

    @Generated
    public boolean isNoAggregation() {
        return this.noAggregation;
    }

    @Generated
    public List<TsChartDataSource> getChartData() {
        return this.chartData;
    }

    @Generated
    public List<TsChartThresholdItem> getThresholdItems() {
        return this.thresholdItems;
    }

    @Generated
    public List<TsChartRangeItem> getRangeItems() {
        return this.rangeItems;
    }

    @Generated
    public boolean isComparisonEnabled() {
        return this.comparisonEnabled;
    }

    @Generated
    public TimeIntervalCalculator.TimeRange getComparisonTimeRange() {
        return this.comparisonTimeRange;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TsChartData)) {
            return false;
        }
        TsChartData other = (TsChartData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isNoAggregation() != other.isNoAggregation()) {
            return false;
        }
        if (this.isComparisonEnabled() != other.isComparisonEnabled()) {
            return false;
        }
        TimeZone this$timeZone = this.getTimeZone();
        TimeZone other$timeZone = other.getTimeZone();
        if (this$timeZone == null ? other$timeZone != null : !this$timeZone.equals(other$timeZone)) {
            return false;
        }
        TimeIntervalCalculator.TimeRange this$timeRange = this.getTimeRange();
        TimeIntervalCalculator.TimeRange other$timeRange = other.getTimeRange();
        if (this$timeRange == null ? other$timeRange != null : !this$timeRange.equals(other$timeRange)) {
            return false;
        }
        List<TsChartDataSource> this$chartData = this.getChartData();
        List<TsChartDataSource> other$chartData = other.getChartData();
        if (this$chartData == null ? other$chartData != null : !((Object)this$chartData).equals(other$chartData)) {
            return false;
        }
        List<TsChartThresholdItem> this$thresholdItems = this.getThresholdItems();
        List<TsChartThresholdItem> other$thresholdItems = other.getThresholdItems();
        if (this$thresholdItems == null ? other$thresholdItems != null : !((Object)this$thresholdItems).equals(other$thresholdItems)) {
            return false;
        }
        List<TsChartRangeItem> this$rangeItems = this.getRangeItems();
        List<TsChartRangeItem> other$rangeItems = other.getRangeItems();
        if (this$rangeItems == null ? other$rangeItems != null : !((Object)this$rangeItems).equals(other$rangeItems)) {
            return false;
        }
        TimeIntervalCalculator.TimeRange this$comparisonTimeRange = this.getComparisonTimeRange();
        TimeIntervalCalculator.TimeRange other$comparisonTimeRange = other.getComparisonTimeRange();
        return !(this$comparisonTimeRange == null ? other$comparisonTimeRange != null : !this$comparisonTimeRange.equals(other$comparisonTimeRange));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TsChartData;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isNoAggregation() ? 79 : 97);
        result = result * 59 + (this.isComparisonEnabled() ? 79 : 97);
        TimeZone $timeZone = this.getTimeZone();
        result = result * 59 + ($timeZone == null ? 43 : $timeZone.hashCode());
        TimeIntervalCalculator.TimeRange $timeRange = this.getTimeRange();
        result = result * 59 + ($timeRange == null ? 43 : $timeRange.hashCode());
        List<TsChartDataSource> $chartData = this.getChartData();
        result = result * 59 + ($chartData == null ? 43 : ((Object)$chartData).hashCode());
        List<TsChartThresholdItem> $thresholdItems = this.getThresholdItems();
        result = result * 59 + ($thresholdItems == null ? 43 : ((Object)$thresholdItems).hashCode());
        List<TsChartRangeItem> $rangeItems = this.getRangeItems();
        result = result * 59 + ($rangeItems == null ? 43 : ((Object)$rangeItems).hashCode());
        TimeIntervalCalculator.TimeRange $comparisonTimeRange = this.getComparisonTimeRange();
        result = result * 59 + ($comparisonTimeRange == null ? 43 : $comparisonTimeRange.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TsChartData(timeZone=" + String.valueOf(this.getTimeZone()) + ", timeRange=" + String.valueOf(this.getTimeRange()) + ", noAggregation=" + this.isNoAggregation() + ", chartData=" + String.valueOf(this.getChartData()) + ", thresholdItems=" + String.valueOf(this.getThresholdItems()) + ", rangeItems=" + String.valueOf(this.getRangeItems()) + ", comparisonEnabled=" + this.isComparisonEnabled() + ", comparisonTimeRange=" + String.valueOf(this.getComparisonTimeRange()) + ")";
    }

    @ConstructorProperties(value={"timeZone", "timeRange", "noAggregation", "chartData", "thresholdItems", "rangeItems", "comparisonEnabled", "comparisonTimeRange"})
    @Generated
    public TsChartData(TimeZone timeZone, TimeIntervalCalculator.TimeRange timeRange, boolean noAggregation, List<TsChartDataSource> chartData, List<TsChartThresholdItem> thresholdItems, List<TsChartRangeItem> rangeItems, boolean comparisonEnabled, TimeIntervalCalculator.TimeRange comparisonTimeRange) {
        this.timeZone = timeZone;
        this.timeRange = timeRange;
        this.noAggregation = noAggregation;
        this.chartData = chartData;
        this.thresholdItems = thresholdItems;
        this.rangeItems = rangeItems;
        this.comparisonEnabled = comparisonEnabled;
        this.comparisonTimeRange = comparisonTimeRange;
    }
}

