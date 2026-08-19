/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.components.TableReportComponent
 */
package org.thingsboard.server.report.renderer;

import java.util.List;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.report.context.ComponentData;

public interface CsvReportComponentRenderer<C extends TableReportComponent> {
    public List<List<String>> render(C var1, ComponentData var2);

    public ReportComponentType getType();
}

