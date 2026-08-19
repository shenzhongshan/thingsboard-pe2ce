/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLineType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.ColorRange;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ThresholdLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;

public class ReportRangeChartSettings
extends ReportTimeSeriesChartSettings {
    private List<ColorRange> rangeColors;
    private String outOfRangeColor;
    private Boolean showRangeThresholds;
    private TimeSeriesChartThreshold rangeThreshold;
    private Boolean fillArea;
    private Float fillAreaOpacity;
    private LineSeriesSettings lineSettings;
    private String rangeUnits;
    private Integer rangeDecimals;

    public ReportRangeChartSettings() {
    }

    public ReportRangeChartSettings(ReportRangeChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportRangeChartSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Range chart");
        this.rangeColors = input.rangeColors != null ? new ArrayList<ColorRange>(input.rangeColors) : new ArrayList();
        this.outOfRangeColor = input.outOfRangeColor != null ? input.outOfRangeColor : "#ccc";
        this.showRangeThresholds = input.showRangeThresholds != null ? input.showRangeThresholds : Boolean.TRUE;
        this.rangeThreshold = new TimeSeriesChartThreshold(input.getRangeThreshold());
        TimeSeriesChartThreshold inputRangeThreshold = input.getRangeThreshold();
        if (inputRangeThreshold == null) {
            inputRangeThreshold = new TimeSeriesChartThreshold();
        }
        this.rangeThreshold.setLineColor(inputRangeThreshold.getLineColor() != null ? inputRangeThreshold.getLineColor() : "#37383b");
        this.rangeThreshold.setLineType(inputRangeThreshold.getLineType() != null ? inputRangeThreshold.getLineType() : ChartLineType.dashed);
        this.rangeThreshold.setStartSymbol(inputRangeThreshold.getStartSymbol() != null ? inputRangeThreshold.getStartSymbol() : ChartShape.circle);
        this.rangeThreshold.setStartSymbolSize(Float.valueOf(inputRangeThreshold.getStartSymbolSize() != null ? inputRangeThreshold.getStartSymbolSize().floatValue() : 5.0f));
        this.rangeThreshold.setEndSymbol(inputRangeThreshold.getEndSymbol() != null ? inputRangeThreshold.getEndSymbol() : ChartShape.arrow);
        this.rangeThreshold.setEndSymbolSize(Float.valueOf(inputRangeThreshold.getEndSymbolSize() != null ? inputRangeThreshold.getEndSymbolSize().floatValue() : 7.0f));
        this.rangeThreshold.setLabelPosition(inputRangeThreshold.getLabelPosition() != null ? inputRangeThreshold.getLabelPosition() : ThresholdLabelPosition.insideEndTop);
        this.rangeThreshold.setLabelColor(inputRangeThreshold.getLabelColor() != null ? inputRangeThreshold.getLabelColor() : "#37383b");
        this.rangeThreshold.setEnableLabelBackground(inputRangeThreshold.getEnableLabelBackground() != null ? inputRangeThreshold.getEnableLabelBackground() : Boolean.TRUE);
        this.fillArea = input.getFillArea() != null ? input.getFillArea() : Boolean.TRUE;
        this.fillAreaOpacity = Float.valueOf(input.getFillAreaOpacity() != null ? input.getFillAreaOpacity().floatValue() : 0.7f);
        this.lineSettings = new LineSeriesSettings(input.getLineSettings());
        this.rangeUnits = input.getRangeUnits() != null ? input.getRangeUnits() : "";
        this.rangeDecimals = input.getRangeDecimals() != null ? input.getRangeDecimals() : 0;
        this.setLegendLabelColor(input.getLegendLabelColor() != null ? input.getLegendLabelColor() : "rgba(0, 0, 0, 0.76)");
    }

    @JsonIgnore
    public TimeSeriesChartKeySettings toTimeSeriesChartKeySettings() {
        TimeSeriesChartKeySettings keySettings = new TimeSeriesChartKeySettings();
        keySettings.setSeriesType(TimeSeriesChartSeriesType.line);
        ChartFillSettings fillSettings = new ChartFillSettings();
        fillSettings.setType(this.fillArea != false ? ChartFillType.opacity : ChartFillType.none);
        fillSettings.setOpacity(this.fillAreaOpacity);
        LineSeriesSettings lineSettings = new LineSeriesSettings(this.lineSettings);
        lineSettings.setFillAreaSettings(fillSettings);
        keySettings.setLineSettings(lineSettings);
        return keySettings;
    }

    @Generated
    public List<ColorRange> getRangeColors() {
        return this.rangeColors;
    }

    @Generated
    public String getOutOfRangeColor() {
        return this.outOfRangeColor;
    }

    @Generated
    public Boolean getShowRangeThresholds() {
        return this.showRangeThresholds;
    }

    @Generated
    public TimeSeriesChartThreshold getRangeThreshold() {
        return this.rangeThreshold;
    }

    @Generated
    public Boolean getFillArea() {
        return this.fillArea;
    }

    @Generated
    public Float getFillAreaOpacity() {
        return this.fillAreaOpacity;
    }

    @Generated
    public LineSeriesSettings getLineSettings() {
        return this.lineSettings;
    }

    @Generated
    public String getRangeUnits() {
        return this.rangeUnits;
    }

    @Generated
    public Integer getRangeDecimals() {
        return this.rangeDecimals;
    }

    @Generated
    public void setRangeColors(List<ColorRange> rangeColors) {
        this.rangeColors = rangeColors;
    }

    @Generated
    public void setOutOfRangeColor(String outOfRangeColor) {
        this.outOfRangeColor = outOfRangeColor;
    }

    @Generated
    public void setShowRangeThresholds(Boolean showRangeThresholds) {
        this.showRangeThresholds = showRangeThresholds;
    }

    @Generated
    public void setRangeThreshold(TimeSeriesChartThreshold rangeThreshold) {
        this.rangeThreshold = rangeThreshold;
    }

    @Generated
    public void setFillArea(Boolean fillArea) {
        this.fillArea = fillArea;
    }

    @Generated
    public void setFillAreaOpacity(Float fillAreaOpacity) {
        this.fillAreaOpacity = fillAreaOpacity;
    }

    @Generated
    public void setLineSettings(LineSeriesSettings lineSettings) {
        this.lineSettings = lineSettings;
    }

    @Generated
    public void setRangeUnits(String rangeUnits) {
        this.rangeUnits = rangeUnits;
    }

    @Generated
    public void setRangeDecimals(Integer rangeDecimals) {
        this.rangeDecimals = rangeDecimals;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportRangeChartSettings)) {
            return false;
        }
        ReportRangeChartSettings other = (ReportRangeChartSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showRangeThresholds = this.getShowRangeThresholds();
        Boolean other$showRangeThresholds = other.getShowRangeThresholds();
        if (this$showRangeThresholds == null ? other$showRangeThresholds != null : !((Object)this$showRangeThresholds).equals(other$showRangeThresholds)) {
            return false;
        }
        Boolean this$fillArea = this.getFillArea();
        Boolean other$fillArea = other.getFillArea();
        if (this$fillArea == null ? other$fillArea != null : !((Object)this$fillArea).equals(other$fillArea)) {
            return false;
        }
        Float this$fillAreaOpacity = this.getFillAreaOpacity();
        Float other$fillAreaOpacity = other.getFillAreaOpacity();
        if (this$fillAreaOpacity == null ? other$fillAreaOpacity != null : !((Object)this$fillAreaOpacity).equals(other$fillAreaOpacity)) {
            return false;
        }
        Integer this$rangeDecimals = this.getRangeDecimals();
        Integer other$rangeDecimals = other.getRangeDecimals();
        if (this$rangeDecimals == null ? other$rangeDecimals != null : !((Object)this$rangeDecimals).equals(other$rangeDecimals)) {
            return false;
        }
        List<ColorRange> this$rangeColors = this.getRangeColors();
        List<ColorRange> other$rangeColors = other.getRangeColors();
        if (this$rangeColors == null ? other$rangeColors != null : !((Object)this$rangeColors).equals(other$rangeColors)) {
            return false;
        }
        String this$outOfRangeColor = this.getOutOfRangeColor();
        String other$outOfRangeColor = other.getOutOfRangeColor();
        if (this$outOfRangeColor == null ? other$outOfRangeColor != null : !this$outOfRangeColor.equals(other$outOfRangeColor)) {
            return false;
        }
        TimeSeriesChartThreshold this$rangeThreshold = this.getRangeThreshold();
        TimeSeriesChartThreshold other$rangeThreshold = other.getRangeThreshold();
        if (this$rangeThreshold == null ? other$rangeThreshold != null : !((Object)this$rangeThreshold).equals(other$rangeThreshold)) {
            return false;
        }
        LineSeriesSettings this$lineSettings = this.getLineSettings();
        LineSeriesSettings other$lineSettings = other.getLineSettings();
        if (this$lineSettings == null ? other$lineSettings != null : !((Object)this$lineSettings).equals(other$lineSettings)) {
            return false;
        }
        String this$rangeUnits = this.getRangeUnits();
        String other$rangeUnits = other.getRangeUnits();
        return !(this$rangeUnits == null ? other$rangeUnits != null : !this$rangeUnits.equals(other$rangeUnits));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportRangeChartSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showRangeThresholds = this.getShowRangeThresholds();
        result = result * 59 + ($showRangeThresholds == null ? 43 : ((Object)$showRangeThresholds).hashCode());
        Boolean $fillArea = this.getFillArea();
        result = result * 59 + ($fillArea == null ? 43 : ((Object)$fillArea).hashCode());
        Float $fillAreaOpacity = this.getFillAreaOpacity();
        result = result * 59 + ($fillAreaOpacity == null ? 43 : ((Object)$fillAreaOpacity).hashCode());
        Integer $rangeDecimals = this.getRangeDecimals();
        result = result * 59 + ($rangeDecimals == null ? 43 : ((Object)$rangeDecimals).hashCode());
        List<ColorRange> $rangeColors = this.getRangeColors();
        result = result * 59 + ($rangeColors == null ? 43 : ((Object)$rangeColors).hashCode());
        String $outOfRangeColor = this.getOutOfRangeColor();
        result = result * 59 + ($outOfRangeColor == null ? 43 : $outOfRangeColor.hashCode());
        TimeSeriesChartThreshold $rangeThreshold = this.getRangeThreshold();
        result = result * 59 + ($rangeThreshold == null ? 43 : ((Object)$rangeThreshold).hashCode());
        LineSeriesSettings $lineSettings = this.getLineSettings();
        result = result * 59 + ($lineSettings == null ? 43 : ((Object)$lineSettings).hashCode());
        String $rangeUnits = this.getRangeUnits();
        result = result * 59 + ($rangeUnits == null ? 43 : $rangeUnits.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportRangeChartSettings(rangeColors=" + String.valueOf(this.getRangeColors()) + ", outOfRangeColor=" + this.getOutOfRangeColor() + ", showRangeThresholds=" + this.getShowRangeThresholds() + ", rangeThreshold=" + String.valueOf(this.getRangeThreshold()) + ", fillArea=" + this.getFillArea() + ", fillAreaOpacity=" + this.getFillAreaOpacity() + ", lineSettings=" + String.valueOf(this.getLineSettings()) + ", rangeUnits=" + this.getRangeUnits() + ", rangeDecimals=" + this.getRangeDecimals() + ")";
    }
}

