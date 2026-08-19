/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.report.renderer.chart;

import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;
import org.thingsboard.server.report.renderer.chart.legend.TbSeriesLegendValuesGenerator;

public interface TbItemRenderer {
    public TbLegendItem getTbLegendItem(TbLegendValuesRequest var1, int var2, int var3);

    public TbSeriesLegendValuesGenerator getTbSeriesLegendValuesGenerator();

    public void setTbSeriesLegendValuesGenerator(TbSeriesLegendValuesGenerator var1);
}

