/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;

public class LegendConfig {
    private LegendPosition position;
    private Boolean sortDataKeys;
    private Boolean showMin;
    private Boolean showMax;
    private Boolean showAvg;
    private Boolean showTotal;
    private Boolean showLatest;

    public LegendConfig() {
    }

    public LegendConfig(LegendConfig input) {
        if (input == null) {
            input = new LegendConfig();
        }
        this.position = input.getPosition() != null ? input.getPosition() : LegendPosition.top;
        this.sortDataKeys = input.getSortDataKeys() != null ? input.getSortDataKeys() : Boolean.FALSE;
        this.showMin = input.getShowMin() != null ? input.getShowMin() : Boolean.FALSE;
        this.showMax = input.getShowMax() != null ? input.getShowMax() : Boolean.FALSE;
        this.showAvg = input.getShowAvg() != null ? input.getShowAvg() : Boolean.FALSE;
        this.showTotal = input.getShowTotal() != null ? input.getShowTotal() : Boolean.FALSE;
        this.showLatest = input.getShowLatest() != null ? input.getShowLatest() : Boolean.FALSE;
    }

    @JsonIgnore
    public boolean isSimpleLegend() {
        return this.showMin == false && this.showMax == false && this.showAvg == false && this.showTotal == false && this.showLatest == false;
    }

    @Generated
    public LegendPosition getPosition() {
        return this.position;
    }

    @Generated
    public Boolean getSortDataKeys() {
        return this.sortDataKeys;
    }

    @Generated
    public Boolean getShowMin() {
        return this.showMin;
    }

    @Generated
    public Boolean getShowMax() {
        return this.showMax;
    }

    @Generated
    public Boolean getShowAvg() {
        return this.showAvg;
    }

    @Generated
    public Boolean getShowTotal() {
        return this.showTotal;
    }

    @Generated
    public Boolean getShowLatest() {
        return this.showLatest;
    }

    @Generated
    public void setPosition(LegendPosition position) {
        this.position = position;
    }

    @Generated
    public void setSortDataKeys(Boolean sortDataKeys) {
        this.sortDataKeys = sortDataKeys;
    }

    @Generated
    public void setShowMin(Boolean showMin) {
        this.showMin = showMin;
    }

    @Generated
    public void setShowMax(Boolean showMax) {
        this.showMax = showMax;
    }

    @Generated
    public void setShowAvg(Boolean showAvg) {
        this.showAvg = showAvg;
    }

    @Generated
    public void setShowTotal(Boolean showTotal) {
        this.showTotal = showTotal;
    }

    @Generated
    public void setShowLatest(Boolean showLatest) {
        this.showLatest = showLatest;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LegendConfig)) {
            return false;
        }
        LegendConfig other = (LegendConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$sortDataKeys = this.getSortDataKeys();
        Boolean other$sortDataKeys = other.getSortDataKeys();
        if (this$sortDataKeys == null ? other$sortDataKeys != null : !((Object)this$sortDataKeys).equals(other$sortDataKeys)) {
            return false;
        }
        Boolean this$showMin = this.getShowMin();
        Boolean other$showMin = other.getShowMin();
        if (this$showMin == null ? other$showMin != null : !((Object)this$showMin).equals(other$showMin)) {
            return false;
        }
        Boolean this$showMax = this.getShowMax();
        Boolean other$showMax = other.getShowMax();
        if (this$showMax == null ? other$showMax != null : !((Object)this$showMax).equals(other$showMax)) {
            return false;
        }
        Boolean this$showAvg = this.getShowAvg();
        Boolean other$showAvg = other.getShowAvg();
        if (this$showAvg == null ? other$showAvg != null : !((Object)this$showAvg).equals(other$showAvg)) {
            return false;
        }
        Boolean this$showTotal = this.getShowTotal();
        Boolean other$showTotal = other.getShowTotal();
        if (this$showTotal == null ? other$showTotal != null : !((Object)this$showTotal).equals(other$showTotal)) {
            return false;
        }
        Boolean this$showLatest = this.getShowLatest();
        Boolean other$showLatest = other.getShowLatest();
        if (this$showLatest == null ? other$showLatest != null : !((Object)this$showLatest).equals(other$showLatest)) {
            return false;
        }
        LegendPosition this$position = this.getPosition();
        LegendPosition other$position = other.getPosition();
        return !(this$position == null ? other$position != null : !((Object)((Object)this$position)).equals((Object)other$position));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LegendConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $sortDataKeys = this.getSortDataKeys();
        result = result * 59 + ($sortDataKeys == null ? 43 : ((Object)$sortDataKeys).hashCode());
        Boolean $showMin = this.getShowMin();
        result = result * 59 + ($showMin == null ? 43 : ((Object)$showMin).hashCode());
        Boolean $showMax = this.getShowMax();
        result = result * 59 + ($showMax == null ? 43 : ((Object)$showMax).hashCode());
        Boolean $showAvg = this.getShowAvg();
        result = result * 59 + ($showAvg == null ? 43 : ((Object)$showAvg).hashCode());
        Boolean $showTotal = this.getShowTotal();
        result = result * 59 + ($showTotal == null ? 43 : ((Object)$showTotal).hashCode());
        Boolean $showLatest = this.getShowLatest();
        result = result * 59 + ($showLatest == null ? 43 : ((Object)$showLatest).hashCode());
        LegendPosition $position = this.getPosition();
        result = result * 59 + ($position == null ? 43 : ((Object)((Object)$position)).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "LegendConfig(position=" + String.valueOf((Object)this.getPosition()) + ", sortDataKeys=" + this.getSortDataKeys() + ", showMin=" + this.getShowMin() + ", showMax=" + this.getShowMax() + ", showAvg=" + this.getShowAvg() + ", showTotal=" + this.getShowTotal() + ", showLatest=" + this.getShowLatest() + ")";
    }
}

