/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.Axis
 *  org.jfree.chart.axis.AxisLocation
 *  org.jfree.chart.axis.DateTickUnit
 *  org.jfree.chart.axis.DateTickUnitType
 *  org.jfree.chart.axis.NumberTickUnit
 *  org.jfree.chart.axis.TickUnit
 *  org.jfree.chart.axis.TickUnitSource
 *  org.jfree.chart.axis.TickUnits
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.plot.XYPlot
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.data.time.SimpleTimePeriod
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.report.configuration.DataKeySettings
 *  org.thingsboard.server.common.data.report.configuration.chart.AxisPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartLineType
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartShape
 *  org.thingsboard.server.common.data.report.configuration.chart.FormatTimeUnit
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartXAxisSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.time.SimpleTimePeriod;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLineType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.FormatTimeUnit;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartXAxisSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.report.context.chart.TsChartSeriesData;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;
import org.thingsboard.server.report.renderer.chart.TbDatasetKey;
import org.thingsboard.server.report.renderer.chart.TbDateAxis;
import org.thingsboard.server.report.renderer.chart.TbNumberAxis;
import org.thingsboard.server.report.renderer.chart.TbStateTick;
import org.thingsboard.server.report.renderer.chart.TbThresholdMarker;
import org.thingsboard.server.report.renderer.chart.TimeseriesBarRenderCtx;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public interface ChartUtils {
    public static TimeSeriesChartKeySettings getSeriesSettings(TsChartSeriesData chartSeriesData) {
        DataKeySettings settings = chartSeriesData.getDataKey().getSettings();
        TimeSeriesChartKeySettings keySettings = null;
        if (settings instanceof TimeSeriesChartKeySettings) {
            keySettings = (TimeSeriesChartKeySettings)settings;
        }
        return new TimeSeriesChartKeySettings(keySettings);
    }

    public static SimpleTimePeriod calculateBarTimePeriod(TsChartSeriesEntry entry, TimeseriesBarRenderCtx barRenderCtx, int barsCount, int barIndex) {
        long time = entry.getTs();
        long start = entry.getInterval().startTs;
        long end = entry.getInterval().endTs;
        long interval = end - start;
        if (barRenderCtx.isNoAggregation()) {
            interval = barRenderCtx.isNoAggregationWidthRelative() ? (long)((double)barRenderCtx.getTimeWindow() * barRenderCtx.getNoAggregationWidth() / 100.0) : (long)barRenderCtx.getNoAggregationWidth();
            start = time - interval / 2L;
        }
        double barGapRatio = barRenderCtx.getBarGap();
        double intervalGapRatio = barRenderCtx.getIntervalGap();
        boolean separateBar = barRenderCtx.isNoAggregation() && TimeSeriesChartNoAggregationBarWidthStrategy.separate.equals((Object)barRenderCtx.getNoAggregationBarWidthStrategy());
        long barInterval = separateBar ? interval : (long)((double)interval / ((double)barsCount + barGapRatio * (double)(barsCount - 1) + intervalGapRatio * 2.0));
        long intervalGap = (long)((double)barInterval * intervalGapRatio);
        long barGap = (long)((double)barInterval * barGapRatio);
        long startTime = separateBar ? start : start + intervalGap + (barInterval + barGap) * (long)barIndex;
        long endTime = startTime + barInterval;
        return new SimpleTimePeriod(startTime, endTime);
    }

    public static TbDateAxis createXAxis(XYPlot plot, TimeSeriesChartXAxisSettings xAxisSettings, TimeIntervalCalculator.TimeRange timeRange, TimeZone timeZone, int index) {
        Locale locale = Locale.getDefault();
        TbDateAxis xAxis = new TbDateAxis(xAxisSettings.getLabel(), timeZone, locale);
        plot.setDomainAxis(index, (ValueAxis)xAxis);
        xAxis.setStandardTickUnits(ChartUtils.createDateTickUnitsFromTicksFormat(xAxisSettings.getTicksFormat(), timeZone, locale));
        AxisLocation location = AxisPosition.bottom.equals((Object)xAxisSettings.getPosition()) ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT;
        plot.setDomainAxisLocation(index, location);
        xAxis.setMinimumDate(new Date(timeRange.startTs));
        xAxis.setMaximumDate(new Date(timeRange.endTs));
        xAxis.setGridlinesVisible(xAxisSettings.getShowSplitLines());
        xAxis.setGridlinePaint(ColorUtils.safeParseCssColor(xAxisSettings.getSplitLinesColor()));
        xAxis.setTickLabelInsets(new RectangleInsets(Axis.DEFAULT_TICK_LABEL_INSETS.getTop(), Axis.DEFAULT_TICK_LABEL_INSETS.getLeft() + 0.5, Axis.DEFAULT_TICK_LABEL_INSETS.getBottom(), Axis.DEFAULT_TICK_LABEL_INSETS.getRight() + 0.5));
        ChartUtils.setupAxisAppearance((ValueAxis)xAxis, (TimeSeriesChartAxisSettings)xAxisSettings);
        return xAxis;
    }

    public static TbNumberAxis createYAxis(XYPlot plot, TimeSeriesChartYAxisSettings yAxisSettings, List<TbStateTick> stateTicks, String units, Integer decimals, int index) {
        TbNumberAxis parent = null;
        if (index > 0) {
            parent = (TbNumberAxis)plot.getRangeAxis();
        }
        TbNumberAxis yAxis = new TbNumberAxis(yAxisSettings.getLabel(), parent);
        plot.setRangeAxis(index, (ValueAxis)yAxis);
        AxisLocation location = AxisPosition.left.equals((Object)yAxisSettings.getPosition()) ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.TOP_OR_RIGHT;
        plot.setRangeAxisLocation(index, location);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setGridlinesVisible(yAxisSettings.getShowSplitLines());
        yAxis.setGridlinePaint(ColorUtils.safeParseCssColor(yAxisSettings.getSplitLinesColor()));
        yAxis.setStateTicks(stateTicks);
        if (yAxisSettings.getSplitNumber() != null) {
            yAxis.setSplitNumber(yAxisSettings.getSplitNumber());
        } else if (yAxisSettings.getInterval() != null && yAxisSettings.getInterval() > 0.0) {
            yAxis.setTickUnit(new NumberTickUnit(yAxisSettings.getInterval().doubleValue()));
        }
        if (yAxisSettings.getMin() != null) {
            yAxis.setAxisMin(yAxisSettings.getMin());
        }
        if (yAxisSettings.getMax() != null) {
            yAxis.setAxisMax(yAxisSettings.getMax());
        }
        int axisDecimals = yAxisSettings.getDecimals() != null ? yAxisSettings.getDecimals() : (decimals != null ? decimals : 2);
        String axisUnits = yAxisSettings.getUnits() != null ? yAxisSettings.getUnits() : units;
        yAxis.setNumberFormatOverride(ChartUtils.createValueFormatter(axisDecimals, axisUnits));
        ChartUtils.setupAxisAppearance((ValueAxis)yAxis, (TimeSeriesChartAxisSettings)yAxisSettings);
        return yAxis;
    }

    public static TbThresholdMarker createThresholdMarker(TsChartThresholdItem item, String units, Integer decimals) {
        TbThresholdMarker marker = new TbThresholdMarker(item.getValue());
        TimeSeriesChartThreshold threshold = item.getSettings();
        marker.setPaint(ColorUtils.safeParseCssColor(threshold.getLineColor()));
        marker.setStroke(ChartUtils.createLineStroke(threshold.getLineType(), threshold.getLineWidth()));
        marker.setStartSymbol(threshold.getStartSymbol(), threshold.getStartSymbolSize().floatValue());
        marker.setEndSymbol(threshold.getEndSymbol(), threshold.getEndSymbolSize().floatValue());
        if (threshold.getShowLabel().booleanValue()) {
            int thresholdDecimals = threshold.getDecimals() != null ? threshold.getDecimals() : (decimals != null ? decimals : 2);
            String thresholdUnits = threshold.getUnits() != null ? threshold.getUnits() : units;
            NumberFormat formatter = ChartUtils.createValueFormatter(thresholdDecimals, thresholdUnits);
            String label = formatter.format(item.getValue());
            marker.setLabel(label);
            marker.setLabelPaint(ColorUtils.safeParseCssColor(threshold.getLabelColor()));
            marker.setLabelFont(AwtFontUtils.toAwtFont(threshold.getLabelFont()));
            if (threshold.getEnableLabelBackground().booleanValue()) {
                marker.setDrawLabelBackground(true);
                marker.setLabelBackgroundColor(ColorUtils.safeParseCssColor(threshold.getLabelBackground()));
            }
            marker.setLabelPosition(threshold.getLabelPosition());
        }
        return marker;
    }

    public static NumberFormat createValueFormatter(int decimals, String units) {
        StringBuilder patternBuilder = new StringBuilder("#");
        if (decimals > 0) {
            patternBuilder.append(".");
        }
        patternBuilder.append("#".repeat(Math.max(0, decimals)));
        if (StringUtils.isNotBlank((String)units)) {
            patternBuilder.append(" '").append(units).append("'");
        }
        return new DecimalFormat(patternBuilder.toString());
    }

    public static void adjustAxisMargins(Axis axis, double top, double left, double bottom, double right) {
        if (StringUtils.isBlank((String)axis.getLabel())) {
            axis.setLabel(" ");
            axis.setLabelFont(AwtFontUtils.ZERO_FONT);
        }
        axis.setLabelInsets(new RectangleInsets(Axis.DEFAULT_AXIS_LABEL_INSETS.getTop() + top, Axis.DEFAULT_AXIS_LABEL_INSETS.getLeft() + left, Axis.DEFAULT_AXIS_LABEL_INSETS.getBottom() + bottom, Axis.DEFAULT_AXIS_LABEL_INSETS.getRight() + right));
    }

    public static Shape createSeriesShape(ChartShape chartShape, float size) {
        Shape result = null;
        double delta = (double)size / 2.0;
        switch (chartShape) {
            case emptyCircle: 
            case circle: {
                result = new Ellipse2D.Double(-delta, -delta, size, size);
                break;
            }
            case rect: {
                result = new Rectangle2D.Double(-delta, -delta, size, size);
                break;
            }
            case roundRect: {
                result = new RoundRectangle2D.Double(-delta, -delta, size, size, size / 4.0f, size / 4.0f);
                break;
            }
            case triangle: {
                int[] xpoints = new int[]{0, (int)delta, (int)(-delta)};
                int[] ypoints = new int[]{(int)(-delta), (int)delta, (int)delta};
                result = new Polygon(xpoints, ypoints, 3);
                break;
            }
            case diamond: {
                int[] xpoints = new int[]{0, (int)delta, 0, (int)(-delta)};
                int[] ypoints = new int[]{(int)(-delta), 0, (int)delta, 0};
                result = new Polygon(xpoints, ypoints, 4);
                break;
            }
            case pin: {
                result = ChartUtils.createPin(0.0, 0.0, size, size);
                break;
            }
            case arrow: {
                result = ChartUtils.createArrow(0.0, 0.0, size, size);
                break;
            }
        }
        return result;
    }

    public static Stroke createLineStroke(ChartLineType lineType, Float lineWidth) {
        float[] dashPattern = null;
        switch (lineType) {
            case solid: {
                break;
            }
            case dashed: {
                dashPattern = new float[]{4.0f * lineWidth.floatValue(), 2.0f * lineWidth.floatValue()};
                break;
            }
            case dotted: {
                dashPattern = new float[]{lineWidth.floatValue()};
            }
        }
        return new BasicStroke(lineWidth.floatValue(), 0, 2, 10.0f, dashPattern, 0.0f);
    }

    public static Paint createFillPaint(ChartFillSettings fillSettings, Color seriesColor) {
        switch (fillSettings.getType()) {
            case none: {
                return ColorUtils.TRANSPARENT;
            }
            case opacity: {
                return ColorUtils.applyOpacity(seriesColor, fillSettings.getOpacity().floatValue());
            }
            case gradient: {
                Color startColor = ColorUtils.setOpacity(seriesColor, fillSettings.getGradient().getStart().floatValue() / 100.0f);
                Color endColor = ColorUtils.setOpacity(seriesColor, fillSettings.getGradient().getEnd().floatValue() / 100.0f);
                return new GradientPaint(0.0f, 0.0f, startColor, 1.0f, 1.0f, endColor);
            }
        }
        return ColorUtils.TRANSPARENT;
    }

    public static Paint createFillPaint(boolean fillArea, float fillAreaOpacity, Color seriesColor) {
        if (!fillArea) {
            return ColorUtils.TRANSPARENT;
        }
        return ColorUtils.applyOpacity(seriesColor, fillAreaOpacity);
    }

    public static Map<TbDatasetKey, List<TsChartSeriesData>> datasetGroupsFromSeries(List<TsChartSeriesData> rawSeries) {
        Map<TbDatasetKey, List<TsChartSeriesData>> groupedSeries = rawSeries.stream().collect(Collectors.groupingBy(s -> new TbDatasetKey(ChartUtils.getSeriesSettings(s), s.getDataSource().isComparison()))).entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        int datasetIndex = 0;
        for (Map.Entry<TbDatasetKey, List<TsChartSeriesData>> entry : groupedSeries.entrySet()) {
            TbDatasetKey key = entry.getKey();
            List<TsChartSeriesData> series = entry.getValue();
            key.setDatasetIndex(datasetIndex);
            series.sort(Comparator.comparing(TsChartSeriesData::getIndex));
            int seriesIndex = 0;
            for (TsChartSeriesData seriesItem : series) {
                seriesItem.setDatasetIndex(datasetIndex);
                seriesItem.setSeriesIndex(seriesIndex);
                ++seriesIndex;
            }
            ++datasetIndex;
        }
        return groupedSeries;
    }

    public static Shape createRectangularShapeWithRoundedCorners(double x, double y, double width, double height, double radiusTopLeft, double radiusTopRight, double radiusBottomRight, double radiusBottomLeft) {
        double maxRadius = Math.min(width, height) / 2.0;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + radiusTopLeft, y);
        path.lineTo(x + width - radiusTopRight, y);
        if (radiusTopRight > 0.0) {
            radiusTopRight = Math.min(maxRadius, radiusTopRight);
            path.append(new Arc2D.Double(x + width - radiusTopRight * 2.0, y, radiusTopRight * 2.0, radiusTopRight * 2.0, 90.0, -90.0, 0), true);
        }
        path.lineTo(x + width, y + height - radiusBottomRight);
        if (radiusBottomRight > 0.0) {
            radiusBottomRight = Math.min(maxRadius, radiusBottomRight);
            path.append(new Arc2D.Double(x + width - radiusBottomRight * 2.0, y + height - radiusBottomRight * 2.0, radiusBottomRight * 2.0, radiusBottomRight * 2.0, 0.0, -90.0, 0), true);
        }
        path.lineTo(x + radiusBottomLeft, y + height);
        if (radiusBottomLeft > 0.0) {
            radiusBottomLeft = Math.min(maxRadius, radiusBottomLeft);
            path.append(new Arc2D.Double(x, y + height - radiusBottomLeft * 2.0, radiusBottomLeft * 2.0, radiusBottomLeft * 2.0, -90.0, -90.0, 0), true);
        }
        path.lineTo(x, y + radiusTopLeft);
        if (radiusTopLeft > 0.0) {
            radiusTopLeft = Math.min(maxRadius, radiusTopLeft);
            path.append(new Arc2D.Double(x, y, radiusTopLeft * 2.0, radiusTopLeft * 2.0, 180.0, -90.0, 0), true);
        }
        path.closePath();
        return path;
    }

    public static List<Point2D> interpolateBezier(List<Point2D> points, float smooth, int bezierNumPoints) {
        ArrayList<Point2D> interpolatedPoints = new ArrayList<Point2D>();
        int len = points.size();
        for (int i = 0; i < len; i += ChartUtils.calculateBezierSegmentPoints(points, i, smooth, bezierNumPoints, interpolatedPoints) + 1) {
        }
        return interpolatedPoints;
    }

    private static int calculateBezierSegmentPoints(List<Point2D> points, int start, float smooth, int bezierNumPoints, List<Point2D> targetPoints) {
        int k;
        int idx = start;
        double cpx0 = 0.0;
        double cpy0 = 0.0;
        double cpx1 = 0.0;
        double cpy1 = 0.0;
        double prevX = 0.0;
        double prevY = 0.0;
        for (k = 0; k < points.size() && idx < points.size(); ++k) {
            Point2D point = points.get(idx);
            double x = point.getX();
            double y = point.getY();
            if (idx == start) {
                cpx0 = x;
                cpy0 = y;
                targetPoints.add(new Point2D.Double(x, y));
            } else {
                double dx = x - prevX;
                double dy = y - prevY;
                if (dx * dx + dy * dy < 0.5) {
                    ++idx;
                    continue;
                }
                if (smooth > 0.0f) {
                    double nextY;
                    int nextIdx = idx + 1;
                    Point2D nextPoint = nextIdx < points.size() ? points.get(nextIdx) : null;
                    double nextX = nextPoint != null ? nextPoint.getX() : 0.0;
                    double d = nextY = nextPoint != null ? nextPoint.getY() : 0.0;
                    while (nextX == x && nextY == y && k < points.size()) {
                        ++k;
                        nextPoint = ++nextIdx < points.size() ? points.get(nextIdx) : null;
                        nextX = nextPoint != null ? nextPoint.getX() : 0.0;
                        nextY = nextPoint != null ? nextPoint.getY() : 0.0;
                        point = points.get(++idx);
                        x = point.getX();
                        y = point.getY();
                        dx = x - prevX;
                        dy = y - prevY;
                    }
                    int tempK = k + 1;
                    double ratioNextSeg = 0.5;
                    double vx = 0.0;
                    double vy = 0.0;
                    double nextCpx0 = 0.0;
                    double nextCpy0 = 0.0;
                    if (tempK >= points.size()) {
                        cpx1 = x;
                        cpy1 = y;
                    } else {
                        vx = nextX - prevX;
                        vy = nextY - prevY;
                        double dx0 = x - prevX;
                        double dx1 = nextX - x;
                        double dy0 = y - prevY;
                        double dy1 = nextY - y;
                        double lenPrevSeg = Math.sqrt(dx0 * dx0 + dy0 * dy0);
                        double lenNextSeg = Math.sqrt(dx1 * dx1 + dy1 * dy1);
                        ratioNextSeg = lenNextSeg / (lenNextSeg + lenPrevSeg);
                        cpx1 = x - vx * (double)smooth * (1.0 - ratioNextSeg);
                        cpy1 = y - vy * (double)smooth * (1.0 - ratioNextSeg);
                        nextCpx0 = x + vx * (double)smooth * ratioNextSeg;
                        nextCpy0 = y + vy * (double)smooth * ratioNextSeg;
                        nextCpx0 = Math.min(nextCpx0, Math.max(nextX, x));
                        nextCpy0 = Math.min(nextCpy0, Math.max(nextY, y));
                        nextCpx0 = Math.min(nextCpx0, Math.max(nextX, x));
                        nextCpy0 = Math.min(nextCpy0, Math.max(nextY, y));
                        vx = nextCpx0 - x;
                        vy = nextCpy0 - y;
                        cpx1 = x - vx * lenPrevSeg / lenNextSeg;
                        cpy1 = y - vy * lenPrevSeg / lenNextSeg;
                        cpx1 = Math.min(cpx1, Math.max(prevX, x));
                        cpy1 = Math.min(cpy1, Math.max(prevY, y));
                        cpx1 = Math.min(cpx1, Math.max(prevX, x));
                        cpy1 = Math.min(cpy1, Math.max(prevY, y));
                        vx = x - cpx1;
                        vy = y - cpy1;
                        nextCpx0 = x + vx * lenNextSeg / lenPrevSeg;
                        nextCpy0 = y + vy * lenNextSeg / lenPrevSeg;
                    }
                    ChartUtils.setBezierCurvePoints(cpx0, cpy0, cpx1, cpy1, x, y, bezierNumPoints, targetPoints);
                    cpx0 = nextCpx0;
                    cpy0 = nextCpy0;
                } else {
                    targetPoints.add(new Point2D.Double(x, y));
                }
            }
            prevX = x;
            prevY = y;
            ++idx;
        }
        return k;
    }

    private static void setBezierCurvePoints(double x1, double y1, double x2, double y2, double x3, double y3, int numPoints, List<Point2D> targetPoints) {
        Point2D lastPoint = targetPoints.get(targetPoints.size() - 1);
        double x0 = lastPoint.getX();
        double y0 = lastPoint.getY();
        float step = 1.0f / (float)(numPoints - 1);
        for (float t = 0.0f; t <= 1.0f; t += step) {
            double x = Math.pow(1.0f - t, 3.0) * x0 + (double)(3.0f * t) * Math.pow(1.0f - t, 2.0) * x1 + 3.0 * Math.pow(t, 2.0) * (double)(1.0f - t) * x2 + Math.pow(t, 3.0) * x3;
            double y = Math.pow(1.0f - t, 3.0) * y0 + (double)(3.0f * t) * Math.pow(1.0f - t, 2.0) * y1 + 3.0 * Math.pow(t, 2.0) * (double)(1.0f - t) * y2 + Math.pow(t, 3.0) * y3;
            targetPoints.add(new Point2D.Double(x, y));
        }
    }

    private static void setupAxisAppearance(ValueAxis axis, TimeSeriesChartAxisSettings axisSettings) {
        axis.setVisible(axisSettings.getShow().booleanValue());
        axis.setLabelFont(AwtFontUtils.toAwtFont(axisSettings.getLabelFont()));
        axis.setLabelPaint((Paint)ColorUtils.safeParseCssColor(axisSettings.getLabelColor()));
        axis.setTickLabelsVisible(axisSettings.getShowTickLabels().booleanValue());
        axis.setTickLabelFont(AwtFontUtils.toAwtFont(axisSettings.getTickLabelFont()));
        axis.setTickLabelPaint((Paint)ColorUtils.safeParseCssColor(axisSettings.getTickLabelColor()));
        axis.setTickMarksVisible(axisSettings.getShowTicks().booleanValue());
        axis.setTickMarkPaint((Paint)ColorUtils.safeParseCssColor(axisSettings.getTicksColor()));
        axis.setAxisLineVisible(axisSettings.getShowLine().booleanValue());
        axis.setAxisLineStroke((Stroke)new BasicStroke(1.0f));
        axis.setAxisLinePaint((Paint)ColorUtils.safeParseCssColor(axisSettings.getLineColor()));
    }

    private static TickUnitSource createDateTickUnitsFromTicksFormat(Map<FormatTimeUnit, String> ticksFormat, TimeZone zone, Locale locale) {
        TickUnits units = new TickUnits();
        SimpleDateFormat f1 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.millisecond), locale);
        SimpleDateFormat f2 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.second), locale);
        SimpleDateFormat f3 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.minute), locale);
        SimpleDateFormat f4 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.hour), locale);
        SimpleDateFormat f5 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.day), locale);
        SimpleDateFormat f6 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.month), locale);
        SimpleDateFormat f7 = new SimpleDateFormat(ticksFormat.get(FormatTimeUnit.year), locale);
        f1.setTimeZone(zone);
        f2.setTimeZone(zone);
        f3.setTimeZone(zone);
        f4.setTimeZone(zone);
        f5.setTimeZone(zone);
        f6.setTimeZone(zone);
        f7.setTimeZone(zone);
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 1, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 5, DateTickUnitType.MILLISECOND, 1, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 10, DateTickUnitType.MILLISECOND, 1, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 25, DateTickUnitType.MILLISECOND, 5, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 50, DateTickUnitType.MILLISECOND, 10, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 100, DateTickUnitType.MILLISECOND, 10, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 250, DateTickUnitType.MILLISECOND, 10, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MILLISECOND, 500, DateTickUnitType.MILLISECOND, 50, (DateFormat)f1));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.SECOND, 1, DateTickUnitType.MILLISECOND, 50, (DateFormat)f2));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.SECOND, 5, DateTickUnitType.SECOND, 1, (DateFormat)f2));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.SECOND, 10, DateTickUnitType.SECOND, 1, (DateFormat)f2));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.SECOND, 30, DateTickUnitType.SECOND, 5, (DateFormat)f2));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 1, DateTickUnitType.SECOND, 5, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 2, DateTickUnitType.SECOND, 10, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 5, DateTickUnitType.MINUTE, 1, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 10, DateTickUnitType.MINUTE, 1, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 15, DateTickUnitType.MINUTE, 5, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 20, DateTickUnitType.MINUTE, 5, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MINUTE, 30, DateTickUnitType.MINUTE, 5, (DateFormat)f3));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.HOUR, 1, DateTickUnitType.MINUTE, 5, (DateFormat)f4));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.HOUR, 2, DateTickUnitType.MINUTE, 10, (DateFormat)f4));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.HOUR, 4, DateTickUnitType.MINUTE, 30, (DateFormat)f4));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.HOUR, 6, DateTickUnitType.HOUR, 1, (DateFormat)f4));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.DAY, 1, DateTickUnitType.HOUR, 1, (DateFormat)f5));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.DAY, 2, DateTickUnitType.HOUR, 1, (DateFormat)f5));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.DAY, 4, DateTickUnitType.HOUR, 1, (DateFormat)f5));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.DAY, 7, DateTickUnitType.DAY, 1, (DateFormat)f5));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.DAY, 15, DateTickUnitType.DAY, 1, (DateFormat)f5));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MONTH, 1, DateTickUnitType.DAY, 1, (DateFormat)f6));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MONTH, 2, DateTickUnitType.DAY, 1, (DateFormat)f6));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MONTH, 3, DateTickUnitType.MONTH, 1, (DateFormat)f6));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MONTH, 4, DateTickUnitType.MONTH, 1, (DateFormat)f6));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.MONTH, 6, DateTickUnitType.MONTH, 1, (DateFormat)f6));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 1, DateTickUnitType.MONTH, 1, (DateFormat)f7));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 2, DateTickUnitType.MONTH, 3, (DateFormat)f7));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 5, DateTickUnitType.YEAR, 1, (DateFormat)f7));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 10, DateTickUnitType.YEAR, 1, (DateFormat)f7));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 25, DateTickUnitType.YEAR, 5, (DateFormat)f7));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 50, DateTickUnitType.YEAR, 10, (DateFormat)f7));
        units.add((TickUnit)new DateTickUnit(DateTickUnitType.YEAR, 100, DateTickUnitType.YEAR, 20, (DateFormat)f7));
        return units;
    }

    private static Shape createPin(double x, double y, double width, double height) {
        double w = width / 5.0 * 3.0;
        double r = w / 2.0;
        double dy = r * r / (height - r);
        double cy = y - height + r + dy;
        double angle = Math.asin(dy / r);
        double dx = Math.cos(angle) * r;
        double tanX = Math.sin(angle);
        double tanY = Math.cos(angle);
        double cpLen = r * 0.6;
        double cpLen2 = r * 0.7;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x - dx, cy + dy);
        Arc2D.Double arc = ChartUtils.createArc(x, cy, r, Math.PI - angle, Math.PI * 2 + angle);
        path.append(arc, true);
        path.curveTo(x + dx - tanX * cpLen, cy + dy + tanY * cpLen, x, y - cpLen2, x, y);
        path.curveTo(x, y - cpLen2, x - dx + tanX * cpLen, cy + dy + tanY * cpLen, x - dx, cy + dy);
        path.closePath();
        return path;
    }

    private static Shape createArrow(double x, double y, double width, double height) {
        double dx = width / 3.0 * 2.0;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + dx, y + height);
        path.lineTo(x, y + height / 4.0 * 3.0);
        path.lineTo(x - dx, y + height);
        path.lineTo(x, y);
        path.closePath();
        return path;
    }

    private static Arc2D.Double createArc(double centerX, double centerY, double radius, double startAngleRad, double endAngleRad) {
        double startAngleDeg = 360.0 - Math.toDegrees(startAngleRad);
        double endAngleDeg = 360.0 - Math.toDegrees(endAngleRad);
        double extent = endAngleDeg - startAngleDeg;
        if (extent > 0.0) {
            extent -= 360.0;
        }
        Rectangle2D.Double rect = new Rectangle2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
        return new Arc2D.Double(rect, startAngleDeg, extent, 0);
    }
}

