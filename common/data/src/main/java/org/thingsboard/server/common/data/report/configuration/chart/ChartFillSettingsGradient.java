/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;

public class ChartFillSettingsGradient {
    private Float start;
    private Float end;

    public ChartFillSettingsGradient() {
    }

    public ChartFillSettingsGradient(ChartFillSettingsGradient input) {
        if (input == null) {
            input = new ChartFillSettingsGradient();
        }
        this.start = Float.valueOf(input.getStart() != null ? input.getStart().floatValue() : 100.0f);
        this.end = Float.valueOf(input.getEnd() != null ? input.getEnd().floatValue() : 0.0f);
    }

    @Generated
    public Float getStart() {
        return this.start;
    }

    @Generated
    public Float getEnd() {
        return this.end;
    }

    @Generated
    public void setStart(Float start) {
        this.start = start;
    }

    @Generated
    public void setEnd(Float end) {
        this.end = end;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ChartFillSettingsGradient)) {
            return false;
        }
        ChartFillSettingsGradient other = (ChartFillSettingsGradient)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Float this$start = this.getStart();
        Float other$start = other.getStart();
        if (this$start == null ? other$start != null : !((Object)this$start).equals(other$start)) {
            return false;
        }
        Float this$end = this.getEnd();
        Float other$end = other.getEnd();
        return !(this$end == null ? other$end != null : !((Object)this$end).equals(other$end));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ChartFillSettingsGradient;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Float $start = this.getStart();
        result = result * 59 + ($start == null ? 43 : ((Object)$start).hashCode());
        Float $end = this.getEnd();
        result = result * 59 + ($end == null ? 43 : ((Object)$end).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ChartFillSettingsGradient(start=" + this.getStart() + ", end=" + this.getEnd() + ")";
    }
}

