/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.plot.PlotOrientation
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.text.TextUtils
 *  org.jfree.chart.ui.LengthAdjustmentType
 *  org.jfree.chart.ui.RectangleAnchor
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.jfree.chart.util.ShapeUtils
 *  org.jfree.data.Range
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;
import org.thingsboard.server.report.renderer.chart.TbThresholdMarker;

public class TbThresholdPainter {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void paintThresholdMarker(Graphics2D g2, XYPlot plot, ValueAxis rangeAxis, Rectangle2D dataArea, TbThresholdMarker marker) {
        Shape savedClip = g2.getClip();
        g2.setClip(null);
        try {
            double value = marker.getValue();
            Range range = rangeAxis.getRange();
            if (!range.contains(value)) {
                return;
            }
            double v = rangeAxis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
            PlotOrientation orientation = plot.getOrientation();
            Line2D.Double line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(v, dataArea.getMinY(), v, dataArea.getMaxY());
            } else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(dataArea.getMinX(), v, dataArea.getMaxX(), v);
            } else {
                throw new IllegalStateException("Unrecognised orientation.");
            }
            Composite originalComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(3, marker.getAlpha()));
            g2.setPaint(marker.getPaint());
            g2.setStroke(marker.getStroke());
            g2.draw(line);
            if (marker.getStartShape() != null || marker.getEndShape() != null) {
                double angle;
                double shapeY;
                double shapeX;
                double endY;
                double startY;
                double endX;
                double startX;
                if (orientation == PlotOrientation.HORIZONTAL) {
                    startX = v;
                    endX = v;
                    startY = dataArea.getMinY();
                    endY = dataArea.getMaxY();
                } else {
                    startX = dataArea.getMinX();
                    endX = dataArea.getMaxX();
                    startY = v;
                    endY = v;
                }
                if (marker.getStartShape() != null) {
                    Shape startShape = marker.getStartShape();
                    shapeX = startX;
                    shapeY = startY;
                    angle = orientation == PlotOrientation.HORIZONTAL ? 0.0 : -1.5707963267948966;
                    startShape = ShapeUtils.createTranslatedShape((Shape)startShape, (double)shapeX, (double)shapeY);
                    startShape = ShapeUtils.rotateShape((Shape)startShape, (double)angle, (float)((float)shapeX), (float)((float)shapeY));
                    this.drawThresholdShape(g2, startShape, marker.getPaint(), marker.getStartShapeFillPaint(), marker.getStartShapeOutlineStroke());
                }
                if (marker.getEndShape() != null) {
                    Shape endShape = marker.getEndShape();
                    shapeX = endX;
                    shapeY = endY;
                    angle = orientation == PlotOrientation.HORIZONTAL ? Math.PI : 1.5707963267948966;
                    endShape = ShapeUtils.createTranslatedShape((Shape)endShape, (double)shapeX, (double)shapeY);
                    endShape = ShapeUtils.rotateShape((Shape)endShape, (double)angle, (float)((float)shapeX), (float)((float)shapeY));
                    this.drawThresholdShape(g2, endShape, marker.getPaint(), marker.getEndShapeFillPaint(), marker.getEndShapeOutlineStroke());
                }
            }
            String label = marker.getLabel();
            RectangleAnchor anchor = marker.getLabelAnchor();
            if (label != null) {
                Font labelFont = marker.getLabelFont();
                g2.setFont(labelFont);
                Point2D coords = this.calculateThresholdMarkerTextAnchorPoint(g2, orientation, dataArea, line.getBounds2D(), marker.getLabelOffset(), marker.getLabelOffsetType(), anchor);
                if (marker.isDrawLabelBackground()) {
                    Rectangle2D r = TextUtils.calcAlignedStringBounds((String)label, (Graphics2D)g2, (float)((float)coords.getX()), (float)((float)coords.getY()), (TextAnchor)marker.getLabelTextAnchor());
                    g2.setPaint(marker.getLabelBackgroundColor());
                    g2.setStroke(new BasicStroke(0.0f));
                    g2.fillRoundRect((int)r.getX() - 3, (int)r.getY() - 2, (int)r.getWidth() + 6, (int)r.getHeight() + 4, 4, 4);
                }
                g2.setPaint(marker.getLabelPaint());
                TextUtils.drawAlignedString((String)label, (Graphics2D)g2, (float)((float)coords.getX()), (float)((float)coords.getY()), (TextAnchor)marker.getLabelTextAnchor());
            }
            g2.setComposite(originalComposite);
        }
        finally {
            g2.setClip(savedClip);
        }
    }

    private void drawThresholdShape(Graphics2D g2, Shape shape, Paint paint, Paint fillPaint, Stroke outlineStroke) {
        if (fillPaint != null) {
            g2.setPaint(fillPaint);
        } else {
            g2.setPaint(paint);
        }
        g2.fill(shape);
        if (outlineStroke != null) {
            g2.setPaint(paint);
            g2.setStroke(outlineStroke);
            g2.draw(shape);
        }
    }

    private Point2D calculateThresholdMarkerTextAnchorPoint(Graphics2D g2, PlotOrientation orientation, Rectangle2D dataArea, Rectangle2D markerArea, RectangleInsets markerOffset, LengthAdjustmentType labelOffsetForRange, RectangleAnchor anchor) {
        Rectangle2D anchorRect = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea, LengthAdjustmentType.EXPAND, labelOffsetForRange);
        } else if (orientation == PlotOrientation.VERTICAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea, labelOffsetForRange, LengthAdjustmentType.EXPAND);
        }
        return anchor.getAnchorPoint(anchorRect);
    }
}

