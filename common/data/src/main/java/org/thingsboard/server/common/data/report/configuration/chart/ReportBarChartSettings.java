/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class ReportBarChartSettings
extends ReportLatestChartSettings {
    private Double axisMin;
    private Double axisMax;
    private Font axisTickLabelFont;
    private String axisTickLabelColor;
    private BarSeriesSettings barSettings;

    public ReportBarChartSettings() {
    }

    public ReportBarChartSettings(ReportBarChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportBarChartSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Bars");
        this.axisMin = input.getAxisMin();
        this.axisMax = input.getAxisMax();
        this.axisTickLabelFont = input.getAxisTickLabelFont() != null ? input.getAxisTickLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.axisTickLabelColor = input.getAxisTickLabelColor() != null ? input.getAxisTickLabelColor() : "rgba(0, 0, 0, 0.54)";
        BarSeriesSettings inputBarSettings = input.getBarSettings();
        this.barSettings = new BarSeriesSettings(inputBarSettings);
        this.barSettings.setBarWidth(inputBarSettings != null && inputBarSettings.getBarWidth() != null ? inputBarSettings.getBarWidth() : 80.0);
        this.barSettings.setShowLabel(inputBarSettings != null && inputBarSettings.getShowLabel() != null ? inputBarSettings.getShowLabel() : true);
    }

    @Generated
    public Double getAxisMin() {
        return this.axisMin;
    }

    @Generated
    public Double getAxisMax() {
        return this.axisMax;
    }

    @Generated
    public Font getAxisTickLabelFont() {
        return this.axisTickLabelFont;
    }

    @Generated
    public String getAxisTickLabelColor() {
        return this.axisTickLabelColor;
    }

    @Generated
    public BarSeriesSettings getBarSettings() {
        return this.barSettings;
    }

    @Generated
    public void setAxisMin(Double axisMin) {
        this.axisMin = axisMin;
    }

    @Generated
    public void setAxisMax(Double axisMax) {
        this.axisMax = axisMax;
    }

    @Generated
    public void setAxisTickLabelFont(Font axisTickLabelFont) {
        this.axisTickLabelFont = axisTickLabelFont;
    }

    @Generated
    public void setAxisTickLabelColor(String axisTickLabelColor) {
        this.axisTickLabelColor = axisTickLabelColor;
    }

    @Generated
    public void setBarSettings(BarSeriesSettings barSettings) {
        this.barSettings = barSettings;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportBarChartSettings)) {
            return false;
        }
        ReportBarChartSettings other = (ReportBarChartSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$axisMin = this.getAxisMin();
        Double other$axisMin = other.getAxisMin();
        if (this$axisMin == null ? other$axisMin != null : !((Object)this$axisMin).equals(other$axisMin)) {
            return false;
        }
        Double this$axisMax = this.getAxisMax();
        Double other$axisMax = other.getAxisMax();
        if (this$axisMax == null ? other$axisMax != null : !((Object)this$axisMax).equals(other$axisMax)) {
            return false;
        }
        Font this$axisTickLabelFont = this.getAxisTickLabelFont();
        Font other$axisTickLabelFont = other.getAxisTickLabelFont();
        if (this$axisTickLabelFont == null ? other$axisTickLabelFont != null : !((Object)this$axisTickLabelFont).equals(other$axisTickLabelFont)) {
            return false;
        }
        String this$axisTickLabelColor = this.getAxisTickLabelColor();
        String other$axisTickLabelColor = other.getAxisTickLabelColor();
        if (this$axisTickLabelColor == null ? other$axisTickLabelColor != null : !this$axisTickLabelColor.equals(other$axisTickLabelColor)) {
            return false;
        }
        BarSeriesSettings this$barSettings = this.getBarSettings();
        BarSeriesSettings other$barSettings = other.getBarSettings();
        return !(this$barSettings == null ? other$barSettings != null : !((Object)this$barSettings).equals(other$barSettings));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportBarChartSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $axisMin = this.getAxisMin();
        result = result * 59 + ($axisMin == null ? 43 : ((Object)$axisMin).hashCode());
        Double $axisMax = this.getAxisMax();
        result = result * 59 + ($axisMax == null ? 43 : ((Object)$axisMax).hashCode());
        Font $axisTickLabelFont = this.getAxisTickLabelFont();
        result = result * 59 + ($axisTickLabelFont == null ? 43 : ((Object)$axisTickLabelFont).hashCode());
        String $axisTickLabelColor = this.getAxisTickLabelColor();
        result = result * 59 + ($axisTickLabelColor == null ? 43 : $axisTickLabelColor.hashCode());
        BarSeriesSettings $barSettings = this.getBarSettings();
        result = result * 59 + ($barSettings == null ? 43 : ((Object)$barSettings).hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportBarChartSettings(axisMin=" + this.getAxisMin() + ", axisMax=" + this.getAxisMax() + ", axisTickLabelFont=" + String.valueOf(this.getAxisTickLabelFont()) + ", axisTickLabelColor=" + this.getAxisTickLabelColor() + ", barSettings=" + String.valueOf(this.getBarSettings()) + ")";
    }
}

