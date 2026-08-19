/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AbstractTableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;

@Schema
public class AlarmTableComponent
extends AbstractTableWithLayoutReportComponent {
    private DataSource alarmSource;
    private TimeWindowConfiguration timewindow;

    @Override
    @JsonIgnore
    public List<DataSource> getDataSources() {
        return List.of(this.alarmSource);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }

    @Generated
    public DataSource getAlarmSource() {
        return this.alarmSource;
    }

    @Generated
    public TimeWindowConfiguration getTimewindow() {
        return this.timewindow;
    }

    @Generated
    public void setAlarmSource(DataSource alarmSource) {
        this.alarmSource = alarmSource;
    }

    @Generated
    public void setTimewindow(TimeWindowConfiguration timewindow) {
        this.timewindow = timewindow;
    }

    @Override
    @Generated
    public String toString() {
        return "AlarmTableComponent(alarmSource=" + String.valueOf(this.getAlarmSource()) + ", timewindow=" + String.valueOf(this.getTimewindow()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AlarmTableComponent)) {
            return false;
        }
        AlarmTableComponent other = (AlarmTableComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        DataSource this$alarmSource = this.getAlarmSource();
        DataSource other$alarmSource = other.getAlarmSource();
        if (this$alarmSource == null ? other$alarmSource != null : !((Object)this$alarmSource).equals(other$alarmSource)) {
            return false;
        }
        TimeWindowConfiguration this$timewindow = this.getTimewindow();
        TimeWindowConfiguration other$timewindow = other.getTimewindow();
        return !(this$timewindow == null ? other$timewindow != null : !((Object)this$timewindow).equals(other$timewindow));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AlarmTableComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        DataSource $alarmSource = this.getAlarmSource();
        result = result * 59 + ($alarmSource == null ? 43 : ((Object)$alarmSource).hashCode());
        TimeWindowConfiguration $timewindow = this.getTimewindow();
        result = result * 59 + ($timewindow == null ? 43 : ((Object)$timewindow).hashCode());
        return result;
    }

    @Generated
    public AlarmTableComponent() {
    }
}

