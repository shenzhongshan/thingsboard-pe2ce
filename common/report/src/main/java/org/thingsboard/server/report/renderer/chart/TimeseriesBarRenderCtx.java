/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy
 */
package org.thingsboard.server.report.renderer.chart;

import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;

public class TimeseriesBarRenderCtx {
    private double barGap;
    private double intervalGap;
    private TimeSeriesChartNoAggregationBarWidthStrategy noAggregationBarWidthStrategy;
    private boolean noAggregationWidthRelative;
    private double noAggregationWidth;
    private long timeWindow;
    private boolean noAggregation;

    @ConstructorProperties(value={"barGap", "intervalGap", "noAggregationBarWidthStrategy", "noAggregationWidthRelative", "noAggregationWidth", "timeWindow", "noAggregation"})
    @Generated
    TimeseriesBarRenderCtx(double barGap, double intervalGap, TimeSeriesChartNoAggregationBarWidthStrategy noAggregationBarWidthStrategy, boolean noAggregationWidthRelative, double noAggregationWidth, long timeWindow, boolean noAggregation) {
        this.barGap = barGap;
        this.intervalGap = intervalGap;
        this.noAggregationBarWidthStrategy = noAggregationBarWidthStrategy;
        this.noAggregationWidthRelative = noAggregationWidthRelative;
        this.noAggregationWidth = noAggregationWidth;
        this.timeWindow = timeWindow;
        this.noAggregation = noAggregation;
    }

    @Generated
    public static TimeseriesBarRenderCtxBuilder builder() {
        return new TimeseriesBarRenderCtxBuilder();
    }

    @Generated
    public double getBarGap() {
        return this.barGap;
    }

    @Generated
    public double getIntervalGap() {
        return this.intervalGap;
    }

    @Generated
    public TimeSeriesChartNoAggregationBarWidthStrategy getNoAggregationBarWidthStrategy() {
        return this.noAggregationBarWidthStrategy;
    }

    @Generated
    public boolean isNoAggregationWidthRelative() {
        return this.noAggregationWidthRelative;
    }

    @Generated
    public double getNoAggregationWidth() {
        return this.noAggregationWidth;
    }

    @Generated
    public long getTimeWindow() {
        return this.timeWindow;
    }

    @Generated
    public boolean isNoAggregation() {
        return this.noAggregation;
    }

    @Generated
    public void setBarGap(double barGap) {
        this.barGap = barGap;
    }

    @Generated
    public void setIntervalGap(double intervalGap) {
        this.intervalGap = intervalGap;
    }

    @Generated
    public void setNoAggregationBarWidthStrategy(TimeSeriesChartNoAggregationBarWidthStrategy noAggregationBarWidthStrategy) {
        this.noAggregationBarWidthStrategy = noAggregationBarWidthStrategy;
    }

    @Generated
    public void setNoAggregationWidthRelative(boolean noAggregationWidthRelative) {
        this.noAggregationWidthRelative = noAggregationWidthRelative;
    }

    @Generated
    public void setNoAggregationWidth(double noAggregationWidth) {
        this.noAggregationWidth = noAggregationWidth;
    }

    @Generated
    public void setTimeWindow(long timeWindow) {
        this.timeWindow = timeWindow;
    }

