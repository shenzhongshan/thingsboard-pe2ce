/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.components.RichTextComponent
 */
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.ReportComponentWithLayoutRenderer;
import org.thingsboard.server.report.util.ThymeleafUtil;

@Component
public class RichTextRenderer
extends ReportComponentWithLayoutRenderer<RichTextComponent> {
    @Override
    protected String renderContent(RichTextComponent richTextComponent, ComponentData reportDataSource) {
        return ThymeleafUtil.renderFromTextString(richTextComponent.getValue(), reportDataSource.getVariables());
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.RICH_TEXT;
    }
}

