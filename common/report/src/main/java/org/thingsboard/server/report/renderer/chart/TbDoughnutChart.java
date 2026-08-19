/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.plot.CenterTextMode
 *  org.jfree.chart.util.Rotation
 *  org.jfree.data.general.DefaultPieDataset
 *  org.jfree.data.general.PieDataset
 *  org.thingsboard.server.common.data.report.configuration.chart.DoughnutLayout
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings
 *  org.thingsboard.server.common.data.report.configuration.style.Font
 *  org.thingsboard.server.common.data.report.configuration.style.FontStyle
 *  org.thingsboard.server.common.data.report.configuration.style.FontWeight
 */
package org.thingsboard.server.report.renderer.chart;

import java.util.List;
import java.util.Map;
import org.jfree.chart.plot.CenterTextMode;
import org.jfree.chart.util.Rotation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.thingsboard.server.common.data.report.configuration.chart.DoughnutLayout;
import org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;
import org.thingsboard.server.report.renderer.chart.TbLatestChart;
import org.thingsboard.server.report.renderer.chart.TbRingPlot;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbDoughnutChart
extends TbLatestChart<ReportDoughnutChartSettings, TbRingPlot> {
    private DefaultPieDataset<Integer> doughnutDataset;

    public TbDoughnutChart(ReportDoughnutChartSettings settings, LatestChartData latestChartData, Map<String, Object> variables) {
        super(settings, latestChartData, variables);
    }

    @Override
    protected TbRingPlot createPlot() {
        this.doughnutDataset = new DefaultPieDataset();
        return new TbRingPlot((PieDataset)this.doughnutDataset);
    }

    @Override
    protected void setupPlot(TbRingPlot plot) {
        Integer key;
        plot.setSegmentsPadAngle(2.0);
        plot.setRoundSegmentCorners(true);
        plot.setAbsoluteSectionDepth(27.0);
        if (((ReportDoughnutChartSettings)this.chartSettings).getAutoScale().booleanValue()) {
            plot.setScaleBase(134.0);
        }
        plot.setSeparatorsVisible(false);
        plot.setLabelGenerator(null);
        plot.setShadowPaint(null);
        plot.setDirection(((ReportDoughnutChartSettings)this.chartSettings).getClockwise() != false ? Rotation.CLOCKWISE : Rotation.ANTICLOCKWISE);
        plot.setSectionOutlinesVisible(false);
        List<LatestChartDataItem> visibleDataItems = this.dataItems.stream().filter(item -> item.isHasValue() && item.getValue() >= 0.0).toList();
        double total = 0.0;
        for (LatestChartDataItem dataItem : visibleDataItems) {
            key = dataItem.getSeriesIndex();
            this.doughnutDataset.setValue(key, dataItem.getValue());
            plot.setSectionPaint(key, ColorUtils.safeParseCssColor(dataItem.getDataKey().getColor()));
            total += dataItem.getValue();
        }
        if (visibleDataItems.isEmpty()) {
            Integer key2 = 0;
            this.doughnutDataset.setValue(key2, 1.0);
            plot.setSectionPaint(key2, ColorUtils.safeParseCssColor("lightgray"));
        }
        for (LatestChartDataItem dataItem : visibleDataItems) {
            key = dataItem.getSeriesIndex();
            if (total != 0.0) continue;
            this.doughnutDataset.setValue(key, 1.0);
        }
        if (((ReportDoughnutChartSettings)this.chartSettings).getLayout() == DoughnutLayout.WITH_TOTAL) {
            plot.setCenterLabel("Total");
            plot.setCenterLabelFont(AwtFontUtils.toAwtFont(Font.builder().family("Roboto").size(Float.valueOf(12.0f)).weight(FontWeight.NORMAL).style(FontStyle.NORMAL).build()));
            plot.setCenterLabelColor(ColorUtils.safeParseCssColor("rgba(0, 0, 0, 0.38)"));
            plot.setCenterTextMode(CenterTextMode.FIXED);
            plot.setCenterText(this.totalText);
            plot.setCenterTextFont(AwtFontUtils.toAwtFont(((ReportDoughnutChartSettings)this.chartSettings).getTotalValueFont()));
            plot.setCenterTextColor(ColorUtils.safeParseCssColor(((ReportDoughnutChartSettings)this.chartSettings).getTotalValueColor()));
        } else {
            plot.setCenterTextMode(CenterTextMode.NONE);
        }
    }
}

