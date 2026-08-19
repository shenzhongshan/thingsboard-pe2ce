/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Heading {
    private String text;
    private Font font;
    private String color;
    private TextAlignment textAlignment;
    private VerticalAlignment verticalAlignment;
    private Integer height;

    @Generated
    public Heading() {
    }

    @Generated
    public String getText() {
        return this.text;
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
    public void setText(String text) {
        this.text = text;
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

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Heading)) {
            return false;
        }
        Heading other = (Heading)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$height = this.getHeight();
        Integer other$height = other.getHeight();
        if (this$height == null ? other$height != null : !((Object)this$height).equals(other$height)) {
            return false;
        }
        String this$text = this.getText();
        String other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) {
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

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Heading;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $height = this.getHeight();
        result = result * 59 + ($height == null ? 43 : ((Object)$height).hashCode());
        String $text = this.getText();
        result = result * 59 + ($text == null ? 43 : $text.hashCode());
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
    public String toString() {
        return "Heading(text=" + this.getText() + ", font=" + String.valueOf(this.getFont()) + ", color=" + this.getColor() + ", textAlignment=" + String.valueOf((Object)this.getTextAlignment()) + ", verticalAlignment=" + String.valueOf((Object)this.getVerticalAlignment()) + ", height=" + this.getHeight() + ")";
    }
}

