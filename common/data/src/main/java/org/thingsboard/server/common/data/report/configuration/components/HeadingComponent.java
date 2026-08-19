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
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

@Schema
public class HeadingComponent
extends AbstractDataWithLayoutReportComponent {
    private String value;
    private Font font;
    private String color;
    private TextAlignment textAlignment;
    private VerticalAlignment verticalAlignment;
    private Integer height;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.HEADING;
    }

    @Generated
    public String getValue() {
        return this.value;
    }

    @Generated
    public Font getFont() {
        return this.font;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public TextAlignment getTextAlignment() {
        return this.textAlignment;
    }

    @Generated
    public VerticalAlignment getVerticalAlignment() {
        return this.verticalAlignment;
    }

    @Generated
    public Integer getHeight() {
        return this.height;
    }

    @Generated
    public void setValue(String value) {
        this.value = value;
    }

    @Generated
    public void setFont(Font font) {
        this.font = font;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public void setTextAlignment(TextAlignment textAlignment) {
        this.textAlignment = textAlignment;
    }

    @Generated
    public void setVerticalAlignment(VerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    @Generated
    public void setHeight(Integer height) {
        this.height = height;
    }

    @Override
    @Generated
    public String toString() {
        return "HeadingComponent(value=" + this.getValue() + ", font=" + String.valueOf(this.getFont()) + ", color=" + this.getColor() + ", textAlignment=" + String.valueOf((Object)this.getTextAlignment()) + ", verticalAlignment=" + String.valueOf((Object)this.getVerticalAlignment()) + ", height=" + this.getHeight() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HeadingComponent)) {
            return false;
        }
        HeadingComponent other = (HeadingComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$height = this.getHeight();
        Integer other$height = other.getHeight();
        if (this$height == null ? other$height != null : !((Object)this$height).equals(other$height)) {
            return false;
        }
        String this$value = this.getValue();
        String other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) {
            return false;
        }
        Font this$font = this.getFont();
        Font other$font = other.getFont();
        if (this$font == null ? other$font != null : !((Object)this$font).equals(other$font)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        TextAlignment this$textAlignment = this.getTextAlignment();
        TextAlignment other$textAlignment = other.getTextAlignment();
        if (this$textAlignment == null ? other$textAlignment != null : !((Object)((Object)this$textAlignment)).equals((Object)other$textAlignment)) {
            return false;
        }
        VerticalAlignment this$verticalAlignment = this.getVerticalAlignment();
        VerticalAlignment other$verticalAlignment = other.getVerticalAlignment();
        return !(this$verticalAlignment == null ? other$verticalAlignment != null : !((Object)((Object)this$verticalAlignment)).equals((Object)other$verticalAlignment));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HeadingComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $height = this.getHeight();
        result = result * 59 + ($height == null ? 43 : ((Object)$height).hashCode());
        String $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : $value.hashCode());
        Font $font = this.getFont();
        result = result * 59 + ($font == null ? 43 : ((Object)$font).hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        TextAlignment $textAlignment = this.getTextAlignment();
        result = result * 59 + ($textAlignment == null ? 43 : ((Object)((Object)$textAlignment)).hashCode());
        VerticalAlignment $verticalAlignment = this.getVerticalAlignment();
        result = result * 59 + ($verticalAlignment == null ? 43 : ((Object)((Object)$verticalAlignment)).hashCode());
        return result;
    }

    @Generated
    public HeadingComponent() {
    }
}

