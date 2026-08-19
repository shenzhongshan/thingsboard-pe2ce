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
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;
import org.thingsboard.server.common.data.report.configuration.components.AbstractChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class LatestChartComponent
extends AbstractChartComponent {
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="subType")
    private ReportLatestChartSettings latestChartSettings;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.LATEST_CHART;
    }

    @Generated
    public ReportLatestChartSettings getLatestChartSettings() {
        return this.latestChartSettings;
    }

    @Generated
    public void setLatestChartSettings(ReportLatestChartSettings latestChartSettings) {
        this.latestChartSettings = latestChartSettings;
    }

    @Override
    @Generated
    public String toString() {
        return "LatestChartComponent(latestChartSettings=" + String.valueOf(this.getLatestChartSettings()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LatestChartComponent)) {
            return false;
        }
        LatestChartComponent other = (LatestChartComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        ReportLatestChartSettings this$latestChartSettings = this.getLatestChartSettings();
        ReportLatestChartSettings other$latestChartSettings = other.getLatestChartSettings();
        return !(this$latestChartSettings == null ? other$latestChartSettings != null : !((Object)this$latestChartSettings).equals(other$latestChartSettings));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LatestChartComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        ReportLatestChartSettings $latestChartSettings = this.getLatestChartSettings();
        result = result * 59 + ($latestChartSettings == null ? 43 : ((Object)$latestChartSettings).hashCode());
        return result;
    }

    @Generated
    public LatestChartComponent() {
    }
}

