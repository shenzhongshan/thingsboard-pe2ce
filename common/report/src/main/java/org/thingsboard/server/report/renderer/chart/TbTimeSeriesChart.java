/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.ChartTheme
 *  org.jfree.chart.JFreeChart
 *  org.jfree.chart.LegendItem
 *  org.jfree.chart.LegendItemCollection
 *  org.jfree.chart.LegendItemSource
 *  org.jfree.chart.StandardChartTheme
 *  org.jfree.chart.axis.Axis
 *  org.jfree.chart.axis.DateAxis
 *  org.jfree.chart.axis.NumberAxis
 *  org.jfree.chart.block.Arrangement
 *  org.jfree.chart.event.TitleChangeListener
 *  org.jfree.chart.labels.ItemLabelAnchor
 *  org.jfree.chart.labels.ItemLabelClip
 *  org.jfree.chart.labels.ItemLabelPosition
 *  org.jfree.chart.labels.XYSeriesLabelGenerator
 *  org.jfree.chart.plot.DatasetRenderingOrder
 *  org.jfree.chart.plot.Marker
 *  org.jfree.chart.plot.Plot
 *  org.jfree.chart.plot.SeriesRenderingOrder
 *  org.jfree.chart.renderer.xy.XYItemRenderer
 *  org.jfree.chart.title.TextTitle
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.Layer
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.jfree.chart.ui.VerticalAlignment
 *  org.jfree.data.time.Millisecond
 *  org.jfree.data.time.RegularTimePeriod
 *  org.jfree.data.time.SimpleTimePeriod
 *  org.jfree.data.time.TimePeriod
 *  org.jfree.data.time.TimePeriodValues
 *  org.jfree.data.time.TimePeriodValuesCollection
 *  org.jfree.data.time.TimeSeries
 *  org.jfree.data.time.TimeSeriesCollection
 *  org.jfree.data.time.TimeTableXYDataset
 *  org.jfree.data.xy.XYDataset
 *  org.thingsboard.server.common.data.report.configuration.chart.AxisPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.BarWithLabelsSeriesSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartFillType
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartShape
 *  org.thingsboard.server.common.data.report.configuration.chart.LegendConfig
 *  org.thingsboard.server.common.data.report.configuration.chart.LegendPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidth
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.Arrangement;
import org.jfree.chart.event.TitleChangeListener;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelClip;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.ui.VerticalAlignment;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.common.data.report.configuration.chart.AxisPosition;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.BarWithLabelsSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ChartShape;
import org.thingsboard.server.common.data.report.configuration.chart.LegendConfig;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.LineSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportTimeSeriesChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartBarWidth;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartNoAggregationBarWidthStrategy;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartSeriesType;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartYAxisSettings;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.context.chart.TsChartSeriesData;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.TbDatasetKey;
import org.thingsboard.server.report.renderer.chart.TbStateTick;
import org.thingsboard.server.report.renderer.chart.TbStateValueConverter;
import org.thingsboard.server.report.renderer.chart.TbThresholdMarker;
import org.thingsboard.server.report.renderer.chart.TbTimeseriesPlot;
import org.thingsboard.server.report.renderer.chart.TbVisualMap;
import org.thingsboard.server.report.renderer.chart.TbXYBarRenderer;
import org.thingsboard.server.report.renderer.chart.TbXYItemLabelGenerator;
import org.thingsboard.server.report.renderer.chart.TbXYLineAndShapeRenderer;
import org.thingsboard.server.report.renderer.chart.TimeseriesBarRenderCtx;
import org.thingsboard.server.report.renderer.chart.layout.TbColumnArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendTitle;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbRangeLegendTitle;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;
import org.thingsboard.server.report.renderer.chart.legend.TbTableLegendTitle;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

