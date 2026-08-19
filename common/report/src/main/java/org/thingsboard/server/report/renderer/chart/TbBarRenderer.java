/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.CategoryAxis
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.entity.EntityCollection
 *  org.jfree.chart.labels.CategoryItemLabelGenerator
 *  org.jfree.chart.labels.ItemLabelAnchor
 *  org.jfree.chart.labels.ItemLabelPosition
 *  org.jfree.chart.plot.CategoryPlot
 *  org.jfree.chart.plot.PlotOrientation
 *  org.jfree.chart.renderer.category.BarRenderer
 *  org.jfree.chart.renderer.category.CategoryItemRendererState
 *  org.jfree.chart.text.TextUtils
 *  org.jfree.chart.ui.GradientPaintTransformer
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.jfree.chart.util.Args
 *  org.jfree.chart.util.BooleanList
 *  org.jfree.chart.util.PaintList
 *  org.jfree.data.ItemKey
 *  org.jfree.data.KeyedValues2DItemKey
 *  org.jfree.data.category.CategoryDataset
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.PaintList;
import org.jfree.data.ItemKey;
import org.jfree.data.KeyedValues2DItemKey;
import org.jfree.data.category.CategoryDataset;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbBarRenderer
extends BarRenderer {
    private final Map<Integer, Float> itemBorderRadiusMap = new HashMap<Integer, Float>();
    private Float defaultItemBorderRadius = Float.valueOf(0.0f);
    private final BooleanList itemLabelsBackgroundVisibleList = new BooleanList();
    private boolean defaultItemLabelsBackgroundVisible = false;
    private final PaintList itemLabelsBackgroundPaintList = new PaintList();
    private transient Paint defaultItemLabelBackgroundPaint = ColorUtils.safeParseCssColor("rgba(255,255,255,0.56)");

    public Float getItemBorderRadius(int row, int column) {
        Float result = this.getSeriesItemBorderRadius(row);
        if (result == null) {
            result = this.defaultItemBorderRadius;
        }
        return result;
    }

    public Float getSeriesItemBorderRadius(int series) {
        return this.itemBorderRadiusMap.get(series);
    }

    public void setSeriesItemBorderRadius(int series, Float borderRadius) {
        this.setSeriesItemBorderRadius(series, borderRadius, true);
    }

    public void setSeriesItemBorderRadius(int series, Float borderRadius, boolean notify) {
        this.itemBorderRadiusMap.put(series, borderRadius);
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Float getDefaultItemBorderRadius() {
        return this.defaultItemBorderRadius;
    }

    public void setDefaultItemBorderRadius(Float borderRadius) {
        this.setDefaultItemBorderRadius(borderRadius, true);
    }

    public void setDefaultItemBorderRadius(Float borderRadius, boolean notify) {
        Args.nullNotPermitted((Object)borderRadius, (String)"borderRadius");
        this.defaultItemBorderRadius = borderRadius;
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public boolean isItemLabelBackgroundVisible(int row, int column) {
        return this.isSeriesItemLabelsBackgroundVisible(row);
    }

    public boolean isSeriesItemLabelsBackgroundVisible(int series) {
        Boolean b = this.itemLabelsBackgroundVisibleList.getBoolean(series);
        if (b == null) {
            return this.defaultItemLabelsBackgroundVisible;
        }
        return b;
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, boolean visible) {
        this.setSeriesItemLabelsBackgroundVisible(series, (Boolean)visible);
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, Boolean visible) {
        this.setSeriesItemLabelsBackgroundVisible(series, visible, true);
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, Boolean visible, boolean notify) {
        this.itemLabelsBackgroundVisibleList.setBoolean(series, visible);
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public boolean getDefaultItemLabelsBackgroundVisible() {
        return this.defaultItemLabelsBackgroundVisible;
    }

    public void setDefaultItemLabelsBackgroundVisible(boolean visible) {
        this.setDefaultItemLabelsBackgroundVisible(visible, true);
    }

    public void setDefaultItemLabelsBackgroundVisible(boolean visible, boolean notify) {
        this.defaultItemLabelsBackgroundVisible = visible;
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Paint getItemLabelBackgroundPaint(int row, int column) {
        Paint result = this.getSeriesItemLabelsBackgroundPaint(row);
        if (result == null) {
            result = this.defaultItemLabelBackgroundPaint;
        }
        return result;
    }

    public Paint getSeriesItemLabelsBackgroundPaint(int series) {
        return this.itemLabelsBackgroundPaintList.getPaint(series);
    }

    public void setSeriesItemLabelsBackgroundPaint(int series, Paint paint) {
        this.setSeriesItemLabelsBackgroundPaint(series, paint, true);
    }

    public void setSeriesItemLabelsBackgroundPaint(int series, Paint paint, boolean notify) {
        this.itemLabelsBackgroundPaintList.setPaint(series, paint);
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Paint getDefaultItemLabelsBackgroundPaint() {
        return this.defaultItemLabelBackgroundPaint;
    }

    public void setDefaultItemLabelsBackgroundPaint(Paint paint) {
        this.setDefaultItemLabelsBackgroundPaint(paint, true);
    }

    public void setDefaultItemLabelsBackgroundPaint(Paint paint, boolean notify) {
        Args.nullNotPermitted((Object)paint, (String)"paint");
        this.defaultItemLabelBackgroundPaint = paint;
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public int getPassCount() {
        return 2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {
        int visibleRow = state.getVisibleSeriesIndex(row);
        if (visibleRow < 0) {
            return;
        }
        Number dataValue = dataset.getValue(row, column);
        if (dataValue == null) {
            return;
        }
        double value = dataValue.doubleValue();
        PlotOrientation orientation = plot.getOrientation();
        double barW0 = this.calculateBarW0(plot, orientation, dataArea, domainAxis, state, visibleRow, column);
        double[] barL0L1 = this.calculateBarL0L1(value);
        if (barL0L1 == null) {
            return;
        }
        RectangleEdge edge = plot.getRangeAxisEdge();
        double transL0 = rangeAxis.valueToJava2D(barL0L1[0], dataArea, edge);
        double transL1 = rangeAxis.valueToJava2D(barL0L1[1], dataArea, edge);
        boolean positive = value >= this.getBase();
        boolean inverted = rangeAxis.isInverted();
        double barL0 = Math.min(transL0, transL1);
        double barLength = Math.abs(transL1 - transL0);
        double barLengthAdj = 0.0;
        if (barLength > 0.0 && barLength < this.getMinimumBarLength()) {
            barLengthAdj = this.getMinimumBarLength() - barLength;
        }
        double barL0Adj = 0.0;
        if (orientation == PlotOrientation.HORIZONTAL) {
            if (positive && inverted || !positive && !inverted) {
                barL0Adj = barLengthAdj;
                RectangleEdge barBase = RectangleEdge.RIGHT;
            } else {
                RectangleEdge barBase = RectangleEdge.LEFT;
            }
        } else if (positive && !inverted || !positive && inverted) {
            barL0Adj = barLengthAdj;
            RectangleEdge barBase = RectangleEdge.BOTTOM;
        } else {
            RectangleEdge barBase = RectangleEdge.TOP;
        }
        Float borderRadius = this.getItemBorderRadius(row, column);
        double radiusTopLeft = 0.0;
        double radiusTopRight = 0.0;
        double radiusBottomRight = 0.0;
        double radiusBottomLeft = 0.0;
        if (orientation.isHorizontal()) {
            if (positive && inverted || !positive && !inverted) {
                radiusTopLeft = borderRadius.floatValue();
                radiusBottomLeft = borderRadius.floatValue();
            } else {
                radiusTopRight = borderRadius.floatValue();
                radiusBottomRight = borderRadius.floatValue();
            }
        } else if (positive && !inverted || !positive && inverted) {
            radiusTopLeft = borderRadius.floatValue();
            radiusTopRight = borderRadius.floatValue();
        } else {
            radiusBottomLeft = borderRadius.floatValue();
            radiusBottomRight = borderRadius.floatValue();
        }
        Shape bar = orientation == PlotOrientation.HORIZONTAL ? ChartUtils.createRectangularShapeWithRoundedCorners(barL0 - barL0Adj, barW0, barLength + barLengthAdj, state.getBarWidth(), radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight) : ChartUtils.createRectangularShapeWithRoundedCorners(barW0, barL0 - barL0Adj, state.getBarWidth(), barLength + barLengthAdj, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        if (pass == 0) {
            if (state.getElementHinting()) {
                KeyedValues2DItemKey key = new KeyedValues2DItemKey(dataset.getRowKey(row), dataset.getColumnKey(column));
                this.beginElementGroup(g2, (ItemKey)key);
            }
            this.paintBar(g2, row, column, bar);
            if (state.getElementHinting()) {
                this.endElementGroup(g2);
            }
        } else if (pass == 1) {
            Shape savedClip = g2.getClip();
            g2.setClip(null);
            try {
                String label;
                CategoryItemLabelGenerator generator;
                if (this.isItemLabelVisible(row, column) && (generator = this.getItemLabelGenerator(row, column)) != null && (label = generator.generateLabel(dataset, row, column)) != null) {
                    Font labelFont = this.getItemLabelFont(row, column);
                    Paint labelPaint = this.getItemLabelPaint(row, column);
                    this.drawLabel(g2, row, column, plot, label, labelFont, labelPaint, bar.getBounds2D(), value < 0.0);
                }
                int datasetIndex = plot.indexOf(dataset);
                this.updateCrosshairValues(state.getCrosshairState(), dataset.getRowKey(row), dataset.getColumnKey(column), value, datasetIndex, barW0, barL0, orientation);
                EntityCollection entities = state.getEntityCollection();
                if (entities != null) {
                    this.addItemEntity(entities, dataset, row, column, bar);
                }
            }
            finally {
                g2.setClip(savedClip);
            }
        }
    }

    protected void paintBar(Graphics2D g2, int row, int column, Shape bar) {
        Paint itemPaint = this.getItemPaint(row, column);
        GradientPaintTransformer t = this.getGradientPaintTransformer();
        if (t != null && itemPaint instanceof GradientPaint) {
            itemPaint = t.transform((GradientPaint)itemPaint, bar);
        }
        g2.setPaint(itemPaint);
        g2.fill(bar);
        if (this.isDrawBarOutline()) {
            Stroke stroke = this.getItemOutlineStroke(row, column);
            Paint paint = this.getItemOutlinePaint(row, column);
            if (stroke != null && paint != null) {
                g2.setStroke(stroke);
                g2.setPaint(paint);
                g2.draw(bar);
            }
        }
    }

    protected void drawLabel(Graphics2D g, int series, int item, CategoryPlot plot, String label, Font labelFont, Paint labelPaint, Rectangle2D bar, boolean negative) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setFont(labelFont);
        ItemLabelPosition position = !negative ? this.getPositiveItemLabelPosition(series, item) : this.getNegativeItemLabelPosition(series, item);
        Rectangle2D drawBar = bar;
        Point2D anchorPoint = this.calculateLabelAnchorPoint(position.getItemLabelAnchor(), drawBar, plot.getOrientation());
        String drawLabel = this.calculateLabeltoDraw(label, anchorPoint, position, drawBar, g2);
        if (drawLabel == null && (position = !negative ? this.getPositiveItemLabelPositionFallback() : this.getNegativeItemLabelPositionFallback()) != null) {
            g2 = (Graphics2D)g.create();
            g2.setFont(labelFont);
            anchorPoint = this.calculateLabelAnchorPoint(position.getItemLabelAnchor(), drawBar, plot.getOrientation());
            drawLabel = this.calculateLabeltoDraw(label, anchorPoint, position, drawBar, g2);
        }
        if (drawLabel != null) {
            float distance;
            float x = (float)anchorPoint.getX();
            float y = (float)anchorPoint.getY();
            boolean drawBackground = this.isItemLabelBackgroundVisible(series, item);
            float f = distance = drawBackground ? 5.0f : 3.0f;
            if (position.getTextAnchor() == TextAnchor.BOTTOM_CENTER) {
                y -= distance;
            } else if (position.getTextAnchor() == TextAnchor.TOP_CENTER) {
                y += distance;
            }
            Rectangle2D bounds = TextUtils.calculateRotatedStringBounds((String)drawLabel, (Graphics2D)g2, (float)x, (float)y, (TextAnchor)position.getTextAnchor(), (double)position.getAngle(), (TextAnchor)position.getRotationAnchor()).getBounds2D();
            if (drawBackground) {
                g2.setPaint(this.getItemLabelBackgroundPaint(series, item));
                g2.setStroke(new BasicStroke(0.0f));
                g2.fillRoundRect((int)bounds.getX() - 3, (int)bounds.getY() - 1, (int)bounds.getWidth() + 6, (int)bounds.getHeight() + 4, 4, 4);
            }
            g2.setPaint(labelPaint);
            TextUtils.drawRotatedString((String)drawLabel, (Graphics2D)g2, (float)x, (float)y, (TextAnchor)position.getTextAnchor(), (double)position.getAngle(), (TextAnchor)position.getRotationAnchor());
        }
    }

    private String calculateLabeltoDraw(String label, Point2D anchorPoint, ItemLabelPosition position, Rectangle2D bar, Graphics2D g2) {
        if (!position.getItemLabelAnchor().isInternal()) {
            return label;
        }
        Rectangle labelBar = this.getItemLabelInsets().createInsetRectangle(bar).getBounds();
        switch (position.getItemLabelClip()) {
            case CLIP: {
                Shape currentClip = g2.getClip();
                if (currentClip == null) {
                    g2.setClip(labelBar);
                } else {
                    g2.setClip(((Rectangle2D)labelBar).createIntersection(currentClip.getBounds2D()));
                }
                return label;
            }
            case NONE: {
                return label;
            }
        }
        String result = label;
        block9: while (result != null && !result.isEmpty()) {
            Rectangle2D labelBounds = TextUtils.calculateRotatedStringBounds((String)result, (Graphics2D)g2, (float)((float)anchorPoint.getX()), (float)((float)anchorPoint.getY()), (TextAnchor)position.getTextAnchor(), (double)position.getAngle(), (TextAnchor)position.getRotationAnchor()).getBounds2D();
            if (((RectangularShape)labelBar).getHeight() >= labelBounds.getHeight() && ((RectangularShape)labelBar).getWidth() >= labelBounds.getWidth()) {
                return result;
            }
            if (((RectangularShape)labelBar).getHeight() < labelBounds.getHeight()) {
                return null;
            }
            switch (position.getItemLabelClip()) {
                case FIT: {
                    return null;
                }
                case TRUNCATE: {
                    String nextResult = result.replaceFirst(".(\\.{3})?$", "...");
                    if ("...".equals(nextResult) || result.equals(nextResult)) {
                        return null;
                    }
                    result = nextResult;
                    continue block9;
                }
                case TRUNCATE_WORD: {
                    String nextResult = result.replaceFirst("\\W+\\w*(\\.{3})?$", "...");
                    if ("...".equals(nextResult) || result.equals(nextResult)) {
                        return null;
                    }
                    result = nextResult;
                    continue block9;
                }
            }
            throw new IllegalStateException("Should never happen");
        }
        return null;
    }

    private Point2D calculateLabelAnchorPoint(ItemLabelAnchor anchor, Rectangle2D bar, PlotOrientation orientation) {
        Point2D.Double result = null;
        RectangleInsets labelInsets = this.getItemLabelInsets();
        Rectangle2D insideBar = labelInsets.createInsetRectangle(bar);
        Rectangle2D outsideBar = labelInsets.createOutsetRectangle(bar);
        if (anchor == ItemLabelAnchor.CENTER) {
            result = new Point2D.Double(bar.getCenterX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.INSIDE1 || anchor == ItemLabelAnchor.INSIDE2) {
            result = new Point2D.Double(insideBar.getMaxX(), insideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.INSIDE3) {
            result = new Point2D.Double(insideBar.getMaxX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.INSIDE4 || anchor == ItemLabelAnchor.INSIDE5) {
            result = new Point2D.Double(insideBar.getMaxX(), insideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.INSIDE6) {
            result = new Point2D.Double(bar.getCenterX(), insideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.INSIDE7 || anchor == ItemLabelAnchor.INSIDE8) {
            result = new Point2D.Double(insideBar.getMinX(), insideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.INSIDE9) {
            result = new Point2D.Double(insideBar.getMinX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.INSIDE10 || anchor == ItemLabelAnchor.INSIDE11) {
            result = new Point2D.Double(insideBar.getMinX(), insideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.INSIDE12) {
            result = new Point2D.Double(bar.getCenterX(), insideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE1 || anchor == ItemLabelAnchor.OUTSIDE2) {
            result = new Point2D.Double(outsideBar.getMaxX(), outsideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE3) {
            result = new Point2D.Double(outsideBar.getMaxX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE4 || anchor == ItemLabelAnchor.OUTSIDE5) {
            result = new Point2D.Double(outsideBar.getMaxX(), outsideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE6) {
            result = new Point2D.Double(bar.getCenterX(), outsideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE7 || anchor == ItemLabelAnchor.OUTSIDE8) {
            result = new Point2D.Double(outsideBar.getMinX(), outsideBar.getMaxY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE9) {
            result = new Point2D.Double(outsideBar.getMinX(), bar.getCenterY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE10 || anchor == ItemLabelAnchor.OUTSIDE11) {
            result = new Point2D.Double(outsideBar.getMinX(), outsideBar.getMinY());
        } else if (anchor == ItemLabelAnchor.OUTSIDE12) {
            result = new Point2D.Double(bar.getCenterX(), outsideBar.getMinY());
        }
        return result;
    }
}

