/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.report.configuration.components.LayoutReportComponent
 *  org.thingsboard.server.common.data.report.configuration.style.Insets
 */
package org.thingsboard.server.report.renderer;

import java.util.HashMap;
import java.util.Map;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.components.LayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.PdfReportComponentRenderer;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

public abstract class ReportComponentWithLayoutRenderer<C extends LayoutReportComponent>
implements PdfReportComponentRenderer<C> {
    private static final int DEFAULT_COMPONENT_MARGIN_SIZE = 0;
    private static final int DEFAULT_COMPONENT_PADDING_SIZE = 0;
    private static final int DEFAULT_COMPONENT_BORDER_SIZE = 0;
    protected int layoutWidthPx;

    @Override
    public String render(C component, ComponentData reportDataSource) {
        Integer borderWidth;
        Insets paddings;
        if (StringUtils.isNotBlank((String)reportDataSource.getError())) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", reportDataSource.getError()));
        }
        Insets margins = component.getMargins();
        if (margins == null) {
            margins = new Insets(0);
        }
        if ((paddings = component.getPaddings()) == null) {
            paddings = new Insets(0);
        }
        if ((borderWidth = component.getBorderWidth()) == null) {
            borderWidth = 0;
        }
        this.layoutWidthPx = reportDataSource.getUsablePageWidthPx();
        this.layoutWidthPx = (int)((float)this.layoutWidthPx - (float)(margins.getLeft() + margins.getRight()) * 4.0f / 3.0f);
        this.layoutWidthPx = (int)((float)this.layoutWidthPx - (float)(paddings.getLeft() + paddings.getRight()) * 4.0f / 3.0f);
        this.layoutWidthPx = (int)((float)this.layoutWidthPx - (float)(borderWidth * 2) * 4.0f / 3.0f);
        String content = this.renderContent(component, reportDataSource);
        HashMap<String, Object> layoutVariables = new HashMap<String, Object>();
        layoutVariables.put("htmlContent", content);
        layoutVariables.put("background", component.getBackground() != null ? ColorUtils.normalizeCssColor(component.getBackground()) : "transparent");
        layoutVariables.put("borderWidth", borderWidth);
        layoutVariables.put("borderRadius", component.getBorderRadius() != null ? component.getBorderRadius() : "0");
        layoutVariables.put("borderColor", component.getBorderColor() != null ? ColorUtils.normalizeCssColor(component.getBorderColor()) : "transparent");
        layoutVariables.put("leftMargin", margins.getLeft());
        layoutVariables.put("rightMargin", margins.getRight());
        layoutVariables.put("topMargin", margins.getTop());
        layoutVariables.put("bottomMargin", margins.getBottom());
        layoutVariables.put("leftPadding", paddings.getLeft());
        layoutVariables.put("rightPadding", paddings.getRight());
        layoutVariables.put("topPadding", paddings.getTop());
        layoutVariables.put("bottomPadding", paddings.getBottom());
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/component-layout", layoutVariables);
    }

    protected abstract String renderContent(C var1, ComponentData var2);
}

