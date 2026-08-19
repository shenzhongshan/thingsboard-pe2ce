/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.timewindow;

import lombok.Generated;

public class FixedTimeWindow {
    private long startTimeMs;
    private long endTimeMs;

    @Generated
    public FixedTimeWindow() {
    }

    @Generated
    public long getStartTimeMs() {
        return this.startTimeMs;
    }

    @Generated
    public long getEndTimeMs() {
        return this.endTimeMs;
    }

    @Generated
    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    @Generated
    public void setEndTimeMs(long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FixedTimeWindow)) {
            return false;
        }
        FixedTimeWindow other = (FixedTimeWindow)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getStartTimeMs() != other.getStartTimeMs()) {
            return false;
        }
        return this.getEndTimeMs() == other.getEndTimeMs();
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof FixedTimeWindow;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $startTimeMs = this.getStartTimeMs();
        result = result * 59 + (int)($startTimeMs >>> 32 ^ $startTimeMs);
        long $endTimeMs = this.getEndTimeMs();
        result = result * 59 + (int)($endTimeMs >>> 32 ^ $endTimeMs);
        return result;
    }

    @Generated
    public String toString() {
        return "FixedTimeWindow(startTimeMs=" + this.getStartTimeMs() + ", endTimeMs=" + this.getEndTimeMs() + ")";
    }
}

