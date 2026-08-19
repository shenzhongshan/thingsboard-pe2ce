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
import org.thingsboard.server.report.renderer.AbstractCsvComponentRenderer;

@Component
public class CsvEntityTableRenderer
extends AbstractCsvComponentRenderer<EntityTableComponent> {
    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ENTITY_TABLE;
    }
}

