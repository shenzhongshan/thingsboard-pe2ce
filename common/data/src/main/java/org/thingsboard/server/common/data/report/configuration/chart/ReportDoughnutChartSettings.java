/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.DoughnutLayout;
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class ReportDoughnutChartSettings
extends ReportLatestChartSettings {
    private DoughnutLayout layout;
    private Boolean clockwise;
    private Font totalValueFont;
    private String totalValueColor;

    public ReportDoughnutChartSettings() {
    }

    public ReportDoughnutChartSettings(ReportDoughnutChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportDoughnutChartSettings();
        }
        this.layout = input.getLayout() != null ? input.getLayout() : DoughnutLayout.DEFAULT;
        this.setShowTotal(this.layout == DoughnutLayout.WITH_TOTAL);
        this.clockwise = input.getClockwise() != null ? input.getClockwise() : Boolean.FALSE;
        this.totalValueFont = input.getTotalValueFont() != null ? input.getTotalValueFont() : Font.builder().family("Roboto").size(Float.valueOf(24.0f)).weight(FontWeight.WEIGHT_500).style(FontStyle.NORMAL).build();
        this.totalValueColor = input.getTotalValueColor() != null ? input.getTotalValueColor() : "rgba(0, 0, 0, 0.87)";
    }

    @Generated
    public DoughnutLayout getLayout() {
        return this.layout;
    }

    @Generated
    public Boolean getClockwise() {
        return this.clockwise;
    }

    @Generated
    public Font getTotalValueFont() {
        return this.totalValueFont;
    }

    @Generated
    public String getTotalValueColor() {
        return this.totalValueColor;
    }

    @Generated
    public void setLayout(DoughnutLayout layout) {
        this.layout = layout;
    }

    @Generated
    public void setClockwise(Boolean clockwise) {
        this.clockwise = clockwise;
    }

    @Generated
    public void setTotalValueFont(Font totalValueFont) {
        this.totalValueFont = totalValueFont;
    }

    @Generated
    public void setTotalValueColor(String totalValueColor) {
        this.totalValueColor = totalValueColor;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportDoughnutChartSettings)) {
            return false;
        }
        ReportDoughnutChartSettings other = (ReportDoughnutChartSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$clockwise = this.getClockwise();
        Boolean other$clockwise = other.getClockwise();
        if (this$clockwise == null ? other$clockwise != null : !((Object)this$clockwise).equals(other$clockwise)) {
            return false;
        }
        DoughnutLayout this$layout = this.getLayout();
        DoughnutLayout other$layout = other.getLayout();
        if (this$layout == null ? other$layout != null : !((Object)((Object)this$layout)).equals((Object)other$layout)) {
            return false;
        }
        Font this$totalValueFont = this.getTotalValueFont();
        Font other$totalValueFont = other.getTotalValueFont();
        if (this$totalValueFont == null ? other$totalValueFont != null : !((Object)this$totalValueFont).equals(other$totalValueFont)) {
            return false;
        }
        String this$totalValueColor = this.getTotalValueColor();
        String other$totalValueColor = other.getTotalValueColor();
        return !(this$totalValueColor == null ? other$totalValueColor != null : !this$totalValueColor.equals(other$totalValueColor));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportDoughnutChartSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $clockwise = this.getClockwise();
        result = result * 59 + ($clockwise == null ? 43 : ((Object)$clockwise).hashCode());
        DoughnutLayout $layout = this.getLayout();
        result = result * 59 + ($layout == null ? 43 : ((Object)((Object)$layout)).hashCode());
        Font $totalValueFont = this.getTotalValueFont();
        result = result * 59 + ($totalValueFont == null ? 43 : ((Object)$totalValueFont).hashCode());
        String $totalValueColor = this.getTotalValueColor();
        result = result * 59 + ($totalValueColor == null ? 43 : $totalValueColor.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportDoughnutChartSettings(layout=" + String.valueOf((Object)this.getLayout()) + ", clockwise=" + this.getClockwise() + ", totalValueFont=" + String.valueOf(this.getTotalValueFont()) + ", totalValueColor=" + this.getTotalValueColor() + ")";
    }
}

