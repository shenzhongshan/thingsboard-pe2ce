/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 */
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.renderer.AbstractCsvComponentRenderer;

@Component
public class CsvAlarmTableRenderer
extends AbstractCsvComponentRenderer<AlarmTableComponent> {
    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }
}

