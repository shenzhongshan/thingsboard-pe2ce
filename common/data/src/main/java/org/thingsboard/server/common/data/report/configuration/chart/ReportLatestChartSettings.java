/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonSubTypes
 *  com.fasterxml.jackson.annotation.JsonSubTypes$Type
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;

@JsonSubTypes(value={@JsonSubTypes.Type(value=ReportBarChartSettings.class, name="latestBarChart"), @JsonSubTypes.Type(value=ReportPieChartSettings.class, name="pieChart"), @JsonSubTypes.Type(value=ReportDoughnutChartSettings.class, name="doughnutChart"), @JsonSubTypes.Type(value=ReportDoughnutChartSettings.class, name="horizontalDoughnutChart")})
public class ReportLatestChartSettings {
    private Boolean showTitle;
    private String title;
    private Font titleFont;
    private String titleColor;
    private TextAlignment titleAlignment;
    private String units;
    private Integer decimals;
    private Boolean autoScale;
    private Boolean sortSeries;
    private Boolean showTotal;
    private Boolean showLegend;
    private LegendPosition legendPosition;
    private Font legendLabelFont;
    private String legendLabelColor;
    private Font legendValueFont;
    private String legendValueColor;
    private Boolean legendShowTotal;

    public ReportLatestChartSettings() {
    }

