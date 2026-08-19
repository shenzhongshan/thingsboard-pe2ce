/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Generated;

public class TbLegendValuesRequest {
    private boolean min;
    private boolean max;
    private boolean avg;
    private boolean total;
    private boolean latest;

    @Generated
    public TbLegendValuesRequest() {
    }

    @Generated
    public boolean isMin() {
        return this.min;
    }

    @Generated
    public boolean isMax() {
        return this.max;
    }

    @Generated
    public boolean isAvg() {
        return this.avg;
    }

    @Generated
    public boolean isTotal() {
        return this.total;
    }

    @Generated
    public boolean isLatest() {
        return this.latest;
    }

    @Generated
    public void setMin(boolean min) {
        this.min = min;
    }

    @Generated
    public void setMax(boolean max) {
        this.max = max;
    }

    @Generated
    public void setAvg(boolean avg) {
        this.avg = avg;
    }

    @Generated
    public void setTotal(boolean total) {
        this.total = total;
    }

    @Generated
    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbLegendValuesRequest)) {
            return false;
        }
        TbLegendValuesRequest other = (TbLegendValuesRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isMin() != other.isMin()) {
            return false;
        }
        if (this.isMax() != other.isMax()) {
            return false;
        }
        if (this.isAvg() != other.isAvg()) {
            return false;
        }
        if (this.isTotal() != other.isTotal()) {
            return false;
        }
        return this.isLatest() == other.isLatest();
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbLegendValuesRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isMin() ? 79 : 97);
        result = result * 59 + (this.isMax() ? 79 : 97);
        result = result * 59 + (this.isAvg() ? 79 : 97);
        result = result * 59 + (this.isTotal() ? 79 : 97);
        result = result * 59 + (this.isLatest() ? 79 : 97);
        return result;
    }

    @Generated
    public String toString() {
        return "TbLegendValuesRequest(min=" + this.isMin() + ", max=" + this.isMax() + ", avg=" + this.isAvg() + ", total=" + this.isTotal() + ", latest=" + this.isLatest() + ")";
    }
}

