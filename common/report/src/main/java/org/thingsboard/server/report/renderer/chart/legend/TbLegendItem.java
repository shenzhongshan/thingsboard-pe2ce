/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.jfree.chart.LegendItem
 */
package org.thingsboard.server.report.renderer.chart.legend;

import lombok.Generated;
import org.jfree.chart.LegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;

public class TbLegendItem {
    private final LegendItem legendItem;
    private final TbLegendValues legendValues;

    public TbLegendItem(LegendItem legendItem, TbLegendValues legendValues) {
        this.legendItem = legendItem;
        this.legendValues = legendValues;
    }

    @Generated
    public LegendItem getLegendItem() {
        return this.legendItem;
    }

    @Generated
    public TbLegendValues getLegendValues() {
        return this.legendValues;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbLegendItem)) {
            return false;
        }
        TbLegendItem other = (TbLegendItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        LegendItem this$legendItem = this.getLegendItem();
        LegendItem other$legendItem = other.getLegendItem();
        if (this$legendItem == null ? other$legendItem != null : !this$legendItem.equals(other$legendItem)) {
            return false;
        }
        TbLegendValues this$legendValues = this.getLegendValues();
        TbLegendValues other$legendValues = other.getLegendValues();
        return !(this$legendValues == null ? other$legendValues != null : !((Object)this$legendValues).equals(other$legendValues));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbLegendItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        LegendItem $legendItem = this.getLegendItem();
        result = result * 59 + ($legendItem == null ? 43 : $legendItem.hashCode());
        TbLegendValues $legendValues = this.getLegendValues();
        result = result * 59 + ($legendValues == null ? 43 : ((Object)$legendValues).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbLegendItem(legendItem=" + String.valueOf(this.getLegendItem()) + ", legendValues=" + String.valueOf(this.getLegendValues()) + ")";
    }
}

