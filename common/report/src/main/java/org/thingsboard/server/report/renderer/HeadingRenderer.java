/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.components.HeadingComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.style.TextAlignment
 *  org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment
 */
package org.thingsboard.server.report.renderer;

import java.util.HashMap;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.ReportComponentWithLayoutRenderer;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

@Component
public class HeadingRenderer
extends ReportComponentWithLayoutRenderer<HeadingComponent> {
    @Override
    public String renderContent(HeadingComponent component, ComponentData reportDataSource) {
        String processedText = ThymeleafUtil.renderFromTextString(component.getValue(), reportDataSource.getVariables());
        HashMap<String, Object> templateVars = new HashMap<String, Object>();
        templateVars.put("color", this.resolveColor(component));
        templateVars.put("fontSize", this.resolveFontSize(component));
        templateVars.put("fontWeight", component.getFont().getWeight().getValue());
        templateVars.put("fontStyle", component.getFont().getStyle().getValue());
        templateVars.put("fontFamily", this.resolveFontFamily(component));
        templateVars.put("textAlignment", this.resolveTextAlignment(component));
        templateVars.put("verticalAlignment", this.resolveVerticalAlignment(component));
        templateVars.put("height", this.resolveHeight(component));
        templateVars.put("value", processedText);
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/heading-template", templateVars);
    }

    private String resolveColor(HeadingComponent component) {
        return component.getColor() != null ? ColorUtils.normalizeCssColor(component.getColor()) : "#000";
    }

    private Float resolveFontSize(HeadingComponent component) {
        Float size = component.getFont().getSize();
        return Float.valueOf(size != null && size.floatValue() > 0.0f ? size.floatValue() : 10.0f);
    }

    private String resolveFontFamily(HeadingComponent component) {
        String family = component.getFont().getFamily();
        return family != null && !family.isEmpty() ? family : "Roboto";
    }

    private String resolveTextAlignment(HeadingComponent component) {
        TextAlignment alignment = component.getTextAlignment();
        return (alignment != null ? alignment : TextAlignment.CENTER).getValue();
    }

    private String resolveVerticalAlignment(HeadingComponent component) {
        VerticalAlignment vertical = component.getVerticalAlignment();
        return (vertical != null ? vertical : VerticalAlignment.MIDDLE).getValue();
    }

    private String resolveHeight(HeadingComponent component) {
        Integer height = component.getHeight();
        return height != null && height > 0 ? height + "pt" : "100%";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.HEADING;
    }
}

