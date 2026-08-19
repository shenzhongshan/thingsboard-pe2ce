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
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.configuration.components.AbstractDataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class SubReportComponent
extends AbstractDataReportComponent {
    private ReportTemplateId templateId;
    private boolean avoidPageBreakInside;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.SUB_REPORT;
    }

    @Generated
    public ReportTemplateId getTemplateId() {
        return this.templateId;
    }

    @Generated
    public boolean isAvoidPageBreakInside() {
        return this.avoidPageBreakInside;
    }

    @Generated
    public void setTemplateId(ReportTemplateId templateId) {
        this.templateId = templateId;
    }

    @Generated
    public void setAvoidPageBreakInside(boolean avoidPageBreakInside) {
        this.avoidPageBreakInside = avoidPageBreakInside;
    }

    @Override
    @Generated
    public String toString() {
        return "SubReportComponent(templateId=" + String.valueOf(this.getTemplateId()) + ", avoidPageBreakInside=" + this.isAvoidPageBreakInside() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SubReportComponent)) {
            return false;
        }
        SubReportComponent other = (SubReportComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isAvoidPageBreakInside() != other.isAvoidPageBreakInside()) {
            return false;
        }
        ReportTemplateId this$templateId = this.getTemplateId();
        ReportTemplateId other$templateId = other.getTemplateId();
        return !(this$templateId == null ? other$templateId != null : !((Object)this$templateId).equals(other$templateId));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SubReportComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isAvoidPageBreakInside() ? 79 : 97);
        ReportTemplateId $templateId = this.getTemplateId();
        result = result * 59 + ($templateId == null ? 43 : ((Object)$templateId).hashCode());
        return result;
    }

    @Generated
    public SubReportComponent() {
    }
}

