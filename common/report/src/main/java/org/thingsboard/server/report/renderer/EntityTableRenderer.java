/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 */
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.renderer.TableWithLayoutComponentRenderer;

@Component
public class EntityTableRenderer
extends TableWithLayoutComponentRenderer<EntityTableComponent> {
    @Override
    protected String dataSourceName() {
        return "data source";
    }

    @Override
    protected String noDataMessage() {
        return "No entities found";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ENTITY_TABLE;
    }
}