    @Generated
    public void setNoAggregation(boolean noAggregation) {
        this.noAggregation = noAggregation;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TimeseriesBarRenderCtx)) {
            return false;
        }
        TimeseriesBarRenderCtx other = (TimeseriesBarRenderCtx)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (Double.compare(this.getBarGap(), other.getBarGap()) != 0) {
            return false;
        }
        if (Double.compare(this.getIntervalGap(), other.getIntervalGap()) != 0) {
            return false;
        }
        if (this.isNoAggregationWidthRelative() != other.isNoAggregationWidthRelative()) {
            return false;
        }
        if (Double.compare(this.getNoAggregationWidth(), other.getNoAggregationWidth()) != 0) {
            return false;
        }
        if (this.getTimeWindow() != other.getTimeWindow()) {
            return false;
        }
        if (this.isNoAggregation() != other.isNoAggregation()) {
            return false;
        }
        TimeSeriesChartNoAggregationBarWidthStrategy this$noAggregationBarWidthStrategy = this.getNoAggregationBarWidthStrategy();
        TimeSeriesChartNoAggregationBarWidthStrategy other$noAggregationBarWidthStrategy = other.getNoAggregationBarWidthStrategy();
        return !(this$noAggregationBarWidthStrategy == null ? other$noAggregationBarWidthStrategy != null : !this$noAggregationBarWidthStrategy.equals(other$noAggregationBarWidthStrategy));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TimeseriesBarRenderCtx;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $barGap = Double.doubleToLongBits(this.getBarGap());
        result = result * 59 + (int)($barGap >>> 32 ^ $barGap);
        long $intervalGap = Double.doubleToLongBits(this.getIntervalGap());
        result = result * 59 + (int)($intervalGap >>> 32 ^ $intervalGap);
        result = result * 59 + (this.isNoAggregationWidthRelative() ? 79 : 97);
        long $noAggregationWidth = Double.doubleToLongBits(this.getNoAggregationWidth());
        result = result * 59 + (int)($noAggregationWidth >>> 32 ^ $noAggregationWidth);
        long $timeWindow = this.getTimeWindow();
        result = result * 59 + (int)($timeWindow >>> 32 ^ $timeWindow);
        result = result * 59 + (this.isNoAggregation() ? 79 : 97);
        TimeSeriesChartNoAggregationBarWidthStrategy $noAggregationBarWidthStrategy = this.getNoAggregationBarWidthStrategy();
        result = result * 59 + ($noAggregationBarWidthStrategy == null ? 43 : $noAggregationBarWidthStrategy.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TimeseriesBarRenderCtx(barGap=" + this.getBarGap() + ", intervalGap=" + this.getIntervalGap() + ", noAggregationBarWidthStrategy=" + String.valueOf(this.getNoAggregationBarWidthStrategy()) + ", noAggregationWidthRelative=" + this.isNoAggregationWidthRelative() + ", noAggregationWidth=" + this.getNoAggregationWidth() + ", timeWindow=" + this.getTimeWindow() + ", noAggregation=" + this.isNoAggregation() + ")";
    }

    @Generated
    public static class TimeseriesBarRenderCtxBuilder {
        @Generated
        private double barGap;
        @Generated
        private double intervalGap;
        @Generated
        private TimeSeriesChartNoAggregationBarWidthStrategy noAggregationBarWidthStrategy;
        @Generated
        private boolean noAggregationWidthRelative;
        @Generated
        private double noAggregationWidth;
        @Generated
        private long timeWindow;
        @Generated
        private boolean noAggregation;

        @Generated
        TimeseriesBarRenderCtxBuilder() {
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder barGap(double barGap) {
            this.barGap = barGap;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder intervalGap(double intervalGap) {
            this.intervalGap = intervalGap;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder noAggregationBarWidthStrategy(TimeSeriesChartNoAggregationBarWidthStrategy noAggregationBarWidthStrategy) {
            this.noAggregationBarWidthStrategy = noAggregationBarWidthStrategy;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder noAggregationWidthRelative(boolean noAggregationWidthRelative) {
            this.noAggregationWidthRelative = noAggregationWidthRelative;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder noAggregationWidth(double noAggregationWidth) {
            this.noAggregationWidth = noAggregationWidth;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder timeWindow(long timeWindow) {
            this.timeWindow = timeWindow;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtxBuilder noAggregation(boolean noAggregation) {
            this.noAggregation = noAggregation;
            return this;
        }

        @Generated
        public TimeseriesBarRenderCtx build() {
            return new TimeseriesBarRenderCtx(this.barGap, this.intervalGap, this.noAggregationBarWidthStrategy, this.noAggregationWidthRelative, this.noAggregationWidth, this.timeWindow, this.noAggregation);
        }

        @Generated
        public String toString() {
            return "TimeseriesBarRenderCtx.TimeseriesBarRenderCtxBuilder(barGap=" + this.barGap + ", intervalGap=" + this.intervalGap + ", noAggregationBarWidthStrategy=" + String.valueOf(this.noAggregationBarWidthStrategy) + ", noAggregationWidthRelative=" + this.noAggregationWidthRelative + ", noAggregationWidth=" + this.noAggregationWidth + ", timeWindow=" + this.timeWindow + ", noAggregation=" + this.noAggregation + ")";
        }
    }
}

