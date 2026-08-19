/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.labels.XYItemLabelGenerator
 *  org.jfree.data.xy.XYDataset
 */
package org.thingsboard.server.report.renderer.chart;

import java.text.NumberFormat;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.renderer.chart.TbStateValueConverter;

public class TbXYItemLabelGenerator
implements XYItemLabelGenerator {
    private final NumberFormat formatter;
    private final TbStateValueConverter stateValueConverter;

    public TbXYItemLabelGenerator(Integer decimals, String units, TbStateValueConverter stateValueConverter) {
        int decimalsInt = decimals != null ? decimals : 2;
        this.formatter = ChartUtils.createValueFormatter(decimalsInt, units);
        this.stateValueConverter = stateValueConverter;
    }

    public String generateLabel(XYDataset dataset, int series, int item) {
        double y = dataset.getYValue(series, item);
        if (Double.isNaN(y) && dataset.getY(series, item) == null) {
            return "";
        }
        String label = null;
        if (this.stateValueConverter != null) {
            label = this.stateValueConverter.formatLabel(y);
        }
        if (label == null) {
            label = this.formatter.format(y);
        }
        return label;
    }
}

