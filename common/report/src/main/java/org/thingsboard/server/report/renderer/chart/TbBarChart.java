/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.CategoryAxis
 *  org.jfree.chart.axis.ValueAxis
 *  org.jfree.chart.labels.CategoryItemLabelGenerator
 *  org.jfree.chart.labels.ItemLabelAnchor
 *  org.jfree.chart.labels.ItemLabelPosition
 *  org.jfree.chart.plot.CategoryPlot
 *  org.jfree.chart.plot.DatasetRenderingOrder
 *  org.jfree.chart.plot.Marker
 *  org.jfree.chart.plot.ValueMarker
 *  org.jfree.chart.renderer.category.CategoryItemRenderer
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.TextAnchor
 *  org.jfree.data.category.CategoryDataset
 *  org.jfree.data.category.DefaultCategoryDataset
 *  org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartFillType
 *  org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.Map;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.thingsboard.server.common.data.report.configuration.chart.BarSeriesSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ChartFillType;
import org.thingsboard.server.common.data.report.configuration.chart.ChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.TbBarRenderer;
import org.thingsboard.server.report.renderer.chart.TbLatestChart;
import org.thingsboard.server.report.renderer.chart.TbNumberAxis;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbBarChart
extends TbLatestChart<ReportBarChartSettings, CategoryPlot>
implements CategoryItemLabelGenerator {
    private CategoryAxis categoryAxis;
    private TbNumberAxis valueAxis;
    private TbBarRenderer renderer;
    private DefaultCategoryDataset dataset;

    public TbBarChart(ReportBarChartSettings chartSettings, LatestChartData latestChartData, Map<String, Object> variables) {
        super(chartSettings, latestChartData, variables);
    }

    @Override
    protected CategoryPlot createPlot() {
        this.categoryAxis = new CategoryAxis(null);
        this.valueAxis = new TbNumberAxis(null, null);
        this.renderer = new TbBarRenderer();
        this.dataset = new DefaultCategoryDataset();
        return new CategoryPlot((CategoryDataset)this.dataset, this.categoryAxis, (ValueAxis)this.valueAxis, (CategoryItemRenderer)this.renderer);
    }

    @Override
    protected void setupPlot(CategoryPlot plot) {
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        this.categoryAxis.setAxisLineVisible(false);
        this.categoryAxis.setTickLabelsVisible(false);
        this.categoryAxis.setTickMarksVisible(false);
        ValueMarker zeroMarker = new ValueMarker(0.0, (Paint)ColorUtils.safeParseCssColor("rgba(0,0,0,0.54)"), (Stroke)new BasicStroke(1.0f));
        plot.addRangeMarker((Marker)zeroMarker);
        if (((ReportBarChartSettings)this.chartSettings).getAxisMin() != null) {
            this.valueAxis.setAxisMin(((ReportBarChartSettings)this.chartSettings).getAxisMin());
            if (((ReportBarChartSettings)this.chartSettings).getAxisMin() > 0.0) {
                zeroMarker.setValue(((ReportBarChartSettings)this.chartSettings).getAxisMin().doubleValue());
            }
        }
        if (((ReportBarChartSettings)this.chartSettings).getAxisMax() != null) {
            this.valueAxis.setAxisMax(((ReportBarChartSettings)this.chartSettings).getAxisMax());
        }
        this.valueAxis.setAxisLineVisible(false);
        this.valueAxis.setTickMarksVisible(false);
        this.valueAxis.setNumberFormatOverride(this.valueFormatter);
        this.valueAxis.setTickLabelFont(AwtFontUtils.toAwtFont(((ReportBarChartSettings)this.chartSettings).getAxisTickLabelFont()));
        this.valueAxis.setTickLabelPaint(ColorUtils.safeParseCssColor(((ReportBarChartSettings)this.chartSettings).getAxisTickLabelColor()));
        this.valueAxis.setAutoRangeIncludesZero(true);
        long visibleItemsCount = this.dataItems.stream().filter(LatestChartDataItem::isHasValue).count();
        if (visibleItemsCount == 0L) {
            this.valueAxis.setVisible(false);
        }
        this.renderer.setShadowVisible(false);
        this.renderer.setDrawBarOutline(true);
        this.renderer.setDefaultOutlineStroke(new BasicStroke(0.0f));
        BarSeriesSettings barSettings = ((ReportBarChartSettings)this.chartSettings).getBarSettings();
        if (visibleItemsCount > 0L) {
            double barWidth = barSettings.getBarWidth() / 100.0;
            double spacing = (double)(1.0f / (float)visibleItemsCount) * (1.0 - barWidth);
            double itemMargin = spacing * (double)(visibleItemsCount - 1L);
            double axisMargin = spacing / 2.0;
            this.renderer.setItemMargin(itemMargin);
            this.categoryAxis.setLowerMargin(axisMargin);
            this.categoryAxis.setUpperMargin(axisMargin);
        }
        for (LatestChartDataItem dataItem : this.dataItems) {
            Color seriesColor;
            this.dataset.addValue(dataItem.getValue(), (Comparable)((Object)("" + dataItem.getSeriesIndex())), (Comparable)((Object)"Latest"));
            Paint seriesPaint = seriesColor = ColorUtils.safeParseCssColor(dataItem.getDataKey().getColor());
            if (!ChartFillType.none.equals((Object)barSettings.getBackgroundSettings().getType())) {
                seriesPaint = ChartUtils.createFillPaint(barSettings.getBackgroundSettings(), seriesColor);
            }
            if (!dataItem.isHasValue()) {
                this.renderer.setSeriesVisible(dataItem.getSeriesIndex(), false);
            }
            this.renderer.setSeriesPaint(dataItem.getSeriesIndex(), seriesPaint);
            if (barSettings.getShowBorder().booleanValue()) {
                this.renderer.setSeriesOutlineStroke(dataItem.getSeriesIndex(), new BasicStroke(barSettings.getBorderWidth().floatValue()));
                this.renderer.setSeriesOutlinePaint(dataItem.getSeriesIndex(), seriesColor);
            }
            if (barSettings.getShowLabel().booleanValue()) {
                this.renderer.setSeriesItemLabelsVisible(dataItem.getSeriesIndex(), true);
                this.renderer.setSeriesItemLabelFont(dataItem.getSeriesIndex(), AwtFontUtils.toAwtFont(barSettings.getLabelFont()));
                this.renderer.setSeriesItemLabelPaint(dataItem.getSeriesIndex(), ColorUtils.safeParseCssColor(barSettings.getLabelColor()));
                this.renderer.setSeriesItemLabelGenerator(dataItem.getSeriesIndex(), this);
                ItemLabelPosition itemLabelPosition = ChartLabelPosition.top.equals((Object)barSettings.getLabelPosition()) ? new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER) : new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER);
                this.renderer.setSeriesPositiveItemLabelPosition(dataItem.getSeriesIndex(), itemLabelPosition);
                this.renderer.setSeriesNegativeItemLabelPosition(dataItem.getSeriesIndex(), itemLabelPosition);
                if (barSettings.getEnableLabelBackground().booleanValue()) {
                    this.renderer.setSeriesItemLabelsBackgroundVisible(dataItem.getSeriesIndex(), true);
                    this.renderer.setSeriesItemLabelsBackgroundPaint(dataItem.getSeriesIndex(), ColorUtils.safeParseCssColor(barSettings.getLabelBackground()));
                }
            }
            this.renderer.setSeriesItemBorderRadius(dataItem.getSeriesIndex(), barSettings.getBorderRadius());
        }
    }

    public String generateRowLabel(CategoryDataset dataset, int row) {
        return "";
    }

    public String generateColumnLabel(CategoryDataset dataset, int column) {
        return "";
    }

    public String generateLabel(CategoryDataset dataset, int row, int column) {
        return ((LatestChartDataItem)this.dataItems.get(row)).getLabel();
    }
}