public class TbTimeSeriesChart
implements XYSeriesLabelGenerator,
TbSeriesLegendValuesGenerator {
    static ChartTheme currentChartTheme = new StandardChartTheme("TbChartTheme");
    private final ReportTimeSeriesChartSettings chartSettings;
    private final TsChartData chartData;
    private final Map<String, Object> variables;
    private final TbStateValueConverter stateValueConverter;
    private final boolean stackMode;
    private final TbVisualMap visualMap;
    private final String units;
    private final Integer decimals;
    private TbTimeseriesPlot plot;
    private JFreeChart chart;
    private List<DateAxis> xAxisList;
    private List<NumberAxis> yAxisList;
    private Map<String, Integer> yAxisIndexMap;
    private Map<Integer, Boolean> yAxisHasDataMap;
    private List<TsChartSeriesData> seriesList;
    private List<TbDatasetKey> datasetKeys;

    public TbTimeSeriesChart(ReportTimeSeriesChartSettings chartSettings, TsChartData chartData, Map<String, Object> variables, TbVisualMap visualMap, String units, Integer decimals) {
        this.chartSettings = chartSettings;
        this.chartData = chartData;
        this.variables = variables;
        this.visualMap = visualMap;
        this.stateValueConverter = !this.chartSettings.getStates().isEmpty() ? new TbStateValueConverter(this.chartSettings.getStates()) : null;
        this.stackMode = !this.chartData.isComparisonEnabled() && this.chartSettings.getStack() != false;
        this.units = units;
        this.decimals = decimals;
    }

    public JFreeChart createChart(Graphics2D g2) {
        this.plot = new TbTimeseriesPlot();
        this.chart = new JFreeChart(null, null, (Plot)this.plot, false);
        currentChartTheme.apply(this.chart);
        this.plot.setVisualMap(this.visualMap);
        this.plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        this.plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        this.plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        this.plot.setInsets(new RectangleInsets(2.0, 0.0, 2.0, 0.0));
        this.chart.setBackgroundPaint((Paint)ColorUtils.TRANSPARENT);
        if (this.chartSettings.getShowTitle().booleanValue()) {
            Font titleFont = AwtFontUtils.toAwtFont(this.chartSettings.getTitleFont());
            String titleText = ThymeleafUtil.renderFromTextString(this.chartSettings.getTitle(), this.variables);
            TextTitle title = new TextTitle(titleText, titleFont);
            title.setPaint((Paint)ColorUtils.safeParseCssColor(this.chartSettings.getTitleColor()));
            HorizontalAlignment alignment = HorizontalAlignment.CENTER;
            switch (this.chartSettings.getTitleAlignment()) {
                case RIGHT: {
                    alignment = HorizontalAlignment.RIGHT;
                    break;
                }
                case LEFT: {
                    alignment = HorizontalAlignment.LEFT;
                }
            }
            title.setHorizontalAlignment(alignment);
            if (!this.chartSettings.getShowLegend().booleanValue() || this.chartSettings.getLegendConfig().getPosition() != LegendPosition.top) {
                title.setPadding(new RectangleInsets(1.0, 1.0, 8.0, 1.0));
            }
            this.chart.setTitle(title);
        }
        this.setupGrid();
        this.setupXAxes();
        this.setupYAxes();
        this.setupData();
        this.postConfigureXAxes();
        this.postConfigureYAxes();
        this.setupThresholds();
        this.setupLegend();
        return this.chart;
    }

    private void setupGrid() {
        if (this.chartSettings.getGrid().getShow().booleanValue()) {
            String gridBackgroundColor = this.chartSettings.getGrid().getBackgroundColor();
            this.plot.setBackgroundPaint(ColorUtils.safeParseCssColor(gridBackgroundColor, null));
            this.plot.setOutlineStroke(new BasicStroke(this.chartSettings.getGrid().getBorderWidth().floatValue()));
            this.plot.setOutlinePaint(ColorUtils.safeParseCssColor(this.chartSettings.getGrid().getBorderColor()));
        } else {
            this.plot.setBackgroundPaint(null);
            this.plot.setOutlinePaint(null);
        }
        this.plot.setDomainGridlinesVisible(false);
        this.plot.setRangeGridlinesVisible(false);
    }

    private void setupXAxes() {
        this.xAxisList = new ArrayList<DateAxis>();
        this.xAxisList.add(ChartUtils.createXAxis(this.plot, this.chartSettings.getXAxis(), this.chartData.getTimeRange(), this.chartData.getTimeZone(), 0));
        if (this.chartData.isComparisonEnabled()) {
            this.xAxisList.add(ChartUtils.createXAxis(this.plot, this.chartSettings.getComparisonXAxis(), this.chartData.getComparisonTimeRange(), this.chartData.getTimeZone(), 1));
        }
    }

    private void setupYAxes() {
        this.yAxisList = new ArrayList<NumberAxis>();
        this.yAxisIndexMap = new HashMap<String, Integer>();
        this.yAxisHasDataMap = new HashMap<Integer, Boolean>();
        List<TimeSeriesChartYAxisSettings> yAxisSettingsList = this.chartSettings.getYAxes().values().stream().sorted(Comparator.comparingInt(TimeSeriesChartYAxisSettings::getOrder)).toList();
        List<TbStateTick> stateTicks = this.stateValueConverter != null ? this.stateValueConverter.getStateTicks() : null;
        for (int index = 0; index < yAxisSettingsList.size(); ++index) {
            TimeSeriesChartYAxisSettings yAxisSettings = yAxisSettingsList.get(index);
            this.yAxisList.add(ChartUtils.createYAxis(this.plot, yAxisSettings, stateTicks, this.units, this.decimals, index));
            this.yAxisIndexMap.put(yAxisSettingsList.get(index).getId(), index);
            this.yAxisHasDataMap.put(index, false);
        }
    }

    private void setupData() {
        ArrayList<TsChartSeriesData> allSeries = new ArrayList<TsChartSeriesData>();
        for (TsChartDataSource dataSource : this.chartData.getChartData()) {
            allSeries.addAll(dataSource.getData());
        }
        this.seriesList = allSeries.stream().sorted(Comparator.comparingInt(TsChartSeriesData::getIndex)).toList();
        Map<TbDatasetKey, List<TsChartSeriesData>> groupedSeries = ChartUtils.datasetGroupsFromSeries(this.seriesList);
        this.datasetKeys = new ArrayList<TbDatasetKey>(groupedSeries.keySet());
        if (groupedSeries.isEmpty()) {
            this.plot.setRenderer(0, (XYItemRenderer)new TbXYLineAndShapeRenderer());
        } else {
            groupedSeries.forEach(this::setupDataset);
        }
    }

    private void setupThresholds() {
        List<TsChartThresholdItem> thresholdItems = this.chartData.getThresholdItems();
        thresholdItems.forEach(thresholdItem -> {
            Optional<TbDatasetKey> foundKey;
            TimeSeriesChartThreshold threshold = thresholdItem.getSettings();
            String yAxisId = threshold.getYAxisId();
            int datasetIndex = -1;
            int axisIndex = this.yAxisIndexMap.get(yAxisId);
            if (axisIndex >= 0 && this.yAxisHasDataMap.get(axisIndex).booleanValue() && (foundKey = this.datasetKeys.stream().filter(key -> key.getYAxisId().equals(yAxisId)).findFirst()).isPresent()) {
                datasetIndex = foundKey.get().getDatasetIndex();
            }
            if (datasetIndex >= 0) {
                TbThresholdMarker marker = ChartUtils.createThresholdMarker(thresholdItem, this.units, this.decimals);
                this.plot.addRangeMarker(datasetIndex, (Marker)marker, Layer.FOREGROUND);
            }
        });
    }

    private void postConfigureXAxes() {
        if (this.xAxisList.size() > 1) {
            if (this.chartSettings.getXAxis().getPosition().equals((Object)AxisPosition.top) && this.chartSettings.getComparisonXAxis().getPosition().equals((Object)AxisPosition.top)) {
                ChartUtils.adjustAxisMargins((Axis)this.xAxisList.get(0), 0.0, 0.0, 4.0, 0.0);
            } else if (this.chartSettings.getXAxis().getPosition().equals((Object)AxisPosition.bottom) && this.chartSettings.getComparisonXAxis().getPosition().equals((Object)AxisPosition.bottom)) {
                ChartUtils.adjustAxisMargins((Axis)this.xAxisList.get(0), 4.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void postConfigureYAxes() {
        List<TimeSeriesChartYAxisSettings> rightAxes;
        for (Map.Entry<Integer, Boolean> entry2 : this.yAxisHasDataMap.entrySet()) {
            if (entry2.getValue().booleanValue()) continue;
            this.yAxisList.get(entry2.getKey()).setVisible(false);
        }
        for (Map.Entry<String, Integer> entry3 : this.yAxisIndexMap.entrySet()) {
            boolean includeZeros = this.datasetKeys.stream().anyMatch(entry -> entry.getYAxisId().equals(entry3.getKey()) && TimeSeriesChartSeriesType.bar.equals((Object)entry.getSeriesType()));
            this.yAxisList.get((Integer)entry3.getValue()).setAutoRangeIncludesZero(includeZeros);
        }
        List<TimeSeriesChartYAxisSettings> yAxisSettingsList = this.chartSettings.getYAxes().values().stream().sorted(Comparator.comparingInt(TimeSeriesChartYAxisSettings::getOrder)).toList();
        List<TimeSeriesChartYAxisSettings> list = yAxisSettingsList.stream().filter(axis -> {
            if (AxisPosition.left.equals((Object)axis.getPosition())) {
                int index = this.yAxisIndexMap.get(axis.getId());
                return this.yAxisHasDataMap.get(index);
            }
            return false;
        }).toList();
        if (list.size() > 1) {
            for (int i = 0; i < list.size() - 1; ++i) {
                TimeSeriesChartYAxisSettings leftAxis = list.get(i);
                int index = yAxisSettingsList.indexOf(leftAxis);
                NumberAxis axis2 = this.yAxisList.get(index);
                ChartUtils.adjustAxisMargins((Axis)axis2, 0.0, 4.0, 0.0, 0.0);
            }
        }
        if ((rightAxes = yAxisSettingsList.stream().filter(axis -> {
            if (AxisPosition.right.equals((Object)axis.getPosition())) {
                int index = this.yAxisIndexMap.get(axis.getId());
                return this.yAxisHasDataMap.get(index);
            }
            return false;
        }).toList()).size() > 1) {
            for (int i = 0; i < rightAxes.size() - 1; ++i) {
                TimeSeriesChartYAxisSettings rightAxis = rightAxes.get(i);
                int index = yAxisSettingsList.indexOf(rightAxis);
                NumberAxis axis3 = this.yAxisList.get(index);
                ChartUtils.adjustAxisMargins((Axis)axis3, 0.0, 0.0, 0.0, 4.0);
            }
        }
    }

    private void setupLegend() {
        if (this.chartSettings.getShowLegend().booleanValue()) {
            LegendConfig legendConfig = this.chartSettings.getLegendConfig();
            org.jfree.chart.title.Title legendTitle = null;
            double hPadding = 8.0;
            if (this.visualMap != null) {
                List<TsChartRangeItem> visibleRangeItems = this.chartData.getRangeItems().stream().filter(TsChartRangeItem::isVisible).toList();
                if (!visibleRangeItems.isEmpty()) {
                    TbRangeLegendTitle legend = new TbRangeLegendTitle(visibleRangeItems);
                    legend.setLegendLabelFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendLabelFont()));
                    legend.setLegendLabelPaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendLabelColor()));
                    legendTitle = legend;
                    hPadding = 16.0;
                }
            } else if (legendConfig.isSimpleLegend()) {
                TbFlowArrangement hLayout = new TbFlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 16.0, 8.0);
                hLayout.setMaxRelativeHeight(0.35);
                TbColumnArrangement vLayout = new TbColumnArrangement(HorizontalAlignment.LEFT, VerticalAlignment.TOP, 16.0, 8.0);
                vLayout.setMaxRelativeWidth(0.35);
                TbLegendTitle legend = new TbLegendTitle((LegendItemSource)this.plot, (Arrangement)hLayout, (Arrangement)vLayout);
                legend.setBackgroundPaint(ColorUtils.TRANSPARENT);
                legend.setItemLabelPadding(new RectangleInsets(0.0, 4.0, 0.0, 0.0));
                legend.setItemFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendLabelFont()));
                legend.setItemPaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendLabelColor()));
                boolean sortAlphabetically = legendConfig.getSortDataKeys();
                LegendItemCollection legendItems = this.plot.getLegendItems();
                ArrayList<LegendItem> items = new ArrayList<LegendItem>();
                for (int i = 0; i < legendItems.getItemCount(); ++i) {
                    items.add(legendItems.get(i));
                }
                if (sortAlphabetically) {
                    items.sort(Comparator.comparing(LegendItem::getLabel, String.CASE_INSENSITIVE_ORDER));
                } else {
                    items.sort((item1, item2) -> {
                        int dataIndex1 = this.findSeriesIndex(item1.getDatasetIndex(), item1.getSeriesIndex());
                        int dataIndex2 = this.findSeriesIndex(item2.getDatasetIndex(), item2.getSeriesIndex());
                        return dataIndex1 - dataIndex2;
                    });
                }
                LegendItemCollection sortedCollection = new LegendItemCollection();
                for (LegendItem item3 : items) {
                    sortedCollection.add(item3);
                }
                this.plot.setFixedLegendItems(sortedCollection);
                legendTitle = legend;
            } else {
                TbTableLegendTitle legend = new TbTableLegendTitle(this.plot, legendConfig);
                legend.setMaxRelativeWidth(0.35);
                legend.setMaxRelativeHeight(0.35);
                legend.setItemLabelPadding(new RectangleInsets(0.0, 4.0, 0.0, 0.0));
                legend.setItemFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendLabelFont()));
                legend.setItemPaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendLabelColor()));
                legend.setLegendColumnTitleFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendColumnTitleFont()));
                legend.setLegendColumnTitlePaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendColumnTitleColor()));
                legend.setLegendValueFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendValueFont()));
                legend.setLegendValuePaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendValueColor()));
                boolean sortAlphabetically = legendConfig.getSortDataKeys();
                if (sortAlphabetically) {
                    legend.setLegendItemComparator(Comparator.comparing(item -> item.getLegendItem().getLabel(), String.CASE_INSENSITIVE_ORDER));
                } else {
                    legend.setLegendItemComparator((item1, item2) -> {
                        int dataIndex1 = this.findSeriesIndex(item1.getLegendItem().getDatasetIndex(), item1.getLegendItem().getSeriesIndex());
                        int dataIndex2 = this.findSeriesIndex(item2.getLegendItem().getDatasetIndex(), item2.getLegendItem().getSeriesIndex());
                        return dataIndex1 - dataIndex2;
                    });
                }
                legendTitle = legend;
            }
            if (legendTitle != null) {
                RectangleEdge position = RectangleEdge.TOP;
                LegendPosition legendPosition = legendConfig.getPosition();
                switch (legendPosition) {
                    case bottom: {
                        position = RectangleEdge.BOTTOM;
                        break;
                    }
                    case left: {
                        position = RectangleEdge.LEFT;
                        break;
                    }
                    case right: {
                        position = RectangleEdge.RIGHT;
                    }
                }
                legendTitle.setPosition(position);
                if (RectangleEdge.isLeftOrRight((RectangleEdge)position)) {
                    legendTitle.setVerticalAlignment(VerticalAlignment.TOP);
                    if (position == RectangleEdge.LEFT) {
                        legendTitle.setPadding(new RectangleInsets(0.0, 0.0, 0.0, hPadding));
                    } else {
                        legendTitle.setPadding(new RectangleInsets(0.0, hPadding, 0.0, 0.0));
                    }
                } else {
                    legendTitle.setPadding(new RectangleInsets(8.0, 0.0, 8.0, 0.0));
                }
                this.chart.addSubtitle(legendTitle);
                legendTitle.addChangeListener((TitleChangeListener)this.chart);
            }
        }
    }

    private void setupDataset(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        this.createDataset(datasetKey, seriesList);
        this.createDatasetRenderer(datasetKey, seriesList);
    }

    private void createDataset(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        int xAxisIndex = datasetKey.isComparison() ? 1 : 0;
        int yAxisIndex = this.yAxisIndexMap.get(datasetKey.getYAxisId());
        if (datasetKey.getSeriesType() == TimeSeriesChartSeriesType.bar) {
            List<TsChartSeriesData> barsList = this.seriesList.stream().filter(barSeries -> !barSeries.isEmpty() && ChartUtils.getSeriesSettings(barSeries).getSeriesType() == TimeSeriesChartSeriesType.bar).toList();
            TimeSeriesChartNoAggregationBarWidthSettings noAggregationBarWidthSettings = this.chartSettings.getNoAggregationBarWidthSettings();
            TimeSeriesChartBarWidth targetBarWidth = TimeSeriesChartNoAggregationBarWidthStrategy.group.equals((Object)noAggregationBarWidthSettings.getStrategy()) ? noAggregationBarWidthSettings.getGroupWidth() : noAggregationBarWidthSettings.getBarWidth();
            TimeseriesBarRenderCtx barRenderCtx = TimeseriesBarRenderCtx.builder().barGap(this.chartSettings.getBarWidthSettings().getBarGap()).intervalGap(this.chartSettings.getBarWidthSettings().getIntervalGap()).timeWindow(this.chartData.getTimeRange().endTs - this.chartData.getTimeRange().startTs).noAggregation(this.chartData.isNoAggregation()).noAggregationBarWidthStrategy(noAggregationBarWidthSettings.getStrategy()).noAggregationWidthRelative(targetBarWidth.getRelative()).noAggregationWidth(targetBarWidth.getRelative() != false ? targetBarWidth.getRelativeWidth() : targetBarWidth.getAbsoluteWidth()).build();
            this.createBarsDataset(datasetKey.getDatasetIndex(), xAxisIndex, yAxisIndex, seriesList, barsList, barRenderCtx);
        } else {
            this.createLinesDataset(datasetKey.getDatasetIndex(), xAxisIndex, yAxisIndex, seriesList);
        }
    }

    private void createDatasetRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        if (datasetKey.getSeriesType() == TimeSeriesChartSeriesType.bar) {
            this.createBarsRenderer(datasetKey, seriesList);
        } else {
            this.createLinesRenderer(datasetKey, seriesList);
        }
    }

    private void createBarsDataset(int datasetIndex, int xAxisIndex, int yAxisIndex, List<TsChartSeriesData> seriesList, List<TsChartSeriesData> allBarsList, TimeseriesBarRenderCtx barRenderCtx) {
        XYDataset dataset;
        boolean hasData = false;
        if (this.stackMode) {
            List<Integer> barDatasets = allBarsList.stream().map(TsChartSeriesData::getDatasetIndex).distinct().sorted().toList();
            int barsCount = barDatasets.size();
            int barIndex = barDatasets.indexOf(datasetIndex);
            TimeTableXYDataset tableDataset = new TimeTableXYDataset(this.chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                String seriesName = datasetIndex + "_" + series.getSeriesIndex();
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted == null) continue;
                    SimpleTimePeriod timePeriod = ChartUtils.calculateBarTimePeriod(tsValue, barRenderCtx, barsCount, barIndex);
                    tableDataset.add((TimePeriod)timePeriod, converted.doubleValue(), (Comparable)((Object)seriesName));
                    hasData = true;
                }
            }
            dataset = tableDataset;
        } else {
            int barsCount = allBarsList.size();
            TimePeriodValuesCollection tpvDataset = new TimePeriodValuesCollection();
            for (TsChartSeriesData series : seriesList) {
                int barIndex = allBarsList.indexOf(series);
                TimePeriodValues timePeriods = new TimePeriodValues(datasetIndex + "_" + series.getSeriesIndex());
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted == null) continue;
                    SimpleTimePeriod timePeriod = ChartUtils.calculateBarTimePeriod(tsValue, barRenderCtx, barsCount, barIndex);
                    timePeriods.add((TimePeriod)timePeriod, (Number)converted);
                    hasData = true;
                }
                tpvDataset.addSeries(timePeriods);
            }
            dataset = tpvDataset;
        }
        this.yAxisHasDataMap.put(yAxisIndex, hasData);
        this.plot.setDataset(datasetIndex, (XYDataset)dataset);
        this.plot.mapDatasetToDomainAxis(datasetIndex, xAxisIndex);
        this.plot.mapDatasetToRangeAxis(datasetIndex, yAxisIndex);
    }

    private void createLinesDataset(int datasetIndex, int xAxisIndex, int yAxisIndex, List<TsChartSeriesData> seriesList) {
        XYDataset dataset;
        Locale locale = Locale.getDefault();
        boolean hasData = false;
        if (this.stackMode) {
            TimeTableXYDataset tableDataset = new TimeTableXYDataset(this.chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                String seriesName = datasetIndex + "_" + series.getSeriesIndex();
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted == null) continue;
                    Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), this.chartData.getTimeZone(), locale);
                    tableDataset.add((TimePeriod)millisecond, converted.doubleValue(), (Comparable)((Object)seriesName));
                    hasData = true;
                }
            }
            dataset = tableDataset;
        } else {
            TimeSeriesCollection tsDataset = new TimeSeriesCollection(this.chartData.getTimeZone());
            for (TsChartSeriesData series : seriesList) {
                TimeSeries timeSeries = new TimeSeries((Comparable)((Object)(datasetIndex + "_" + series.getSeriesIndex())));
                for (TsChartSeriesEntry tsValue : series.getData()) {
                    Double converted = this.convertValue(tsValue);
                    if (converted == null) continue;
                    Millisecond millisecond = new Millisecond(new Date(tsValue.getTs()), this.chartData.getTimeZone(), locale);
                    timeSeries.add((RegularTimePeriod)millisecond, (Number)converted);
                    hasData = true;
                }
                tsDataset.addSeries(timeSeries);
            }
            dataset = tsDataset;
        }
        this.yAxisHasDataMap.put(yAxisIndex, hasData);
        this.plot.setDataset(datasetIndex, (XYDataset)dataset);
        this.plot.mapDatasetToDomainAxis(datasetIndex, xAxisIndex);
        this.plot.mapDatasetToRangeAxis(datasetIndex, yAxisIndex);
    }

    private Double convertValue(TsChartSeriesEntry tsValue) {
        if (this.stateValueConverter != null) {
            return this.stateValueConverter.convertValue(tsValue.getValue());
        }
        return tsValue.getDoubleValue();
    }

    private void createBarsRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        TbXYBarRenderer renderer = this.createBarRenderer();
        for (TsChartSeriesData series : seriesList) {
            ItemLabelPosition negativeItemLabelPosition;
            ItemLabelPosition positiveItemLabelPosition;
            TbXYItemLabelGenerator labelGenerator;
            Color seriesColor;
            BarSeriesSettings barSettings = ChartUtils.getSeriesSettings(series).getBarSettings();
            Paint seriesPaint = seriesColor = ColorUtils.safeParseCssColor(series.getDataKey().getColor());
            if (!ChartFillType.none.equals((Object)barSettings.getBackgroundSettings().getType())) {
                seriesPaint = ChartUtils.createFillPaint(barSettings.getBackgroundSettings(), seriesColor);
            }
            renderer.setSeriesPaint(series.getSeriesIndex(), seriesPaint);
            if (barSettings.getShowBorder().booleanValue()) {
                renderer.setSeriesOutlineStroke(series.getSeriesIndex(), new BasicStroke(barSettings.getBorderWidth().floatValue()));
                renderer.setSeriesOutlinePaint(series.getSeriesIndex(), seriesColor);
            }
            renderer.setSeriesItemBorderRadius(series.getSeriesIndex(), barSettings.getBorderRadius());
            if (barSettings instanceof BarWithLabelsSeriesSettings) {
                BarWithLabelsSeriesSettings barWithLabelsSettings = (BarWithLabelsSeriesSettings)barSettings;
                if (!barWithLabelsSettings.getShowLabel().booleanValue() && !barWithLabelsSettings.getShowSeriesLabel().booleanValue()) continue;
                renderer.setItemLabelInsets(RectangleInsets.ZERO_INSETS);
                renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), new ItemLabelPosition(ItemLabelAnchor.INSIDE6, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -1.5707963267948966, ItemLabelClip.NONE));
                renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), new ItemLabelPosition(ItemLabelAnchor.INSIDE6, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_LEFT, -1.5707963267948966, ItemLabelClip.NONE));
                if (barWithLabelsSettings.getShowLabel().booleanValue()) {
                    renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesItemLabelFont(series.getSeriesIndex(), AwtFontUtils.toAwtFont(barWithLabelsSettings.getLabelFont()));
                    renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), ColorUtils.safeParseCssColor(barWithLabelsSettings.getLabelColor()));
                    labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits(), this.stateValueConverter);
                    renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                }
                if (!barWithLabelsSettings.getShowSeriesLabel().booleanValue()) continue;
                renderer.setSeriesLabelsVisible(series.getSeriesIndex(), true);
                renderer.setSeriesLabelFont(series.getSeriesIndex(), AwtFontUtils.toAwtFont(barWithLabelsSettings.getSeriesLabelFont()));
                renderer.setSeriesLabelPaint(series.getSeriesIndex(), ColorUtils.safeParseCssColor(barWithLabelsSettings.getSeriesLabelColor()));
                renderer.setSeriesLabelGenerator(series.getSeriesIndex(), this);
                continue;
            }
            if (!barSettings.getShowLabel().booleanValue()) continue;
            renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
            renderer.setSeriesItemLabelFont(series.getSeriesIndex(), AwtFontUtils.toAwtFont(barSettings.getLabelFont()));
            renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), ColorUtils.safeParseCssColor(barSettings.getLabelColor()));
            labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits(), this.stateValueConverter);
            renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
            if (ChartLabelPosition.top.equals((Object)barSettings.getLabelPosition())) {
                positiveItemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
                negativeItemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
            } else {
                positiveItemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                negativeItemLabelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER);
            }
            renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), positiveItemLabelPosition);
            renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), negativeItemLabelPosition);
            if (!barSettings.getEnableLabelBackground().booleanValue()) continue;
            renderer.setSeriesItemLabelsBackgroundVisible(series.getSeriesIndex(), true);
            renderer.setSeriesItemLabelsBackgroundPaint(series.getSeriesIndex(), ColorUtils.safeParseCssColor(barSettings.getLabelBackground()));
        }
        this.plot.setRenderer(datasetKey.getDatasetIndex(), (XYItemRenderer)renderer);
    }

    private void createLinesRenderer(TbDatasetKey datasetKey, List<TsChartSeriesData> seriesList) {
        TbXYLineAndShapeRenderer renderer = this.createLineRenderer(datasetKey);
        for (TsChartSeriesData series : seriesList) {
            LineSeriesSettings lineSettings = ChartUtils.getSeriesSettings(series).getLineSettings();
            Color seriesColor = ColorUtils.safeParseCssColor(series.getDataKey().getColor());
            Paint fillPaint = ChartUtils.createFillPaint(lineSettings.getFillAreaSettings(), seriesColor);
            renderer.setSeriesPaint(series.getSeriesIndex(), seriesColor);
            renderer.setSeriesFillPaint(series.getSeriesIndex(), fillPaint);
            if (lineSettings.getShowLine().booleanValue()) {
                renderer.setSeriesLinesVisible(series.getSeriesIndex(), true);
                Stroke lineStroke = ChartUtils.createLineStroke(lineSettings.getLineType(), lineSettings.getLineWidth());
                renderer.setSeriesStroke(series.getSeriesIndex(), lineStroke);
            } else {
                renderer.setSeriesLinesVisible(series.getSeriesIndex(), false);
            }
            if (lineSettings.getShowPoints().booleanValue() || series.getData().size() == 1) {
                Shape seriesShape = ChartUtils.createSeriesShape(lineSettings.getPointShape(), lineSettings.getPointSize().floatValue());
                if (seriesShape != null) {
                    renderer.setSeriesShapesVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesShape(series.getSeriesIndex(), seriesShape);
                    if (ChartShape.emptyCircle.equals((Object)lineSettings.getPointShape())) {
                        renderer.setSeriesShapesFillPaint(series.getSeriesIndex(), Color.WHITE);
                        renderer.setSeriesOutlineStroke(series.getSeriesIndex(), new BasicStroke(2.0f));
                    }
                    if (!lineSettings.getShowPointLabel().booleanValue()) continue;
                    renderer.setSeriesItemLabelsVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesItemLabelFont(series.getSeriesIndex(), AwtFontUtils.toAwtFont(lineSettings.getPointLabelFont()));
                    renderer.setSeriesItemLabelPaint(series.getSeriesIndex(), ColorUtils.safeParseCssColor(lineSettings.getPointLabelColor()));
                    TbXYItemLabelGenerator labelGenerator = new TbXYItemLabelGenerator(series.getDataKey().getDecimals(), series.getDataKey().getUnits(), this.stateValueConverter);
                    renderer.setSeriesItemLabelGenerator(series.getSeriesIndex(), labelGenerator);
                    ItemLabelPosition itemLabelPosition = ChartLabelPosition.top.equals((Object)lineSettings.getPointLabelPosition()) ? new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER) : new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                    renderer.setItemLabelInsets(RectangleInsets.ZERO_INSETS);
                    renderer.setSeriesPositiveItemLabelPosition(series.getSeriesIndex(), itemLabelPosition);
                    renderer.setSeriesNegativeItemLabelPosition(series.getSeriesIndex(), itemLabelPosition);
                    if (!lineSettings.getEnablePointLabelBackground().booleanValue()) continue;
                    renderer.setSeriesItemLabelsBackgroundVisible(series.getSeriesIndex(), true);
                    renderer.setSeriesItemLabelsBackgroundPaint(series.getSeriesIndex(), ColorUtils.safeParseCssColor(lineSettings.getPointLabelBackground()));
                    continue;
                }
                renderer.setSeriesShapesVisible(series.getSeriesIndex(), false);
                continue;
            }
            renderer.setSeriesShapesVisible(series.getSeriesIndex(), false);
        }
        this.plot.setRenderer(datasetKey.getDatasetIndex(), (XYItemRenderer)renderer);
    }

    private TbXYBarRenderer createBarRenderer() {
        TbXYBarRenderer barRenderer = new TbXYBarRenderer(this.stackMode);
        barRenderer.setShadowVisible(false);
        barRenderer.setDrawBarOutline(true);
        barRenderer.setDefaultOutlineStroke(new BasicStroke(0.0f));
        barRenderer.setLegendItemLabelGenerator(this);
        barRenderer.setTbSeriesLegendValuesGenerator(this);
        return barRenderer;
    }

    private TbXYLineAndShapeRenderer createLineRenderer(TbDatasetKey datasetKey) {
        TbXYLineAndShapeRenderer.LineInterpolationType interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.NONE;
        if (datasetKey.isStepLine()) {
            interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.STEP;
        } else if (datasetKey.isSmoothLine()) {
            interpolationType = TbXYLineAndShapeRenderer.LineInterpolationType.SMOOTH;
        }
        TbXYLineAndShapeRenderer lineRenderer = new TbXYLineAndShapeRenderer(interpolationType, datasetKey.isFillArea() ? TbXYLineAndShapeRenderer.FillType.TO_ZERO : TbXYLineAndShapeRenderer.FillType.NONE, this.stackMode);
        if (datasetKey.isStepLine()) {
            double stepPoint = 0.0;
            switch (datasetKey.getStepType()) {
                case start: {
                    stepPoint = 0.0;
                    break;
                }
                case middle: {
                    stepPoint = 0.5;
                    break;
                }
                case end: {
                    stepPoint = 1.0;
                }
            }
            lineRenderer.setStepPoint(stepPoint);
        } else if (datasetKey.isSmoothLine()) {
            lineRenderer.setPrecision(100);
            lineRenderer.setSmooth(0.25f);
        }
        lineRenderer.setLegendItemLabelGenerator(this);
        lineRenderer.setTbSeriesLegendValuesGenerator(this);
        return lineRenderer;
    }

    public String generateLabel(XYDataset dataset, int seriesIndex) {
        int datasetIndex = this.plot.indexOf(dataset);
        Optional<TsChartSeriesData> seriesOpt = this.findSeries(datasetIndex, seriesIndex);
        if (seriesOpt.isPresent()) {
            TsChartSeriesData series = seriesOpt.get();
            return ThymeleafUtil.renderFromTextString(series.getDataKey().getLabel(), series.getDataSource().getVariables());
        }
        return "Undefined";
    }

    @Override
    public TbLegendValues generateLegendValues(TbLegendValuesRequest request, XYDataset dataset, int seriesIndex) {
        int datasetIndex = this.plot.indexOf(dataset);
        Optional<TsChartSeriesData> seriesOpt = this.findSeries(datasetIndex, seriesIndex);
        if (seriesOpt.isPresent()) {
            TsChartSeriesData series = seriesOpt.get();
            int decimals = series.getDataKey().getDecimals() != null ? series.getDataKey().getDecimals() : 2;
            String units = series.getDataKey().getUnits();
            NumberFormat valueFormatter = ChartUtils.createValueFormatter(decimals, units);
            TbLegendValues result = new TbLegendValues();
            if (request.isMin()) {
                Double min = series.calcMin();
                result.setMin(min != null ? valueFormatter.format(min) : "");
            }
            if (request.isMax()) {
                Double max = series.calcMax();
                result.setMax(max != null ? valueFormatter.format(max) : "");
            }
            if (request.isAvg()) {
                Double avg = series.calcAvg();
                result.setAvg(avg != null ? valueFormatter.format(avg) : "");
            }
            if (request.isTotal()) {
                Double total = series.calcTotal();
                result.setTotal(total != null ? valueFormatter.format(total) : "");
            }
            if (request.isLatest()) {
                Double latest = series.calcLatest();
                result.setLatest(latest != null ? valueFormatter.format(latest) : "");
            }
            return result;
        }
        return null;
    }

    private int findSeriesIndex(int datasetIndex, int seriesIndex) {
        Optional<TsChartSeriesData> series = this.findSeries(datasetIndex, seriesIndex);
        return series.map(TsChartSeriesData::getIndex).orElse(-1);
    }

    private Optional<TsChartSeriesData> findSeries(int datasetIndex, int seriesIndex) {
        return this.seriesList.stream().filter(s -> s.getDatasetIndex() == datasetIndex && s.getSeriesIndex() == seriesIndex).findFirst();
    }
}

