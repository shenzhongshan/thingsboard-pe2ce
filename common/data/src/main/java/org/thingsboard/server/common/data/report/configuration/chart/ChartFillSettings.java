/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettingsGradient;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;

public class ChartFillSettings {
    private ChartFillType type;
    private Float opacity;
    private ChartFillSettingsGradient gradient;

    public ChartFillSettings() {
    }

    public ChartFillSettings(ChartFillSettings input) {
        if (input == null) {
            input = new ChartFillSettings();
        }
        this.type = input.getType() != null ? input.getType() : ChartFillType.none;
        this.opacity = Float.valueOf(input.getOpacity() != null ? input.getOpacity().floatValue() : 0.4f);
        this.gradient = new ChartFillSettingsGradient(input.getGradient());
    }

    @Generated
    public ChartFillType getType() {
        return this.type;
    }

    @Generated
    public Float getOpacity() {
        return this.opacity;
    }

    @Generated
    public ChartFillSettingsGradient getGradient() {
        return this.gradient;
    }

    @Generated
    public void setType(ChartFillType type) {
        this.type = type;
    }

    @Generated
    public void setOpacity(Float opacity) {
        this.opacity = opacity;
    }

    @Generated
    public void setGradient(ChartFillSettingsGradient gradient) {
        this.gradient = gradient;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ChartFillSettings)) {
            return false;
        }
        ChartFillSettings other = (ChartFillSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Float this$opacity = this.getOpacity();
        Float other$opacity = other.getOpacity();
        if (this$opacity == null ? other$opacity != null : !((Object)this$opacity).equals(other$opacity)) {
            return false;
        }
        ChartFillType this$type = this.getType();
        ChartFillType other$type = other.getType();
        if (this$type == null ? other$type != null : !((Object)((Object)this$type)).equals((Object)other$type)) {
            return false;
        }
        ChartFillSettingsGradient this$gradient = this.getGradient();
        ChartFillSettingsGradient other$gradient = other.getGradient();
        return !(this$gradient == null ? other$gradient != null : !((Object)this$gradient).equals(other$gradient));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ChartFillSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Float $opacity = this.getOpacity();
        result = result * 59 + ($opacity == null ? 43 : ((Object)$opacity).hashCode());
        ChartFillType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)((Object)$type)).hashCode());
        ChartFillSettingsGradient $gradient = this.getGradient();
        result = result * 59 + ($gradient == null ? 43 : ((Object)$gradient).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ChartFillSettings(type=" + String.valueOf((Object)this.getType()) + ", opacity=" + this.getOpacity() + ", gradient=" + String.valueOf(this.getGradient()) + ")";
    }
}

