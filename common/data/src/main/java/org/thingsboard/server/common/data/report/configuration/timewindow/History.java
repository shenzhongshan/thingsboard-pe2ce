/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.timewindow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.timewindow.FixedTimeWindow;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;

@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class History {
    private int historyType;
    private Interval interval;
    private long timewindowMs;
    private FixedTimeWindow fixedTimewindow;
    private QuickTimeInterval quickInterval;

    @Generated
    public History() {
    }

    @Generated
    public int getHistoryType() {
        return this.historyType;
    }

    @Generated
    public Interval getInterval() {
        return this.interval;
    }

    @Generated
    public long getTimewindowMs() {
        return this.timewindowMs;
    }

    @Generated
    public FixedTimeWindow getFixedTimewindow() {
        return this.fixedTimewindow;
    }

    @Generated
    public QuickTimeInterval getQuickInterval() {
        return this.quickInterval;
    }

    @Generated
    public void setHistoryType(int historyType) {
        this.historyType = historyType;
    }

    @Generated
    public void setInterval(Interval interval) {
        this.interval = interval;
    }

    @Generated
    public void setTimewindowMs(long timewindowMs) {
        this.timewindowMs = timewindowMs;
    }

    @Generated
    public void setFixedTimewindow(FixedTimeWindow fixedTimewindow) {
        this.fixedTimewindow = fixedTimewindow;
    }

    @Generated
    public void setQuickInterval(QuickTimeInterval quickInterval) {
        this.quickInterval = quickInterval;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof History)) {
            return false;
        }
        History other = (History)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getHistoryType() != other.getHistoryType()) {
            return false;
        }
        if (this.getTimewindowMs() != other.getTimewindowMs()) {
            return false;
        }
        Interval this$interval = this.getInterval();
        Interval other$interval = other.getInterval();
        if (this$interval == null ? other$interval != null : !((Object)this$interval).equals(other$interval)) {
            return false;
        }
        FixedTimeWindow this$fixedTimewindow = this.getFixedTimewindow();
        FixedTimeWindow other$fixedTimewindow = other.getFixedTimewindow();
        if (this$fixedTimewindow == null ? other$fixedTimewindow != null : !((Object)this$fixedTimewindow).equals(other$fixedTimewindow)) {
            return false;
        }
        QuickTimeInterval this$quickInterval = this.getQuickInterval();
        QuickTimeInterval other$quickInterval = other.getQuickInterval();
        return !(this$quickInterval == null ? other$quickInterval != null : !((Object)((Object)this$quickInterval)).equals((Object)other$quickInterval));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof History;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getHistoryType();
        long $timewindowMs = this.getTimewindowMs();
        result = result * 59 + (int)($timewindowMs >>> 32 ^ $timewindowMs);
        Interval $interval = this.getInterval();
        result = result * 59 + ($interval == null ? 43 : ((Object)$interval).hashCode());
        FixedTimeWindow $fixedTimewindow = this.getFixedTimewindow();
        result = result * 59 + ($fixedTimewindow == null ? 43 : ((Object)$fixedTimewindow).hashCode());
        QuickTimeInterval $quickInterval = this.getQuickInterval();
        result = result * 59 + ($quickInterval == null ? 43 : ((Object)((Object)$quickInterval)).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "History(historyType=" + this.getHistoryType() + ", interval=" + String.valueOf(this.getInterval()) + ", timewindowMs=" + this.getTimewindowMs() + ", fixedTimewindow=" + String.valueOf(this.getFixedTimewindow()) + ", quickInterval=" + String.valueOf((Object)this.getQuickInterval()) + ")";
    }
}

