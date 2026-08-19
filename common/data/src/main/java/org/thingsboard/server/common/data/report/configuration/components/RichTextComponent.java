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
import org.thingsboard.server.common.data.report.configuration.components.AbstractDataWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class RichTextComponent
extends AbstractDataWithLayoutReportComponent {
    private String value;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.RICH_TEXT;
    }

    @Generated
    public String getValue() {
        return this.value;
    }

    @Generated
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    @Generated
    public String toString() {
        return "RichTextComponent(value=" + this.getValue() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RichTextComponent)) {
            return false;
        }
        RichTextComponent other = (RichTextComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$value = this.getValue();
        String other$value = other.getValue();
        return !(this$value == null ? other$value != null : !this$value.equals(other$value));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof RichTextComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : $value.hashCode());
        return result;
    }

    @Generated
    public RichTextComponent() {
    }
}