    public ReportLatestChartSettings(ReportLatestChartSettings input) {
        if (input == null) {
            input = new ReportLatestChartSettings();
        }
        this.showTitle = input.getShowTitle() != null ? input.getShowTitle() : Boolean.TRUE;
        this.title = input.getTitle() != null ? input.getTitle() : "Chart";
        this.titleFont = input.getTitleFont() != null ? input.getTitleFont() : Font.builder().family("Roboto").size(Float.valueOf(18.0f)).weight(FontWeight.WEIGHT_500).style(FontStyle.NORMAL).build();
        this.titleColor = input.getTitleColor() != null ? input.getTitleColor() : "rgba(0, 0, 0, 0.87)";
        this.titleAlignment = input.getTitleAlignment() != null ? input.getTitleAlignment() : TextAlignment.CENTER;
        this.units = input.getUnits();
        this.decimals = input.getDecimals() != null ? input.getDecimals() : 0;
        this.autoScale = input.getAutoScale() != null ? input.getAutoScale() : Boolean.FALSE;
        this.sortSeries = input.getSortSeries() != null ? input.getSortSeries() : Boolean.FALSE;
        this.showTotal = input.getShowTotal() != null ? input.getShowTotal() : Boolean.FALSE;
        this.showLegend = input.getShowLegend() != null ? input.getShowLegend() : Boolean.TRUE;
        this.legendPosition = input.getLegendPosition() != null ? input.getLegendPosition() : LegendPosition.bottom;
        this.legendLabelFont = input.getLegendLabelFont() != null ? input.getLegendLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.legendLabelColor = input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.38)";
        this.legendValueFont = input.getLegendValueFont() != null ? input.getLegendValueFont() : Font.builder().family("Roboto").size(Float.valueOf(14.0f)).weight(FontWeight.WEIGHT_500).style(FontStyle.NORMAL).build();
        this.legendValueColor = input.getLegendValueColor() != null ? input.getLegendValueColor() : "rgba(0, 0, 0, 0.87)";
        this.legendShowTotal = input.getLegendShowTotal() != null ? input.getLegendShowTotal() : Boolean.TRUE;
    }

    @Generated
    public Boolean getShowTitle() {
        return this.showTitle;
    }

    @Generated
    public String getTitle() {
        return this.title;
    }

    @Generated
    public Font getTitleFont() {
        return this.titleFont;
    }

    @Generated
    public String getTitleColor() {
        return this.titleColor;
    }

    @Generated
    public TextAlignment getTitleAlignment() {
        return this.titleAlignment;
    }

    @Generated
    public String getUnits() {
        return this.units;
    }

    @Generated
    public Integer getDecimals() {
        return this.decimals;
    }

    @Generated
    public Boolean getAutoScale() {
        return this.autoScale;
    }

    @Generated
    public Boolean getSortSeries() {
        return this.sortSeries;
    }

    @Generated
    public Boolean getShowTotal() {
        return this.showTotal;
    }

    @Generated
    public Boolean getShowLegend() {
        return this.showLegend;
    }

    @Generated
    public LegendPosition getLegendPosition() {
        return this.legendPosition;
    }

    @Generated
    public Font getLegendLabelFont() {
        return this.legendLabelFont;
    }

    @Generated
    public String getLegendLabelColor() {
        return this.legendLabelColor;
    }

    @Generated
    public Font getLegendValueFont() {
        return this.legendValueFont;
    }

    @Generated
    public String getLegendValueColor() {
        return this.legendValueColor;
    }

    @Generated
    public Boolean getLegendShowTotal() {
        return this.legendShowTotal;
    }

    @Generated
    public void setShowTitle(Boolean showTitle) {
        this.showTitle = showTitle;
    }

    @Generated
    public void setTitle(String title) {
        this.title = title;
    }

    @Generated
    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    @Generated
    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    @Generated
    public void setTitleAlignment(TextAlignment titleAlignment) {
        this.titleAlignment = titleAlignment;
    }

    @Generated
    public void setUnits(String units) {
        this.units = units;
    }

    @Generated
    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    @Generated
    public void setAutoScale(Boolean autoScale) {
        this.autoScale = autoScale;
    }

    @Generated
    public void setSortSeries(Boolean sortSeries) {
        this.sortSeries = sortSeries;
    }

    @Generated
    public void setShowTotal(Boolean showTotal) {
        this.showTotal = showTotal;
    }

    @Generated
    public void setShowLegend(Boolean showLegend) {
        this.showLegend = showLegend;
    }

    @Generated
    public void setLegendPosition(LegendPosition legendPosition) {
        this.legendPosition = legendPosition;
    }

    @Generated
    public void setLegendLabelFont(Font legendLabelFont) {
        this.legendLabelFont = legendLabelFont;
    }

    @Generated
    public void setLegendLabelColor(String legendLabelColor) {
        this.legendLabelColor = legendLabelColor;
    }

    @Generated
    public void setLegendValueFont(Font legendValueFont) {
        this.legendValueFont = legendValueFont;
    }

    @Generated
    public void setLegendValueColor(String legendValueColor) {
        this.legendValueColor = legendValueColor;
    }

    @Generated
    public void setLegendShowTotal(Boolean legendShowTotal) {
        this.legendShowTotal = legendShowTotal;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportLatestChartSettings)) {
            return false;
        }
        ReportLatestChartSettings other = (ReportLatestChartSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showTitle = this.getShowTitle();
        Boolean other$showTitle = other.getShowTitle();
        if (this$showTitle == null ? other$showTitle != null : !((Object)this$showTitle).equals(other$showTitle)) {
            return false;
        }
        Integer this$decimals = this.getDecimals();
        Integer other$decimals = other.getDecimals();
        if (this$decimals == null ? other$decimals != null : !((Object)this$decimals).equals(other$decimals)) {
            return false;
        }
        Boolean this$autoScale = this.getAutoScale();
        Boolean other$autoScale = other.getAutoScale();
        if (this$autoScale == null ? other$autoScale != null : !((Object)this$autoScale).equals(other$autoScale)) {
            return false;
        }
        Boolean this$sortSeries = this.getSortSeries();
        Boolean other$sortSeries = other.getSortSeries();
        if (this$sortSeries == null ? other$sortSeries != null : !((Object)this$sortSeries).equals(other$sortSeries)) {
            return false;
        }
        Boolean this$showTotal = this.getShowTotal();
        Boolean other$showTotal = other.getShowTotal();
        if (this$showTotal == null ? other$showTotal != null : !((Object)this$showTotal).equals(other$showTotal)) {
            return false;
        }
        Boolean this$showLegend = this.getShowLegend();
        Boolean other$showLegend = other.getShowLegend();
        if (this$showLegend == null ? other$showLegend != null : !((Object)this$showLegend).equals(other$showLegend)) {
            return false;
        }
        Boolean this$legendShowTotal = this.getLegendShowTotal();
        Boolean other$legendShowTotal = other.getLegendShowTotal();
        if (this$legendShowTotal == null ? other$legendShowTotal != null : !((Object)this$legendShowTotal).equals(other$legendShowTotal)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        Font this$titleFont = this.getTitleFont();
        Font other$titleFont = other.getTitleFont();
        if (this$titleFont == null ? other$titleFont != null : !((Object)this$titleFont).equals(other$titleFont)) {
            return false;
        }
        String this$titleColor = this.getTitleColor();
        String other$titleColor = other.getTitleColor();
        if (this$titleColor == null ? other$titleColor != null : !this$titleColor.equals(other$titleColor)) {
            return false;
        }
        TextAlignment this$titleAlignment = this.getTitleAlignment();
        TextAlignment other$titleAlignment = other.getTitleAlignment();
        if (this$titleAlignment == null ? other$titleAlignment != null : !((Object)((Object)this$titleAlignment)).equals((Object)other$titleAlignment)) {
            return false;
        }
        String this$units = this.getUnits();
        String other$units = other.getUnits();
        if (this$units == null ? other$units != null : !this$units.equals(other$units)) {
            return false;
        }
        LegendPosition this$legendPosition = this.getLegendPosition();
        LegendPosition other$legendPosition = other.getLegendPosition();
        if (this$legendPosition == null ? other$legendPosition != null : !((Object)((Object)this$legendPosition)).equals((Object)other$legendPosition)) {
            return false;
        }
        Font this$legendLabelFont = this.getLegendLabelFont();
        Font other$legendLabelFont = other.getLegendLabelFont();
        if (this$legendLabelFont == null ? other$legendLabelFont != null : !((Object)this$legendLabelFont).equals(other$legendLabelFont)) {
            return false;
        }
        String this$legendLabelColor = this.getLegendLabelColor();
        String other$legendLabelColor = other.getLegendLabelColor();
        if (this$legendLabelColor == null ? other$legendLabelColor != null : !this$legendLabelColor.equals(other$legendLabelColor)) {
            return false;
        }
        Font this$legendValueFont = this.getLegendValueFont();
        Font other$legendValueFont = other.getLegendValueFont();
        if (this$legendValueFont == null ? other$legendValueFont != null : !((Object)this$legendValueFont).equals(other$legendValueFont)) {
            return false;
        }
        String this$legendValueColor = this.getLegendValueColor();
        String other$legendValueColor = other.getLegendValueColor();
        return !(this$legendValueColor == null ? other$legendValueColor != null : !this$legendValueColor.equals(other$legendValueColor));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportLatestChartSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showTitle = this.getShowTitle();
        result = result * 59 + ($showTitle == null ? 43 : ((Object)$showTitle).hashCode());
        Integer $decimals = this.getDecimals();
        result = result * 59 + ($decimals == null ? 43 : ((Object)$decimals).hashCode());
        Boolean $autoScale = this.getAutoScale();
        result = result * 59 + ($autoScale == null ? 43 : ((Object)$autoScale).hashCode());
        Boolean $sortSeries = this.getSortSeries();
        result = result * 59 + ($sortSeries == null ? 43 : ((Object)$sortSeries).hashCode());
        Boolean $showTotal = this.getShowTotal();
        result = result * 59 + ($showTotal == null ? 43 : ((Object)$showTotal).hashCode());
        Boolean $showLegend = this.getShowLegend();
        result = result * 59 + ($showLegend == null ? 43 : ((Object)$showLegend).hashCode());
        Boolean $legendShowTotal = this.getLegendShowTotal();
        result = result * 59 + ($legendShowTotal == null ? 43 : ((Object)$legendShowTotal).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        Font $titleFont = this.getTitleFont();
        result = result * 59 + ($titleFont == null ? 43 : ((Object)$titleFont).hashCode());
        String $titleColor = this.getTitleColor();
        result = result * 59 + ($titleColor == null ? 43 : $titleColor.hashCode());
        TextAlignment $titleAlignment = this.getTitleAlignment();
        result = result * 59 + ($titleAlignment == null ? 43 : ((Object)((Object)$titleAlignment)).hashCode());
        String $units = this.getUnits();
        result = result * 59 + ($units == null ? 43 : $units.hashCode());
        LegendPosition $legendPosition = this.getLegendPosition();
        result = result * 59 + ($legendPosition == null ? 43 : ((Object)((Object)$legendPosition)).hashCode());
        Font $legendLabelFont = this.getLegendLabelFont();
        result = result * 59 + ($legendLabelFont == null ? 43 : ((Object)$legendLabelFont).hashCode());
        String $legendLabelColor = this.getLegendLabelColor();
        result = result * 59 + ($legendLabelColor == null ? 43 : $legendLabelColor.hashCode());
        Font $legendValueFont = this.getLegendValueFont();
        result = result * 59 + ($legendValueFont == null ? 43 : ((Object)$legendValueFont).hashCode());
        String $legendValueColor = this.getLegendValueColor();
        result = result * 59 + ($legendValueColor == null ? 43 : $legendValueColor.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ReportLatestChartSettings(showTitle=" + this.getShowTitle() + ", title=" + this.getTitle() + ", titleFont=" + String.valueOf(this.getTitleFont()) + ", titleColor=" + this.getTitleColor() + ", titleAlignment=" + String.valueOf((Object)this.getTitleAlignment()) + ", units=" + this.getUnits() + ", decimals=" + this.getDecimals() + ", autoScale=" + this.getAutoScale() + ", sortSeries=" + this.getSortSeries() + ", showTotal=" + this.getShowTotal() + ", showLegend=" + this.getShowLegend() + ", legendPosition=" + String.valueOf((Object)this.getLegendPosition()) + ", legendLabelFont=" + String.valueOf(this.getLegendLabelFont()) + ", legendLabelColor=" + this.getLegendLabelColor() + ", legendValueFont=" + String.valueOf(this.getLegendValueFont()) + ", legendValueColor=" + this.getLegendValueColor() + ", legendShowTotal=" + this.getLegendShowTotal() + ")";
    }
}

