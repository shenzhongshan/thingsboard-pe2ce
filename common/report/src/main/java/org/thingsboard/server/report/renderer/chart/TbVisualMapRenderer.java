/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.report.renderer.chart;

import java.awt.Paint;
import org.thingsboard.server.report.renderer.chart.TbVisualMap;

public interface TbVisualMapRenderer {
    public int getAreaPass();

    public int getItemPass();

    public void setCurrentVisualMapPaint(Paint var1);

    public void setCurrentVisualMapFillPaint(Paint var1);

    public void setVisualMap(TbVisualMap var1);
}

