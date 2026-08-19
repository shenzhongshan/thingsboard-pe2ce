/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class BarWithLabelsSeriesSettings
extends BarSeriesSettings {
    private Boolean showSeriesLabel;
    private Font seriesLabelFont;
    private String seriesLabelColor;

    public BarWithLabelsSeriesSettings() {
    }

    public BarWithLabelsSeriesSettings(BarWithLabelsSeriesSettings input) {
        super(input);
        if (input == null) {
            input = new BarWithLabelsSeriesSettings();
        }
        this.showSeriesLabel = input.getShowSeriesLabel() != null ? input.getShowSeriesLabel() : Boolean.TRUE;
        this.seriesLabelFont = input.getSeriesLabelFont() != null ? input.getSeriesLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.seriesLabelColor = input.getSeriesLabelColor() != null ? input.getSeriesLabelColor() : "rgba(0, 0, 0, 0.54)";
    }

    @Generated
    public Boolean getShowSeriesLabel() {
        return this.showSeriesLabel;
    }

    @Generated
    public Font getSeriesLabelFont() {
        return this.seriesLabelFont;
    }

    @Generated
    public String getSeriesLabelColor() {
        return this.seriesLabelColor;
    }

    @Generated
    public void setShowSeriesLabel(Boolean showSeriesLabel) {
        this.showSeriesLabel = showSeriesLabel;
    }

    @Generated
    public void setSeriesLabelFont(Font seriesLabelFont) {
        this.seriesLabelFont = seriesLabelFont;
    }

    @Generated
    public void setSeriesLabelColor(String seriesLabelColor) {
        this.seriesLabelColor = seriesLabelColor;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BarWithLabelsSeriesSettings)) {
            return false;
        }
        BarWithLabelsSeriesSettings other = (BarWithLabelsSeriesSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showSeriesLabel = this.getShowSeriesLabel();
        Boolean other$showSeriesLabel = other.getShowSeriesLabel();
        if (this$showSeriesLabel == null ? other$showSeriesLabel != null : !((Object)this$showSeriesLabel).equals(other$showSeriesLabel)) {
            return false;
        }
        Font this$seriesLabelFont = this.getSeriesLabelFont();
        Font other$seriesLabelFont = other.getSeriesLabelFont();
        if (this$seriesLabelFont == null ? other$seriesLabelFont != null : !((Object)this$seriesLabelFont).equals(other$seriesLabelFont)) {
            return false;
        }
        String this$seriesLabelColor = this.getSeriesLabelColor();
        String other$seriesLabelColor = other.getSeriesLabelColor();
        return !(this$seriesLabelColor == null ? other$seriesLabelColor != null : !this$seriesLabelColor.equals(other$seriesLabelColor));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof BarWithLabelsSeriesSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showSeriesLabel = this.getShowSeriesLabel();
        result = result * 59 + ($showSeriesLabel == null ? 43 : ((Object)$showSeriesLabel).hashCode());
        Font $seriesLabelFont = this.getSeriesLabelFont();
        result = result * 59 + ($seriesLabelFont == null ? 43 : ((Object)$seriesLabelFont).hashCode());
        String $seriesLabelColor = this.getSeriesLabelColor();
        result = result * 59 + ($seriesLabelColor == null ? 43 : $seriesLabelColor.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "BarWithLabelsSeriesSettings(showSeriesLabel=" + this.getShowSeriesLabel() + ", seriesLabelFont=" + String.valueOf(this.getSeriesLabelFont()) + ", seriesLabelColor=" + this.getSeriesLabelColor() + ")";
    }
}

