/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

public class CellSettings {
    private Font font;
    private String color;
    private String backgroundColor;
    private TextAlignment textAlignment;
    private VerticalAlignment verticalAlignment;

    @Generated
    public CellSettings() {
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
    public String getBackgroundColor() {
        return this.backgroundColor;
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
    public void setFont(Font font) {
        this.font = font;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CellSettings)) {
            return false;
        }
        CellSettings other = (CellSettings)o;
        if (!other.canEqual(this)) {
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
        String this$backgroundColor = this.getBackgroundColor();
        String other$backgroundColor = other.getBackgroundColor();
        if (this$backgroundColor == null ? other$backgroundColor != null : !this$backgroundColor.equals(other$backgroundColor)) {
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

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof CellSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Font $font = this.getFont();
        result = result * 59 + ($font == null ? 43 : ((Object)$font).hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        String $backgroundColor = this.getBackgroundColor();
        result = result * 59 + ($backgroundColor == null ? 43 : $backgroundColor.hashCode());
        TextAlignment $textAlignment = this.getTextAlignment();
        result = result * 59 + ($textAlignment == null ? 43 : ((Object)((Object)$textAlignment)).hashCode());
        VerticalAlignment $verticalAlignment = this.getVerticalAlignment();
        result = result * 59 + ($verticalAlignment == null ? 43 : ((Object)((Object)$verticalAlignment)).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "CellSettings(font=" + String.valueOf(this.getFont()) + ", color=" + this.getColor() + ", backgroundColor=" + this.getBackgroundColor() + ", textAlignment=" + String.valueOf((Object)this.getTextAlignment()) + ", verticalAlignment=" + String.valueOf((Object)this.getVerticalAlignment()) + ")";
    }
}

