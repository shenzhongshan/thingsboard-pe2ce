/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.JFreeChart
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings
 *  org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 */
package org.thingsboard.server.report.renderer;

import java.awt.Graphics2D;
import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.chart.ReportBarChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType;
import org.thingsboard.server.common.data.report.configuration.chart.ReportDoughnutChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportPieChartSettings;
import org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.renderer.ChartRenderer;
import org.thingsboard.server.report.renderer.chart.TbBarChart;
import org.thingsboard.server.report.renderer.chart.TbDoughnutChart;
import org.thingsboard.server.report.renderer.chart.TbLatestChart;
import org.thingsboard.server.report.renderer.chart.TbPieChart;

@Component
public class LatestChartRenderer
extends ChartRenderer<LatestChartComponent> {
    @Override
    protected JFreeChart createChart(Graphics2D g2, LatestChartComponent component, ComponentData reportDataSource) {
        TbLatestChart<?, ?> latestChart = this.createLatestChart(component, reportDataSource.getLatestChartData(), reportDataSource.getVariables());
        return latestChart.createChart(g2);
    }

    private TbLatestChart<?, ?> createLatestChart(LatestChartComponent component, LatestChartData latestChartData, Map<String, Object> variables) {
        if (ReportComponentSubType.LATEST_BAR_CHART == component.getSubType()) {
            ReportBarChartSettings reportBarChartSettings = new ReportBarChartSettings((ReportBarChartSettings)component.getLatestChartSettings());
            return new TbBarChart(reportBarChartSettings, latestChartData, variables);
        }
        if (ReportComponentSubType.PIE_CHART == component.getSubType()) {
            ReportPieChartSettings reportPieChartSettings = new ReportPieChartSettings((ReportPieChartSettings)component.getLatestChartSettings());
            return new TbPieChart(reportPieChartSettings, latestChartData, variables);
        }
        if (ReportComponentSubType.DOUGHNUT_CHART == component.getSubType() || ReportComponentSubType.HORIZONTAL_DOUGHNUT_CHART == component.getSubType()) {
            ReportDoughnutChartSettings reportDoughnutChartSettings = new ReportDoughnutChartSettings((ReportDoughnutChartSettings)component.getLatestChartSettings());
            return new TbDoughnutChart(reportDoughnutChartSettings, latestChartData, variables);
        }
        throw new IllegalArgumentException("Latest chart with subType '" + String.valueOf(component.getSubType()) + "' is not supported");
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.LATEST_CHART;
    }
}

