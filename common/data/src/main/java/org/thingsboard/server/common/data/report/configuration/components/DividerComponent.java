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
import org.thingsboard.server.common.data.report.configuration.components.AbstractLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.BorderLength;
import org.thingsboard.server.common.data.report.configuration.style.BorderType;

@Schema
public class DividerComponent
extends AbstractLayoutReportComponent {
    private BorderLength length;
    private BorderType borderType;
    private Integer widthPx;
    private String color;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DIVIDER;
    }

    @Generated
    public BorderLength getLength() {
        return this.length;
    }

    @Generated
    public BorderType getBorderType() {
        return this.borderType;
    }

    @Generated
    public Integer getWidthPx() {
        return this.widthPx;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public void setLength(BorderLength length) {
        this.length = length;
    }

    @Generated
    public void setBorderType(BorderType borderType) {
        this.borderType = borderType;
    }

    @Generated
    public void setWidthPx(Integer widthPx) {
        this.widthPx = widthPx;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    @Generated
    public String toString() {
        return "DividerComponent(length=" + String.valueOf((Object)this.getLength()) + ", borderType=" + String.valueOf((Object)this.getBorderType()) + ", widthPx=" + this.getWidthPx() + ", color=" + this.getColor() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DividerComponent)) {
            return false;
        }
        DividerComponent other = (DividerComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$widthPx = this.getWidthPx();
        Integer other$widthPx = other.getWidthPx();
        if (this$widthPx == null ? other$widthPx != null : !((Object)this$widthPx).equals(other$widthPx)) {
            return false;
        }
        BorderLength this$length = this.getLength();
        BorderLength other$length = other.getLength();
        if (this$length == null ? other$length != null : !((Object)((Object)this$length)).equals((Object)other$length)) {
            return false;
        }
        BorderType this$borderType = this.getBorderType();
        BorderType other$borderType = other.getBorderType();
        if (this$borderType == null ? other$borderType != null : !((Object)((Object)this$borderType)).equals((Object)other$borderType)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        return !(this$color == null ? other$color != null : !this$color.equals(other$color));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DividerComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $widthPx = this.getWidthPx();
        result = result * 59 + ($widthPx == null ? 43 : ((Object)$widthPx).hashCode());
        BorderLength $length = this.getLength();
        result = result * 59 + ($length == null ? 43 : ((Object)((Object)$length)).hashCode());
        BorderType $borderType = this.getBorderType();
        result = result * 59 + ($borderType == null ? 43 : ((Object)((Object)$borderType)).hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        return result;
    }

    @Generated
    public DividerComponent() {
    }
}

