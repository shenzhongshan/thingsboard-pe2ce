/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.components.AbstractTableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

@Schema
public class TimeseriesTableComponent
extends AbstractTableWithLayoutReportComponent {
    private TimeWindowConfiguration timewindow;
    private boolean showTimestamp;
    private String timestampLabel;
    private String timestampPattern;
    private ColumnSettings timestampColumnSettings;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

    @Generated
    public TimeWindowConfiguration getTimewindow() {
        return this.timewindow;
    }

    @Generated
    public boolean isShowTimestamp() {
        return this.showTimestamp;
    }

    @Generated
    public String getTimestampLabel() {
        return this.timestampLabel;
    }

    @Generated
    public String getTimestampPattern() {
        return this.timestampPattern;
    }

    @Generated
    public ColumnSettings getTimestampColumnSettings() {
        return this.timestampColumnSettings;
    }

    @Generated
    public void setTimewindow(TimeWindowConfiguration timewindow) {
        this.timewindow = timewindow;
    }

    @Generated
    public void setShowTimestamp(boolean showTimestamp) {
        this.showTimestamp = showTimestamp;
    }

    @Generated
    public void setTimestampLabel(String timestampLabel) {
        this.timestampLabel = timestampLabel;
    }

    @Generated
    public void setTimestampPattern(String timestampPattern) {
        this.timestampPattern = timestampPattern;
    }

    @Generated
    public void setTimestampColumnSettings(ColumnSettings timestampColumnSettings) {
        this.timestampColumnSettings = timestampColumnSettings;
    }

    @Override
    @Generated
    public String toString() {
        return "TimeseriesTableComponent(timewindow=" + String.valueOf(this.getTimewindow()) + ", showTimestamp=" + this.isShowTimestamp() + ", timestampLabel=" + this.getTimestampLabel() + ", timestampPattern=" + this.getTimestampPattern() + ", timestampColumnSettings=" + String.valueOf(this.getTimestampColumnSettings()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeseriesTableComponent)) {
            return false;
        }
        TimeseriesTableComponent other = (TimeseriesTableComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isShowTimestamp() != other.isShowTimestamp()) {
            return false;
        }
        TimeWindowConfiguration this$timewindow = this.getTimewindow();
        TimeWindowConfiguration other$timewindow = other.getTimewindow();
        if (this$timewindow == null ? other$timewindow != null : !((Object)this$timewindow).equals(other$timewindow)) {
            return false;
        }
        String this$timestampLabel = this.getTimestampLabel();
        String other$timestampLabel = other.getTimestampLabel();
        if (this$timestampLabel == null ? other$timestampLabel != null : !this$timestampLabel.equals(other$timestampLabel)) {
            return false;
        }
        String this$timestampPattern = this.getTimestampPattern();
        String other$timestampPattern = other.getTimestampPattern();
        if (this$timestampPattern == null ? other$timestampPattern != null : !this$timestampPattern.equals(other$timestampPattern)) {
            return false;
        }
        ColumnSettings this$timestampColumnSettings = this.getTimestampColumnSettings();
        ColumnSettings other$timestampColumnSettings = other.getTimestampColumnSettings();
        return !(this$timestampColumnSettings == null ? other$timestampColumnSettings != null : !((Object)this$timestampColumnSettings).equals(other$timestampColumnSettings));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeseriesTableComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isShowTimestamp() ? 79 : 97);
        TimeWindowConfiguration $timewindow = this.getTimewindow();
        result = result * 59 + ($timewindow == null ? 43 : ((Object)$timewindow).hashCode());
        String $timestampLabel = this.getTimestampLabel();
        result = result * 59 + ($timestampLabel == null ? 43 : $timestampLabel.hashCode());
        String $timestampPattern = this.getTimestampPattern();
        result = result * 59 + ($timestampPattern == null ? 43 : $timestampPattern.hashCode());
        ColumnSettings $timestampColumnSettings = this.getTimestampColumnSettings();
        result = result * 59 + ($timestampColumnSettings == null ? 43 : ((Object)$timestampColumnSettings).hashCode());
        return result;
    }

    @Generated
    public TimeseriesTableComponent() {
    }
}

