/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;

public class TimeSeriesChartGridSettings {
    private Boolean show;
    private String backgroundColor;
    private Float borderWidth;
    private String borderColor;

    public TimeSeriesChartGridSettings() {
    }

    public TimeSeriesChartGridSettings(TimeSeriesChartGridSettings input) {
        if (input == null) {
            input = new TimeSeriesChartGridSettings();
        }
        this.show = input.getShow() != null ? input.getShow() : Boolean.FALSE;
        this.backgroundColor = input.getBackgroundColor() != null ? input.getBackgroundColor() : null;
        this.borderWidth = Float.valueOf(input.getBorderWidth() != null ? input.getBorderWidth().floatValue() : 1.0f);
        this.borderColor = input.getBorderColor() != null ? input.getBorderColor() : "#ccc";
    }

    @Generated
    public Boolean getShow() {
        return this.show;
    }

    @Generated
    public String getBackgroundColor() {
        return this.backgroundColor;
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
    public void setShow(Boolean show) {
        this.show = show;
    }

    @Generated
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeSeriesChartGridSettings)) {
            return false;
        }
        TimeSeriesChartGridSettings other = (TimeSeriesChartGridSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$show = this.getShow();
        Boolean other$show = other.getShow();
        if (this$show == null ? other$show != null : !((Object)this$show).equals(other$show)) {
            return false;
        }
        Float this$borderWidth = this.getBorderWidth();
        Float other$borderWidth = other.getBorderWidth();
        if (this$borderWidth == null ? other$borderWidth != null : !((Object)this$borderWidth).equals(other$borderWidth)) {
            return false;
        }
        String this$backgroundColor = this.getBackgroundColor();
        String other$backgroundColor = other.getBackgroundColor();
        if (this$backgroundColor == null ? other$backgroundColor != null : !this$backgroundColor.equals(other$backgroundColor)) {
            return false;
        }
        String this$borderColor = this.getBorderColor();
        String other$borderColor = other.getBorderColor();
        return !(this$borderColor == null ? other$borderColor != null : !this$borderColor.equals(other$borderColor));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeSeriesChartGridSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $show = this.getShow();
        result = result * 59 + ($show == null ? 43 : ((Object)$show).hashCode());
        Float $borderWidth = this.getBorderWidth();
        result = result * 59 + ($borderWidth == null ? 43 : ((Object)$borderWidth).hashCode());
        String $backgroundColor = this.getBackgroundColor();
        result = result * 59 + ($backgroundColor == null ? 43 : $backgroundColor.hashCode());
        String $borderColor = this.getBorderColor();
        result = result * 59 + ($borderColor == null ? 43 : $borderColor.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeSeriesChartGridSettings(show=" + this.getShow() + ", backgroundColor=" + this.getBackgroundColor() + ", borderWidth=" + this.getBorderWidth() + ", borderColor=" + this.getBorderColor() + ")";
    }
}

