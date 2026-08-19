/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.timewindow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.timewindow.AggregationConfiguration;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;

@JsonIgnoreProperties(ignoreUnknown=true)
public class TimeWindowConfiguration {
    private History history;
    private AggregationConfiguration aggregation;
    private String timezone;

    @Generated
    public TimeWindowConfiguration() {
    }

    @Generated
    public History getHistory() {
        return this.history;
    }

    @Generated
    public AggregationConfiguration getAggregation() {
        return this.aggregation;
    }

    @Generated
    public String getTimezone() {
        return this.timezone;
    }

    @Generated
    public void setHistory(History history) {
        this.history = history;
    }

    @Generated
    public void setAggregation(AggregationConfiguration aggregation) {
        this.aggregation = aggregation;
    }

    @Generated
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeWindowConfiguration)) {
            return false;
        }
        TimeWindowConfiguration other = (TimeWindowConfiguration)o;
        if (!other.canEqual(this)) {
            return false;
        }
        History this$history = this.getHistory();
        History other$history = other.getHistory();
        if (this$history == null ? other$history != null : !((Object)this$history).equals(other$history)) {
            return false;
        }
        AggregationConfiguration this$aggregation = this.getAggregation();
        AggregationConfiguration other$aggregation = other.getAggregation();
        if (this$aggregation == null ? other$aggregation != null : !((Object)this$aggregation).equals(other$aggregation)) {
            return false;
        }
        String this$timezone = this.getTimezone();
        String other$timezone = other.getTimezone();
        return !(this$timezone == null ? other$timezone != null : !this$timezone.equals(other$timezone));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeWindowConfiguration;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        History $history = this.getHistory();
        result = result * 59 + ($history == null ? 43 : ((Object)$history).hashCode());
        AggregationConfiguration $aggregation = this.getAggregation();
        result = result * 59 + ($aggregation == null ? 43 : ((Object)$aggregation).hashCode());
        String $timezone = this.getTimezone();
        result = result * 59 + ($timezone == null ? 43 : $timezone.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeWindowConfiguration(history=" + String.valueOf(this.getHistory()) + ", aggregation=" + String.valueOf(this.getAggregation()) + ", timezone=" + this.getTimezone() + ")";
    }
}

