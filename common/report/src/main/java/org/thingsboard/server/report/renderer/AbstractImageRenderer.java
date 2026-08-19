/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent
 *  org.thingsboard.server.common.data.report.configuration.image.ImageAlignment
 *  org.thingsboard.server.common.data.report.configuration.image.ImageWidthType
 */
package org.thingsboard.server.report.renderer;

import java.util.Map;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageAlignment;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.ReportComponentWithLayoutRenderer;
import org.thingsboard.server.report.util.ThymeleafUtil;

public abstract class AbstractImageRenderer<C extends AbstractImageComponent>
extends ReportComponentWithLayoutRenderer<C> {
    @Override
    public String renderContent(C imageComponent, ComponentData reportDataSource) {
        Object imageWidth;
        String imageUrl = this.getImageUrl(imageComponent, reportDataSource);
        String layoutWidth = this.layoutWidthPx + "px";
        if (ImageWidthType.ORIGINAL == imageComponent.getWidthType()) {
            imageWidth = "auto";
        } else if (ImageWidthType.CUSTOM == imageComponent.getWidthType()) {
            int customWidth = imageComponent.getCustomWidth() >= 1 ? imageComponent.getCustomWidth() : 100;
            imageWidth = customWidth + "px";
        } else {
            imageWidth = layoutWidth;
        }
        String imageAlign = imageComponent.getAlignment() != null ? imageComponent.getAlignment().getValue() : ImageAlignment.CENTER.getValue();
        Map<String, Object> componentVariables = Map.of("layoutWidth", layoutWidth, "imageUrl", StringUtils.isBlank((String)imageUrl) ? "tb-empty-image" : imageUrl, "imageWidth", imageWidth, "imageAlign", imageAlign);
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/image", componentVariables);
    }

    protected abstract String getImageUrl(C var1, ComponentData var2);
}

