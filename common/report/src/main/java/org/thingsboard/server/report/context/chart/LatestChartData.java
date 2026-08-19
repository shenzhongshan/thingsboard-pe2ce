/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.context.chart;

import java.beans.ConstructorProperties;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.report.context.chart.LatestChartDataSource;

public class LatestChartData {
    private final List<LatestChartDataSource> chartData;

    @Generated
    public List<LatestChartDataSource> getChartData() {
        return this.chartData;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LatestChartData)) {
            return false;
        }
        LatestChartData other = (LatestChartData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<LatestChartDataSource> this$chartData = this.getChartData();
        List<LatestChartDataSource> other$chartData = other.getChartData();
        return !(this$chartData == null ? other$chartData != null : !((Object)this$chartData).equals(other$chartData));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LatestChartData;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<LatestChartDataSource> $chartData = this.getChartData();
        result = result * 59 + ($chartData == null ? 43 : ((Object)$chartData).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "LatestChartData(chartData=" + String.valueOf(this.getChartData()) + ")";
    }

    @ConstructorProperties(value={"chartData"})
    @Generated
    public LatestChartData(List<LatestChartDataSource> chartData) {
        this.chartData = chartData;
    }
}

