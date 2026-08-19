/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.entity.ChartEntity
 *  org.jfree.chart.entity.EntityCollection
 *  org.jfree.chart.entity.PieSectionEntity
 *  org.jfree.chart.labels.PieToolTipGenerator
 *  org.jfree.chart.plot.CenterTextMode
 *  org.jfree.chart.plot.PiePlotState
 *  org.jfree.chart.plot.RingPlot
 *  org.jfree.chart.text.TextBlock
 *  org.jfree.chart.text.TextBlockAnchor
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.urls.PieURLGenerator
 *  org.jfree.chart.util.LineUtils
 *  org.jfree.chart.util.Rotation
 *  org.jfree.chart.util.ShapeUtils
 *  org.jfree.chart.util.UnitType
 *  org.jfree.data.general.PieDataset
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.labels.PieToolTipGenerator;
import org.jfree.chart.plot.CenterTextMode;
import org.jfree.chart.plot.PiePlotState;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.text.TextBlock;
import org.jfree.chart.text.TextBlockAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.urls.PieURLGenerator;
import org.jfree.chart.util.LineUtils;
import org.jfree.chart.util.Rotation;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.chart.util.UnitType;
import org.jfree.data.general.PieDataset;
import org.thingsboard.server.report.util.AwtFontUtils;

