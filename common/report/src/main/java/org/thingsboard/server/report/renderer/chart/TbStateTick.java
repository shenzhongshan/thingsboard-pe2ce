/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.renderer.chart;

import java.beans.ConstructorProperties;
import lombok.Generated;

public class TbStateTick {
    private Double value;
    private String label;

    @Generated
    public Double getValue() {
        return this.value;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public void setValue(Double value) {
        this.value = value;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbStateTick)) {
            return false;
        }
        TbStateTick other = (TbStateTick)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$value = this.getValue();
        Double other$value = other.getValue();
        if (this$value == null ? other$value != null : !((Object)this$value).equals(other$value)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        return !(this$label == null ? other$label != null : !this$label.equals(other$label));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbStateTick;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : ((Object)$value).hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbStateTick(value=" + this.getValue() + ", label=" + this.getLabel() + ")";
    }

    @ConstructorProperties(value={"value", "label"})
    @Generated
    public TbStateTick(Double value, String label) {
        this.value = value;
        this.label = label;
    }
}

