/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.jfree.chart.axis.AxisState
 *  org.jfree.chart.axis.NumberAxis
 *  org.jfree.chart.axis.NumberTick
 *  org.jfree.chart.axis.NumberTickUnit
 *  org.jfree.chart.axis.Tick
 *  org.jfree.chart.axis.TickType
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.axis.ValueTick
 *  org.jfree.chart.plot.CategoryPlot
 *  org.jfree.chart.plot.Plot
 *  org.jfree.chart.plot.PlotRenderingInfo
 *  org.jfree.chart.plot.ValueAxisPlot
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.jfree.chart.util.Args
 *  org.jfree.data.Range
 *  org.jfree.data.RangeType
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.thingsboard.server.report.renderer.chart.TbNumberTickUnitSource;
import org.thingsboard.server.report.renderer.chart.TbStateTick;
import org.thingsboard.server.report.util.ColorUtils;

public class TbNumberAxis
extends NumberAxis {
    private boolean gridlinesVisible;
    private transient Stroke gridlineStroke;
    private transient Paint gridlinePaint;
    private Double axisMin;
    private Double axisMax;
    private Integer splitNumber;
    private final TbNumberTickUnitSource tbNumberTickUnitSource = new TbNumberTickUnitSource();
    private final TbNumberAxis parentAxis;
    private List<TbStateTick> stateTicks;
    private TbNumberAxisTicks ticks;

    public TbNumberAxis(String label, TbNumberAxis parentAxis) {
        super(label);
        this.setUpperMargin(0.0);
        this.setLowerMargin(0.0);
        this.parentAxis = parentAxis;
        this.gridlinesVisible = true;
        this.gridlineStroke = new BasicStroke(1.0f);
        this.gridlinePaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.12)");
    }

    public boolean isGridlinesVisible() {
        return this.gridlinesVisible;
    }

    public void setGridlinesVisible(boolean visible) {
        if (this.gridlinesVisible != visible) {
            this.gridlinesVisible = visible;
            this.fireChangeEvent();
        }
    }

    public Stroke getGridlineStroke() {
        return this.gridlineStroke;
    }

    public void setGridlineStroke(Stroke stroke) {
        Args.nullNotPermitted((Object)stroke, (String)"stroke");
        this.gridlineStroke = stroke;
        this.fireChangeEvent();
    }

    public Paint getGridlinePaint() {
        return this.gridlinePaint;
    }

    public void setGridlinePaint(Paint paint) {
        Args.nullNotPermitted((Object)paint, (String)"paint");
        this.gridlinePaint = paint;
        this.fireChangeEvent();
    }

    public void setStateTicks(List<TbStateTick> stateTicks) {
        this.stateTicks = stateTicks;
    }

    public AxisState draw(Graphics2D g2, double cursor, Rectangle2D plotArea, Rectangle2D dataArea, RectangleEdge edge, PlotRenderingInfo plotState) {
        AxisState state = super.draw(g2, cursor, plotArea, dataArea, edge, plotState);
        if (this.isVisible()) {
            this.drawGridlines(g2, dataArea, state.getTicks());
        }
        return state;
    }

    protected void drawGridlines(Graphics2D g2, Rectangle2D area, List<ValueTick> ticks) {
        if (this.isGridlinesVisible()) {
            for (ValueTick tick : ticks) {
                if (tick.getTickType() != TickType.MAJOR) continue;
                Plot plot = this.getPlot();
                if (plot instanceof XYPlot) {
                    XYPlot xyPlot = (XYPlot)plot;
                    xyPlot.getRenderer().drawRangeLine(g2, xyPlot, (ValueAxis)this, area, tick.getValue(), this.getGridlinePaint(), this.getGridlineStroke());
                    continue;
                }
                if (!(plot instanceof CategoryPlot)) continue;
                CategoryPlot categoryPlot = (CategoryPlot)plot;
                categoryPlot.getRenderer().drawRangeLine(g2, categoryPlot, (ValueAxis)this, area, tick.getValue(), this.getGridlinePaint(), this.getGridlineStroke());
            }
        }
    }

    protected List<Tick> refreshTicksVertical(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        ArrayList<Tick> result = new ArrayList<Tick>();
        Font tickLabelFont = this.getTickLabelFont();
        g2.setFont(tickLabelFont);
        if ((this.splitNumber == null || this.splitNumber == 0) && this.isAutoTickUnitSelection()) {
            this.selectAutoTickUnit(g2, dataArea, edge);
        }
        if (this.ticks == null) {
            this.adjustTicksAndRange();
        }
        if (this.ticks.getTicksCount() <= 500) {
            double labelHeight;
            boolean drawFirstLabel = true;
            boolean drawLastLabel = true;
            if (this.ticks.checkFirstTickIntersection()) {
                double nextLabelPos;
                labelHeight = this.calculateTickLabelHeight(g2, "123");
                double firstLabelPos = this.valueToJava2D(this.ticks.firstTickValue(this.getRange()), dataArea, edge);
                if (firstLabelPos - labelHeight / 2.0 < (nextLabelPos = this.valueToJava2D(this.ticks.getTickValue(1, this.getRange()), dataArea, edge)) + labelHeight / 2.0) {
                    drawFirstLabel = false;
                }
            }
            if (this.ticks.checkLastTickIntersection()) {
                double prevLabelPos;
                labelHeight = this.calculateTickLabelHeight(g2, "123");
                double lastLabelPos = this.valueToJava2D(this.ticks.lastTickValue(this.getRange()), dataArea, edge);
                if (lastLabelPos + labelHeight / 2.0 > (prevLabelPos = this.valueToJava2D(this.ticks.getTickValue(this.ticks.getTicksCount() - 2, this.getRange()), dataArea, edge)) - labelHeight / 2.0) {
                    drawLastLabel = false;
                }
            }
            for (int i = 0; i < this.ticks.getTicksCount(); ++i) {
                TextAnchor rotationAnchor;
                TextAnchor anchor;
                boolean drawLabel;
                double currentTickValue = this.ticks.getTickValue(i, this.getRange());
                if (currentTickValue == 0.0 && Double.compare(currentTickValue, 0.0) < 0) {
                    currentTickValue = 0.0;
                }
                String tickLabel = null;
                boolean bl = drawLabel = i == 0 && drawFirstLabel || i == this.ticks.getTicksCount() - 1 && drawLastLabel || i > 0 && i < this.ticks.getTicksCount() - 1;
                if (drawLabel) {
                    NumberFormat formatter;
                    tickLabel = this.ticks.isStateTicks() ? this.ticks.getStateTickLabel(i) : ((formatter = this.getNumberFormatOverride()) != null ? formatter.format(currentTickValue) : this.getTickUnit().valueToString(currentTickValue));
                }
                double angle = 0.0;
                if (edge == RectangleEdge.LEFT) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else {
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                }
                NumberTick tick = new NumberTick((Number)currentTickValue, tickLabel, anchor, rotationAnchor, angle);
                result.add((Tick)tick);
            }
        }
        return result;
    }

    protected void autoAdjustRange() {
        Plot plot = this.getPlot();
        if (plot == null) {
            return;
        }
        if (plot instanceof ValueAxisPlot) {
            ValueAxisPlot vap = (ValueAxisPlot)plot;
            Range r = vap.getDataRange((ValueAxis)this);
            if (r == null) {
                r = this.getDefaultAutoRange();
            }
            double upper = r.getUpperBound();
            double lower = r.getLowerBound();
            if (this.getRangeType() == RangeType.POSITIVE) {
                lower = Math.max(0.0, lower);
                upper = Math.max(0.0, upper);
            } else if (this.getRangeType() == RangeType.NEGATIVE) {
                lower = Math.min(0.0, lower);
                upper = Math.min(0.0, upper);
            }
            if (this.getAutoRangeIncludesZero()) {
                lower = Math.min(lower, 0.0);
                upper = Math.max(upper, 0.0);
            }
            double range = upper - lower;
            double fixedAutoRange = this.getFixedAutoRange();
            if (fixedAutoRange > 0.0) {
                lower = upper - fixedAutoRange;
            } else {
                double minRange = this.getAutoRangeMinimumSize();
                if (range < minRange) {
                    double expand = (minRange - range) / 2.0;
                    if ((lower -= expand) == (upper += expand)) {
                        double adjust = Math.abs(lower) / 10.0;
                        lower -= adjust;
                        upper += adjust;
                    }
                    if (this.getRangeType() == RangeType.POSITIVE) {
                        if (lower < 0.0) {
                            upper -= lower;
                            lower = 0.0;
                        }
                    } else if (this.getRangeType() == RangeType.NEGATIVE && upper > 0.0) {
                        lower -= upper;
                        upper = 0.0;
                    }
                }
                if (this.getAutoRangeStickyZero()) {
                    upper = upper <= 0.0 ? Math.min(0.0, upper + this.getUpperMargin() * range) : (upper += this.getUpperMargin() * range);
                    lower = lower >= 0.0 ? Math.max(0.0, lower - this.getLowerMargin() * range) : (lower -= this.getLowerMargin() * range);
                } else {
                    upper += this.getUpperMargin() * range;
                    lower -= this.getLowerMargin() * range;
                }
            }
            if (this.axisMin != null) {
                lower = this.axisMin;
            }
            if (this.axisMax != null) {
                upper = this.axisMax;
            }
            if (upper <= lower) {
                upper = lower + 1.0;
            }
            this.setRange(new Range(lower, upper), false, false);
        }
    }

    protected double estimateMaximumTickLabelHeight(Graphics2D g2) {
        double result = this.calculateTickLabelHeight(g2, "123");
        return result * 2.0;
    }

    private double calculateTickLabelHeight(Graphics2D g2, String label) {
        RectangleInsets tickLabelInsets = this.getTickLabelInsets();
        double result = tickLabelInsets.getTop() + tickLabelInsets.getBottom();
        Font tickLabelFont = this.getTickLabelFont();
        FontRenderContext frc = g2.getFontRenderContext();
        return result += (double)tickLabelFont.getLineMetrics(label, frc).getHeight();
    }

    private void adjustTicksAndRange() {
        double currentAxisMax;
        double currentAxisMin;
        double upper;
        double lower = this.axisMin != null ? this.axisMin.doubleValue() : this.getRange().getLowerBound();
        double d = upper = this.axisMax != null ? this.axisMax.doubleValue() : this.getRange().getUpperBound();
        if (upper <= lower) {
            upper = lower + 1.0;
        }
        double length = upper - lower;
        NumberTickUnit tickUnit = this.getTickUnit();
        double size = tickUnit.getSize();
        if (this.parentAxis != null) {
            Range parentRange;
            if (this.axisMin == null && (parentRange = this.parentAxis.getRange()).getLowerBound() < 0.0 && parentRange.getUpperBound() > 0.0) {
                double zeroDistance = Math.abs(parentRange.getLowerBound()) / parentRange.getLength();
                lower -= zeroDistance * length;
                length = upper - lower;
            }
            size = this.calculateChildSize(length);
            int parentSplitCount = this.parentAxis.getTicks().getTicksCount() - 1;
            length = (double)parentSplitCount * size;
            if (this.axisMin == null) {
                lower = this.axisMax == null && upper == 0.0 ? upper - length : this.nearestLower(lower, size);
            }
            if (this.axisMax == null) {
                upper = lower + length;
                if (upper < this.getRange().getUpperBound()) {
                    double newLength = this.getRange().getUpperBound() - lower;
                    size = this.calculateChildSize(newLength);
                    length = (double)parentSplitCount * size;
                    upper = lower + length;
                }
                this.setRange(new Range(lower, upper), false, false);
            }
        } else {
            if (this.splitNumber != null && this.splitNumber > 0) {
                size = length / (double)this.splitNumber.intValue();
            }
            if (this.isAutoTickUnitSelection()) {
                tickUnit = (NumberTickUnit)this.tbNumberTickUnitSource.getCeilingTickUnit(size);
                this.setTickUnit(tickUnit, false, false);
            }
        }
        size = tickUnit.getSize();
        lower = this.getRange().getLowerBound();
        upper = this.getRange().getUpperBound();
        if (this.axisMin == null) {
            if (this.parentAxis == null) {
                lower = this.nearestLower(lower, size);
            }
            currentAxisMin = lower;
        } else {
            currentAxisMin = this.axisMin;
        }
        if (this.axisMax == null) {
            if (this.parentAxis == null) {
                upper = this.nearestUpper(lower, upper, size);
            }
            currentAxisMax = upper;
        } else {
            currentAxisMax = this.axisMax;
        }
        if (currentAxisMax <= currentAxisMin) {
            currentAxisMax = currentAxisMin + 1.0;
        }
        this.setRange(new Range(currentAxisMin, currentAxisMax), false, false);
        this.calculateTicks();
    }

    private double calculateChildSize(double length) {
        NumberTickUnit tickUnit = this.getTickUnit();
        double size = tickUnit.getSize();
        int parentSplitCount = this.parentAxis.getTicks().getTicksCount() - 1;
        boolean roundToNearest = true;
        if (this.splitNumber != null && this.splitNumber > 0) {
            size = length / (double)this.splitNumber.intValue();
        } else if (this.isAutoTickUnitSelection()) {
            size = length / (double)parentSplitCount;
            roundToNearest = false;
        }
        if (this.isAutoTickUnitSelection()) {
            tickUnit = (NumberTickUnit)this.tbNumberTickUnitSource.getCeilingTickUnit(size, roundToNearest);
            this.setTickUnit(tickUnit, false, false);
            size = tickUnit.getSize();
        }
        return size;
    }

    private double nearestLower(double lower, double size) {
        BigDecimal sizeDecimal = BigDecimal.valueOf(size);
        BigDecimal rest = BigDecimal.valueOf(Math.abs(lower)).remainder(sizeDecimal);
        if (rest.compareTo(BigDecimal.ZERO) > 0) {
            if (lower < 0.0) {
                rest = sizeDecimal.subtract(rest);
            }
            return BigDecimal.valueOf(lower).subtract(rest).doubleValue();
        }
        return lower;
    }

    private double nearestUpper(double lower, double upper, double size) {
        BigDecimal upperDecimal = BigDecimal.valueOf(upper);
        BigDecimal sizeDecimal = BigDecimal.valueOf(size);
        BigDecimal length = upperDecimal.subtract(BigDecimal.valueOf(lower));
        BigDecimal rest = length.remainder(sizeDecimal);
        if (rest.compareTo(BigDecimal.ZERO) > 0) {
            return upperDecimal.add(sizeDecimal.subtract(rest)).doubleValue();
        }
        return upper;
    }

    private TbNumberAxisTicks getTicks() {
        return this.ticks;
    }

    private void calculateTicks() {
        if (this.stateTicks != null) {
            this.ticks = new TbNumberAxisTicks(true);
            for (TbStateTick stateTick : this.stateTicks) {
                this.ticks.addStateTickValue(stateTick.getValue(), stateTick.getLabel(), this.getRange());
            }
        } else if (this.parentAxis == null) {
            double lowestVisibleTickValue;
            NumberTickUnit tu = this.getTickUnit();
            double size = tu.getSize();
            this.ticks = new TbNumberAxisTicks();
            double currentTickValue = this.getRange().getLowerBound();
            double maxTickValue = this.getRange().getUpperBound();
            this.ticks.addTickValue(currentTickValue, this.getRange());
            double d = lowestVisibleTickValue = this.isAutoTickUnitSelection() ? this.calculateLowestVisibleTickValue() : currentTickValue;
            if (lowestVisibleTickValue > currentTickValue) {
                this.ticks.setAdditionalFistTick(true);
                currentTickValue = lowestVisibleTickValue;
            } else {
                currentTickValue += size;
            }
            while (currentTickValue < maxTickValue) {
                this.ticks.addTickValue(currentTickValue, this.getRange());
                currentTickValue += size;
            }
            if (maxTickValue > this.getRange().getLowerBound()) {
                if (maxTickValue != currentTickValue) {
                    this.ticks.setAdditionalLastTick(true);
                }
                this.ticks.addTickValue(maxTickValue, this.getRange());
            }
        } else {
            Double unitSize = !this.isAutoTickUnitSelection() ? Double.valueOf(this.getTickUnit().getSize()) : null;
            this.ticks = this.parentAxis.getTicks().computeChildTicks(this.splitNumber, unitSize, this.getRange());
        }
    }

    @Generated
    public void setAxisMin(Double axisMin) {
        this.axisMin = axisMin;
    }

    @Generated
    public void setAxisMax(Double axisMax) {
        this.axisMax = axisMax;
    }

    @Generated
    public void setSplitNumber(Integer splitNumber) {
        this.splitNumber = splitNumber;
    }

    private static class TbNumberAxisTicks {
        private final List<Double> tickValues = new ArrayList<Double>();
        private final List<String> stateTickLabels = new ArrayList<String>();
        private final boolean stateTicks;
        private boolean additionalFistTick = false;
        private boolean additionalLastTick = false;

        public TbNumberAxisTicks() {
            this(false);
        }

        public TbNumberAxisTicks(boolean stateTicks) {
            this.stateTicks = stateTicks;
        }

        public void addTickValue(double tickValue, Range range) {
            this.addTick((tickValue - range.getLowerBound()) / range.getLength());
        }

        public void addStateTickValue(double tickValue, String label, Range range) {
            this.addStateTick((tickValue - range.getLowerBound()) / range.getLength(), label);
        }

        public void addTick(double tick) {
            this.tickValues.add(tick);
        }

        public void addStateTick(double tick, String label) {
            this.tickValues.add(tick);
            this.stateTickLabels.add(label);
        }

        public int getTicksCount() {
            return this.tickValues.size();
        }

        public Double getTickValue(int index, Range range) {
            return range.getLowerBound() + this.tickValues.get(index) * range.getLength();
        }

        public String getStateTickLabel(int index) {
            return this.stateTickLabels.get(index);
        }

        public Double firstTickValue(Range range) {
            return this.getTickValue(0, range);
        }

        public Double lastTickValue(Range range) {
            return this.getTickValue(this.tickValues.size() - 1, range);
        }

        public boolean checkFirstTickIntersection() {
            return this.additionalFistTick && this.tickValues.size() > 2;
        }

        public boolean checkLastTickIntersection() {
            return this.additionalLastTick && this.tickValues.size() > 2;
        }

        public TbNumberAxisTicks computeChildTicks(Integer splitCount, Double unitSize, Range range) {
            double parentStep;
            double unitStep;
            if (this.additionalFistTick || this.additionalLastTick || (splitCount == null || splitCount == 0) && unitSize == null) {
                return this;
            }
            Integer step = null;
            int parentSplitCount = this.tickValues.size() - 1;
            if (splitCount == null || splitCount == 0) {
                unitStep = unitSize / range.getLength();
                splitCount = (int)(1.0 / unitStep);
            }
            if (parentSplitCount != splitCount && parentSplitCount % splitCount == 0) {
                step = parentSplitCount / splitCount;
            }
            if (step == null && unitSize != null && (unitStep = unitSize / range.getLength()) != (parentStep = 1.0 / (double)(this.tickValues.size() - 1)) && unitStep % parentStep == 0.0) {
                step = (int)(unitStep / parentStep);
            }
            if (step != null) {
                TbNumberAxisTicks ticks = new TbNumberAxisTicks();
                for (int i = 0; i < this.tickValues.size(); i += step.intValue()) {
                    ticks.addTick(this.tickValues.get(i));
                }
                if ((this.tickValues.size() - 1) % step != 0) {
                    ticks.addTick(this.tickValues.get(this.tickValues.size() - 1));
                }
                return ticks;
            }
            return this;
        }

        @Generated
        public boolean isStateTicks() {
            return this.stateTicks;
        }

        @Generated
        public boolean isAdditionalFistTick() {
            return this.additionalFistTick;
        }

        @Generated
        public void setAdditionalFistTick(boolean additionalFistTick) {
            this.additionalFistTick = additionalFistTick;
        }

        @Generated
        public boolean isAdditionalLastTick() {
            return this.additionalLastTick;
        }

        @Generated
        public void setAdditionalLastTick(boolean additionalLastTick) {
            this.additionalLastTick = additionalLastTick;
        }
    }
}

