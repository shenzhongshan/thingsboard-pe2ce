/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.BarWithLabelsSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidthSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class ReportBarChartWithLabelsSettings
extends ReportTimeSeriesChartSettings {
    private Boolean showBarLabel;
    private Font barLabelFont;
    private String barLabelColor;
    private Boolean showBarValue;
    private Font barValueFont;
    private String barValueColor;
    private Boolean showBarBorder;
    private Float barBorderWidth;
    private Float barBorderRadius;
    private ChartFillSettings barBackgroundSettings;
    private String barUnits;
    private Integer barDecimals;

    public ReportBarChartWithLabelsSettings() {
    }

    public ReportBarChartWithLabelsSettings(ReportBarChartWithLabelsSettings input) {
        super(input);
        if (input == null) {
            input = new ReportBarChartWithLabelsSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Bar chart with labels");
        this.showBarLabel = input.getShowBarLabel() != null ? input.getShowBarLabel() : Boolean.TRUE;
        this.barLabelFont = input.getBarLabelFont() != null ? input.getBarLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.barLabelColor = input.getBarLabelColor() != null ? input.getBarLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showBarValue = input.getShowBarValue() != null ? input.getShowBarValue() : Boolean.TRUE;
        this.barValueFont = input.getBarValueFont() != null ? input.getBarValueFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.BOLD).style(FontStyle.NORMAL).build();
        this.barValueColor = input.getBarValueColor() != null ? input.getBarValueColor() : "rgba(0, 0, 0, 0.76)";
        this.showBarBorder = input.getShowBarBorder() != null ? input.getShowBarBorder() : Boolean.FALSE;
        this.barBorderWidth = Float.valueOf(input.getBarBorderWidth() != null ? input.getBarBorderWidth().floatValue() : 2.0f);
        this.barBorderRadius = Float.valueOf(input.getBarBorderRadius() != null ? input.getBarBorderRadius().floatValue() : 0.0f);
        this.barBackgroundSettings = new ChartFillSettings(input.getBarBackgroundSettings());
        this.barUnits = input.getBarUnits() != null ? input.getBarUnits() : "%";
        this.barDecimals = input.getBarDecimals() != null ? input.getBarDecimals() : 0;
        TimeSeriesChartBarWidthSettings barWidthSettings = input.getBarWidthSettings();
        if (barWidthSettings == null) {
            barWidthSettings = new TimeSeriesChartBarWidthSettings();
        }
        barWidthSettings.setBarGap(barWidthSettings.getBarGap() != null ? barWidthSettings.getBarGap() : 0.0);
        barWidthSettings.setIntervalGap(barWidthSettings.getIntervalGap() != null ? barWidthSettings.getIntervalGap() : 0.5);
        this.setBarWidthSettings(barWidthSettings);
    }

    @JsonIgnore
    public TimeSeriesChartKeySettings toTimeSeriesChartKeySettings() {
        TimeSeriesChartKeySettings keySettings = new TimeSeriesChartKeySettings();
        keySettings.setSeriesType(TimeSeriesChartSeriesType.bar);
        BarWithLabelsSeriesSettings barSettings = new BarWithLabelsSeriesSettings();
        barSettings.setShowBorder(this.getShowBarBorder());
        barSettings.setBorderWidth(this.getBarBorderWidth());
        barSettings.setBorderRadius(this.getBarBorderRadius());
        barSettings.setBackgroundSettings(this.getBarBackgroundSettings());
        barSettings.setShowLabel(this.getShowBarValue());
        barSettings.setLabelFont(this.getBarValueFont());
        barSettings.setLabelColor(this.getBarValueColor());
        barSettings.setShowSeriesLabel(this.getShowBarLabel());
        barSettings.setSeriesLabelFont(this.getBarLabelFont());
        barSettings.setSeriesLabelColor(this.getBarLabelColor());
        keySettings.setBarSettings(barSettings);
        return keySettings;
    }

    @Generated
    public Boolean getShowBarLabel() {
        return this.showBarLabel;
    }

    @Generated
    public Font getBarLabelFont() {
        return this.barLabelFont;
    }

    @Generated
    public String getBarLabelColor() {
        return this.barLabelColor;
    }

    @Generated
    public Boolean getShowBarValue() {
        return this.showBarValue;
    }

    @Generated
    public Font getBarValueFont() {
        return this.barValueFont;
    }

    @Generated
    public String getBarValueColor() {
        return this.barValueColor;
    }

    @Generated
    public Boolean getShowBarBorder() {
        return this.showBarBorder;
    }

    @Generated
    public Float getBarBorderWidth() {
        return this.barBorderWidth;
    }

    @Generated
    public Float getBarBorderRadius() {
        return this.barBorderRadius;
    }

    @Generated
    public ChartFillSettings getBarBackgroundSettings() {
        return this.barBackgroundSettings;
    }

    @Generated
    public String getBarUnits() {
        return this.barUnits;
    }

    @Generated
    public Integer getBarDecimals() {
        return this.barDecimals;
    }

    @Generated
    public void setShowBarLabel(Boolean showBarLabel) {
        this.showBarLabel = showBarLabel;
    }

    @Generated
    public void setBarLabelFont(Font barLabelFont) {
        this.barLabelFont = barLabelFont;
    }

    @Generated
    public void setBarLabelColor(String barLabelColor) {
        this.barLabelColor = barLabelColor;
    }

    @Generated
    public void setShowBarValue(Boolean showBarValue) {
        this.showBarValue = showBarValue;
    }

    @Generated
    public void setBarValueFont(Font barValueFont) {
        this.barValueFont = barValueFont;
    }

    @Generated
    public void setBarValueColor(String barValueColor) {
        this.barValueColor = barValueColor;
    }

    @Generated
    public void setShowBarBorder(Boolean showBarBorder) {
        this.showBarBorder = showBarBorder;
    }

    @Generated
    public void setBarBorderWidth(Float barBorderWidth) {
        this.barBorderWidth = barBorderWidth;
    }

    @Generated
    public void setBarBorderRadius(Float barBorderRadius) {
        this.barBorderRadius = barBorderRadius;
    }

    @Generated
    public void setBarBackgroundSettings(ChartFillSettings barBackgroundSettings) {
        this.barBackgroundSettings = barBackgroundSettings;
    }

    @Generated
    public void setBarUnits(String barUnits) {
        this.barUnits = barUnits;
    }

    @Generated
    public void setBarDecimals(Integer barDecimals) {
        this.barDecimals = barDecimals;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportBarChartWithLabelsSettings)) {
            return false;
        }
        ReportBarChartWithLabelsSettings other = (ReportBarChartWithLabelsSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showBarLabel = this.getShowBarLabel();
        Boolean other$showBarLabel = other.getShowBarLabel();
        if (this$showBarLabel == null ? other$showBarLabel != null : !((Object)this$showBarLabel).equals(other$showBarLabel)) {
            return false;
        }
        Boolean this$showBarValue = this.getShowBarValue();
        Boolean other$showBarValue = other.getShowBarValue();
        if (this$showBarValue == null ? other$showBarValue != null : !((Object)this$showBarValue).equals(other$showBarValue)) {
            return false;
        }
        Boolean this$showBarBorder = this.getShowBarBorder();
        Boolean other$showBarBorder = other.getShowBarBorder();
        if (this$showBarBorder == null ? other$showBarBorder != null : !((Object)this$showBarBorder).equals(other$showBarBorder)) {
            return false;
        }
        Float this$barBorderWidth = this.getBarBorderWidth();
        Float other$barBorderWidth = other.getBarBorderWidth();
        if (this$barBorderWidth == null ? other$barBorderWidth != null : !((Object)this$barBorderWidth).equals(other$barBorderWidth)) {
            return false;
        }
        Float this$barBorderRadius = this.getBarBorderRadius();
        Float other$barBorderRadius = other.getBarBorderRadius();
        if (this$barBorderRadius == null ? other$barBorderRadius != null : !((Object)this$barBorderRadius).equals(other$barBorderRadius)) {
            return false;
        }
        Integer this$barDecimals = this.getBarDecimals();
        Integer other$barDecimals = other.getBarDecimals();
        if (this$barDecimals == null ? other$barDecimals != null : !((Object)this$barDecimals).equals(other$barDecimals)) {
            return false;
        }
        Font this$barLabelFont = this.getBarLabelFont();
        Font other$barLabelFont = other.getBarLabelFont();
        if (this$barLabelFont == null ? other$barLabelFont != null : !((Object)this$barLabelFont).equals(other$barLabelFont)) {
            return false;
        }
        String this$barLabelColor = this.getBarLabelColor();
        String other$barLabelColor = other.getBarLabelColor();
        if (this$barLabelColor == null ? other$barLabelColor != null : !this$barLabelColor.equals(other$barLabelColor)) {
            return false;
        }
        Font this$barValueFont = this.getBarValueFont();
        Font other$barValueFont = other.getBarValueFont();
        if (this$barValueFont == null ? other$barValueFont != null : !((Object)this$barValueFont).equals(other$barValueFont)) {
            return false;
        }
        String this$barValueColor = this.getBarValueColor();
        String other$barValueColor = other.getBarValueColor();
        if (this$barValueColor == null ? other$barValueColor != null : !this$barValueColor.equals(other$barValueColor)) {
            return false;
        }
        ChartFillSettings this$barBackgroundSettings = this.getBarBackgroundSettings();
        ChartFillSettings other$barBackgroundSettings = other.getBarBackgroundSettings();
        if (this$barBackgroundSettings == null ? other$barBackgroundSettings != null : !((Object)this$barBackgroundSettings).equals(other$barBackgroundSettings)) {
            return false;
        }
        String this$barUnits = this.getBarUnits();
        String other$barUnits = other.getBarUnits();
        return !(this$barUnits == null ? other$barUnits != null : !this$barUnits.equals(other$barUnits));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportBarChartWithLabelsSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showBarLabel = this.getShowBarLabel();
        result = result * 59 + ($showBarLabel == null ? 43 : ((Object)$showBarLabel).hashCode());
        Boolean $showBarValue = this.getShowBarValue();
        result = result * 59 + ($showBarValue == null ? 43 : ((Object)$showBarValue).hashCode());
        Boolean $showBarBorder = this.getShowBarBorder();
        result = result * 59 + ($showBarBorder == null ? 43 : ((Object)$showBarBorder).hashCode());
        Float $barBorderWidth = this.getBarBorderWidth();
        result = result * 59 + ($barBorderWidth == null ? 43 : ((Object)$barBorderWidth).hashCode());
        Float $barBorderRadius = this.getBarBorderRadius();
        result = result * 59 + ($barBorderRadius == null ? 43 : ((Object)$barBorderRadius).hashCode());
        Integer $barDecimals = this.getBarDecimals();
        result = result * 59 + ($barDecimals == null ? 43 : ((Object)$barDecimals).hashCode());
        Font $barLabelFont = this.getBarLabelFont();
        result = result * 59 + ($barLabelFont == null ? 43 : ((Object)$barLabelFont).hashCode());
        String $barLabelColor = this.getBarLabelColor();
        result = result * 59 + ($barLabelColor == null ? 43 : $barLabelColor.hashCode());
        Font $barValueFont = this.getBarValueFont();
        result = result * 59 + ($barValueFont == null ? 43 : ((Object)$barValueFont).hashCode());
        String $barValueColor = this.getBarValueColor();
        result = result * 59 + ($barValueColor == null ? 43 : $barValueColor.hashCode());
        ChartFillSettings $barBackgroundSettings = this.getBarBackgroundSettings();
        result = result * 59 + ($barBackgroundSettings == null ? 43 : ((Object)$barBackgroundSettings).hashCode());
        String $barUnits = this.getBarUnits();
        result = result * 59 + ($barUnits == null ? 43 : $barUnits.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportBarChartWithLabelsSettings(showBarLabel=" + this.getShowBarLabel() + ", barLabelFont=" + String.valueOf(this.getBarLabelFont()) + ", barLabelColor=" + this.getBarLabelColor() + ", showBarValue=" + this.getShowBarValue() + ", barValueFont=" + String.valueOf(this.getBarValueFont()) + ", barValueColor=" + this.getBarValueColor() + ", showBarBorder=" + this.getShowBarBorder() + ", barBorderWidth=" + this.getBarBorderWidth() + ", barBorderRadius=" + this.getBarBorderRadius() + ", barBackgroundSettings=" + String.valueOf(this.getBarBackgroundSettings()) + ", barUnits=" + this.getBarUnits() + ", barDecimals=" + this.getBarDecimals() + ")";
    }
}

