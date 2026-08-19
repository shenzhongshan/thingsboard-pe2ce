/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Generated;

public class DataKeyComparisonSettings {
    private Boolean showValuesForComparison;
    private String comparisonValuesLabel;
    private String color;

    public DataKeyComparisonSettings() {
    }

    public DataKeyComparisonSettings(DataKeyComparisonSettings input) {
        if (input == null) {
            input = new DataKeyComparisonSettings();
        }
        this.showValuesForComparison = input.getShowValuesForComparison() != null ? input.getShowValuesForComparison() : Boolean.FALSE;
        this.comparisonValuesLabel = input.getComparisonValuesLabel() != null ? input.getComparisonValuesLabel() : "";
        this.color = input.getColor() != null ? input.getColor() : "";
    }

    @Generated
    public Boolean getShowValuesForComparison() {
        return this.showValuesForComparison;
    }

    @Generated
    public String getComparisonValuesLabel() {
        return this.comparisonValuesLabel;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public void setShowValuesForComparison(Boolean showValuesForComparison) {
        this.showValuesForComparison = showValuesForComparison;
    }

    @Generated
    public void setComparisonValuesLabel(String comparisonValuesLabel) {
        this.comparisonValuesLabel = comparisonValuesLabel;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DataKeyComparisonSettings)) {
            return false;
        }
        DataKeyComparisonSettings other = (DataKeyComparisonSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$showValuesForComparison = this.getShowValuesForComparison();
        Boolean other$showValuesForComparison = other.getShowValuesForComparison();
        if (this$showValuesForComparison == null ? other$showValuesForComparison != null : !((Object)this$showValuesForComparison).equals(other$showValuesForComparison)) {
            return false;
        }
        String this$comparisonValuesLabel = this.getComparisonValuesLabel();
        String other$comparisonValuesLabel = other.getComparisonValuesLabel();
        if (this$comparisonValuesLabel == null ? other$comparisonValuesLabel != null : !this$comparisonValuesLabel.equals(other$comparisonValuesLabel)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        return !(this$color == null ? other$color != null : !this$color.equals(other$color));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DataKeyComparisonSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $showValuesForComparison = this.getShowValuesForComparison();
        result = result * 59 + ($showValuesForComparison == null ? 43 : ((Object)$showValuesForComparison).hashCode());
        String $comparisonValuesLabel = this.getComparisonValuesLabel();
        result = result * 59 + ($comparisonValuesLabel == null ? 43 : $comparisonValuesLabel.hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "DataKeyComparisonSettings(showValuesForComparison=" + this.getShowValuesForComparison() + ", comparisonValuesLabel=" + this.getComparisonValuesLabel() + ", color=" + this.getColor() + ")";
    }
}

