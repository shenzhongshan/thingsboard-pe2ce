/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.AxisSpace
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.labels.ItemLabelAnchor
 *  org.jfree.chart.labels.ItemLabelPosition
 *  org.jfree.chart.plot.CrosshairState
 *  org.jfree.chart.plot.PlotRenderingInfo
 *  org.jfree.chart.plot.PlotState
 *  org.jfree.chart.plot.SeriesRenderingOrder
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.renderer.RendererUtils
 *  org.jfree.chart.renderer.xy.XYItemRenderer
 *  org.jfree.chart.renderer.xy.XYItemRendererState
 *  org.jfree.chart.ui.Layer
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.data.Range
 *  org.jfree.data.general.DatasetUtils
 *  org.jfree.data.xy.XYDataset
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.RendererUtils;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.TbItemRenderer;
import org.thingsboard.server.report.renderer.chart.TbThresholdMarker;
import org.thingsboard.server.report.renderer.chart.TbVisualMap;
import org.thingsboard.server.report.renderer.chart.TbVisualMapArea;
import org.thingsboard.server.report.renderer.chart.TbVisualMapRenderer;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItemSource;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;

public class TbTimeseriesPlot
extends XYPlot
implements TbLegendItemSource {
    private TbVisualMap visualMap;

    public void setVisualMap(TbVisualMap visualMap) {
        this.visualMap = visualMap;
    }

    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
        this.updateInsets(g2, area);
        super.draw(g2, area, anchor, parentState, info);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index, PlotRenderingInfo info, CrosshairState crosshairState) {
        boolean foundData = false;
        XYDataset dataset = this.getDataset(index);
        if (!DatasetUtils.isEmptyOrNull((XYDataset)dataset)) {
            foundData = true;
            ValueAxis xAxis = this.getDomainAxisForDataset(index);
            ValueAxis yAxis = this.getRangeAxisForDataset(index);
            if (xAxis == null || yAxis == null) {
                return foundData;
            }
            XYItemRenderer renderer = this.getRenderer(index);
            if (renderer == null && (renderer = this.getRenderer()) == null) {
                return foundData;
            }
            XYItemRendererState state = renderer.initialise(g2, dataArea, (XYPlot)this, dataset, info);
            if (this.visualMap != null && !this.visualMap.isEmpty() && renderer instanceof TbVisualMapRenderer) {
                TbVisualMapRenderer visualMapRenderer = (TbVisualMapRenderer)renderer;
                int passCount = renderer.getPassCount();
                for (int pass = 0; pass < passCount; ++pass) {
                    if (pass == visualMapRenderer.getAreaPass()) {
                        Shape savedClip = g2.getClip();
                        try {
                            List<TbVisualMapArea> visualAreas = this.visualMap.calculateAreas(yAxis, dataArea);
                            for (TbVisualMapArea visualArea : visualAreas) {
                                g2.setClip(visualArea.getArea());
                                visualMapRenderer.setVisualMap(null);
                                visualMapRenderer.setCurrentVisualMapPaint(visualArea.getPaint());
                                visualMapRenderer.setCurrentVisualMapFillPaint(visualArea.getFillPaint());
                                this.drawSeries(g2, dataArea, xAxis, yAxis, renderer, state, dataset, pass, pass, info, crosshairState);
                            }
                            continue;
                        }
                        finally {
                            g2.setClip(savedClip);
                        }
                    }
                    visualMapRenderer.setCurrentVisualMapPaint(null);
                    visualMapRenderer.setCurrentVisualMapFillPaint(null);
                    if (pass == visualMapRenderer.getItemPass()) {
                        visualMapRenderer.setVisualMap(this.visualMap);
                    } else {
                        visualMapRenderer.setVisualMap(null);
                    }
                    this.drawSeries(g2, dataArea, xAxis, yAxis, renderer, state, dataset, pass, pass, info, crosshairState);
                }
            } else {
                int passCount = renderer.getPassCount();
                int startPass = 0;
                int endPass = passCount - 1;
                this.drawSeries(g2, dataArea, xAxis, yAxis, renderer, state, dataset, startPass, endPass, info, crosshairState);
            }
        }
        return foundData;
    }

    private void drawSeries(Graphics2D g2, Rectangle2D dataArea, ValueAxis xAxis, ValueAxis yAxis, XYItemRenderer renderer, XYItemRendererState state, XYDataset dataset, int startPass, int endPass, PlotRenderingInfo info, CrosshairState crosshairState) {
        SeriesRenderingOrder seriesOrder = this.getSeriesRenderingOrder();
        int passCount = renderer.getPassCount();
        if (seriesOrder == SeriesRenderingOrder.REVERSE) {
            for (int pass = startPass; pass <= endPass; ++pass) {
                int seriesCount = dataset.getSeriesCount();
                for (int series = seriesCount - 1; series >= 0; --series) {
                    int firstItem = 0;
                    int lastItem = dataset.getItemCount(series) - 1;
                    if (lastItem == -1) continue;
                    if (state.getProcessVisibleItemsOnly()) {
                        int[] itemBounds = RendererUtils.findLiveItems((XYDataset)dataset, (int)series, (double)xAxis.getLowerBound(), (double)xAxis.getUpperBound());
                        firstItem = Math.max(itemBounds[0] - 1, 0);
                        lastItem = Math.min(itemBounds[1] + 1, lastItem);
                    }
                    state.startSeriesPass(dataset, series, firstItem, lastItem, pass, passCount);
                    for (int item = firstItem; item <= lastItem; ++item) {
                        renderer.drawItem(g2, state, dataArea, info, (XYPlot)this, xAxis, yAxis, dataset, series, item, crosshairState, pass);
                    }
                    state.endSeriesPass(dataset, series, firstItem, lastItem, pass, passCount);
                }
            }
        } else {
            for (int pass = startPass; pass <= endPass; ++pass) {
                int seriesCount = dataset.getSeriesCount();
                for (int series = 0; series < seriesCount; ++series) {
                    int firstItem = 0;
                    int lastItem = dataset.getItemCount(series) - 1;
                    if (state.getProcessVisibleItemsOnly()) {
                        int[] itemBounds = RendererUtils.findLiveItems((XYDataset)dataset, (int)series, (double)xAxis.getLowerBound(), (double)xAxis.getUpperBound());
                        firstItem = Math.max(itemBounds[0] - 1, 0);
                        lastItem = Math.min(itemBounds[1] + 1, lastItem);
                    }
                    state.startSeriesPass(dataset, series, firstItem, lastItem, pass, passCount);
                    for (int item = firstItem; item <= lastItem; ++item) {
                        renderer.drawItem(g2, state, dataArea, info, (XYPlot)this, xAxis, yAxis, dataset, series, item, crosshairState, pass);
                    }
                    state.endSeriesPass(dataset, series, firstItem, lastItem, pass, passCount);
                }
            }
        }
    }

    private void updateInsets(Graphics2D g2, Rectangle2D area) {
        Rectangle2D measureArea = (Rectangle2D)area.clone();
        RectangleInsets insets = this.getInsets();
        insets.trim(measureArea);
        AxisSpace space = this.calculateAxisSpace(g2, measureArea);
        Rectangle2D dataArea = space.shrink(measureArea, null);
        this.getAxisOffset().trim(dataArea);
        dataArea = this.integerise(dataArea);
        double dataAreaLeft = dataArea.getMinX();
        double dataAreaRight = area.getMaxX() - dataArea.getMaxX();
        boolean hasTopLabels = this.hasTopLabels();
        List<TbThresholdMarker> thresholdMarkers = this.computeVisibleThresholdMarkers();
        double[] offset = new double[]{0.0, 0.0};
        for (TbThresholdMarker marker : thresholdMarkers) {
            double[] markerOffset = marker.measureOffset(g2);
            offset[0] = Math.max(offset[0], markerOffset[0]);
            offset[1] = Math.max(offset[1], markerOffset[1]);
            if (marker.getLabel() == null || marker.getLabelPosition().name().endsWith("Bottom")) continue;
            hasTopLabels = true;
        }
        offset[0] = offset[0] - dataAreaLeft;
        offset[1] = offset[1] - dataAreaRight;
        double minTopOffset = hasTopLabels ? 25.0 : 5.0;
        double topOffset = Math.max(insets.getTop(), minTopOffset);
        if (offset[0] > 0.0 || offset[1] > 0.0 || topOffset > insets.getTop()) {
            double leftOffset = offset[0] > 0.0 ? offset[0] : 0.0;
            double rightOffset = offset[1] > 0.0 ? offset[1] : 0.0;
            this.setInsets(new RectangleInsets(topOffset, leftOffset += insets.getLeft(), insets.getBottom(), rightOffset += insets.getRight()));
        }
    }

    private boolean hasTopLabels() {
        for (XYDataset dataset : this.getDatasets().values()) {
            if (dataset == null) continue;
            int datasetIndex = this.indexOf(dataset);
            XYItemRenderer renderer = this.getRenderer(datasetIndex);
            for (int series = 0; series < dataset.getSeriesCount(); ++series) {
                if (!renderer.isSeriesItemLabelsVisible(series)) continue;
                ItemLabelPosition position = renderer.getSeriesPositiveItemLabelPosition(series);
                if (position.getItemLabelAnchor().equals((Object)ItemLabelAnchor.OUTSIDE12)) {
                    return true;
                }
                position = renderer.getSeriesNegativeItemLabelPosition(series);
                if (!position.getItemLabelAnchor().equals((Object)ItemLabelAnchor.OUTSIDE12)) continue;
                return true;
            }
        }
        return false;
    }

    private List<TbThresholdMarker> computeVisibleThresholdMarkers() {
        ArrayList<TbThresholdMarker> thresholdMarkers = new ArrayList<TbThresholdMarker>();
        for (XYDataset dataset : this.getDatasets().values()) {
            int datasetIndex = this.indexOf(dataset);
            Collection markers = this.getRangeMarkers(datasetIndex, Layer.FOREGROUND);
            ValueAxis axis = this.getRangeAxisForDataset(datasetIndex);
            if (markers == null || axis == null) continue;
            Range range = axis.getRange();
            markers.forEach(marker -> {
                TbThresholdMarker thresholdMarker;
                double value;
                if (marker instanceof TbThresholdMarker && range.contains(value = (thresholdMarker = (TbThresholdMarker)((Object)marker)).getValue())) {
                    thresholdMarkers.add(thresholdMarker);
                }
            });
        }
        return thresholdMarkers;
    }

    private Rectangle integerise(Rectangle2D rect) {
        int x0 = (int)Math.ceil(rect.getMinX());
        int y0 = (int)Math.ceil(rect.getMinY());
        int x1 = (int)Math.floor(rect.getMaxX());
        int y1 = (int)Math.floor(rect.getMaxY());
        return new Rectangle(x0, y0, x1 - x0, y1 - y0);
    }

    @Override
    public List<TbLegendItem> getTbLegendItems(TbLegendValuesRequest request) {
        ArrayList<TbLegendItem> result = new ArrayList<TbLegendItem>();
        for (XYDataset dataset : this.getDatasets().values()) {
            if (dataset == null) continue;
            int datasetIndex = this.indexOf(dataset);
            XYItemRenderer renderer = this.getRenderer(datasetIndex);
            if (renderer == null) {
                renderer = this.getRenderer(0);
            }
            if (!(renderer instanceof TbItemRenderer)) continue;
            TbItemRenderer itemRenderer = (TbItemRenderer)renderer;
            int seriesCount = dataset.getSeriesCount();
            for (int i = 0; i < seriesCount; ++i) {
                TbLegendItem item;
                if (!renderer.isSeriesVisible(i) || !renderer.isSeriesVisibleInLegend(i) || (item = itemRenderer.getTbLegendItem(request, datasetIndex, i)) == null) continue;
                result.add(item);
            }
        }
        return result;
    }
}

