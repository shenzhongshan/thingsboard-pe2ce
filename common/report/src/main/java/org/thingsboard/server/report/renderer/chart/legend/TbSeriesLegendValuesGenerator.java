/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.data.xy.XYDataset
 */
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.data.xy.XYDataset;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValues;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;

public interface TbSeriesLegendValuesGenerator {
    public TbLegendValues generateLegendValues(TbLegendValuesRequest var1, XYDataset var2, int var3);
}

