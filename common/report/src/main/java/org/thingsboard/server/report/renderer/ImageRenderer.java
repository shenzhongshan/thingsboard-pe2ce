/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.components.DataReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ImageComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.image.ImageSourceType
 */
package org.thingsboard.server.report.renderer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.AbstractImageRenderer;
import org.thingsboard.server.report.util.ReportUtils;

@Component
public class ImageRenderer
extends AbstractImageRenderer<ImageComponent> {
    @Override
    protected String getImageUrl(ImageComponent component, ComponentData reportDataSource) {
        String imageUrl = "";
        if (ImageSourceType.ENTITY_KEY == component.getSourceType()) {
            if (!reportDataSource.getEntityDatas().isEmpty()) {
                List dataKeys;
                Map<String, String> entityData = reportDataSource.getEntityDatas().get(0);
                Optional<DataSource> dataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
                if (dataSource.isPresent() && (dataKeys = dataSource.get().getDataKeys()) != null && !dataKeys.isEmpty()) {
                    DataKey dataKey = (DataKey)dataKeys.get(0);
                    imageUrl = entityData.get(dataKey.getLabel());
                }
            }
        } else {
            imageUrl = component.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = "/assets/report/components/image-placeholder.svg";
            }
        }
        return imageUrl;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.IMAGE;
    }
}

