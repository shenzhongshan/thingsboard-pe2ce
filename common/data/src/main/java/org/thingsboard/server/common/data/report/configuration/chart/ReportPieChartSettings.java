/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.PieChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public class ReportPieChartSettings
extends ReportLatestChartSettings {
    private Boolean showLabel;
    private PieChartLabelPosition labelPosition;
    private Font labelFont;
    private String labelColor;
    private Float borderWidth;
    private String borderColor;
    private Double radius;
    private Boolean clockwise;

    public ReportPieChartSettings() {
    }

    public ReportPieChartSettings(ReportPieChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportPieChartSettings();
        }
        this.showLabel = input.getShowLabel() != null ? input.getShowLabel() : Boolean.TRUE;
        this.labelPosition = input.getLabelPosition() != null ? input.getLabelPosition() : PieChartLabelPosition.outside;
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "#000";
        this.borderWidth = Float.valueOf(input.getBorderWidth() != null ? input.getBorderWidth().floatValue() : 0.0f);
        this.borderColor = input.getBorderColor() != null ? input.getBorderColor() : "#000";
        this.radius = input.getRadius() != null ? input.getRadius() : 80.0;
        this.clockwise = input.getClockwise() != null ? input.getClockwise() : Boolean.FALSE;
    }

    @Generated
    public Boolean getShowLabel() {
        return this.showLabel;
    }

    @Generated
    public PieChartLabelPosition getLabelPosition() {
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
    public Float getBorderWidth() {
        return this.borderWidth;
    }

    @Generated
    public String getBorderColor() {
        return this.borderColor;
    }

    @Generated
    public Double getRadius() {
        return this.radius;
    }

    @Generated
    public Boolean getClockwise() {
        return this.clockwise;
    }

    @Generated
    public void setShowLabel(Boolean showLabel) {
        this.showLabel = showLabel;
    }

    @Generated
    public void setLabelPosition(PieChartLabelPosition labelPosition) {
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
    public void setBorderWidth(Float borderWidth) {
        this.borderWidth = borderWidth;
    }

    @Generated
    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    @Generated
    public void setRadius(Double radius) {
        this.radius = radius;
    }

    @Generated
    public void setClockwise(Boolean clockwise) {
        this.clockwise = clockwise;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportPieChartSettings)) {
            return false;
        }
        ReportPieChartSettings other = (ReportPieChartSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showLabel = this.getShowLabel();
        Boolean other$showLabel = other.getShowLabel();
        if (this$showLabel == null ? other$showLabel != null : !((Object)this$showLabel).equals(other$showLabel)) {
            return false;
        }
        Float this$borderWidth = this.getBorderWidth();
        Float other$borderWidth = other.getBorderWidth();
        if (this$borderWidth == null ? other$borderWidth != null : !((Object)this$borderWidth).equals(other$borderWidth)) {
            return false;
        }
        Double this$radius = this.getRadius();
        Double other$radius = other.getRadius();
        if (this$radius == null ? other$radius != null : !((Object)this$radius).equals(other$radius)) {
            return false;
        }
        Boolean this$clockwise = this.getClockwise();
        Boolean other$clockwise = other.getClockwise();
        if (this$clockwise == null ? other$clockwise != null : !((Object)this$clockwise).equals(other$clockwise)) {
            return false;
        }
        PieChartLabelPosition this$labelPosition = this.getLabelPosition();
        PieChartLabelPosition other$labelPosition = other.getLabelPosition();
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
        String this$borderColor = this.getBorderColor();
        String other$borderColor = other.getBorderColor();
        return !(this$borderColor == null ? other$borderColor != null : !this$borderColor.equals(other$borderColor));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportPieChartSettings;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showLabel = this.getShowLabel();
        result = result * 59 + ($showLabel == null ? 43 : ((Object)$showLabel).hashCode());
        Float $borderWidth = this.getBorderWidth();
        result = result * 59 + ($borderWidth == null ? 43 : ((Object)$borderWidth).hashCode());
        Double $radius = this.getRadius();
        result = result * 59 + ($radius == null ? 43 : ((Object)$radius).hashCode());
        Boolean $clockwise = this.getClockwise();
        result = result * 59 + ($clockwise == null ? 43 : ((Object)$clockwise).hashCode());
        PieChartLabelPosition $labelPosition = this.getLabelPosition();
        result = result * 59 + ($labelPosition == null ? 43 : ((Object)((Object)$labelPosition)).hashCode());
        Font $labelFont = this.getLabelFont();
        result = result * 59 + ($labelFont == null ? 43 : ((Object)$labelFont).hashCode());
        String $labelColor = this.getLabelColor();
        result = result * 59 + ($labelColor == null ? 43 : $labelColor.hashCode());
        String $borderColor = this.getBorderColor();
        result = result * 59 + ($borderColor == null ? 43 : $borderColor.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportPieChartSettings(showLabel=" + this.getShowLabel() + ", labelPosition=" + String.valueOf((Object)this.getLabelPosition()) + ", labelFont=" + String.valueOf(this.getLabelFont()) + ", labelColor=" + this.getLabelColor() + ", borderWidth=" + this.getBorderWidth() + ", borderColor=" + this.getBorderColor() + ", radius=" + this.getRadius() + ", clockwise=" + this.getClockwise() + ")";
    }
}

