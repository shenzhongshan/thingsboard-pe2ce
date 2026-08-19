/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.AxisState
 *  org.jfree.chart.axis.DateAxis
 *  org.jfree.chart.axis.TickType
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.axis.ValueTick
 *  org.jfree.chart.plot.PlotRenderingInfo
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.util.Args
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.Args;
import org.thingsboard.server.report.util.ColorUtils;

public class TbDateAxis
extends DateAxis {
    private boolean gridlinesVisible = true;
    private transient Stroke gridlineStroke = new BasicStroke(1.0f);
    private transient Paint gridlinePaint = ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.12)");

    public TbDateAxis(String label, TimeZone zone, Locale locale) {
        super(label, zone, locale);
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
                XYPlot xyPlot = (XYPlot)this.getPlot();
                xyPlot.getRenderer().drawDomainLine(g2, xyPlot, (ValueAxis)this, area, tick.getValue(), this.getGridlinePaint(), this.getGridlineStroke());
            }
        }
    }
}

