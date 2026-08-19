/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.timewindow;

import lombok.Generated;
import org.thingsboard.server.common.data.kv.Aggregation;

public class AggregationConfiguration {
    private Aggregation type;
    private int limit;

    @Generated
    public AggregationConfiguration() {
    }

    @Generated
    public Aggregation getType() {
        return this.type;
    }

    @Generated
    public int getLimit() {
        return this.limit;
    }

    @Generated
    public void setType(Aggregation type) {
        this.type = type;
    }

    @Generated
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AggregationConfiguration)) {
            return false;
        }
        AggregationConfiguration other = (AggregationConfiguration)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getLimit() != other.getLimit()) {
            return false;
        }
        Aggregation this$type = this.getType();
        Aggregation other$type = other.getType();
        return !(this$type == null ? other$type != null : !((Object)((Object)this$type)).equals((Object)other$type));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AggregationConfiguration;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getLimit();
        Aggregation $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)((Object)$type)).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "AggregationConfiguration(type=" + String.valueOf((Object)this.getType()) + ", limit=" + this.getLimit() + ")";
    }
}

