/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  jakarta.validation.constraints.NotNull
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Generated;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class DashboardComponent
extends AbstractImageComponent {
    @NotNull
    @Schema(description="Dashboard report configuration.")
    private DashboardReportConfig config;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DASHBOARD;
    }

    @Generated
    public DashboardReportConfig getConfig() {
        return this.config;
    }

    @Generated
    public void setConfig(DashboardReportConfig config) {
        this.config = config;
    }

    @Override
    @Generated
    public String toString() {
        return "DashboardComponent(config=" + String.valueOf(this.getConfig()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DashboardComponent)) {
            return false;
        }
        DashboardComponent other = (DashboardComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        DashboardReportConfig this$config = this.getConfig();
        DashboardReportConfig other$config = other.getConfig();
        return !(this$config == null ? other$config != null : !((Object)this$config).equals(other$config));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DashboardComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        DashboardReportConfig $config = this.getConfig();
        result = result * 59 + ($config == null ? 43 : ((Object)$config).hashCode());
        return result;
    }

    @Generated
    public DashboardComponent() {
    }
}

