/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 */
package org.thingsboard.server.report.context.chart;

import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;

public class TsChartSeriesEntry {
    private final long ts;
    private final TimeIntervalCalculator.TimeRange interval;
    private final String value;
    private final Double doubleValue;

    @Generated
    public long getTs() {
        return this.ts;
    }

    @Generated
    public TimeIntervalCalculator.TimeRange getInterval() {
        return this.interval;
    }

    @Generated
    public String getValue() {
        return this.value;
    }

    @Generated
    public Double getDoubleValue() {
        return this.doubleValue;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TsChartSeriesEntry)) {
            return false;
        }
        TsChartSeriesEntry other = (TsChartSeriesEntry)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getTs() != other.getTs()) {
            return false;
        }
        Double this$doubleValue = this.getDoubleValue();
        Double other$doubleValue = other.getDoubleValue();
        if (this$doubleValue == null ? other$doubleValue != null : !((Object)this$doubleValue).equals(other$doubleValue)) {
            return false;
        }
        TimeIntervalCalculator.TimeRange this$interval = this.getInterval();
        TimeIntervalCalculator.TimeRange other$interval = other.getInterval();
        if (this$interval == null ? other$interval != null : !this$interval.equals(other$interval)) {
            return false;
        }
        String this$value = this.getValue();
        String other$value = other.getValue();
        return !(this$value == null ? other$value != null : !this$value.equals(other$value));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TsChartSeriesEntry;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $ts = this.getTs();
        result = result * 59 + (int)($ts >>> 32 ^ $ts);
        Double $doubleValue = this.getDoubleValue();
        result = result * 59 + ($doubleValue == null ? 43 : ((Object)$doubleValue).hashCode());
        TimeIntervalCalculator.TimeRange $interval = this.getInterval();
        result = result * 59 + ($interval == null ? 43 : $interval.hashCode());
        String $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : $value.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TsChartSeriesEntry(ts=" + this.getTs() + ", interval=" + String.valueOf(this.getInterval()) + ", value=" + this.getValue() + ", doubleValue=" + this.getDoubleValue() + ")";
    }

    @ConstructorProperties(value={"ts", "interval", "value", "doubleValue"})
    @Generated
    public TsChartSeriesEntry(long ts, TimeIntervalCalculator.TimeRange interval, String value, Double doubleValue) {
        this.ts = ts;
        this.interval = interval;
        this.value = value;
        this.doubleValue = doubleValue;
    }
}

