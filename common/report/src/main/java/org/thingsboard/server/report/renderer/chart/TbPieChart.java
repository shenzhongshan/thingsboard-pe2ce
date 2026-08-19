/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.labels.PieSectionLabelGenerator
 *  org.jfree.chart.plot.PieLabelLinkStyle
 *  org.jfree.chart.ui.RectangleInsets
 *  org.jfree.chart.util.Rotation
 *  org.jfree.chart.util.UnitType
 *  org.jfree.data.general.DefaultPieDataset
 *  org.jfree.data.general.PieDataset
 *  org.thingsboard.server.common.data.report.configuration.chart.PieChartLabelPosition
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.BasicStroke;
import java.text.AttributedString;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.Rotation;
import org.jfree.chart.util.UnitType;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.thingsboard.server.common.data.report.configuration.chart.PieChartLabelPosition;
import org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataItem;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.TbLatestChart;
import org.thingsboard.server.report.renderer.chart.TbPiePlot;
import org.thingsboard.server.report.util.AwtFontUtils;
import org.thingsboard.server.report.util.ColorUtils;

public class TbPieChart
extends TbLatestChart<ReportPieChartSettings, TbPiePlot<Integer>>
implements PieSectionLabelGenerator {
    private DefaultPieDataset<Integer> pieDataset;
    private Map<Integer, String> percents;

    public TbPieChart(ReportPieChartSettings settings, LatestChartData latestChartData, Map<String, Object> variables) {
        super(settings, latestChartData, variables);
    }

    @Override
    protected TbPiePlot<Integer> createPlot() {
        this.pieDataset = new DefaultPieDataset();
        return new TbPiePlot<Integer>((PieDataset<Integer>)this.pieDataset);
    }

    @Override
    protected void setupPlot(TbPiePlot<Integer> plot) {
        plot.setPieRadius(((ReportPieChartSettings)this.chartSettings).getRadius());
        if (((ReportPieChartSettings)this.chartSettings).getShowLabel().booleanValue()) {
            plot.setLabelGenerator(this);
            plot.setLabelFont(AwtFontUtils.toAwtFont(((ReportPieChartSettings)this.chartSettings).getLabelFont()));
            plot.setLabelPaint(ColorUtils.safeParseCssColor(((ReportPieChartSettings)this.chartSettings).getLabelColor()));
            plot.setLabelShadowPaint(null);
            plot.setLabelBackgroundPaint(null);
            plot.setLabelOutlinePaint(null);
            if (((ReportPieChartSettings)this.chartSettings).getLabelPosition() == PieChartLabelPosition.outside) {
                plot.setSimpleLabels(false);
                plot.setLabelLinkStyle(PieLabelLinkStyle.QUAD_CURVE);
            } else {
                plot.setSimpleLabels(true);
                plot.setSimpleLabelOffset(new RectangleInsets(UnitType.RELATIVE, 0.28, 0.28, 0.28, 0.28));
            }
        } else {
            plot.setLabelGenerator(null);
        }
        plot.setShadowPaint(null);
        plot.setDirection(((ReportPieChartSettings)this.chartSettings).getClockwise() != false ? Rotation.CLOCKWISE : Rotation.ANTICLOCKWISE);
        if (((ReportPieChartSettings)this.chartSettings).getBorderWidth().floatValue() > 0.0f) {
            plot.setSectionOutlinesVisible(true);
            plot.setDefaultSectionOutlineStroke(new BasicStroke(((ReportPieChartSettings)this.chartSettings).getBorderWidth().floatValue()));
            plot.setDefaultSectionOutlinePaint(ColorUtils.safeParseCssColor(((ReportPieChartSettings)this.chartSettings).getBorderColor()));
        } else {
            plot.setSectionOutlinesVisible(false);
        }
        this.percents = new HashMap<Integer, String>();
        List<LatestChartDataItem> visibleDataItems = this.dataItems.stream().filter(item -> item.isHasValue() && item.getValue() >= 0.0).toList();
        double total = 0.0;
        for (LatestChartDataItem dataItem : visibleDataItems) {
            Integer key = dataItem.getSeriesIndex();
            this.pieDataset.setValue(key, dataItem.getValue());
            plot.setSectionPaint(key, ColorUtils.safeParseCssColor(dataItem.getDataKey().getColor()));
            total += dataItem.getValue();
        }
        if (visibleDataItems.isEmpty()) {
            plot.setLabelGenerator(null);
            Integer key = 0;
            this.pieDataset.setValue(key, 1.0);
            plot.setSectionPaint(key, ColorUtils.safeParseCssColor("lightgray"));
        }
        NumberFormat percentFormat = ChartUtils.createValueFormatter(0, "%");
        for (LatestChartDataItem dataItem : visibleDataItems) {
            Integer key = dataItem.getSeriesIndex();
            double percent = 0.0;
            if (total == 0.0) {
                this.pieDataset.setValue(key, 1.0);
            } else {
                percent = dataItem.getValue() / total * 100.0;
            }
            this.percents.put(key, percentFormat.format(percent));
        }
    }

    public String generateSectionLabel(PieDataset dataset, Comparable key) {
        return ((LatestChartDataItem)this.dataItems.get((Integer)key)).getLabel() + "\n" + this.percents.get((int)((Integer)key));
    }

    public AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
        return null;
    }
}

