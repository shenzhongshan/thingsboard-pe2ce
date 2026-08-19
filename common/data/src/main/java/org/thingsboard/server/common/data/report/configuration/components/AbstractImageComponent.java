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
import org.thingsboard.server.common.data.report.configuration.components.AbstractDataWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageAlignment;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;

@Schema
public abstract class AbstractImageComponent
extends AbstractDataWithLayoutReportComponent {
    private ImageWidthType widthType;
    private int customWidth;
    private ImageAlignment alignment;

    @Generated
    public ImageWidthType getWidthType() {
        return this.widthType;
    }

    @Generated
    public int getCustomWidth() {
        return this.customWidth;
    }

    @Generated
    public ImageAlignment getAlignment() {
        return this.alignment;
    }

    @Generated
    public void setWidthType(ImageWidthType widthType) {
        this.widthType = widthType;
    }

    @Generated
    public void setCustomWidth(int customWidth) {
        this.customWidth = customWidth;
    }

    @Generated
    public void setAlignment(ImageAlignment alignment) {
        this.alignment = alignment;
    }

    @Override
    @Generated
    public String toString() {
        return "AbstractImageComponent(widthType=" + String.valueOf((Object)this.getWidthType()) + ", customWidth=" + this.getCustomWidth() + ", alignment=" + String.valueOf((Object)this.getAlignment()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractImageComponent)) {
            return false;
        }
        AbstractImageComponent other = (AbstractImageComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getCustomWidth() != other.getCustomWidth()) {
            return false;
        }
        ImageWidthType this$widthType = this.getWidthType();
        ImageWidthType other$widthType = other.getWidthType();
        if (this$widthType == null ? other$widthType != null : !((Object)((Object)this$widthType)).equals((Object)other$widthType)) {
            return false;
        }
        ImageAlignment this$alignment = this.getAlignment();
        ImageAlignment other$alignment = other.getAlignment();
        return !(this$alignment == null ? other$alignment != null : !((Object)((Object)this$alignment)).equals((Object)other$alignment));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractImageComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getCustomWidth();
        ImageWidthType $widthType = this.getWidthType();
        result = result * 59 + ($widthType == null ? 43 : ((Object)((Object)$widthType)).hashCode());
        ImageAlignment $alignment = this.getAlignment();
        result = result * 59 + ($alignment == null ? 43 : ((Object)((Object)$alignment)).hashCode());
        return result;
    }

    @Generated
    public AbstractImageComponent() {
    }

    @ConstructorProperties(value={"widthType", "customWidth", "alignment"})
    @Generated
    public AbstractImageComponent(ImageWidthType widthType, int customWidth, ImageAlignment alignment) {
        this.widthType = widthType;
        this.customWidth = customWidth;
        this.alignment = alignment;
    }
}

