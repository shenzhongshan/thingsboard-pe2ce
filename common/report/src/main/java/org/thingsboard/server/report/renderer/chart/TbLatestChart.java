/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.ChartTheme
 *  org.jfree.chart.JFreeChart
 *  org.jfree.chart.StandardChartTheme
 *  org.jfree.chart.event.TitleChangeListener
 *  org.jfree.chart.plot.Plot
 *  org.jfree.chart.title.TextTitle
 *  org.jfree.chart.title.Title
 *  org.jfree.chart.ui.HorizontalAlignment
 *  org.jfree.chart.ui.RectangleEdge
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.ui.VerticalAlignment
 *  org.thingsboard.server.common.data.report.configuration.chart.LegendPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.event.TitleChangeListener;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.common.data.report.configuration.chart.LegendPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportLatestChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.legend.TbLatestChartLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLatestLegendTitle;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

public abstract class TbLatestChart<S extends ReportLatestChartSettings, P extends Plot> {
    static ChartTheme currentChartTheme = new StandardChartTheme("TbChartTheme");
    protected final S chartSettings;
    protected final LatestChartData latestChartData;
    private final Map<String, Object> variables;
    protected final List<LatestChartDataItem> dataItems;
    protected final NumberFormat valueFormatter;
    protected List<TbLatestChartLegendItem> legendItems;
    protected double total = 0.0;
    protected String totalText = "N/A";
    protected JFreeChart chart;

    public TbLatestChart(S chartSettings, LatestChartData latestChartData, Map<String, Object> variables) {
        this.chartSettings = chartSettings;
        this.latestChartData = latestChartData;
        this.variables = variables;
        Comparator<LatestChartDataItem> comparator = chartSettings.getSortSeries() != false ? Comparator.comparing(LatestChartDataItem::getLabel, String.CASE_INSENSITIVE_ORDER) : Comparator.comparing(LatestChartDataItem::getIndex);
        this.dataItems = latestChartData.getChartData().stream().flatMap(ds -> ds.getItems().stream()).sorted(comparator).toList();
        for (int i = 0; i < this.dataItems.size(); ++i) {
            this.dataItems.get(i).setSeriesIndex(i);
        }
        int decimals = chartSettings.getDecimals() != null ? chartSettings.getDecimals() : 2;
        String units = chartSettings.getUnits();
        this.valueFormatter = ChartUtils.createValueFormatter(decimals, units);
        List<LatestChartDataItem> dataItemsWithValue = this.dataItems.stream().filter(LatestChartDataItem::isHasValue).toList();
        boolean hasTotalValue = !dataItemsWithValue.isEmpty();
        for (LatestChartDataItem item2 : dataItemsWithValue) {
            this.total += item2.getValue();
        }
        if (hasTotalValue) {
            this.totalText = this.valueFormatter.format(this.total);
        }
        if (chartSettings.getShowLegend().booleanValue()) {
            this.legendItems = this.dataItems.stream().map(item -> {
                TbLatestChartLegendItem legendItem = new TbLatestChartLegendItem();
                legendItem.setLabel(item.getLabel());
                legendItem.setColor(item.getDataKey().getColor());
                if (item.isHasValue()) {
                    legendItem.setHasValue(true);
                    legendItem.setValue(this.valueFormatter.format(item.getValue()));
                } else {
                    legendItem.setHasValue(false);
                    legendItem.setValue("--");
                }
                return legendItem;
            }).toList();
            if (!chartSettings.getShowTotal().booleanValue() && chartSettings.getLegendShowTotal().booleanValue()) {
                TbLatestChartLegendItem legendItem = new TbLatestChartLegendItem();
                legendItem.setLabel("Total");
                legendItem.setHasValue(hasTotalValue);
                if (hasTotalValue) {
                    legendItem.setValue(this.totalText);
                } else {
                    legendItem.setValue("--");
                }
                legendItem.setTotal(true);
                legendItem.setColor("rgba(0, 0, 0, 0.06)");
                this.legendItems = new ArrayList<TbLatestChartLegendItem>(this.legendItems);
                this.legendItems.add(legendItem);
            }
        }
    }

    public JFreeChart createChart(Graphics2D g2) {
        P plot = this.createPlot();
        this.chart = new JFreeChart(null, null, plot, false);
        currentChartTheme.apply(this.chart);
        plot.setBackgroundPaint(null);
        plot.setOutlinePaint(null);
        plot.setInsets(new RectangleInsets(2.0, 0.0, 2.0, 0.0));
        this.chart.setBackgroundPaint((Paint)ColorUtils.TRANSPARENT);
        this.setupPlot(plot);
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
            if (!this.chartSettings.getShowLegend().booleanValue() || this.chartSettings.getLegendPosition() != LegendPosition.top) {
                title.setPadding(new RectangleInsets(1.0, 1.0, 8.0, 1.0));
            }
            this.chart.setTitle(title);
        }
        if (this.chartSettings.getShowLegend().booleanValue()) {
            this.setupLegend();
        }
        return this.chart;
    }

    protected abstract P createPlot();

    protected abstract void setupPlot(P var1);

    private void setupLegend() {
        TbLatestLegendTitle legend = new TbLatestLegendTitle(this.legendItems);
        legend.setLegendLabelFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendLabelFont()));
        legend.setLegendLabelPaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendLabelColor()));
        legend.setLegendValueFont(AwtFontUtils.toAwtFont(this.chartSettings.getLegendValueFont()));
        legend.setLegendValuePaint(ColorUtils.safeParseCssColor(this.chartSettings.getLegendValueColor()));
        RectangleEdge position = RectangleEdge.TOP;
        LegendPosition legendPosition = this.chartSettings.getLegendPosition();
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
        legend.setPosition(position);
        if (RectangleEdge.isLeftOrRight((RectangleEdge)position)) {
            legend.setVerticalAlignment(VerticalAlignment.CENTER);
            if (position == RectangleEdge.LEFT) {
                legend.setPadding(new RectangleInsets(0.0, 0.0, 0.0, 24.0));
            } else {
                legend.setPadding(new RectangleInsets(0.0, 24.0, 0.0, 0.0));
            }
        } else if (position == RectangleEdge.TOP) {
            legend.setPadding(new RectangleInsets(8.0, 0.0, 16.0, 0.0));
        } else {
            legend.setPadding(new RectangleInsets(16.0, 0.0, 8.0, 0.0));
        }
        this.chart.addSubtitle((Title)legend);
        legend.addChangeListener((TitleChangeListener)this.chart);
    }
}

