/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

@Schema
public abstract class AbstractReportComponent
implements ReportComponent {
    private ReportComponentSubType subType;

    @Override
    @Generated
    public ReportComponentSubType getSubType() {
        return this.subType;
    }

    @Generated
    public void setSubType(ReportComponentSubType subType) {
        this.subType = subType;
    }

    @Generated
    public String toString() {
        return "AbstractReportComponent(subType=" + String.valueOf((Object)this.getSubType()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractReportComponent)) {
            return false;
        }
        AbstractReportComponent other = (AbstractReportComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        ReportComponentSubType this$subType = this.getSubType();
        ReportComponentSubType other$subType = other.getSubType();
        return !(this$subType == null ? other$subType != null : !((Object)((Object)this$subType)).equals((Object)other$subType));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractReportComponent;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        ReportComponentSubType $subType = this.getSubType();
        result = result * 59 + ($subType == null ? 43 : ((Object)((Object)$subType)).hashCode());
        return result;
    }

    @Generated
    public AbstractReportComponent() {
    }

    @ConstructorProperties(value={"subType"})
    @Generated
    public AbstractReportComponent(ReportComponentSubType subType) {
        this.subType = subType;
    }
}

