/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Font {
    private Float size;
    private FontWeight weight;
    private FontStyle style;
    private String family;

    @Generated
    public static FontBuilder builder() {
        return new FontBuilder();
    }

    @Generated
    public Float getSize() {
        return this.size;
    }

    @Generated
    public FontWeight getWeight() {
        return this.weight;
    }

    @Generated
    public FontStyle getStyle() {
        return this.style;
    }

    @Generated
    public String getFamily() {
        return this.family;
    }

    @Generated
    public void setSize(Float size) {
        this.size = size;
    }

    @Generated
    public void setWeight(FontWeight weight) {
        this.weight = weight;
    }

    @Generated
    public void setStyle(FontStyle style) {
        this.style = style;
    }

    @Generated
    public void setFamily(String family) {
        this.family = family;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Font)) {
            return false;
        }
        Font other = (Font)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Float this$size = this.getSize();
        Float other$size = other.getSize();
        if (this$size == null ? other$size != null : !((Object)this$size).equals(other$size)) {
            return false;
        }
        FontWeight this$weight = this.getWeight();
        FontWeight other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !((Object)((Object)this$weight)).equals((Object)other$weight)) {
            return false;
        }
        FontStyle this$style = this.getStyle();
        FontStyle other$style = other.getStyle();
        if (this$style == null ? other$style != null : !((Object)((Object)this$style)).equals((Object)other$style)) {
            return false;
        }
        String this$family = this.getFamily();
        String other$family = other.getFamily();
        return !(this$family == null ? other$family != null : !this$family.equals(other$family));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Font;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Float $size = this.getSize();
        result = result * 59 + ($size == null ? 43 : ((Object)$size).hashCode());
        FontWeight $weight = this.getWeight();
        result = result * 59 + ($weight == null ? 43 : ((Object)((Object)$weight)).hashCode());
        FontStyle $style = this.getStyle();
        result = result * 59 + ($style == null ? 43 : ((Object)((Object)$style)).hashCode());
        String $family = this.getFamily();
        result = result * 59 + ($family == null ? 43 : $family.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "Font(size=" + this.getSize() + ", weight=" + String.valueOf((Object)this.getWeight()) + ", style=" + String.valueOf((Object)this.getStyle()) + ", family=" + this.getFamily() + ")";
    }

    @Generated
    public Font() {
    }

    @ConstructorProperties(value={"size", "weight", "style", "family"})
    @Generated
    public Font(Float size, FontWeight weight, FontStyle style, String family) {
        this.size = size;
        this.weight = weight;
        this.style = style;
        this.family = family;
    }

    @Generated
    public static class FontBuilder {
        @Generated
        private Float size;
        @Generated
        private FontWeight weight;
        @Generated
        private FontStyle style;
        @Generated
        private String family;

        @Generated
        FontBuilder() {
        }

        @Generated
        public FontBuilder size(Float size) {
            this.size = size;
            return this;
        }

        @Generated
        public FontBuilder weight(FontWeight weight) {
            this.weight = weight;
            return this;
        }

        @Generated
        public FontBuilder style(FontStyle style) {
            this.style = style;
            return this;
        }

        @Generated
        public FontBuilder family(String family) {
            this.family = family;
            return this;
        }

        @Generated
        public Font build() {
            return new Font(this.size, this.weight, this.style, this.family);
        }

        @Generated
        public String toString() {
            return "Font.FontBuilder(size=" + this.size + ", weight=" + String.valueOf((Object)this.weight) + ", style=" + String.valueOf((Object)this.style) + ", family=" + this.family + ")";
        }
    }
}

