/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.labels.PieSectionLabelGenerator
 *  org.jfree.chart.plot.PieLabelLinkStyle
 *  org.jfree.chart.plot.PieLabelRecord
 *  org.jfree.chart.plot.PiePlot
 *  org.jfree.chart.plot.PiePlotState
 *  org.jfree.chart.plot.PlotRenderingInfo
 *  org.jfree.chart.text.TextBlock
 *  org.jfree.chart.text.TextBox
 *  org.jfree.chart.text.TextUtils
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.RectangleAnchor
 *  org.jfree.data.general.DatasetUtils
 *  org.jfree.data.general.PieDataset
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.List;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.plot.PieLabelRecord;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlotState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.text.TextBlock;
import org.jfree.chart.text.TextBox;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.general.PieDataset;

public class TbPiePlot<K extends Comparable<K>>
extends PiePlot<K> {
    private Double pieRadius = null;

    public TbPiePlot(PieDataset<K> dataset) {
        super(dataset);
    }

    public void setPieRadius(Double pieRadius) {
        this.pieRadius = pieRadius;
    }

    protected double getLabelLinkDepth() {
        return 0.0;
    }

    protected void drawPie(Graphics2D g2, Rectangle2D plotArea, PlotRenderingInfo info) {
        Rectangle2D.Double pieArea;
        PiePlotState state = this.initialise(g2, plotArea, this, null, info);
        double labelReserve = 0.0;
        if (this.getLabelGenerator() != null && !this.getSimpleLabels()) {
            labelReserve = this.getLabelGap() + this.getMaximumLabelWidth();
        }
        double gapHorizontal = plotArea.getWidth() * labelReserve * 2.0;
        double gapVertical = plotArea.getHeight() * this.getInteriorGap() * 2.0;
        double linkX = plotArea.getX() + gapHorizontal / 2.0;
        double linkY = plotArea.getY() + gapVertical / 2.0;
        double linkW = plotArea.getWidth() - gapHorizontal;
        double linkH = plotArea.getHeight() - gapVertical;
        if (this.isCircular()) {
            double min = Math.min(linkW, linkH) / 2.0;
            linkX = (linkX + linkX + linkW) / 2.0 - min;
            linkY = (linkY + linkY + linkH) / 2.0 - min;
            linkW = 2.0 * min;
            linkH = 2.0 * min;
        }
        Rectangle2D.Double linkArea = new Rectangle2D.Double(linkX, linkY, linkW, linkH);
        state.setLinkArea((Rectangle2D)linkArea);
        double lm = 0.0;
        if (!this.getSimpleLabels()) {
            lm = this.getLabelLinkMargin();
        }
        double hh = ((RectangularShape)linkArea).getWidth() * lm * 2.0;
        double vv = ((RectangularShape)linkArea).getHeight() * lm * 2.0;
        Rectangle2D.Double explodeArea = new Rectangle2D.Double(linkX + hh / 2.0, linkY + vv / 2.0, linkW - hh, linkH - vv);
        state.setExplodedPieArea((Rectangle2D)explodeArea);
        if (this.pieRadius != null) {
            double min = Math.min(plotArea.getWidth(), plotArea.getHeight());
            double size = min * this.pieRadius / 100.0;
            double x = plotArea.getCenterX() - size / 2.0;
            double y = plotArea.getCenterY() - size / 2.0;
            pieArea = new Rectangle2D.Double(x, y, size, size);
            double horizontalSpace = plotArea.getWidth() - ((RectangularShape)pieArea).getWidth();
            double verticalSpace = plotArea.getHeight() - ((RectangularShape)pieArea).getHeight();
            double hLabelSpace = Math.min(horizontalSpace, gapHorizontal);
            double vLabelSpace = Math.min(verticalSpace, gapVertical);
            double maxLinkLength = 30.0;
            double maxLinkHeight = 15.0;
            if (horizontalSpace - hLabelSpace > maxLinkLength) {
                hLabelSpace += horizontalSpace - hLabelSpace - maxLinkLength;
            }
            if (verticalSpace - vLabelSpace > maxLinkHeight) {
                vLabelSpace += verticalSpace - vLabelSpace - maxLinkHeight;
            }
            linkX = plotArea.getX() + hLabelSpace / 2.0;
            linkY = plotArea.getY() + vLabelSpace / 2.0;
            linkW = plotArea.getWidth() - hLabelSpace;
            linkH = plotArea.getHeight() - vLabelSpace;
            linkArea = new Rectangle2D.Double(linkX, linkY, linkW, linkH);
            state.setLinkArea((Rectangle2D)linkArea);
        } else {
            double maximumExplodePercent = this.getMaximumExplodePercent();
            double percent = maximumExplodePercent / (1.0 + maximumExplodePercent);
            double h1 = ((RectangularShape)explodeArea).getWidth() * percent;
            double v1 = ((RectangularShape)explodeArea).getHeight() * percent;
            pieArea = new Rectangle2D.Double(((RectangularShape)explodeArea).getX() + h1 / 2.0, ((RectangularShape)explodeArea).getY() + v1 / 2.0, ((RectangularShape)explodeArea).getWidth() - h1, ((RectangularShape)explodeArea).getHeight() - v1);
        }
        state.setPieArea((Rectangle2D)pieArea);
        state.setPieCenterX(pieArea.getCenterX());
        state.setPieCenterY(pieArea.getCenterY());
        state.setPieWRadius(((RectangularShape)pieArea).getWidth() / 2.0);
        state.setPieHRadius(((RectangularShape)pieArea).getHeight() / 2.0);
        if (this.getDataset() != null && this.getDataset().getKeys().size() > 0) {
            List keys = this.getDataset().getKeys();
            double totalValue = DatasetUtils.calculatePieDatasetTotal((PieDataset)this.getDataset());
            int passesRequired = state.getPassesRequired();
            for (int pass = 0; pass < passesRequired; ++pass) {
                double runningTotal = 0.0;
                for (int section = 0; section < keys.size(); ++section) {
                    double value;
                    Number n = this.getDataset().getValue(section);
                    if (n == null || !((value = n.doubleValue()) > 0.0)) continue;
                    runningTotal += value;
                    this.drawItem(g2, section, explodeArea, state, pass);
                }
            }
            if (this.getSimpleLabels()) {
                this.drawSimpleLabels(g2, keys, totalValue, plotArea, linkArea, state);
            } else {
                this.drawLabels(g2, keys, totalValue, plotArea, linkArea, state);
            }
        } else {
            this.drawNoDataMessage(g2, plotArea);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void drawSimpleLabels(Graphics2D g2, List<K> keys, double totalValue, Rectangle2D plotArea, Rectangle2D pieArea, PiePlotState state) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            Composite originalComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(3, 1.0f));
            Rectangle2D labelsArea = this.getSimpleLabelOffset().createInsetRectangle(pieArea);
            double runningTotal = 0.0;
            for (K key : keys) {
                String label;
                boolean include;
                double v = 0.0;
                Number n = this.getDataset().getValue(key);
                if (n == null) {
                    include = !this.getIgnoreNullValues();
                } else {
                    v = n.doubleValue();
                    include = this.getIgnoreZeroValues() ? v > 0.0 : v >= 0.0;
                }
                if (!include) continue;
                double mid = this.getStartAngle() + this.getDirection().getFactor() * (((runningTotal += v) - v / 2.0) * 360.0) / totalValue;
                Arc2D.Double arc = new Arc2D.Double(labelsArea, this.getStartAngle(), mid - this.getStartAngle(), 0);
                int x = (int)arc.getEndPoint().getX();
                int y = (int)arc.getEndPoint().getY();
                PieSectionLabelGenerator myLabelGenerator = this.getLabelGenerator();
                if (myLabelGenerator == null || (label = myLabelGenerator.generateSectionLabel(this.getDataset(), key)) == null) continue;
                TextBlock block = TextUtils.createTextBlock((String)label, (Font)this.getLabelFont(), (Paint)this.getLabelPaint());
                TextBox labelBox = new TextBox(block);
                labelBox.setBackgroundPaint(this.getLabelBackgroundPaint());
                labelBox.setOutlinePaint(this.getLabelOutlinePaint());
                labelBox.setOutlineStroke(this.getLabelOutlineStroke());
                if (this.getShadowGenerator() == null) {
                    labelBox.setShadowPaint(this.getLabelShadowPaint());
                } else {
                    labelBox.setShadowPaint(null);
                }
                labelBox.setInteriorGap(this.getLabelPadding());
                labelBox.draw(g2, (float)x, (float)y, RectangleAnchor.CENTER);
            }
            g2.setComposite(originalComposite);
        }
        finally {
            g2.setClip(savedClip);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void drawLeftLabel(Graphics2D g2, PiePlotState state, PieLabelRecord record) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            double anchorX = state.getLinkArea().getMinX();
            double targetX = anchorX - record.getGap();
            double targetY = record.getAllocatedY();
            if (this.getLabelLinksVisible()) {
                double elbowY;
                double theta = record.getAngle();
                double linkX = state.getPieCenterX() + Math.cos(theta) * state.getPieWRadius() * record.getLinkPercent();
                double linkY = state.getPieCenterY() - Math.sin(theta) * state.getPieHRadius() * record.getLinkPercent();
                double elbowX = state.getPieCenterX() + Math.cos(theta) * state.getLinkArea().getWidth() / 2.0;
                double anchorY = elbowY = state.getPieCenterY() - Math.sin(theta) * state.getLinkArea().getHeight() / 2.0;
                g2.setPaint(this.getSectionPaint(record.getKey()));
                g2.setStroke(this.getLabelLinkStroke());
                PieLabelLinkStyle style = this.getLabelLinkStyle();
                if (style.equals((Object)PieLabelLinkStyle.STANDARD)) {
                    g2.draw(new Line2D.Double(linkX, linkY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, targetX, targetY));
                } else if (style.equals((Object)PieLabelLinkStyle.QUAD_CURVE)) {
                    QuadCurve2D.Float q = new QuadCurve2D.Float();
                    ((QuadCurve2D)q).setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY);
                    g2.draw(q);
                    g2.draw(new Line2D.Double(elbowX, elbowY, linkX, linkY));
                } else if (style.equals((Object)PieLabelLinkStyle.CUBIC_CURVE)) {
                    CubicCurve2D.Float c = new CubicCurve2D.Float();
                    ((CubicCurve2D)c).setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY, linkX, linkY);
                    g2.draw(c);
                }
            }
            TextBox tb = record.getLabel();
            tb.getTextBlock().setLineAlignment(HorizontalAlignment.RIGHT);
            tb.draw(g2, (float)targetX, (float)targetY, RectangleAnchor.RIGHT);
        }
        finally {
            g2.setClip(savedClip);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void drawRightLabel(Graphics2D g2, PiePlotState state, PieLabelRecord record) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            double anchorX = state.getLinkArea().getMaxX();
            double targetX = anchorX + record.getGap();
            double targetY = record.getAllocatedY();
            if (this.getLabelLinksVisible()) {
                double elbowY;
                double theta = record.getAngle();
                double linkX = state.getPieCenterX() + Math.cos(theta) * state.getPieWRadius() * record.getLinkPercent();
                double linkY = state.getPieCenterY() - Math.sin(theta) * state.getPieHRadius() * record.getLinkPercent();
                double elbowX = state.getPieCenterX() + Math.cos(theta) * state.getLinkArea().getWidth() / 2.0;
                double anchorY = elbowY = state.getPieCenterY() - Math.sin(theta) * state.getLinkArea().getHeight() / 2.0;
                g2.setPaint(this.getSectionPaint(record.getKey()));
                g2.setStroke(this.getLabelLinkStroke());
                PieLabelLinkStyle style = this.getLabelLinkStyle();
                if (style.equals((Object)PieLabelLinkStyle.STANDARD)) {
                    g2.draw(new Line2D.Double(linkX, linkY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, elbowX, elbowY));
                    g2.draw(new Line2D.Double(anchorX, anchorY, targetX, targetY));
                } else if (style.equals((Object)PieLabelLinkStyle.QUAD_CURVE)) {
                    QuadCurve2D.Float q = new QuadCurve2D.Float();
                    ((QuadCurve2D)q).setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY);
                    g2.draw(q);
                    g2.draw(new Line2D.Double(elbowX, elbowY, linkX, linkY));
                } else if (style.equals((Object)PieLabelLinkStyle.CUBIC_CURVE)) {
                    CubicCurve2D.Float c = new CubicCurve2D.Float();
                    ((CubicCurve2D)c).setCurve(targetX, targetY, anchorX, anchorY, elbowX, elbowY, linkX, linkY);
                    g2.draw(c);
                }
            }
            TextBox tb = record.getLabel();
            tb.getTextBlock().setLineAlignment(HorizontalAlignment.LEFT);
            tb.draw(g2, (float)targetX, (float)targetY, RectangleAnchor.LEFT);
        }
        finally {
            g2.setClip(savedClip);
        }
    }
}

