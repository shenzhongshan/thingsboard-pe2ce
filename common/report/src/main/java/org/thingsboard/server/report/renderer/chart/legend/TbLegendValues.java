/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Generated;

public class TbLegendValues {
    private String min;
    private String max;
    private String avg;
    private String total;
    private String latest;

    @Generated
    public TbLegendValues() {
    }

    @Generated
    public String getMin() {
        return this.min;
    }

    @Generated
    public String getMax() {
        return this.max;
    }

    @Generated
    public String getAvg() {
        return this.avg;
    }

    @Generated
    public String getTotal() {
        return this.total;
    }

    @Generated
    public String getLatest() {
        return this.latest;
    }

    @Generated
    public void setMin(String min) {
        this.min = min;
    }

    @Generated
    public void setMax(String max) {
        this.max = max;
    }

    @Generated
    public void setAvg(String avg) {
        this.avg = avg;
    }

    @Generated
    public void setTotal(String total) {
        this.total = total;
    }

    @Generated
    public void setLatest(String latest) {
        this.latest = latest;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbLegendValues)) {
            return false;
        }
        TbLegendValues other = (TbLegendValues)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$min = this.getMin();
        String other$min = other.getMin();
        if (this$min == null ? other$min != null : !this$min.equals(other$min)) {
            return false;
        }
        String this$max = this.getMax();
        String other$max = other.getMax();
        if (this$max == null ? other$max != null : !this$max.equals(other$max)) {
            return false;
        }
        String this$avg = this.getAvg();
        String other$avg = other.getAvg();
        if (this$avg == null ? other$avg != null : !this$avg.equals(other$avg)) {
            return false;
        }
        String this$total = this.getTotal();
        String other$total = other.getTotal();
        if (this$total == null ? other$total != null : !this$total.equals(other$total)) {
            return false;
        }
        String this$latest = this.getLatest();
        String other$latest = other.getLatest();
        return !(this$latest == null ? other$latest != null : !this$latest.equals(other$latest));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbLegendValues;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $min = this.getMin();
        result = result * 59 + ($min == null ? 43 : $min.hashCode());
        String $max = this.getMax();
        result = result * 59 + ($max == null ? 43 : $max.hashCode());
        String $avg = this.getAvg();
        result = result * 59 + ($avg == null ? 43 : $avg.hashCode());
        String $total = this.getTotal();
        result = result * 59 + ($total == null ? 43 : $total.hashCode());
        String $latest = this.getLatest();
        result = result * 59 + ($latest == null ? 43 : $latest.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbLegendValues(min=" + this.getMin() + ", max=" + this.getMax() + ", avg=" + this.getAvg() + ", total=" + this.getTotal() + ", latest=" + this.getLatest() + ")";
    }
}

