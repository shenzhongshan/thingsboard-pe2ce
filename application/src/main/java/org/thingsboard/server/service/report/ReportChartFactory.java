/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.report;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * CE substitution for PE's ~60-class custom JFreeChart styling wrapper hierarchy: charts are built
 * directly against the JFreeChart API with reasonable default styling. Functionally equivalent
 * output (a chart image embedded in the report), much less code — see the module-level note in
 * {@link org.thingsboard.server.common.data.report.ReportComponentType}.
 */
@Component
public class ReportChartFactory {

    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 350;

    /** A time-series line chart: one line per key in {@code seriesByKey}. */
    public String timeSeriesChartDataUri(String title, Map<String, List<TsKvEntry>> seriesByKey) throws Exception {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (Map.Entry<String, List<TsKvEntry>> entry : seriesByKey.entrySet()) {
            TimeSeries series = new TimeSeries(entry.getKey());
            for (TsKvEntry kv : entry.getValue()) {
                try {
                    series.addOrUpdate(new Millisecond(new java.util.Date(kv.getTs())), Double.parseDouble(kv.getValueAsString()));
                } catch (NumberFormatException ignored) {
                    // non-numeric telemetry values can't be charted; skip
                }
            }
            dataset.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Time", "Value", dataset, true, false, false);
        return toDataUri(chart);
    }

    /** A latest-values bar chart: one bar per key. */
    public String latestBarChartDataUri(String title, Map<String, Double> latestByKey) throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        latestByKey.forEach((key, value) -> dataset.addValue(value, "Latest", key));
        JFreeChart chart = ChartFactory.createBarChart(title, "Key", "Value", dataset, PlotOrientation.VERTICAL, false, false, false);
        return toDataUri(chart);
    }

    /** A latest-values pie chart: one slice per key. */
    public String latestPieChartDataUri(String title, Map<String, Double> latestByKey) throws Exception {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        latestByKey.forEach(dataset::setValue);
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, false, false);
        return toDataUri(chart);
    }

    private String toDataUri(JFreeChart chart) throws Exception {
        BufferedImage image = chart.createBufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

}
