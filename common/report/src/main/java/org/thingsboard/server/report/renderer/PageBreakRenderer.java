/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.components.PageBreakComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 */
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.PageBreakComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.PdfReportComponentRenderer;

@Component
public class PageBreakRenderer
implements PdfReportComponentRenderer<PageBreakComponent> {
    @Override
    public String render(PageBreakComponent component, ComponentData reportDataSource) {
        return "<div class=\"page-break\"></div>";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.PAGE_BREAK;
    }
}

