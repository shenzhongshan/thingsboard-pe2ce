/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.LegendItem
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.entity.EntityCollection
 *  org.jfree.chart.labels.ItemLabelAnchor
 *  org.jfree.chart.labels.ItemLabelPosition
 *  org.jfree.chart.labels.XYItemLabelGenerator
 *  org.jfree.chart.labels.XYSeriesLabelGenerator
 *  org.jfree.chart.plot.CrosshairState
 *  org.jfree.chart.plot.Marker
 *  org.jfree.chart.plot.PlotOrientation
 *  org.jfree.chart.plot.PlotRenderingInfo
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.renderer.xy.XYBarRenderer
 *  org.jfree.chart.renderer.xy.XYItemRendererState
 *  org.jfree.chart.text.TextUtils
 *  org.jfree.chart.ui.GradientPaintTransformer
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.jfree.chart.util.Args
 *  org.jfree.chart.util.BooleanList
 *  org.jfree.chart.util.PaintList
 *  org.jfree.data.Range
 *  org.jfree.data.general.DatasetUtils
 *  org.jfree.data.xy.IntervalXYDataset
 *  org.jfree.data.xy.TableXYDataset
 *  org.jfree.data.xy.XYDataset
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Color;
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
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.PaintList;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.TbItemRenderer;
import org.thingsboard.server.report.renderer.chart.TbThresholdMarker;
import org.thingsboard.server.report.renderer.chart.TbThresholdPainter;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbXYBarRenderer
extends XYBarRenderer
implements TbItemRenderer {
    private final TbThresholdPainter thresholdPainter;
    private final Map<Integer, Float> itemBorderRadiusMap;
    private Float defaultItemBorderRadius;
    private final BooleanList seriesLabelsVisibleList;
    private boolean defaultSeriesLabelsVisible;
    private Map<Integer, Font> seriesLabelFontMap;
    private Font defaultSeriesLabelFont;
    private PaintList seriesLabelPaintList;
    private transient Paint defaultSeriesLabelPaint;
    private final Map<Integer, XYSeriesLabelGenerator> seriesLabelGeneratorMap;
    private XYSeriesLabelGenerator defaultSeriesLabelGenerator;
    private final BooleanList itemLabelsBackgroundVisibleList;
    private boolean defaultItemLabelsBackgroundVisible;
    private final PaintList itemLabelsBackgroundPaintList;
    private transient Paint defaultItemLabelBackgroundPaint;
    private TbSeriesLegendValuesGenerator seriesLegendValuesGenerator;
    private final boolean stackMode;

    public TbXYBarRenderer(boolean stackMode) {
        this.stackMode = stackMode;
        this.itemBorderRadiusMap = new HashMap<Integer, Float>();
        this.defaultItemBorderRadius = Float.valueOf(0.0f);
        this.itemLabelsBackgroundVisibleList = new BooleanList();
        this.defaultItemLabelsBackgroundVisible = false;
        this.itemLabelsBackgroundPaintList = new PaintList();
        this.defaultItemLabelBackgroundPaint = ColorUtils.safeParseCssColor("rgba(255,255,255,0.56)");
        this.thresholdPainter = new TbThresholdPainter();
        this.seriesLabelsVisibleList = new BooleanList();
        this.defaultSeriesLabelsVisible = false;
        this.seriesLabelFontMap = new HashMap<Integer, Font>();
        this.defaultSeriesLabelFont = AwtFontUtils.newFont("sans-serif", 0, 10.0f);
        this.seriesLabelPaintList = new PaintList();
        this.defaultSeriesLabelPaint = Color.BLACK;
        this.seriesLabelGeneratorMap = new HashMap<Integer, XYSeriesLabelGenerator>();
    }

    public boolean getStackMode() {
        return this.stackMode;
    }

    public boolean isSeriesLabelsVisible(int series) {
        Boolean b = this.seriesLabelsVisibleList.getBoolean(series);
        if (b == null) {
            return this.defaultSeriesLabelsVisible;
        }
        return b;
    }

    public void setSeriesLabelsVisible(int series, boolean visible) {
        this.setSeriesLabelsVisible(series, (Boolean)visible);
    }

    public void setSeriesLabelsVisible(int series, Boolean visible) {
        this.setSeriesLabelsVisible(series, visible, true);
    }

    public void setSeriesLabelsVisible(int series, Boolean visible, boolean notify) {
        this.seriesLabelsVisibleList.setBoolean(series, visible);
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public void clearSeriesLabelsVisible(boolean notify) {
        this.seriesLabelsVisibleList.clear();
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public boolean getDefaultSeriesLabelsVisible() {
        return this.defaultSeriesLabelsVisible;
    }

    public void setDefaultSeriesLabelsVisible(boolean visible) {
        this.setDefaultSeriesLabelsVisible(visible, true);
    }

    public void setDefaultSeriesLabelsVisible(boolean visible, boolean notify) {
        this.defaultSeriesLabelsVisible = visible;
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Font getSeriesLabelFont(int row, int column) {
        Font result = this.getSeriesLabelFont(row);
        if (result == null) {
            result = this.defaultSeriesLabelFont;
        }
        return result;
    }

    public Font getSeriesLabelFont(int series) {
        return this.seriesLabelFontMap.get(series);
    }

    public void setSeriesLabelFont(int series, Font font) {
        this.setSeriesLabelFont(series, font, true);
    }

    public void setSeriesLabelFont(int series, Font font, boolean notify) {
        this.seriesLabelFontMap.put(series, font);
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public void clearSeriesLabelFonts(boolean notify) {
        this.seriesLabelFontMap.clear();
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Font getDefaultSeriesLabelFont() {
        return this.defaultSeriesLabelFont;
    }

    public void setDefaultSeriesLabelFont(Font font) {
        Args.nullNotPermitted((Object)font, (String)"font");
        this.setDefaultSeriesLabelFont(font, true);
    }

    public void setDefaultSeriesLabelFont(Font font, boolean notify) {
        this.defaultSeriesLabelFont = font;
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Paint getSeriesLabelPaint(int row, int column) {
        Paint result = this.getSeriesLabelPaint(row);
        if (result == null) {
            result = this.defaultSeriesLabelPaint;
        }
        return result;
    }

    public Paint getSeriesLabelPaint(int series) {
        return this.seriesLabelPaintList.getPaint(series);
    }

    public void setSeriesLabelPaint(int series, Paint paint) {
        this.setSeriesLabelPaint(series, paint, true);
    }

    public void setSeriesLabelPaint(int series, Paint paint, boolean notify) {
        this.seriesLabelPaintList.setPaint(series, paint);
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public void clearSeriesLabelPaints(boolean notify) {
        this.seriesLabelPaintList.clear();
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public Paint getDefaultSeriesLabelPaint() {
        return this.defaultSeriesLabelPaint;
    }

    public void setDefaultSeriesLabelPaint(Paint paint) {
        this.setDefaultSeriesLabelPaint(paint, true);
    }

    public void setDefaultSeriesLabelPaint(Paint paint, boolean notify) {
        Args.nullNotPermitted((Object)paint, (String)"paint");
        this.defaultSeriesLabelPaint = paint;
        if (notify) {
            this.fireChangeEvent();
        }
    }

    public XYSeriesLabelGenerator getSeriesLabelGenerator(int series) {
        XYSeriesLabelGenerator generator = this.seriesLabelGeneratorMap.get(series);
        if (generator == null) {
            generator = this.defaultSeriesLabelGenerator;
        }
        return generator;
    }

    public void setSeriesLabelGenerator(int series, XYSeriesLabelGenerator generator) {
        this.seriesLabelGeneratorMap.put(series, generator);
        this.fireChangeEvent();
    }

    public XYSeriesLabelGenerator getDefaultSeriesLabelGenerator() {
        return this.defaultSeriesLabelGenerator;
    }

    public void setDefaultSeriesLabelGenerator(XYSeriesLabelGenerator generator) {
        this.defaultSeriesLabelGenerator = generator;
        this.fireChangeEvent();
    }

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

    @Override
    public TbSeriesLegendValuesGenerator getTbSeriesLegendValuesGenerator() {
        return this.seriesLegendValuesGenerator;
    }

    @Override
    public void setTbSeriesLegendValuesGenerator(TbSeriesLegendValuesGenerator seriesLegendValuesGenerator) {
        this.seriesLegendValuesGenerator = seriesLegendValuesGenerator;
    }

    public int getPassCount() {
        return 2;
    }

    public Range findRangeBounds(XYDataset dataset) {
        if (this.stackMode) {
            if (dataset != null) {
                return DatasetUtils.findStackedRangeBounds((TableXYDataset)((TableXYDataset)dataset));
            }
            return null;
        }
        return super.findRangeBounds(dataset);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        double value1;
        double value0;
        if (!this.getItemVisible(series, item)) {
            return;
        }
        IntervalXYDataset intervalDataset = (IntervalXYDataset)dataset;
        if (this.stackMode) {
            double value = intervalDataset.getYValue(series, item);
            if (Double.isNaN(value)) {
                return;
            }
            double positiveBase = 0.0;
            double negativeBase = 0.0;
            for (int i = 0; i < series; ++i) {
                double v = dataset.getYValue(i, item);
                if (Double.isNaN(v) || !this.isSeriesVisible(i)) continue;
                if (v > 0.0) {
                    positiveBase += v;
                    continue;
                }
                negativeBase += v;
            }
            if (value > 0.0) {
                value0 = positiveBase;
                value1 = positiveBase + value;
            } else {
                value0 = negativeBase;
                value1 = negativeBase + value;
            }
        } else {
            if (this.getUseYInterval()) {
                value0 = intervalDataset.getStartYValue(series, item);
                value1 = intervalDataset.getEndYValue(series, item);
            } else {
                value0 = this.getBase();
                value1 = intervalDataset.getYValue(series, item);
            }
            if (Double.isNaN(value0) || Double.isNaN(value1)) {
                return;
            }
            if (value0 <= value1 ? !rangeAxis.getRange().intersects(value0, value1) : !rangeAxis.getRange().intersects(value1, value0)) {
                return;
            }
        }
        double translatedValue0 = rangeAxis.valueToJava2D(value0, dataArea, plot.getRangeAxisEdge());
        double translatedValue1 = rangeAxis.valueToJava2D(value1, dataArea, plot.getRangeAxisEdge());
        double bottom = Math.min(translatedValue0, translatedValue1);
        double top = Math.max(translatedValue0, translatedValue1);
        double startX = intervalDataset.getStartXValue(series, item);
        if (Double.isNaN(startX)) {
            return;
        }
        double endX = intervalDataset.getEndXValue(series, item);
        if (Double.isNaN(endX)) {
            return;
        }
        if (startX <= endX ? !domainAxis.getRange().intersects(startX, endX) : !domainAxis.getRange().intersects(endX, startX)) {
            return;
        }
        if (this.getBarAlignmentFactor() >= 0.0 && this.getBarAlignmentFactor() <= 1.0) {
            double x = intervalDataset.getXValue(series, item);
            double interval = endX - startX;
            startX = x - interval * this.getBarAlignmentFactor();
            endX = startX + interval;
        }
        RectangleEdge location = plot.getDomainAxisEdge();
        double translatedStartX = domainAxis.valueToJava2D(startX, dataArea, location);
        double translatedEndX = domainAxis.valueToJava2D(endX, dataArea, location);
        double translatedWidth = Math.max(1.0, Math.abs(translatedEndX - translatedStartX));
        double left = Math.min(translatedStartX, translatedEndX);
        if (this.getMargin() > 0.0) {
            double cut = translatedWidth * this.getMargin();
            translatedWidth -= cut;
            left += cut / 2.0;
        }
        PlotOrientation orientation = plot.getOrientation();
        boolean positive = value1 > 0.0;
        boolean inverted = rangeAxis.isInverted();
        Float borderRadius = this.getItemBorderRadius(series, item);
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
        Shape bar = null;
        if (orientation.isHorizontal()) {
            bottom = Math.max(bottom, dataArea.getMinX());
            top = Math.min(top, dataArea.getMaxX());
            bar = ChartUtils.createRectangularShapeWithRoundedCorners(bottom, left, top - bottom, translatedWidth, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        } else if (orientation.isVertical()) {
            bottom = Math.max(bottom, dataArea.getMinY());
            top = Math.min(top, dataArea.getMaxY());
            bar = ChartUtils.createRectangularShapeWithRoundedCorners(left, bottom, translatedWidth, top - bottom, radiusTopLeft, radiusTopRight, radiusBottomLeft, radiusBottomRight);
        }
        if (pass == 0) {
            if (state.getElementHinting()) {
                this.beginElementGroup(g2, dataset.getSeriesKey(series), item);
            }
            this.paintBar(g2, series, item, bar);
            if (state.getElementHinting()) {
                this.endElementGroup(g2);
            }
        } else if (pass == 1) {
            Shape savedClip = g2.getClip();
            g2.setClip(null);
            try {
                Paint labelPaint;
                Font labelFont;
                String label;
                XYItemLabelGenerator generator;
                XYSeriesLabelGenerator seriesGenerator;
                Rectangle2D labelBounds = null;
                if (this.isItemLabelVisible(series, item) && (generator = this.getItemLabelGenerator(series, item)) != null && (label = generator.generateLabel(dataset, series, item)) != null) {
                    labelFont = this.getItemLabelFont(series, item);
                    labelPaint = this.getItemLabelPaint(series, item);
                    labelBounds = this.drawLabel(g2, series, item, plot, label, labelFont, labelPaint, bar.getBounds2D(), null, value1 < 0.0);
                }
                if (this.isSeriesLabelsVisible(series) && (seriesGenerator = this.getSeriesLabelGenerator(series)) != null && (label = seriesGenerator.generateLabel(dataset, series)) != null) {
                    labelFont = this.getSeriesLabelFont(series, item);
                    labelPaint = this.getSeriesLabelPaint(series, item);
                    this.drawLabel(g2, series, item, plot, label, labelFont, labelPaint, bar.getBounds2D(), labelBounds, value1 < 0.0);
                }
                double x1 = (startX + endX) / 2.0;
                double y1 = dataset.getYValue(series, item);
                double transX1 = domainAxis.valueToJava2D(x1, dataArea, location);
                int datasetIndex = plot.indexOf(dataset);
                this.updateCrosshairValues(crosshairState, x1, y1, datasetIndex, transX1, translatedValue1, plot.getOrientation());
                EntityCollection entities = state.getEntityCollection();
                if (entities != null) {
                    this.addEntity(entities, bar, dataset, series, item, 0.0, 0.0);
                }
            }
            finally {
                g2.setClip(savedClip);
            }
        }
    }

    public void drawRangeMarker(Graphics2D g2, XYPlot plot, ValueAxis rangeAxis, Marker marker, Rectangle2D dataArea) {
        if (marker instanceof TbThresholdMarker) {
            this.thresholdPainter.paintThresholdMarker(g2, plot, rangeAxis, dataArea, (TbThresholdMarker)marker);
        } else {
            super.drawRangeMarker(g2, plot, rangeAxis, marker, dataArea);
        }
    }

    @Override
    public TbLegendItem getTbLegendItem(TbLegendValuesRequest request, int datasetIndex, int series) {
        LegendItem legendItem;
        if (this.seriesLegendValuesGenerator != null && (legendItem = this.getLegendItem(datasetIndex, series)) != null) {
            XYPlot plot = this.getPlot();
            XYDataset dataset = plot.getDataset(datasetIndex);
            TbLegendValues legendValues = this.seriesLegendValuesGenerator.generateLegendValues(request, dataset, series);
            return new TbLegendItem(legendItem, legendValues);
        }
        return null;
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

    protected Rectangle2D drawLabel(Graphics2D g, int series, int item, XYPlot plot, String label, Font labelFont, Paint labelPaint, Rectangle2D bar, Rectangle2D prevLabelBounds, boolean negative) {
        Point2D anchorPoint;
        String drawLabel;
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setFont(labelFont);
        ItemLabelPosition position = !negative ? this.getPositiveItemLabelPosition(series, item) : this.getNegativeItemLabelPosition(series, item);
        Rectangle2D drawBar = bar;
        if (position.getItemLabelAnchor().isInternal()) {
            if (this.isShowLabelInsideVisibleBar() && g2.getClipBounds() != null) {
                drawBar = drawBar.createIntersection(g2.getClipBounds().getBounds2D());
            }
            Rectangle2D labelBar = this.getItemLabelInsets().createInsetRectangle(drawBar);
            if (this.getMinimumLabelSize() != null && (labelBar.getWidth() < this.getMinimumLabelSize().getWidth() || labelBar.getHeight() < this.getMinimumLabelSize().getHeight())) {
                return null;
            }
        }
        if ((drawLabel = this.calculateLabeltoDraw(label, anchorPoint = this.calculateLabelAnchorPoint(position.getItemLabelAnchor(), drawBar, plot.getOrientation()), position, drawBar, g2)) == null && (position = !negative ? this.getPositiveItemLabelPositionFallback() : this.getNegativeItemLabelPositionFallback()) != null) {
            g2 = (Graphics2D)g.create();
            g2.setFont(labelFont);
            if (position.getItemLabelAnchor().isInternal()) {
                if (this.isShowLabelInsideVisibleBar() && g2.getClipBounds() != null) {
                    drawBar = drawBar.createIntersection(g2.getClipBounds().getBounds2D());
                }
                Rectangle2D labelBar = this.getItemLabelInsets().createInsetRectangle(drawBar);
                if (this.getMinimumLabelSize() != null && (labelBar.getWidth() < this.getMinimumLabelSize().getWidth() || labelBar.getHeight() < this.getMinimumLabelSize().getHeight())) {
                    return null;
                }
            }
            anchorPoint = this.calculateLabelAnchorPoint(position.getItemLabelAnchor(), drawBar, plot.getOrientation());
            drawLabel = this.calculateLabeltoDraw(label, anchorPoint, position, drawBar, g2);
        }
        if (drawLabel != null) {
            float x = (float)anchorPoint.getX();
            float y = (float)anchorPoint.getY();
            boolean drawBackground = this.isItemLabelBackgroundVisible(series, item);
            if (position.getItemLabelAnchor().isInternal()) {
                y -= 15.0f;
                if (prevLabelBounds != null) {
                    y -= 3.0f + (float)prevLabelBounds.getHeight();
                }
            } else {
                float distance;
                float f = distance = drawBackground ? 5.0f : 3.0f;
                if (position.getTextAnchor() == TextAnchor.BOTTOM_CENTER) {
                    y -= distance;
                } else if (position.getTextAnchor() == TextAnchor.TOP_CENTER) {
                    y += distance;
                }
            }
            Rectangle2D bounds = TextUtils.calculateRotatedStringBounds((String)drawLabel, (Graphics2D)g2, (float)x, (float)y, (TextAnchor)position.getTextAnchor(), (double)position.getAngle(), (TextAnchor)position.getRotationAnchor()).getBounds2D();
            if (position.getItemLabelAnchor().isInternal() && bar.getWidth() - bounds.getWidth() < 0.0) {
                return null;
            }
            if (drawBackground) {
                g2.setPaint(this.getItemLabelBackgroundPaint(series, item));
                g2.setStroke(new BasicStroke(0.0f));
                g2.fillRoundRect((int)bounds.getX() - 3, (int)bounds.getY() - 1, (int)bounds.getWidth() + 6, (int)bounds.getHeight() + 4, 4, 4);
            }
            g2.setPaint(labelPaint);
            TextUtils.drawRotatedString((String)drawLabel, (Graphics2D)g2, (float)x, (float)y, (TextAnchor)position.getTextAnchor(), (double)position.getAngle(), (TextAnchor)position.getRotationAnchor());
            return bounds;
        }
        return null;
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

