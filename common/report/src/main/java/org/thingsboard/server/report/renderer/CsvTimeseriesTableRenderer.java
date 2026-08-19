/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataKeySettings
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent
 */
package org.thingsboard.server.report.renderer;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.renderer.AbstractCsvComponentRenderer;

@Component
public class CsvTimeseriesTableRenderer
extends AbstractCsvComponentRenderer<TimeseriesTableComponent> {
    @Override
    public List<DataKey> getColumns(TimeseriesTableComponent component, DataSource dataSource) {
        LinkedList<DataKey> dataKeys = new LinkedList<DataKey>();
        if (component.isShowTimestamp()) {
            DataKey tsDataKey = DataKey.builder().name("ts").label(component.getTimestampLabel()).settings((DataKeySettings)component.getTimestampColumnSettings()).usePostProcessing(false).build();
            dataKeys.add(tsDataKey);
        }
        Optional.ofNullable(dataSource.getDataKeys()).ifPresent(dataKeys::addAll);
        Optional.ofNullable(dataSource.getLatestDataKeys()).ifPresent(dataKeys::addAll);
        return dataKeys;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }
}

