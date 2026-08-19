/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonTypeInfo
 *  com.fasterxml.jackson.annotation.JsonTypeInfo$As
 *  com.fasterxml.jackson.annotation.JsonTypeInfo$Id
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.components.AbstractChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

@Schema
public class TimeseriesChartComponent
extends AbstractChartComponent {
    private TimeWindowConfiguration timewindow;
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="subType", defaultImpl=ReportTimeSeriesChartSettings.class)
    private ReportTimeSeriesChartSettings timeSeriesChartSettings;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_CHART;
    }

    @Generated
    public TimeWindowConfiguration getTimewindow() {
        return this.timewindow;
    }

    @Generated
    public ReportTimeSeriesChartSettings getTimeSeriesChartSettings() {
        return this.timeSeriesChartSettings;
    }

    @Generated
    public void setTimewindow(TimeWindowConfiguration timewindow) {
        this.timewindow = timewindow;
    }

    @Generated
    public void setTimeSeriesChartSettings(ReportTimeSeriesChartSettings timeSeriesChartSettings) {
        this.timeSeriesChartSettings = timeSeriesChartSettings;
    }

    @Override
    @Generated
    public String toString() {
        return "TimeseriesChartComponent(timewindow=" + String.valueOf(this.getTimewindow()) + ", timeSeriesChartSettings=" + String.valueOf(this.getTimeSeriesChartSettings()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeseriesChartComponent)) {
            return false;
        }
        TimeseriesChartComponent other = (TimeseriesChartComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        TimeWindowConfiguration this$timewindow = this.getTimewindow();
        TimeWindowConfiguration other$timewindow = other.getTimewindow();
        if (this$timewindow == null ? other$timewindow != null : !((Object)this$timewindow).equals(other$timewindow)) {
            return false;
        }
        ReportTimeSeriesChartSettings this$timeSeriesChartSettings = this.getTimeSeriesChartSettings();
        ReportTimeSeriesChartSettings other$timeSeriesChartSettings = other.getTimeSeriesChartSettings();
        return !(this$timeSeriesChartSettings == null ? other$timeSeriesChartSettings != null : !((Object)this$timeSeriesChartSettings).equals(other$timeSeriesChartSettings));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeseriesChartComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        TimeWindowConfiguration $timewindow = this.getTimewindow();
        result = result * 59 + ($timewindow == null ? 43 : ((Object)$timewindow).hashCode());
        ReportTimeSeriesChartSettings $timeSeriesChartSettings = this.getTimeSeriesChartSettings();
        result = result * 59 + ($timeSeriesChartSettings == null ? 43 : ((Object)$timeSeriesChartSettings).hashCode());
        return result;
    }

    @Generated
    public TimeseriesChartComponent() {
    }
}

