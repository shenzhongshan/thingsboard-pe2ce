/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;

public class TimeSeriesChartBarWidth {
    private Boolean relative;
    private Double relativeWidth;
    private Double absoluteWidth;

    public TimeSeriesChartBarWidth() {
    }

    public TimeSeriesChartBarWidth(TimeSeriesChartBarWidth input) {
        if (input == null) {
            input = new TimeSeriesChartBarWidth();
        }
        this.relative = input.getRelative() != null ? input.getRelative() : Boolean.TRUE;
        this.relativeWidth = input.getRelativeWidth() != null ? input.getRelativeWidth() : 2.0;
        this.absoluteWidth = input.getAbsoluteWidth() != null ? input.getAbsoluteWidth() : 1000.0;
    }

    @Generated
    public Boolean getRelative() {
        return this.relative;
    }

    @Generated
    public Double getRelativeWidth() {
        return this.relativeWidth;
    }

    @Generated
    public Double getAbsoluteWidth() {
        return this.absoluteWidth;
    }

    @Generated
    public void setRelative(Boolean relative) {
        this.relative = relative;
    }

    @Generated
    public void setRelativeWidth(Double relativeWidth) {
        this.relativeWidth = relativeWidth;
    }

    @Generated
    public void setAbsoluteWidth(Double absoluteWidth) {
        this.absoluteWidth = absoluteWidth;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartBarWidth)) {
            return false;
        }
        TimeSeriesChartBarWidth other = (TimeSeriesChartBarWidth)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$relative = this.getRelative();
        Boolean other$relative = other.getRelative();
        if (this$relative == null ? other$relative != null : !((Object)this$relative).equals(other$relative)) {
            return false;
        }
        Double this$relativeWidth = this.getRelativeWidth();
        Double other$relativeWidth = other.getRelativeWidth();
        if (this$relativeWidth == null ? other$relativeWidth != null : !((Object)this$relativeWidth).equals(other$relativeWidth)) {
            return false;
        }
        Double this$absoluteWidth = this.getAbsoluteWidth();
        Double other$absoluteWidth = other.getAbsoluteWidth();
        return !(this$absoluteWidth == null ? other$absoluteWidth != null : !((Object)this$absoluteWidth).equals(other$absoluteWidth));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartBarWidth;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $relative = this.getRelative();
        result = result * 59 + ($relative == null ? 43 : ((Object)$relative).hashCode());
        Double $relativeWidth = this.getRelativeWidth();
        result = result * 59 + ($relativeWidth == null ? 43 : ((Object)$relativeWidth).hashCode());
        Double $absoluteWidth = this.getAbsoluteWidth();
        result = result * 59 + ($absoluteWidth == null ? 43 : ((Object)$absoluteWidth).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartBarWidth(relative=" + this.getRelative() + ", relativeWidth=" + this.getRelativeWidth() + ", absoluteWidth=" + this.getAbsoluteWidth() + ")";
    }
}

