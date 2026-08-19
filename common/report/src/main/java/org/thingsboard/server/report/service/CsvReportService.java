/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.apache.commons.lang3.exception.ExceptionUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.id.ReportTemplateId
 *  org.thingsboard.server.common.data.id.TenantId
 *  org.thingsboard.server.common.data.job.task.ReportTask
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.report.ReportData
 *  org.thingsboard.server.common.data.report.ReportTemplate
 *  org.thingsboard.server.common.data.report.TbReportFormat
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig
 *  org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.DataReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.components.SubReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.TableReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent
 *  org.thingsboard.server.common.data.util.DataSourceUtils
 */
package org.thingsboard.server.report.service;

import org.thingsboard.common.util.JacksonUtil;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.util.DataSourceUtils;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.renderer.CsvReportComponentRenderer;
import org.thingsboard.server.report.service.AbstractReportService;
import org.thingsboard.server.report.util.CsvUtils;
import org.thingsboard.server.report.util.ReportUtils;

@Service
public class CsvReportService
extends AbstractReportService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(CsvReportService.class);
    private final Map<ReportComponentType, CsvReportComponentRenderer<TableReportComponent>> componentsRenderers = new HashMap<ReportComponentType, CsvReportComponentRenderer<TableReportComponent>>();

    private CsvReportService(List<CsvReportComponentRenderer> renderers) {
        renderers.forEach(renderer -> {
            ReportComponentType type = renderer.getType();
            if (type != null) {
                this.componentsRenderers.put(type, (CsvReportComponentRenderer<TableReportComponent>)renderer);
            }
        });
    }

    @Override
    public ReportData generateReport(ReportTask task, TbReportCtx ctx) {
        TenantId tenantId = task.getTenantId();
        log.trace("[{}] Executing generateReport, reportRequest [{}]", (Object)tenantId, (Object)task);
        ReportTemplateConfig configuration = task.getReportTemplateConfig();
        EntityData stateEntity = task.getOriginator() != null ? DataSourceUtils.entityDataFromEntityId((EntityId)task.getOriginator()) : null;
        List<List<String>> content = this.renderContent(ctx, stateEntity);
        byte[] csvBytes = CsvUtils.generateCsv(content);
        String reportName = ReportUtils.prepareReportName(configuration.getNamePattern(), new Date(), task.getTimezone(), TbReportFormat.CSV.getExtension());
        return ReportData.builder().data(csvBytes).contentType(configuration.getFormat().getContentType()).name(reportName).build();
    }

    private List<List<String>> renderContent(TbReportCtx ctx, EntityData stateEntity) {
        LinkedList<List<String>> content = new LinkedList<List<String>>();
        List<ReportComponent> components = ctx.getConfiguration().getComponents();
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        block5: for (ReportComponent component : components) {
            switch (component.getType()) {
                case SUB_REPORT: {
                    content.addAll(this.renderSubReport(ctx, (SubReportComponent)component, stateEntityId));
                    continue block5;
                }
                case TIME_SERIES_TABLE: {
                    content.addAll(this.renderTimeseriesTables(ctx, (TableReportComponent)component, stateEntityId));
                    continue block5;
                }
                case ALARM_TABLE: 
                case ENTITY_TABLE: {
                    content.addAll(this.renderTableComponent(ctx, (TableReportComponent)component, stateEntity));
                    continue block5;
                }
            }
            throw new IllegalArgumentException("Unsupported component type: " + String.valueOf(component.getType()));
        }
        return content;
    }

    private List<List<String>> renderTableComponent(TbReportCtx ctx, TableReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = this.getComponentData(ctx, (ReportComponent)component, stateEntity);
            return this.componentsRenderers.get(component.getType()).render(component, componentData);
        }
        catch (Exception e) {
            log.error("[{}] Failed to render component of type [{}]", new Object[]{ctx.getTenantId(), component.getType(), e});
            return this.renderError("Failed to render component of type: " + String.valueOf(component.getType()), e);
        }
    }

    private List<List<String>> renderSubReport(TbReportCtx ctx, SubReportComponent subReportComponent, EntityId stateEntityId) {
        ReportTemplateId templateId = subReportComponent.getTemplateId();
        if (templateId == null) {
            return this.renderError("Report template id is not configured for Subreport component");
        }
        LinkedList<List<String>> content = new LinkedList<List<String>>();
        try {
            ReportTemplate reportTemplate = this.dataService.findReportTemplate(templateId, ctx);
            if (reportTemplate == null) {
                return this.renderError("Template with id " + String.valueOf(templateId) + " not found. Please check the configuration.");
            }
            TbReportCtx subReportCtx = ctx.createSubReportCxt(reportTemplate.getConfiguration());
            List<EntityData> entities = this.getSubReportEntities(ctx, (DataReportComponent)subReportComponent, stateEntityId);
            for (EntityData entity : entities) {
                content.addAll(this.renderContent(subReportCtx, entity));
            }
            return content;
        }
        catch (Exception e) {
            log.error("[{}] Failed to render Subreport, template id: {}", new Object[]{ctx.getTenantId(), templateId, e});
            return this.renderError("Failed to render sub-report " + String.valueOf(templateId), e);
        }
    }

    private List<List<String>> renderTimeseriesTables(TbReportCtx ctx, TableReportComponent component, EntityId stateEntityId) {
        LinkedList<List<String>> content = new LinkedList<List<String>>();
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
        if (dataSource.isEmpty()) {
            return this.renderError("Data source is not configured for time series table");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return this.renderError("At least one time series column should be specified for time series table");
        }
        DataSource latestDataSource = DataSource.builder().type(ds.getType()).deviceId(ds.getDeviceId()).entityAliasId(ds.getEntityAliasId()).filterId(ds.getFilterId()).dataKeys(ds.getLatestDataKeys()).build();
        List<EntityData> entityDatas = this.fetchEntities(ctx, latestDataSource, stateEntityId);
        for (EntityData entity : entityDatas) {
            content.addAll(this.renderTableComponent(ctx, component, entity));
        }
        return content;
    }

    private ComponentData getComponentData(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        ComponentData componentData = switch (component.getType()) {
            case TIME_SERIES_TABLE -> this.buildTsComponentData(0, ctx, (TimeseriesTableComponent)component, stateEntity);
            case ALARM_TABLE -> this.buildAlarmComponentData(0, ctx, (AlarmTableComponent)component, stateEntity);
            case ENTITY_TABLE -> this.buildEntityComponentData(ctx, (EntityTableComponent)component, stateEntity);
            default -> throw new IllegalArgumentException("Unsupported component type: " + String.valueOf(component.getType()));
        };
        this.populateReportVars(componentData, ctx);
        return componentData;
    }

    private ComponentData buildEntityComponentData(TbReportCtx ctx, EntityTableComponent component, EntityData stateEntity) {
        Optional<DataSource> singleDataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
        if (singleDataSource.isEmpty()) {
            return new ComponentData(0);
        }
        List<Map<String, String>> entityDatas = this.collectEntityDatas(ctx, singleDataSource.get(), stateEntity != null ? stateEntity.getEntityId() : null);
        HashMap<String, Object> variables = new HashMap<String, Object>(this.toStringMap(stateEntity, (List<DataKey>)singleDataSource.get().getDataKeys(), ctx, null));
        return new ComponentData(0, null, entityDatas, variables);
    }

    private List<List<String>> renderError(String errorDescription) {
        return this.renderError(errorDescription, null);
    }

    private List<List<String>> renderError(String errorDescription, Exception e) {
        if (e instanceof InterruptedException || ExceptionUtils.getRootCause((Throwable)e) instanceof InterruptedException) {
            throw new RuntimeException(e);
        }
        if (e != null) {
            errorDescription = (String)errorDescription + " Error: " + e.getMessage();
        }
        return List.of(List.of(errorDescription));
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.CSV;
    }
}

