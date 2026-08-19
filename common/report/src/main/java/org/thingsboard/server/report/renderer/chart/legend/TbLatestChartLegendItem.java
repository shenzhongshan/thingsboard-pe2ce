/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Generated;

public class TbLatestChartLegendItem {
    private String color;
    private String label;
    private String value;
    private boolean hasValue;
    private boolean total;

    @Generated
    public TbLatestChartLegendItem() {
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public String getValue() {
        return this.value;
    }

    @Generated
    public boolean isHasValue() {
        return this.hasValue;
    }

    @Generated
    public boolean isTotal() {
        return this.total;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setValue(String value) {
        this.value = value;
    }

    @Generated
    public void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
    }

    @Generated
    public void setTotal(boolean total) {
        this.total = total;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbLatestChartLegendItem)) {
            return false;
        }
        TbLatestChartLegendItem other = (TbLatestChartLegendItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isHasValue() != other.isHasValue()) {
            return false;
        }
        if (this.isTotal() != other.isTotal()) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        if (this$color == null ? other$color != null : !this$color.equals(other$color)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
            return false;
        }
        String this$value = this.getValue();
        String other$value = other.getValue();
        return !(this$value == null ? other$value != null : !this$value.equals(other$value));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbLatestChartLegendItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isHasValue() ? 79 : 97);
        result = result * 59 + (this.isTotal() ? 79 : 97);
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        String $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : $value.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbLatestChartLegendItem(color=" + this.getColor() + ", label=" + this.getLabel() + ", value=" + this.getValue() + ", hasValue=" + this.isHasValue() + ", total=" + this.isTotal() + ")";
    }
}

