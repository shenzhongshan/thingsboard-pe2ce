/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class BarSeriesSettings {
    private Boolean showBorder;
    private Float borderWidth;
    private Float borderRadius;
    private Double barWidth;
    private Boolean showLabel;
    private ChartLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Boolean enableLabelBackground;
    private String labelBackground;
    private ChartFillSettings backgroundSettings;

    public BarSeriesSettings() {
    }

    public BarSeriesSettings(BarSeriesSettings input) {
        if (input == null) {
            input = new BarSeriesSettings();
        }
        this.showBorder = input.getShowBorder() != null ? input.getShowBorder() : Boolean.FALSE;
        this.borderWidth = Float.valueOf(input.getBorderWidth() != null ? input.getBorderWidth().floatValue() : 2.0f);
        this.borderRadius = Float.valueOf(input.getBorderRadius() != null ? input.getBorderRadius().floatValue() : 0.0f);
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.FALSE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : ChartLabelPosition.top;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(11.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enableLabelBackground = input.getEnableLabelBackground() != null ? input.getEnableLabelBackground() : Boolean.FALSE;
        this.labelBackground = input.getLabelBackground() != null ? input.getLabelBackground() : "rgba(255,255,255,0.56)";
        this.backgroundSettings = new ChartFillSettings(input.getBackgroundSettings());
    }

    @Generated
    public Boolean getShowBorder() {
        return this.showBorder;
    }

    @Generated
    public Float getBorderWidth() {
        return this.borderWidth;
    }

    @Generated
    public Float getBorderRadius() {
        return this.borderRadius;
    }

    @Generated
    public Double getBarWidth() {
        return this.barWidth;
    }

    @Generated
    public Boolean getShowLabel() {
        return this.showLabel;
    }

    @Generated
    public ChartLabelPosition getLabelPosition() {
        return this.labelPosition;
    }

    @Generated
    public Font getLabelFont() {
        return this.labelFont;
    }

    @Generated
    public String getLabelColor() {
        return this.labelColor;
    }

    @Generated
    public Boolean getEnableLabelBackground() {
        return this.enableLabelBackground;
    }

    @Generated
    public String getLabelBackground() {
        return this.labelBackground;
    }

    @Generated
    public ChartFillSettings getBackgroundSettings() {
        return this.backgroundSettings;
    }

    @Generated
    public void setShowBorder(Boolean showBorder) {
        this.showBorder = showBorder;
    }

    @Generated
    public void setBorderWidth(Float borderWidth) {
        this.borderWidth = borderWidth;
    }

    @Generated
    public void setBorderRadius(Float borderRadius) {
        this.borderRadius = borderRadius;
    }

    @Generated
    public void setBarWidth(Double barWidth) {
        this.barWidth = barWidth;
    }

    @Generated
    public void setShowLabel(Boolean showLabel) {
        this.showLabel = showLabel;
    }

    @Generated
    public void setLabelPosition(ChartLabelPosition labelPosition) {
        this.labelPosition = labelPosition;
    }

    @Generated
    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
    }

    @Generated
    public void setLabelColor(String labelColor) {
        this.labelColor = labelColor;
    }

    @Generated
    public void setEnableLabelBackground(Boolean enableLabelBackground) {
        this.enableLabelBackground = enableLabelBackground;
    }

    @Generated
    public void setLabelBackground(String labelBackground) {
        this.labelBackground = labelBackground;
    }

    @Generated
    public void setBackgroundSettings(ChartFillSettings backgroundSettings) {
        this.backgroundSettings = backgroundSettings;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BarSeriesSettings)) {
            return false;
        }
        BarSeriesSettings other = (BarSeriesSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showBorder = this.getShowBorder();
        Boolean other$showBorder = other.getShowBorder();
        if (this$showBorder == null ? other$showBorder != null : !((Object)this$showBorder).equals(other$showBorder)) {
            return false;
        }
        Float this$borderWidth = this.getBorderWidth();
        Float other$borderWidth = other.getBorderWidth();
        if (this$borderWidth == null ? other$borderWidth != null : !((Object)this$borderWidth).equals(other$borderWidth)) {
            return false;
        }
        Float this$borderRadius = this.getBorderRadius();
        Float other$borderRadius = other.getBorderRadius();
        if (this$borderRadius == null ? other$borderRadius != null : !((Object)this$borderRadius).equals(other$borderRadius)) {
            return false;
        }
        Double this$barWidth = this.getBarWidth();
        Double other$barWidth = other.getBarWidth();
        if (this$barWidth == null ? other$barWidth != null : !((Object)this$barWidth).equals(other$barWidth)) {
            return false;
        }
        Boolean this$showLabel = this.getShowLabel();
        Boolean other$showLabel = other.getShowLabel();
        if (this$showLabel == null ? other$showLabel != null : !((Object)this$showLabel).equals(other$showLabel)) {
            return false;
        }
        Boolean this$enableLabelBackground = this.getEnableLabelBackground();
        Boolean other$enableLabelBackground = other.getEnableLabelBackground();
        if (this$enableLabelBackground == null ? other$enableLabelBackground != null : !((Object)this$enableLabelBackground).equals(other$enableLabelBackground)) {
            return false;
        }
        ChartLabelPosition this$labelPosition = this.getLabelPosition();
        ChartLabelPosition other$labelPosition = other.getLabelPosition();
        if (this$labelPosition == null ? other$labelPosition != null : !((Object)((Object)this$labelPosition)).equals((Object)other$labelPosition)) {
            return false;
        }
        Font this$labelFont = this.getLabelFont();
        Font other$labelFont = other.getLabelFont();
        if (this$labelFont == null ? other$labelFont != null : !((Object)this$labelFont).equals(other$labelFont)) {
            return false;
        }
        String this$labelColor = this.getLabelColor();
        String other$labelColor = other.getLabelColor();
        if (this$labelColor == null ? other$labelColor != null : !this$labelColor.equals(other$labelColor)) {
            return false;
        }
        String this$labelBackground = this.getLabelBackground();
        String other$labelBackground = other.getLabelBackground();
        if (this$labelBackground == null ? other$labelBackground != null : !this$labelBackground.equals(other$labelBackground)) {
            return false;
        }
        ChartFillSettings this$backgroundSettings = this.getBackgroundSettings();
        ChartFillSettings other$backgroundSettings = other.getBackgroundSettings();
        return !(this$backgroundSettings == null ? other$backgroundSettings != null : !((Object)this$backgroundSettings).equals(other$backgroundSettings));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof BarSeriesSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showBorder = this.getShowBorder();
        result = result * 59 + ($showBorder == null ? 43 : ((Object)$showBorder).hashCode());
        Float $borderWidth = this.getBorderWidth();
        result = result * 59 + ($borderWidth == null ? 43 : ((Object)$borderWidth).hashCode());
        Float $borderRadius = this.getBorderRadius();
        result = result * 59 + ($borderRadius == null ? 43 : ((Object)$borderRadius).hashCode());
        Double $barWidth = this.getBarWidth();
        result = result * 59 + ($barWidth == null ? 43 : ((Object)$barWidth).hashCode());
        Boolean $showLabel = this.getShowLabel();
        result = result * 59 + ($showLabel == null ? 43 : ((Object)$showLabel).hashCode());
        Boolean $enableLabelBackground = this.getEnableLabelBackground();
        result = result * 59 + ($enableLabelBackground == null ? 43 : ((Object)$enableLabelBackground).hashCode());
        ChartLabelPosition $labelPosition = this.getLabelPosition();
        result = result * 59 + ($labelPosition == null ? 43 : ((Object)((Object)$labelPosition)).hashCode());
        Font $labelFont = this.getLabelFont();
        result = result * 59 + ($labelFont == null ? 43 : ((Object)$labelFont).hashCode());
        String $labelColor = this.getLabelColor();
        result = result * 59 + ($labelColor == null ? 43 : $labelColor.hashCode());
        String $labelBackground = this.getLabelBackground();
        result = result * 59 + ($labelBackground == null ? 43 : $labelBackground.hashCode());
        ChartFillSettings $backgroundSettings = this.getBackgroundSettings();
        result = result * 59 + ($backgroundSettings == null ? 43 : ((Object)$backgroundSettings).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "BarSeriesSettings(showBorder=" + this.getShowBorder() + ", borderWidth=" + this.getBorderWidth() + ", borderRadius=" + this.getBorderRadius() + ", barWidth=" + this.getBarWidth() + ", showLabel=" + this.getShowLabel() + ", labelPosition=" + String.valueOf((Object)this.getLabelPosition()) + ", labelFont=" + String.valueOf(this.getLabelFont()) + ", labelColor=" + this.getLabelColor() + ", enableLabelBackground=" + this.getEnableLabelBackground() + ", labelBackground=" + this.getLabelBackground() + ", backgroundSettings=" + String.valueOf(this.getBackgroundSettings()) + ")";
    }
}

