/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.TableSortOrder
 *  org.thingsboard.server.common.data.report.configuration.components.TableReportComponent
 *  org.thingsboard.server.common.data.report.configuration.style.Heading
 */
package org.thingsboard.server.report.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.CsvReportComponentRenderer;
import org.thingsboard.server.report.util.ReportUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

public abstract class AbstractCsvComponentRenderer<C extends TableReportComponent>
implements CsvReportComponentRenderer<C> {
    @Override
    public List<List<String>> render(C component, ComponentData componentData) {
        Optional<DataSource> dataSourceOpt = ReportUtils.getSingleDataSource(component);
        if (dataSourceOpt.isEmpty()) {
            return List.of(List.of("Data source is not configured for " + component.getType().name() + " component"));
        }
        DataSource dataSource = dataSourceOpt.get();
        List<DataKey> columns = this.getColumns(component, dataSource);
        ArrayList<List<String>> csvContent = new ArrayList<List<String>>();
        this.addHeading((TableReportComponent)component, componentData, (List<List<String>>)csvContent);
        this.addHeaderRow(columns, csvContent);
        this.addDataRows(columns, component.getTableSortOrder(), componentData, csvContent);
        return csvContent;
    }

    private void addHeading(TableReportComponent component, ComponentData componentData, List<List<String>> content) {
        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            Heading tableHeading = component.getTableHeading();
            String headingText = ThymeleafUtil.renderFromTextString(tableHeading.getText(), componentData.getVariables());
            content.add(List.of(headingText));
        }
    }

    private void addHeaderRow(List<DataKey> columns, List<List<String>> content) {
        List headers = columns.stream().map(DataKey::getLabel).collect(Collectors.toList());
        content.add(headers);
    }

    private void addDataRows(List<DataKey> columns, TableSortOrder tableSortOrder, ComponentData componentData, List<List<String>> content) {
        List<Map<String, String>> entityDatas = componentData.getEntityDatas();
        ReportUtils.sortRowsByTableSortOrder(entityDatas, tableSortOrder);
        for (Map<String, String> row : entityDatas) {
            List formattedValues = columns.stream().map(col -> (String)row.get(col.getLabel())).collect(Collectors.toList());
            content.add(formattedValues);
        }
    }

    public List<DataKey> getColumns(C component, DataSource dataSource) {
        return dataSource.getDataKeys();
    }
}

