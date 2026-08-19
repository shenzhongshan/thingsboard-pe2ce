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
import org.thingsboard.server.common.data.report.configuration.components.AbstractDataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.LayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Insets;

@Schema
public abstract class AbstractDataWithLayoutReportComponent
extends AbstractDataReportComponent
implements LayoutReportComponent {
    private Insets margins;
    private Insets paddings;
    private String background;
    private Integer borderWidth;
    private Integer borderRadius;
    private String borderColor;

    @Override
    @Generated
    public Insets getMargins() {
        return this.margins;
    }

    @Override
    @Generated
    public Insets getPaddings() {
        return this.paddings;
    }

    @Override
    @Generated
    public String getBackground() {
        return this.background;
    }

    @Override
    @Generated
    public Integer getBorderWidth() {
        return this.borderWidth;
    }

    @Override
    @Generated
    public Integer getBorderRadius() {
        return this.borderRadius;
    }

    @Override
    @Generated
    public String getBorderColor() {
        return this.borderColor;
    }

    @Generated
    public void setMargins(Insets margins) {
        this.margins = margins;
    }

    @Generated
    public void setPaddings(Insets paddings) {
        this.paddings = paddings;
    }

    @Generated
    public void setBackground(String background) {
        this.background = background;
    }

    @Generated
    public void setBorderWidth(Integer borderWidth) {
        this.borderWidth = borderWidth;
    }

    @Generated
    public void setBorderRadius(Integer borderRadius) {
        this.borderRadius = borderRadius;
    }

    @Generated
    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    @Override
    @Generated
    public String toString() {
        return "AbstractDataWithLayoutReportComponent(margins=" + String.valueOf(this.getMargins()) + ", paddings=" + String.valueOf(this.getPaddings()) + ", background=" + this.getBackground() + ", borderWidth=" + this.getBorderWidth() + ", borderRadius=" + this.getBorderRadius() + ", borderColor=" + this.getBorderColor() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractDataWithLayoutReportComponent)) {
            return false;
        }
        AbstractDataWithLayoutReportComponent other = (AbstractDataWithLayoutReportComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$borderWidth = this.getBorderWidth();
        Integer other$borderWidth = other.getBorderWidth();
        if (this$borderWidth == null ? other$borderWidth != null : !((Object)this$borderWidth).equals(other$borderWidth)) {
            return false;
        }
        Integer this$borderRadius = this.getBorderRadius();
        Integer other$borderRadius = other.getBorderRadius();
        if (this$borderRadius == null ? other$borderRadius != null : !((Object)this$borderRadius).equals(other$borderRadius)) {
            return false;
        }
        Insets this$margins = this.getMargins();
        Insets other$margins = other.getMargins();
        if (this$margins == null ? other$margins != null : !((Object)this$margins).equals(other$margins)) {
            return false;
        }
        Insets this$paddings = this.getPaddings();
        Insets other$paddings = other.getPaddings();
        if (this$paddings == null ? other$paddings != null : !((Object)this$paddings).equals(other$paddings)) {
            return false;
        }
        String this$background = this.getBackground();
        String other$background = other.getBackground();
        if (this$background == null ? other$background != null : !this$background.equals(other$background)) {
            return false;
        }
        String this$borderColor = this.getBorderColor();
        String other$borderColor = other.getBorderColor();
        return !(this$borderColor == null ? other$borderColor != null : !this$borderColor.equals(other$borderColor));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractDataWithLayoutReportComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $borderWidth = this.getBorderWidth();
        result = result * 59 + ($borderWidth == null ? 43 : ((Object)$borderWidth).hashCode());
        Integer $borderRadius = this.getBorderRadius();
        result = result * 59 + ($borderRadius == null ? 43 : ((Object)$borderRadius).hashCode());
        Insets $margins = this.getMargins();
        result = result * 59 + ($margins == null ? 43 : ((Object)$margins).hashCode());
        Insets $paddings = this.getPaddings();
        result = result * 59 + ($paddings == null ? 43 : ((Object)$paddings).hashCode());
        String $background = this.getBackground();
        result = result * 59 + ($background == null ? 43 : $background.hashCode());
        String $borderColor = this.getBorderColor();
        result = result * 59 + ($borderColor == null ? 43 : $borderColor.hashCode());
        return result;
    }

    @Generated
    public AbstractDataWithLayoutReportComponent() {
    }

    @ConstructorProperties(value={"margins", "paddings", "background", "borderWidth", "borderRadius", "borderColor"})
    @Generated
    public AbstractDataWithLayoutReportComponent(Insets margins, Insets paddings, String background, Integer borderWidth, Integer borderRadius, String borderColor) {
        this.margins = margins;
        this.paddings = paddings;
        this.background = background;
        this.borderWidth = borderWidth;
        this.borderRadius = borderRadius;
        this.borderColor = borderColor;
    }
}

