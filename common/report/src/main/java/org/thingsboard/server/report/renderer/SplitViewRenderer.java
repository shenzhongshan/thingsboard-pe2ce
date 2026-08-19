/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent
 *  org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment
 */
package org.thingsboard.server.report.renderer;

import java.util.HashMap;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.ReportComponentWithLayoutRenderer;
import org.thingsboard.server.report.util.ThymeleafUtil;

@Component
public class SplitViewRenderer
extends ReportComponentWithLayoutRenderer<SplitViewComponent> {
    @Override
    protected String renderContent(SplitViewComponent component, ComponentData reportDataSource) {
        HashMap<String, Object> componentVariables = new HashMap<String, Object>(reportDataSource.getVariables());
        componentVariables.put("leftVerticalAlignment", this.resolveVerticalAlignment(component.getLeftVerticalAlignment()));
        componentVariables.put("rightVerticalAlignment", this.resolveVerticalAlignment(component.getRightVerticalAlignment()));
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/split-view-template", componentVariables);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.SPLIT_VIEW;
    }

    private String resolveVerticalAlignment(VerticalAlignment vertical) {
        return (vertical != null ? vertical : VerticalAlignment.MIDDLE).getValue();
    }
}