public class TbRingPlot
extends RingPlot {
    private double segmentsPadAngle = 0.0;
    private boolean roundSegmentCorners = false;
    private double absoluteSectionDepth = 0.0;
    private double scaleBase = 0.0;
    private String centerLabel;
    private Font centerLabelFont;
    private Color centerLabelColor;

    public TbRingPlot(PieDataset dataset) {
        super(dataset);
        this.setInteriorGap(0.0);
        this.setLabelLinkMargin(0.0);
        this.centerLabel = null;
        this.centerLabelFont = DEFAULT_LABEL_FONT;
        this.centerLabelColor = Color.BLACK;
    }

    public void setSegmentsPadAngle(double segmentsPadAngle) {
        this.segmentsPadAngle = segmentsPadAngle;
    }

    public void setRoundSegmentCorners(boolean roundSegmentCorners) {
        this.roundSegmentCorners = roundSegmentCorners;
    }

    public void setAbsoluteSectionDepth(double absoluteSectionDepth) {
        this.absoluteSectionDepth = absoluteSectionDepth;
    }

    public void setScaleBase(double scaleBase) {
        this.scaleBase = scaleBase;
    }

    public void setCenterLabel(String centerLabel) {
        this.centerLabel = centerLabel;
    }

    public void setCenterLabelFont(Font centerLabelFont) {
        this.centerLabelFont = centerLabelFont;
    }

    public void setCenterLabelColor(Color centerLabelColor) {
        this.centerLabelColor = centerLabelColor;
    }

    protected void drawItem(Graphics2D g2, int section, Rectangle2D dataArea, PiePlotState state, int currentPass) {
        PieDataset dataset = this.getDataset();
        Number n = dataset.getValue(section);
        if (n == null) {
            return;
        }
        double value = n.doubleValue();
        double angle1 = 0.0;
        double angle2 = 0.0;
        Rotation direction = this.getDirection();
        if (direction == Rotation.CLOCKWISE) {
            angle1 = state.getLatestAngle();
            angle2 = angle1 - value / state.getTotal() * 360.0;
        } else if (direction == Rotation.ANTICLOCKWISE) {
            angle1 = state.getLatestAngle();
            angle2 = angle1 + value / state.getTotal() * 360.0;
        } else {
            throw new IllegalStateException("Rotation type not recognised.");
        }
        double angle = angle2 - angle1;
        if (Math.abs(angle) > this.getMinimumArcAngleToDraw()) {
            Comparable key = this.getSectionKey(section);
            double ep = 0.0;
            double mep = this.getMaximumExplodePercent();
            if (mep > 0.0) {
                ep = this.getExplodePercent(key) / mep;
            }
            Rectangle2D arcBounds = this.getArcBounds(state.getPieArea(), state.getExplodedPieArea(), angle1, angle, ep);
            double padAngle = dataset.getItemCount() > 1 ? this.segmentsPadAngle : 0.0;
            boolean roundedCorners = dataset.getItemCount() > 1 && this.roundSegmentCorners;
            double angleOffset = padAngle / 2.0;
            double sectionDepth = this.absoluteSectionDepth > 0.0 ? this.absoluteSectionDepth / this.computeSize(arcBounds) : this.getSectionDepth();
            double depth = sectionDepth / 2.0;
            if (roundedCorners) {
                float absDepth = (float)(depth * arcBounds.getHeight());
                float depthOffsetAngle = (float)((double)(absDepth / 2.0f) / ((arcBounds.getHeight() - (double)absDepth) / 2.0) * 180.0 / Math.PI);
                angleOffset += (double)depthOffsetAngle;
            }
            Arc2D.Double arc = new Arc2D.Double(arcBounds, angle1 + (angleOffset *= direction.getFactor()), angle - angleOffset * 2.0, 0);
            RectangleInsets s = new RectangleInsets(UnitType.RELATIVE, depth, depth, depth, depth);
            Rectangle2D.Double innerArcBounds = new Rectangle2D.Double();
            ((Rectangle2D)innerArcBounds).setRect(arcBounds);
            s.trim((Rectangle2D)innerArcBounds);
            Arc2D.Double arc2 = new Arc2D.Double(innerArcBounds, angle1 + angle - angleOffset, -(angle - angleOffset * 2.0), 0);
            GeneralPath path = new GeneralPath();
            path.moveTo((float)arc.getStartPoint().getX(), (float)arc.getStartPoint().getY());
            path.append(arc.getPathIterator(null), false);
            if (roundedCorners) {
                path.append(this.roundJoin(arc.getEndPoint(), arc2.getStartPoint()).getPathIterator(null), true);
            }
            path.append(arc2.getPathIterator(null), true);
            if (roundedCorners) {
                path.append(this.roundJoin(arc2.getEndPoint(), arc.getStartPoint()).getPathIterator(null), true);
            }
            path.closePath();
            Line2D.Double separator = new Line2D.Double(arc2.getEndPoint(), arc.getStartPoint());
            if (currentPass == 0) {
                Paint shadowPaint = this.getShadowPaint();
                double shadowXOffset = this.getShadowXOffset();
                double shadowYOffset = this.getShadowYOffset();
                if (shadowPaint != null && this.getShadowGenerator() == null) {
                    Shape shadowArc = ShapeUtils.createTranslatedShape((Shape)path, (double)((float)shadowXOffset), (double)((float)shadowYOffset));
                    g2.setPaint(shadowPaint);
                    g2.fill(shadowArc);
                }
            } else if (currentPass == 1) {
                EntityCollection entities;
                Paint paint = this.lookupSectionPaint(key);
                g2.setPaint(paint);
                g2.fill(path);
                Paint outlinePaint = this.lookupSectionOutlinePaint(key);
                Stroke outlineStroke = this.lookupSectionOutlineStroke(key);
                if (this.getSectionOutlinesVisible() && outlinePaint != null && outlineStroke != null) {
                    g2.setPaint(outlinePaint);
                    g2.setStroke(outlineStroke);
                    g2.draw(path);
                }
                if (section == 0) {
                    AffineTransform currentTransform = g2.getTransform();
                    double scale = this.computeScaleFactor(arcBounds);
                    float factor = 1.0f;
                    if (scale > 0.0) {
                        g2.scale(scale, scale);
                        factor = (float)scale;
                    }
                    String nstr = null;
                    if (this.getCenterTextMode().equals((Object)CenterTextMode.VALUE)) {
                        nstr = this.getCenterTextFormatter().format(n);
                    } else if (this.getCenterTextMode().equals((Object)CenterTextMode.FIXED)) {
                        nstr = this.getCenterText();
                    }
                    if (this.centerLabel != null || nstr != null) {
                        TextBlock centerBlock = new TextBlock();
                        if (this.centerLabel != null) {
                            centerBlock.addLine(this.centerLabel, this.centerLabelFont, (Paint)this.centerLabelColor);
                        }
                        if (nstr != null) {
                            if (this.centerLabel != null) {
                                centerBlock.addLine("", AwtFontUtils.ZERO_FONT.deriveFont(1.0f), (Paint)Color.BLACK);
                            }
                            centerBlock.addLine(nstr, this.getCenterTextFont(), (Paint)this.getCenterTextColor());
                        }
                        centerBlock.draw(g2, (float)dataArea.getCenterX() / factor, (float)dataArea.getCenterY() / factor, TextBlockAnchor.CENTER);
                    }
                    g2.setTransform(currentTransform);
                }
                if (state.getInfo() != null && (entities = state.getEntityCollection()) != null) {
                    String tip = null;
                    PieToolTipGenerator toolTipGenerator = this.getToolTipGenerator();
                    if (toolTipGenerator != null) {
                        tip = toolTipGenerator.generateToolTip(dataset, key);
                    }
                    String url = null;
                    PieURLGenerator urlGenerator = this.getURLGenerator();
                    if (urlGenerator != null) {
                        url = urlGenerator.generateURL(dataset, key, this.getPieIndex());
                    }
                    PieSectionEntity entity = new PieSectionEntity((Shape)path, dataset, this.getPieIndex(), section, key, tip, url);
                    entities.add((ChartEntity)entity);
                }
            } else if (currentPass == 2 && this.getSeparatorsVisible()) {
                Line2D extendedSeparator = LineUtils.extendLine((Line2D)separator, (double)this.getInnerSeparatorExtension(), (double)this.getOuterSeparatorExtension());
                g2.setStroke(this.getSeparatorStroke());
                g2.setPaint(this.getSeparatorPaint());
                g2.draw(extendedSeparator);
            }
        }
        state.setLatestAngle(angle2);
    }

    private double computeSize(Rectangle2D arcBounds) {
        if (this.scaleBase > 0.0) {
            return this.scaleBase;
        }
        return Math.min(arcBounds.getWidth(), arcBounds.getHeight());
    }

    private double computeScaleFactor(Rectangle2D arcBounds) {
        if (this.scaleBase > 0.0) {
            double size = Math.min(arcBounds.getWidth(), arcBounds.getHeight());
            return size / this.scaleBase;
        }
        return 0.0;
    }

    private Arc2D.Double roundJoin(Point2D startPoint, Point2D endPoint) {
        float joinCenterX = (float)(startPoint.getX() + endPoint.getX()) / 2.0f;
        float joinCenterY = (float)(startPoint.getY() + endPoint.getY()) / 2.0f;
        double startAngle = 360.0 - Math.toDegrees(Math.atan2(startPoint.getY() - (double)joinCenterY, startPoint.getX() - (double)joinCenterX));
        double distance = startPoint.distance(endPoint);
        return new Arc2D.Double((double)joinCenterX - distance / 2.0, (double)joinCenterY - distance / 2.0, distance, distance, startAngle, 180.0 * this.getDirection().getFactor(), 0);
    }
}

