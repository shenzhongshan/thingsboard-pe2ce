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
import org.thingsboard.server.common.data.report.configuration.components.AbstractReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class PageBreakComponent
extends AbstractReportComponent
implements ReportComponent {
    @Override
    public ReportComponentType getType() {
        return ReportComponentType.PAGE_BREAK;
    }

    @Override
    @Generated
    public String toString() {
        return "PageBreakComponent()";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PageBreakComponent)) {
            return false;
        }
        PageBreakComponent other = (PageBreakComponent)o;
        return other.canEqual(this);
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PageBreakComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        boolean result = true;
        return 1;
    }

    @Generated
    public PageBreakComponent() {
    }
}

