/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

public abstract class TimeSeriesChartAxisSettings {
    private Boolean show;
    private String label;
    private Font labelFont;
    private String labelColor;
    private AxisPosition position;
    private Boolean showTickLabels;
    private Font tickLabelFont;
    private String tickLabelColor;
    private Boolean showTicks;
    private String ticksColor;
    private Boolean showLine;
    private String lineColor;
    private Boolean showSplitLines;
    private String splitLinesColor;

    protected TimeSeriesChartAxisSettings() {
    }

    protected TimeSeriesChartAxisSettings(TimeSeriesChartAxisSettings input) {
        this.show = input.getShow() != null ? input.getShow() : Boolean.TRUE;
        this.label = input.getLabel() != null ? input.getLabel() : "";
        this.labelFont = input.getLabelFont() != null ? input.getLabelFont() : Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.BOLD).style(FontStyle.NORMAL).build();
        this.labelColor = input.getLabelColor() != null ? input.getLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showTickLabels = input.getShowTickLabels() != null ? input.getShowTickLabels() : Boolean.TRUE;
        this.tickLabelColor = input.getTickLabelColor() != null ? input.getTickLabelColor() : "rgba(0, 0, 0, 0.54)";
        this.showTicks = input.getShowTicks() != null ? input.getShowTicks() : Boolean.TRUE;
        this.ticksColor = input.getTicksColor() != null ? input.getTicksColor() : "rgba(0, 0, 0, 0.54)";
        this.showLine = input.getShowLine() != null ? input.getShowLine() : Boolean.TRUE;
        this.lineColor = input.getLineColor() != null ? input.getLineColor() : "rgba(0, 0, 0, 0.54)";
        this.showSplitLines = input.getShowSplitLines() != null ? input.getShowSplitLines() : Boolean.TRUE;
        this.splitLinesColor = input.getShowSplitLines() != null ? input.getSplitLinesColor() : "rgba(0, 0, 0, 0.12)";
    }

    @Generated
    public Boolean getShow() {
        return this.show;
    }

    @Generated
    public String getLabel() {
        return this.label;
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
    public AxisPosition getPosition() {
        return this.position;
    }

    @Generated
    public Boolean getShowTickLabels() {
        return this.showTickLabels;
    }

    @Generated
    public Font getTickLabelFont() {
        return this.tickLabelFont;
    }

    @Generated
    public String getTickLabelColor() {
        return this.tickLabelColor;
    }

    @Generated
    public Boolean getShowTicks() {
        return this.showTicks;
    }

    @Generated
    public String getTicksColor() {
        return this.ticksColor;
    }

    @Generated
    public Boolean getShowLine() {
        return this.showLine;
    }

    @Generated
    public String getLineColor() {
        return this.lineColor;
    }

    @Generated
    public Boolean getShowSplitLines() {
        return this.showSplitLines;
    }

    @Generated
    public String getSplitLinesColor() {
        return this.splitLinesColor;
    }

    @Generated
    public void setShow(Boolean show) {
        this.show = show;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
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
    public void setPosition(AxisPosition position) {
        this.position = position;
    }

    @Generated
    public void setShowTickLabels(Boolean showTickLabels) {
        this.showTickLabels = showTickLabels;
    }

    @Generated
    public void setTickLabelFont(Font tickLabelFont) {
        this.tickLabelFont = tickLabelFont;
    }

    @Generated
    public void setTickLabelColor(String tickLabelColor) {
        this.tickLabelColor = tickLabelColor;
    }

    @Generated
    public void setShowTicks(Boolean showTicks) {
        this.showTicks = showTicks;
    }

    @Generated
    public void setTicksColor(String ticksColor) {
        this.ticksColor = ticksColor;
    }

    @Generated
    public void setShowLine(Boolean showLine) {
        this.showLine = showLine;
    }

    @Generated
    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }

    @Generated
    public void setShowSplitLines(Boolean showSplitLines) {
        this.showSplitLines = showSplitLines;
    }

    @Generated
    public void setSplitLinesColor(String splitLinesColor) {
        this.splitLinesColor = splitLinesColor;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartAxisSettings)) {
            return false;
        }
        TimeSeriesChartAxisSettings other = (TimeSeriesChartAxisSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$show = this.getShow();
        Boolean other$show = other.getShow();
        if (this$show == null ? other$show != null : !((Object)this$show).equals(other$show)) {
            return false;
        }
        Boolean this$showTickLabels = this.getShowTickLabels();
        Boolean other$showTickLabels = other.getShowTickLabels();
        if (this$showTickLabels == null ? other$showTickLabels != null : !((Object)this$showTickLabels).equals(other$showTickLabels)) {
            return false;
        }
        Boolean this$showTicks = this.getShowTicks();
        Boolean other$showTicks = other.getShowTicks();
        if (this$showTicks == null ? other$showTicks != null : !((Object)this$showTicks).equals(other$showTicks)) {
            return false;
        }
        Boolean this$showLine = this.getShowLine();
        Boolean other$showLine = other.getShowLine();
        if (this$showLine == null ? other$showLine != null : !((Object)this$showLine).equals(other$showLine)) {
            return false;
        }
        Boolean this$showSplitLines = this.getShowSplitLines();
        Boolean other$showSplitLines = other.getShowSplitLines();
        if (this$showSplitLines == null ? other$showSplitLines != null : !((Object)this$showSplitLines).equals(other$showSplitLines)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
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
        AxisPosition this$position = this.getPosition();
        AxisPosition other$position = other.getPosition();
        if (this$position == null ? other$position != null : !((Object)((Object)this$position)).equals((Object)other$position)) {
            return false;
        }
        Font this$tickLabelFont = this.getTickLabelFont();
        Font other$tickLabelFont = other.getTickLabelFont();
        if (this$tickLabelFont == null ? other$tickLabelFont != null : !((Object)this$tickLabelFont).equals(other$tickLabelFont)) {
            return false;
        }
        String this$tickLabelColor = this.getTickLabelColor();
        String other$tickLabelColor = other.getTickLabelColor();
        if (this$tickLabelColor == null ? other$tickLabelColor != null : !this$tickLabelColor.equals(other$tickLabelColor)) {
            return false;
        }
        String this$ticksColor = this.getTicksColor();
        String other$ticksColor = other.getTicksColor();
        if (this$ticksColor == null ? other$ticksColor != null : !this$ticksColor.equals(other$ticksColor)) {
            return false;
        }
        String this$lineColor = this.getLineColor();
        String other$lineColor = other.getLineColor();
        if (this$lineColor == null ? other$lineColor != null : !this$lineColor.equals(other$lineColor)) {
            return false;
        }
        String this$splitLinesColor = this.getSplitLinesColor();
        String other$splitLinesColor = other.getSplitLinesColor();
        return !(this$splitLinesColor == null ? other$splitLinesColor != null : !this$splitLinesColor.equals(other$splitLinesColor));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartAxisSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $show = this.getShow();
        result = result * 59 + ($show == null ? 43 : ((Object)$show).hashCode());
        Boolean $showTickLabels = this.getShowTickLabels();
        result = result * 59 + ($showTickLabels == null ? 43 : ((Object)$showTickLabels).hashCode());
        Boolean $showTicks = this.getShowTicks();
        result = result * 59 + ($showTicks == null ? 43 : ((Object)$showTicks).hashCode());
        Boolean $showLine = this.getShowLine();
        result = result * 59 + ($showLine == null ? 43 : ((Object)$showLine).hashCode());
        Boolean $showSplitLines = this.getShowSplitLines();
        result = result * 59 + ($showSplitLines == null ? 43 : ((Object)$showSplitLines).hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        Font $labelFont = this.getLabelFont();
        result = result * 59 + ($labelFont == null ? 43 : ((Object)$labelFont).hashCode());
        String $labelColor = this.getLabelColor();
        result = result * 59 + ($labelColor == null ? 43 : $labelColor.hashCode());
        AxisPosition $position = this.getPosition();
        result = result * 59 + ($position == null ? 43 : ((Object)((Object)$position)).hashCode());
        Font $tickLabelFont = this.getTickLabelFont();
        result = result * 59 + ($tickLabelFont == null ? 43 : ((Object)$tickLabelFont).hashCode());
        String $tickLabelColor = this.getTickLabelColor();
        result = result * 59 + ($tickLabelColor == null ? 43 : $tickLabelColor.hashCode());
        String $ticksColor = this.getTicksColor();
        result = result * 59 + ($ticksColor == null ? 43 : $ticksColor.hashCode());
        String $lineColor = this.getLineColor();
        result = result * 59 + ($lineColor == null ? 43 : $lineColor.hashCode());
        String $splitLinesColor = this.getSplitLinesColor();
        result = result * 59 + ($splitLinesColor == null ? 43 : $splitLinesColor.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartAxisSettings(show=" + this.getShow() + ", label=" + this.getLabel() + ", labelFont=" + String.valueOf(this.getLabelFont()) + ", labelColor=" + this.getLabelColor() + ", position=" + String.valueOf((Object)this.getPosition()) + ", showTickLabels=" + this.getShowTickLabels() + ", tickLabelFont=" + String.valueOf(this.getTickLabelFont()) + ", tickLabelColor=" + this.getTickLabelColor() + ", showTicks=" + this.getShowTicks() + ", ticksColor=" + this.getTicksColor() + ", showLine=" + this.getShowLine() + ", lineColor=" + this.getLineColor() + ", showSplitLines=" + this.getShowSplitLines() + ", splitLinesColor=" + this.getSplitLinesColor() + ")";
    }
}

