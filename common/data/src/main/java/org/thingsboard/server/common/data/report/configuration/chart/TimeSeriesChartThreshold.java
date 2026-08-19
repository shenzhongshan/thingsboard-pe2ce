/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLineType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.ThresholdLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceConfig;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceType;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class TimeSeriesChartThreshold
extends ValueSourceConfig {
    @JsonProperty(value="yAxisId")
    private String yAxisId;
    private String units;
    private Integer decimals;
    private String lineColor;
    private ChartLineType lineType;
    private Float lineWidth;
    private ChartShape startSymbol;
    private Float startSymbolSize;
    private ChartShape endSymbol;
    private Float endSymbolSize;
    private Boolean showLabel;
    private ThresholdLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Boolean enableLabelBackground;
    private String labelBackground;

    public TimeSeriesChartThreshold() {
    }

    public TimeSeriesChartThreshold(TimeSeriesChartThreshold input) {
        if (input == null) {
            input = new TimeSeriesChartThreshold();
        }
        this.setType(input.getType() != null ? input.getType() : ValueSourceType.constant);
        this.yAxisId = input.getYAxisId() != null ? input.getYAxisId() : "default";
        this.units = input.getUnits();
        this.decimals = input.getDecimals();
        this.lineColor = input.getLineColor() != null ? input.getLineColor() : "rgba(0, 0, 0, 0.76)";
        this.lineType = input.getLineType() != null ? input.getLineType() : ChartLineType.solid;
        this.lineWidth = Float.valueOf(input.getLineWidth() != null ? input.getLineWidth().floatValue() : 1.0f);
        this.startSymbol = input.getStartSymbol() != null ? input.getStartSymbol() : ChartShape.none;
        this.startSymbolSize = Float.valueOf(input.getStartSymbolSize() != null ? input.getStartSymbolSize().floatValue() : 5.0f);
        this.endSymbol = input.getEndSymbol() != null ? input.getEndSymbol() : ChartShape.arrow;
        this.endSymbolSize = Float.valueOf(input.getEndSymbolSize() != null ? input.getEndSymbolSize().floatValue() : 5.0f);
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.TRUE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : ThresholdLabelPosition.end;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enableLabelBackground = input.getEnableLabelBackground() != null ? input.getEnableLabelBackground() : Boolean.FALSE;
        this.labelBackground = input.getLabelBackground() != null ? input.getLabelBackground() : "rgba(255,255,255,0.56)";
    }

    @Generated
    public String getYAxisId() {
        return this.yAxisId;
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
    public String getLineColor() {
        return this.lineColor;
    }

    @Generated
    public ChartLineType getLineType() {
        return this.lineType;
    }

    @Generated
    public Float getLineWidth() {
        return this.lineWidth;
    }

    @Generated
    public ChartShape getStartSymbol() {
        return this.startSymbol;
    }

    @Generated
    public Float getStartSymbolSize() {
        return this.startSymbolSize;
    }

    @Generated
    public ChartShape getEndSymbol() {
        return this.endSymbol;
    }

    @Generated
    public Float getEndSymbolSize() {
        return this.endSymbolSize;
    }

    @Generated
    public Boolean getShowLabel() {
        return this.showLabel;
    }

    @Generated
    public ThresholdLabelPosition getLabelPosition() {
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
    public void setYAxisId(String yAxisId) {
        this.yAxisId = yAxisId;
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
    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }

    @Generated
    public void setLineType(ChartLineType lineType) {
        this.lineType = lineType;
    }

    @Generated
    public void setLineWidth(Float lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Generated
    public void setStartSymbol(ChartShape startSymbol) {
        this.startSymbol = startSymbol;
    }

    @Generated
    public void setStartSymbolSize(Float startSymbolSize) {
        this.startSymbolSize = startSymbolSize;
    }

    @Generated
    public void setEndSymbol(ChartShape endSymbol) {
        this.endSymbol = endSymbol;
    }

    @Generated
    public void setEndSymbolSize(Float endSymbolSize) {
        this.endSymbolSize = endSymbolSize;
    }

    @Generated
    public void setShowLabel(Boolean showLabel) {
        this.showLabel = showLabel;
    }

    @Generated
    public void setLabelPosition(ThresholdLabelPosition labelPosition) {
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

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartThreshold)) {
            return false;
        }
        TimeSeriesChartThreshold other = (TimeSeriesChartThreshold)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$decimals = this.getDecimals();
        Integer other$decimals = other.getDecimals();
        if (this$decimals == null ? other$decimals != null : !((Object)this$decimals).equals(other$decimals)) {
            return false;
        }
        Float this$lineWidth = this.getLineWidth();
        Float other$lineWidth = other.getLineWidth();
        if (this$lineWidth == null ? other$lineWidth != null : !((Object)this$lineWidth).equals(other$lineWidth)) {
            return false;
        }
        Float this$startSymbolSize = this.getStartSymbolSize();
        Float other$startSymbolSize = other.getStartSymbolSize();
        if (this$startSymbolSize == null ? other$startSymbolSize != null : !((Object)this$startSymbolSize).equals(other$startSymbolSize)) {
            return false;
        }
        Float this$endSymbolSize = this.getEndSymbolSize();
        Float other$endSymbolSize = other.getEndSymbolSize();
        if (this$endSymbolSize == null ? other$endSymbolSize != null : !((Object)this$endSymbolSize).equals(other$endSymbolSize)) {
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
        String this$yAxisId = this.getYAxisId();
        String other$yAxisId = other.getYAxisId();
        if (this$yAxisId == null ? other$yAxisId != null : !this$yAxisId.equals(other$yAxisId)) {
            return false;
        }
        String this$units = this.getUnits();
        String other$units = other.getUnits();
        if (this$units == null ? other$units != null : !this$units.equals(other$units)) {
            return false;
        }
        String this$lineColor = this.getLineColor();
        String other$lineColor = other.getLineColor();
        if (this$lineColor == null ? other$lineColor != null : !this$lineColor.equals(other$lineColor)) {
            return false;
        }
        ChartLineType this$lineType = this.getLineType();
        ChartLineType other$lineType = other.getLineType();
        if (this$lineType == null ? other$lineType != null : !((Object)((Object)this$lineType)).equals((Object)other$lineType)) {
            return false;
        }
        ChartShape this$startSymbol = this.getStartSymbol();
        ChartShape other$startSymbol = other.getStartSymbol();
        if (this$startSymbol == null ? other$startSymbol != null : !((Object)((Object)this$startSymbol)).equals((Object)other$startSymbol)) {
            return false;
        }
        ChartShape this$endSymbol = this.getEndSymbol();
        ChartShape other$endSymbol = other.getEndSymbol();
        if (this$endSymbol == null ? other$endSymbol != null : !((Object)((Object)this$endSymbol)).equals((Object)other$endSymbol)) {
            return false;
        }
        ThresholdLabelPosition this$labelPosition = this.getLabelPosition();
        ThresholdLabelPosition other$labelPosition = other.getLabelPosition();
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
        return !(this$labelBackground == null ? other$labelBackground != null : !this$labelBackground.equals(other$labelBackground));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartThreshold;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $decimals = this.getDecimals();
        result = result * 59 + ($decimals == null ? 43 : ((Object)$decimals).hashCode());
        Float $lineWidth = this.getLineWidth();
        result = result * 59 + ($lineWidth == null ? 43 : ((Object)$lineWidth).hashCode());
        Float $startSymbolSize = this.getStartSymbolSize();
        result = result * 59 + ($startSymbolSize == null ? 43 : ((Object)$startSymbolSize).hashCode());
        Float $endSymbolSize = this.getEndSymbolSize();
        result = result * 59 + ($endSymbolSize == null ? 43 : ((Object)$endSymbolSize).hashCode());
        Boolean $showLabel = this.getShowLabel();
        result = result * 59 + ($showLabel == null ? 43 : ((Object)$showLabel).hashCode());
        Boolean $enableLabelBackground = this.getEnableLabelBackground();
        result = result * 59 + ($enableLabelBackground == null ? 43 : ((Object)$enableLabelBackground).hashCode());
        String $yAxisId = this.getYAxisId();
        result = result * 59 + ($yAxisId == null ? 43 : $yAxisId.hashCode());
        String $units = this.getUnits();
        result = result * 59 + ($units == null ? 43 : $units.hashCode());
        String $lineColor = this.getLineColor();
        result = result * 59 + ($lineColor == null ? 43 : $lineColor.hashCode());
        ChartLineType $lineType = this.getLineType();
        result = result * 59 + ($lineType == null ? 43 : ((Object)((Object)$lineType)).hashCode());
        ChartShape $startSymbol = this.getStartSymbol();
        result = result * 59 + ($startSymbol == null ? 43 : ((Object)((Object)$startSymbol)).hashCode());
        ChartShape $endSymbol = this.getEndSymbol();
        result = result * 59 + ($endSymbol == null ? 43 : ((Object)((Object)$endSymbol)).hashCode());
        ThresholdLabelPosition $labelPosition = this.getLabelPosition();
        result = result * 59 + ($labelPosition == null ? 43 : ((Object)((Object)$labelPosition)).hashCode());
        Font $labelFont = this.getLabelFont();
        result = result * 59 + ($labelFont == null ? 43 : ((Object)$labelFont).hashCode());
        String $labelColor = this.getLabelColor();
        result = result * 59 + ($labelColor == null ? 43 : $labelColor.hashCode());
        String $labelBackground = this.getLabelBackground();
        result = result * 59 + ($labelBackground == null ? 43 : $labelBackground.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "TimeSeriesChartThreshold(yAxisId=" + this.getYAxisId() + ", units=" + this.getUnits() + ", decimals=" + this.getDecimals() + ", lineColor=" + this.getLineColor() + ", lineType=" + String.valueOf((Object)this.getLineType()) + ", lineWidth=" + this.getLineWidth() + ", startSymbol=" + String.valueOf((Object)this.getStartSymbol()) + ", startSymbolSize=" + this.getStartSymbolSize() + ", endSymbol=" + String.valueOf((Object)this.getEndSymbol()) + ", endSymbolSize=" + this.getEndSymbolSize() + ", showLabel=" + this.getShowLabel() + ", labelPosition=" + String.valueOf((Object)this.getLabelPosition()) + ", labelFont=" + String.valueOf(this.getLabelFont()) + ", labelColor=" + this.getLabelColor() + ", enableLabelBackground=" + this.getEnableLabelBackground() + ", labelBackground=" + this.getLabelBackground() + ")";
    }
}

