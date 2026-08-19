/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.report.renderer.chart.legend;

import java.util.List;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendItem;
import org.thingsboard.server.report.renderer.chart.legend.TbLegendValuesRequest;

public interface TbLegendItemSource {
    public List<TbLegendItem> getTbLegendItems(TbLegendValuesRequest var1);
}

