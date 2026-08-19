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
import org.thingsboard.server.common.data.report.configuration.chart.ChartLineType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesStepType;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class LineSeriesSettings {
    private Boolean showLine;
    private Boolean step;
    private LineSeriesStepType stepType;
    private Boolean smooth;
    private ChartLineType lineType;
    private Float lineWidth;
    private Boolean showPoints;
    private Boolean showPointLabel;
    private ChartLabelPosition pointLabelPosition;
    private Font pointLabelFont;
    private String pointLabelColor;
    private Boolean enablePointLabelBackground;
    private String pointLabelBackground;
    private ChartShape pointShape;
    private Float pointSize;
    private ChartFillSettings fillAreaSettings;

    public LineSeriesSettings() {
    }

    public LineSeriesSettings(LineSeriesSettings input) {
        if (input == null) {
            input = new LineSeriesSettings();
        }
        this.showLine = input.getShowLine() != null ? input.getShowLine() : Boolean.TRUE;
        this.step = input.getStep() != null ? input.getStep() : Boolean.FALSE;
        this.stepType = input.getStepType() != null ? input.getStepType() : LineSeriesStepType.start;
        this.smooth = input.getSmooth() != null ? input.getSmooth() : Boolean.FALSE;
        this.lineType = input.getLineType() != null ? input.getLineType() : ChartLineType.solid;
        this.lineWidth = Float.valueOf(input.getLineWidth() != null ? input.getLineWidth().floatValue() : 2.0f);
        this.showPoints = input.getShowPoints() != null ? input.getShowPoints() : Boolean.FALSE;
        this.showPointLabel = input.getShowPointLabel() != null ? input.getShowPointLabel() : Boolean.FALSE;
        this.pointLabelPosition = input.getPointLabelPosition() != null ? input.getPointLabelPosition() : ChartLabelPosition.top;
        this.pointLabelFont = input.getPointLabelFont() != null ? input.getPointLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(11.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.pointLabelColor = input.getPointLabelColor() != null ? input.getPointLabelColor() : "rgba(0, 0, 0, 0.76)";
        this.enablePointLabelBackground = input.getEnablePointLabelBackground() != null ? input.getEnablePointLabelBackground() : Boolean.FALSE;
        this.pointLabelBackground = input.getPointLabelBackground() != null ? input.getPointLabelBackground() : "rgba(255,255,255,0.56)";
        this.pointShape = input.getPointShape() != null ? input.getPointShape() : ChartShape.emptyCircle;
        this.pointSize = Float.valueOf(input.getPointSize() != null ? input.getPointSize().floatValue() : 4.0f);
        this.fillAreaSettings = new ChartFillSettings(input.getFillAreaSettings());
    }

    @Generated
    public Boolean getShowLine() {
        return this.showLine;
    }

    @Generated
    public Boolean getStep() {
        return this.step;
    }

    @Generated
    public LineSeriesStepType getStepType() {
        return this.stepType;
    }

    @Generated
    public Boolean getSmooth() {
        return this.smooth;
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
    public Boolean getShowPoints() {
        return this.showPoints;
    }

    @Generated
    public Boolean getShowPointLabel() {
        return this.showPointLabel;
    }

    @Generated
    public ChartLabelPosition getPointLabelPosition() {
        return this.pointLabelPosition;
    }

    @Generated
    public Font getPointLabelFont() {
        return this.pointLabelFont;
    }

    @Generated
    public String getPointLabelColor() {
        return this.pointLabelColor;
    }

    @Generated
    public Boolean getEnablePointLabelBackground() {
        return this.enablePointLabelBackground;
    }

    @Generated
    public String getPointLabelBackground() {
        return this.pointLabelBackground;
    }

    @Generated
    public ChartShape getPointShape() {
        return this.pointShape;
    }

    @Generated
    public Float getPointSize() {
        return this.pointSize;
    }

    @Generated
    public ChartFillSettings getFillAreaSettings() {
        return this.fillAreaSettings;
    }

    @Generated
    public void setShowLine(Boolean showLine) {
        this.showLine = showLine;
    }

    @Generated
    public void setStep(Boolean step) {
        this.step = step;
    }

    @Generated
    public void setStepType(LineSeriesStepType stepType) {
        this.stepType = stepType;
    }

    @Generated
    public void setSmooth(Boolean smooth) {
        this.smooth = smooth;
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
    public void setShowPoints(Boolean showPoints) {
        this.showPoints = showPoints;
    }

    @Generated
    public void setShowPointLabel(Boolean showPointLabel) {
        this.showPointLabel = showPointLabel;
    }

    @Generated
    public void setPointLabelPosition(ChartLabelPosition pointLabelPosition) {
        this.pointLabelPosition = pointLabelPosition;
    }

    @Generated
    public void setPointLabelFont(Font pointLabelFont) {
        this.pointLabelFont = pointLabelFont;
    }

    @Generated
    public void setPointLabelColor(String pointLabelColor) {
        this.pointLabelColor = pointLabelColor;
    }

    @Generated
    public void setEnablePointLabelBackground(Boolean enablePointLabelBackground) {
        this.enablePointLabelBackground = enablePointLabelBackground;
    }

    @Generated
    public void setPointLabelBackground(String pointLabelBackground) {
        this.pointLabelBackground = pointLabelBackground;
    }

    @Generated
    public void setPointShape(ChartShape pointShape) {
        this.pointShape = pointShape;
    }

    @Generated
    public void setPointSize(Float pointSize) {
        this.pointSize = pointSize;
    }

    @Generated
    public void setFillAreaSettings(ChartFillSettings fillAreaSettings) {
        this.fillAreaSettings = fillAreaSettings;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LineSeriesSettings)) {
            return false;
        }
        LineSeriesSettings other = (LineSeriesSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showLine = this.getShowLine();
        Boolean other$showLine = other.getShowLine();
        if (this$showLine == null ? other$showLine != null : !((Object)this$showLine).equals(other$showLine)) {
            return false;
        }
        Boolean this$step = this.getStep();
        Boolean other$step = other.getStep();
        if (this$step == null ? other$step != null : !((Object)this$step).equals(other$step)) {
            return false;
        }
        Boolean this$smooth = this.getSmooth();
        Boolean other$smooth = other.getSmooth();
        if (this$smooth == null ? other$smooth != null : !((Object)this$smooth).equals(other$smooth)) {
            return false;
        }
        Float this$lineWidth = this.getLineWidth();
        Float other$lineWidth = other.getLineWidth();
        if (this$lineWidth == null ? other$lineWidth != null : !((Object)this$lineWidth).equals(other$lineWidth)) {
            return false;
        }
        Boolean this$showPoints = this.getShowPoints();
        Boolean other$showPoints = other.getShowPoints();
        if (this$showPoints == null ? other$showPoints != null : !((Object)this$showPoints).equals(other$showPoints)) {
            return false;
        }
        Boolean this$showPointLabel = this.getShowPointLabel();
        Boolean other$showPointLabel = other.getShowPointLabel();
        if (this$showPointLabel == null ? other$showPointLabel != null : !((Object)this$showPointLabel).equals(other$showPointLabel)) {
            return false;
        }
        Boolean this$enablePointLabelBackground = this.getEnablePointLabelBackground();
        Boolean other$enablePointLabelBackground = other.getEnablePointLabelBackground();
        if (this$enablePointLabelBackground == null ? other$enablePointLabelBackground != null : !((Object)this$enablePointLabelBackground).equals(other$enablePointLabelBackground)) {
            return false;
        }
        Float this$pointSize = this.getPointSize();
        Float other$pointSize = other.getPointSize();
        if (this$pointSize == null ? other$pointSize != null : !((Object)this$pointSize).equals(other$pointSize)) {
            return false;
        }
        LineSeriesStepType this$stepType = this.getStepType();
        LineSeriesStepType other$stepType = other.getStepType();
        if (this$stepType == null ? other$stepType != null : !((Object)((Object)this$stepType)).equals((Object)other$stepType)) {
            return false;
        }
        ChartLineType this$lineType = this.getLineType();
        ChartLineType other$lineType = other.getLineType();
        if (this$lineType == null ? other$lineType != null : !((Object)((Object)this$lineType)).equals((Object)other$lineType)) {
            return false;
        }
        ChartLabelPosition this$pointLabelPosition = this.getPointLabelPosition();
        ChartLabelPosition other$pointLabelPosition = other.getPointLabelPosition();
        if (this$pointLabelPosition == null ? other$pointLabelPosition != null : !((Object)((Object)this$pointLabelPosition)).equals((Object)other$pointLabelPosition)) {
            return false;
        }
        Font this$pointLabelFont = this.getPointLabelFont();
        Font other$pointLabelFont = other.getPointLabelFont();
        if (this$pointLabelFont == null ? other$pointLabelFont != null : !((Object)this$pointLabelFont).equals(other$pointLabelFont)) {
            return false;
        }
        String this$pointLabelColor = this.getPointLabelColor();
        String other$pointLabelColor = other.getPointLabelColor();
        if (this$pointLabelColor == null ? other$pointLabelColor != null : !this$pointLabelColor.equals(other$pointLabelColor)) {
            return false;
        }
        String this$pointLabelBackground = this.getPointLabelBackground();
        String other$pointLabelBackground = other.getPointLabelBackground();
        if (this$pointLabelBackground == null ? other$pointLabelBackground != null : !this$pointLabelBackground.equals(other$pointLabelBackground)) {
            return false;
        }
        ChartShape this$pointShape = this.getPointShape();
        ChartShape other$pointShape = other.getPointShape();
        if (this$pointShape == null ? other$pointShape != null : !((Object)((Object)this$pointShape)).equals((Object)other$pointShape)) {
            return false;
        }
        ChartFillSettings this$fillAreaSettings = this.getFillAreaSettings();
        ChartFillSettings other$fillAreaSettings = other.getFillAreaSettings();
        return !(this$fillAreaSettings == null ? other$fillAreaSettings != null : !((Object)this$fillAreaSettings).equals(other$fillAreaSettings));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LineSeriesSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showLine = this.getShowLine();
        result = result * 59 + ($showLine == null ? 43 : ((Object)$showLine).hashCode());
        Boolean $step = this.getStep();
        result = result * 59 + ($step == null ? 43 : ((Object)$step).hashCode());
        Boolean $smooth = this.getSmooth();
        result = result * 59 + ($smooth == null ? 43 : ((Object)$smooth).hashCode());
        Float $lineWidth = this.getLineWidth();
        result = result * 59 + ($lineWidth == null ? 43 : ((Object)$lineWidth).hashCode());
        Boolean $showPoints = this.getShowPoints();
        result = result * 59 + ($showPoints == null ? 43 : ((Object)$showPoints).hashCode());
        Boolean $showPointLabel = this.getShowPointLabel();
        result = result * 59 + ($showPointLabel == null ? 43 : ((Object)$showPointLabel).hashCode());
        Boolean $enablePointLabelBackground = this.getEnablePointLabelBackground();
        result = result * 59 + ($enablePointLabelBackground == null ? 43 : ((Object)$enablePointLabelBackground).hashCode());
        Float $pointSize = this.getPointSize();
        result = result * 59 + ($pointSize == null ? 43 : ((Object)$pointSize).hashCode());
        LineSeriesStepType $stepType = this.getStepType();
        result = result * 59 + ($stepType == null ? 43 : ((Object)((Object)$stepType)).hashCode());
        ChartLineType $lineType = this.getLineType();
        result = result * 59 + ($lineType == null ? 43 : ((Object)((Object)$lineType)).hashCode());
        ChartLabelPosition $pointLabelPosition = this.getPointLabelPosition();
        result = result * 59 + ($pointLabelPosition == null ? 43 : ((Object)((Object)$pointLabelPosition)).hashCode());
        Font $pointLabelFont = this.getPointLabelFont();
        result = result * 59 + ($pointLabelFont == null ? 43 : ((Object)$pointLabelFont).hashCode());
        String $pointLabelColor = this.getPointLabelColor();
        result = result * 59 + ($pointLabelColor == null ? 43 : $pointLabelColor.hashCode());
        String $pointLabelBackground = this.getPointLabelBackground();
        result = result * 59 + ($pointLabelBackground == null ? 43 : $pointLabelBackground.hashCode());
        ChartShape $pointShape = this.getPointShape();
        result = result * 59 + ($pointShape == null ? 43 : ((Object)((Object)$pointShape)).hashCode());
        ChartFillSettings $fillAreaSettings = this.getFillAreaSettings();
        result = result * 59 + ($fillAreaSettings == null ? 43 : ((Object)$fillAreaSettings).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "LineSeriesSettings(showLine=" + this.getShowLine() + ", step=" + this.getStep() + ", stepType=" + String.valueOf((Object)this.getStepType()) + ", smooth=" + this.getSmooth() + ", lineType=" + String.valueOf((Object)this.getLineType()) + ", lineWidth=" + this.getLineWidth() + ", showPoints=" + this.getShowPoints() + ", showPointLabel=" + this.getShowPointLabel() + ", pointLabelPosition=" + String.valueOf((Object)this.getPointLabelPosition()) + ", pointLabelFont=" + String.valueOf(this.getPointLabelFont()) + ", pointLabelColor=" + this.getPointLabelColor() + ", enablePointLabelBackground=" + this.getEnablePointLabelBackground() + ", pointLabelBackground=" + this.getPointLabelBackground() + ", pointShape=" + String.valueOf((Object)this.getPointShape()) + ", pointSize=" + this.getPointSize() + ", fillAreaSettings=" + String.valueOf(this.getFillAreaSettings()) + ")";
    }
}

