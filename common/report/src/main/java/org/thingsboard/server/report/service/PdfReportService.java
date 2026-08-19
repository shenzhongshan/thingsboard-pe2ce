/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.util.concurrent.SettableFuture
 *  lombok.Generated
 *  org.apache.commons.lang3.exception.ExceptionUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig
 *  org.thingsboard.server.common.data.dashboardreport.DashboardReportData
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.id.ReportTemplateId
 *  org.thingsboard.server.common.data.id.TenantId
 *  org.thingsboard.server.common.data.job.task.ReportTask
 *  org.thingsboard.server.common.data.kv.Aggregation
 *  org.thingsboard.server.common.data.kv.BasicTsKvEntry
 *  org.thingsboard.server.common.data.kv.KvEntry
 *  org.thingsboard.server.common.data.kv.TsKvEntry
 *  org.thingsboard.server.common.data.page.SortOrder$Direction
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.report.ReportData
 *  org.thingsboard.server.common.data.report.ReportTemplate
 *  org.thingsboard.server.common.data.report.TbReportFormat
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 *  org.thingsboard.server.common.data.report.configuration.DataSourceType
 *  org.thingsboard.server.common.data.report.configuration.HeaderFooter
 *  org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig
 *  org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig
 *  org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration
 *  org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType
 *  org.thingsboard.server.common.data.report.configuration.chart.ReportRangeChartSettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings
 *  org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold
 *  org.thingsboard.server.common.data.report.configuration.chart.ValueSourceConfig
 *  org.thingsboard.server.common.data.report.configuration.chart.ValueSourceType
 *  org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent
 *  org.thingsboard.server.common.data.report.configuration.components.DashboardComponent
 *  org.thingsboard.server.common.data.report.configuration.components.DataReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ErrorComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ImageComponent
 *  org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 *  org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent
 *  org.thingsboard.server.common.data.report.configuration.components.SubReportComponent
 *  org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent
 *  org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent
 *  org.thingsboard.server.common.data.report.configuration.image.ImageSourceType
 *  org.thingsboard.server.common.data.report.configuration.style.Insets
 *  org.thingsboard.server.common.data.report.configuration.style.PageOrientation
 *  org.thingsboard.server.common.data.report.configuration.style.PageSize
 *  org.thingsboard.server.common.data.report.configuration.timewindow.History
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator$TimeRange
 *  org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration
 *  org.thingsboard.server.common.data.util.DataSourceUtils
 *  org.xhtmlrenderer.pdf.ITextRenderer
 */
package org.thingsboard.server.report.service;

import org.thingsboard.common.util.JacksonUtil;
import com.google.common.util.concurrent.SettableFuture;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Generated;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.ReportComponentSubType;
import org.thingsboard.server.common.data.report.configuration.chart.ReportRangeChartSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceConfig;
import org.thingsboard.server.common.data.report.configuration.chart.ValueSourceType;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ErrorComponent;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.LatestChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SplitViewComponent;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesChartComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.common.data.util.DataSourceUtils;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.HeaderFooterRenderLayout;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.chart.DataPostProcessFunction;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.LatestChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartRangeItem;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;
import org.thingsboard.server.report.renderer.PdfReportComponentRenderer;
import org.thingsboard.server.report.renderer.chart.ChartUtils;
import org.thingsboard.server.report.service.AbstractReportService;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.HtmlRenderUtils;
import org.thingsboard.server.report.util.ReportQueryUtils;
import org.thingsboard.server.report.util.ReportUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;
import org.thingsboard.server.report.util.WebReportClient;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class PdfReportService
extends AbstractReportService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);
    private final Map<ReportComponentType, PdfReportComponentRenderer<ReportComponent>> componentsRenderers = new EnumMap<ReportComponentType, PdfReportComponentRenderer<ReportComponent>>(ReportComponentType.class);
    private final WebReportClient webReportClient;

    private PdfReportService(List<PdfReportComponentRenderer> renderers, WebReportClient webReportClient) {
        renderers.forEach(renderer -> {
            ReportComponentType type = renderer.getType();
            if (type != null) {
                this.componentsRenderers.put(type, (PdfReportComponentRenderer<ReportComponent>)renderer);
            }
        });
        this.webReportClient = webReportClient;
    }

    @Override
    public ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception {
        TenantId tenantId = task.getTenantId();
        log.trace("[{}] Executing generateReport, reportRequest [{}]", (Object)tenantId, (Object)task);
        PdfReportTemplateConfig configuration = (PdfReportTemplateConfig)task.getReportTemplateConfig();
        Dimension pageSize = this.computePageSize(configuration);
        Insets pageMargins = this.computePageMargins(configuration);
        int usablePageWidthPx = (int)((float)(pageSize.width - pageMargins.getLeft() - pageMargins.getRight()) * 4.0f / 3.0f);
        ITextRenderer renderer = HtmlRenderUtils.createRenderer(this.dataService, ctx, usablePageWidthPx);
        EntityData stateEntity = task.getOriginator() != null ? DataSourceUtils.entityDataFromEntityId((EntityId)task.getOriginator()) : null;
        HeaderFooterRenderLayout headerLayout = this.renderHeaderFooter(renderer, ctx, configuration.getHeader(), usablePageWidthPx, stateEntity);
        HeaderFooterRenderLayout footerLayout = this.renderHeaderFooter(renderer, ctx, configuration.getFooter(), usablePageWidthPx, stateEntity);
        HashMap<String, Object> reportVariables = new HashMap<String, Object>();
        this.fillPageLayoutVariables(reportVariables, configuration, headerLayout, footerLayout, pageSize, pageMargins);
        reportVariables.put("pageContent", this.renderContent(usablePageWidthPx, ctx, configuration.getComponents(), stateEntity));
        String renderedHtmlContent = ThymeleafUtil.renderFromHtmlTemplate("html/report-template", reportVariables);
        Document doc = HtmlRenderUtils.parseDom(renderedHtmlContent);
        renderer.setDocument(doc);
        renderer.layout();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();){
            renderer.createPDF((OutputStream)outputStream);
            byte[] reportBytes = outputStream.toByteArray();
            String reportName = ReportUtils.prepareReportName(configuration.getNamePattern(), new Date(), task.getTimezone(), TbReportFormat.PDF.getExtension());
            ReportData reportData = ReportData.builder().data(reportBytes).contentType(configuration.getFormat().getContentType()).name(reportName).build();
            return reportData;
        }
    }

    private HeaderFooterRenderLayout renderHeaderFooter(ITextRenderer renderer, TbReportCtx ctx, HeaderFooter headerFooter, int usablePageWidthPx, EntityData stateEntity) throws Exception {
        int heightPx;
        String htmlContent;
        if (headerFooter == null) {
            return new HeaderFooterRenderLayout();
        }
        HeaderFooterRenderLayout headerFooterRenderLayout = new HeaderFooterRenderLayout();
        headerFooterRenderLayout.setEnabled(headerFooter.isEnabled());
        if (headerFooter.isEnabled()) {
            htmlContent = this.renderContent(usablePageWidthPx, ctx, headerFooter.getComponents(), stateEntity);
            headerFooterRenderLayout.setHtmlContent(htmlContent);
            heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setHeightPx(heightPx);
        }
        headerFooterRenderLayout.setFirstPageEnabled(headerFooter.getFirstPage() != null && headerFooter.getFirstPage().isEnabled());
        if (headerFooterRenderLayout.isFirstPageEnabled()) {
            htmlContent = this.renderContent(usablePageWidthPx, ctx, headerFooter.getFirstPage().getComponents(), stateEntity);
            headerFooterRenderLayout.setFirstPageHtmlContent(htmlContent);
            heightPx = HtmlRenderUtils.measureHtmlHeight(renderer, htmlContent, usablePageWidthPx);
            headerFooterRenderLayout.setFirstPageHeightPx(heightPx);
        }
        return headerFooterRenderLayout;
    }

    private String renderContent(int usablePageWidthPx, TbReportCtx ctx, List<ReportComponent> components, EntityData stateEntity) {
        StringBuilder content = new StringBuilder();
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        for (ReportComponent component : components) {
            ReportUtils.prepareReportComponent(component);
            ReportComponentType type = component.getType();
            if (type == ReportComponentType.SUB_REPORT) {
                content.append(this.renderSubReport(usablePageWidthPx, ctx, (DataReportComponent)((SubReportComponent)component), stateEntityId));
                continue;
            }
            if (type == ReportComponentType.DASHBOARD) {
                content.append(this.renderDashboard(usablePageWidthPx, ctx, stateEntityId, (DataReportComponent)component));
                continue;
            }
            if (type == ReportComponentType.TIME_SERIES_TABLE) {
                content.append(this.renderTimeseriesTables(usablePageWidthPx, ctx, stateEntityId, (DataReportComponent)((TimeseriesTableComponent)component)));
                continue;
            }
            content.append(this.renderComponent(usablePageWidthPx, ctx, component, stateEntity));
        }
        return content.toString();
    }

    private String renderComponent(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = this.getComponentData(usablePageWidthPx, ctx, component, stateEntity);
            return this.componentsRenderers.get(component.getType()).render(component, componentData);
        }
        catch (Exception e) {
            log.error("[{}] Failed to render component of type [{}]", new Object[]{ctx.getTenantId(), component.getType(), e});
            String componentSubType = component.getSubType() != null ? " [" + String.valueOf(component.getSubType()) + "]" : "";
            return this.renderError(usablePageWidthPx, "Failed to render component of type: " + String.valueOf(component.getType()) + componentSubType, e);
        }
    }

    private ComponentData getComponentData(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        ComponentData componentData = switch (component.getType()) {
            case TIME_SERIES_TABLE -> this.buildTsComponentData(usablePageWidthPx, ctx, (TimeseriesTableComponent)component, stateEntity);
            case TIME_SERIES_CHART -> this.buildTsChartComponentData(usablePageWidthPx, ctx, (TimeseriesChartComponent)component, stateEntity);
            case LATEST_CHART -> this.buildLatestChartComponentData(usablePageWidthPx, ctx, (LatestChartComponent)component, stateEntity);
            case ALARM_TABLE -> this.buildAlarmComponentData(usablePageWidthPx, ctx, (AlarmTableComponent)component, stateEntity);
            case DASHBOARD -> this.buildDashboardComponentData(usablePageWidthPx, ctx, (DashboardComponent)component, stateEntity);
            case SPLIT_VIEW -> this.buildSplitViewComponentData(usablePageWidthPx, ctx, (SplitViewComponent)component, stateEntity);
            case IMAGE -> this.buildImageComponentData(usablePageWidthPx, ctx, (ImageComponent)component);
            default -> this.buildMultipleDataSourceData(usablePageWidthPx, ctx, component, stateEntity);
        };
        this.populateReportVars(componentData, ctx);
        return componentData;
    }

    private String renderTimeseriesTables(int usablePageWidthPx, TbReportCtx ctx, EntityId stateEntityId, DataReportComponent component) {
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return this.renderError(usablePageWidthPx, "Data source is not configured for time series table");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return this.renderError(usablePageWidthPx, "At least one time series column should be specified for time series table");
        }
        DataSource latestDataSource = DataSource.builder().type(ds.getType()).deviceId(ds.getDeviceId()).entityAliasId(ds.getEntityAliasId()).filterId(ds.getFilterId()).dataKeys(ds.getLatestDataKeys()).build();
        List<EntityData> entityDatas = this.fetchEntities(ctx, latestDataSource, stateEntityId);
        for (EntityData entity : entityDatas) {
            content.append(this.renderComponent(usablePageWidthPx, ctx, (ReportComponent)component, entity));
        }
        return content.toString();
    }

    private String renderDashboard(int usablePageWidthPx, TbReportCtx ctx, EntityId stateEntityId, DataReportComponent component) {
        List<EntityData> entityDatas;
        StringBuilder content = new StringBuilder();
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            entityDatas = new ArrayList();
            entityDatas.add(null);
        } else {
            DataSource dashboardDataSource = dataSource.get();
            entityDatas = this.fetchEntities(ctx, dashboardDataSource, stateEntityId);
        }
        for (EntityData entity : entityDatas) {
            content.append(this.renderComponent(usablePageWidthPx, ctx, (ReportComponent)component, entity));
        }
        return content.toString();
    }

    private String renderSubReport(int usablePageWidthPx, TbReportCtx ctx, DataReportComponent component, EntityId stateEntityId) {
        SubReportComponent subReportComponent = (SubReportComponent)component;
        ReportTemplateId templateId = subReportComponent.getTemplateId();
        if (templateId == null) {
            return this.renderError(usablePageWidthPx, "Report template id is not configured for SubReport");
        }
        StringBuilder content = new StringBuilder();
        try {
            ReportTemplate reportTemplate = this.dataService.findReportTemplate(templateId, ctx);
            if (reportTemplate == null) {
                return this.renderError(usablePageWidthPx, "Template with id " + String.valueOf(templateId) + " not found. Please check the configuration.");
            }
            PdfReportTemplateConfig reportConfiguration = (PdfReportTemplateConfig) reportTemplate.getConfiguration();
            TbReportCtx subReportCtx = ctx.createSubReportCxt((ReportTemplateConfig)reportConfiguration);
            List<EntityData> entities = this.getSubReportEntities(ctx, component, stateEntityId);
            for (EntityData entity : entities) {
                if (subReportComponent.isAvoidPageBreakInside()) {
                    content.append("<div class=\"no-page-break\">");
                }
                content.append(this.renderContent(usablePageWidthPx, subReportCtx, reportConfiguration.getComponents(), entity));
                if (!subReportComponent.isAvoidPageBreakInside()) continue;
                content.append("</div>");
            }
            return content.toString();
        }
        catch (Exception e) {
            log.error("[{}] Failed to render Subreport, template id: {}", new Object[]{ctx.getTenantId(), templateId, e});
            return this.renderError(usablePageWidthPx, "Failed to render sub-report " + String.valueOf(templateId), e);
        }
    }

    private String renderError(int usablePageWidthPx, String errorMessage) {
        return this.renderError(usablePageWidthPx, errorMessage, null);
    }

    private String renderError(int usablePageWidthPx, String errorMessage, Exception e) {
        if (e instanceof InterruptedException || ExceptionUtils.getRootCause((Throwable)e) instanceof InterruptedException) {
            throw new RuntimeException(e);
        }
        return this.componentsRenderers.get(ReportComponentType.ERROR).render((ReportComponent)new ErrorComponent(errorMessage, e), new ComponentData(usablePageWidthPx));
    }

    private ComponentData buildLatestChartComponentData(int usablePageWidthPx, TbReportCtx ctx, LatestChartComponent component, EntityData stateEntity) {
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
        if (dataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx, "Data source is not configured for the chart [" + String.valueOf(component.getSubType()) + "]");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return new ComponentData(usablePageWidthPx, "At least one series should be specified for the chart [" + String.valueOf(component.getSubType()) + "]");
        }
        List<EntityData> entityDatas = this.fetchEntities(ctx, ds, stateEntity != null ? stateEntity.getEntityId() : null, ReportQueryUtils.DEFAULT_TS_CHART_SORT_ORDER);
        ArrayList<LatestChartDataSource> chartData = new ArrayList<LatestChartDataSource>();
        int dataIndex = 0;
        DataPostProcessFunction dataPostProcessFunction = (dataKey, timestamp, value) -> this.postProcess(ctx, dataKey, timestamp, value, true);
        for (EntityData entityData : entityDatas) {
            LatestChartDataSource chartDataSource = new LatestChartDataSource(ds, entityData, dataPostProcessFunction, dataIndex);
            chartData.add(chartDataSource);
            ++dataIndex;
        }
        int keyIndex = 0;
        for (LatestChartDataSource chartDataSource : chartData) {
            for (DataKey dataKey2 : chartDataSource.getDataKeys()) {
                if (chartDataSource.isGenerated()) {
                    dataKey2.setColor(ColorUtils.getMaterialColor(keyIndex));
                }
                ++keyIndex;
            }
        }
        LatestChartData latestChartData = new LatestChartData(chartData);
        HashMap<String, String> variables = new HashMap<String, String>();
        if (stateEntity != null) {
            this.putEntityInfoData(stateEntity, variables);
        } else if (!entityDatas.isEmpty()) {
            this.putEntityInfoData(entityDatas.get(0), variables);
        }
        return new ComponentData(usablePageWidthPx, latestChartData, new HashMap<String, Object>(variables));
    }

    /*
     * WARNING - void declaration
     */
    private ComponentData buildTsChartComponentData(int usablePageWidthPx, TbReportCtx ctx, TimeseriesChartComponent component, EntityData stateEntity) {
        TimeIntervalCalculator.TimeRange var30_36 = null;
        ReportRangeChartSettings rangeChartSettings;
        List colorRanges;
        Optional<DataSource> dataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
        if (dataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx, "Data source is not configured for time series chart [" + String.valueOf(component.getSubType()) + "]");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return new ComponentData(usablePageWidthPx, "At least one series should be specified for time series chart [" + String.valueOf(component.getSubType()) + "]");
        }
        List<TimeSeriesChartThreshold> thresholds = null;
        boolean comparisonEnabled = false;
        if (component.getTimeSeriesChartSettings() != null) {
            thresholds = component.getTimeSeriesChartSettings().getThresholds();
            boolean bl = comparisonEnabled = component.getTimeSeriesChartSettings().getComparisonEnabled() != null ? component.getTimeSeriesChartSettings().getComparisonEnabled() : false;
        }
        if (thresholds == null) {
            thresholds = new ArrayList();
        }
        thresholds = thresholds.stream().filter(ValueSourceConfig::isValidSource).toList();
        ArrayList<TsChartThresholdItem> thresholdItems = new ArrayList<TsChartThresholdItem>(thresholds.stream().filter(t -> ValueSourceType.constant.equals((Object)t.getType())).map(t -> new TsChartThresholdItem((TimeSeriesChartThreshold)t, t.getValue())).toList());
        DataSource latestDataSource = DataSource.builder().type(ds.getType()).deviceId(ds.getDeviceId()).entityAliasId(ds.getEntityAliasId()).filterId(ds.getFilterId()).dataKeys(ds.getLatestDataKeys()).build();
        boolean singleEntity = ReportComponentSubType.RANGE_CHART == component.getSubType();
        List<EntityData> entityDatas = this.fetchEntities(ctx, latestDataSource, stateEntity != null ? stateEntity.getEntityId() : null, ReportQueryUtils.DEFAULT_TS_CHART_SORT_ORDER, singleEntity);
        List<TimeSeriesChartThreshold> latestKeyThresholds = thresholds.stream().filter(t -> ValueSourceType.latestKey.equals((Object)t.getType())).toList();
        List<TsChartThresholdItem> latestThresholdItems = ReportUtils.collectThresholdItems(latestKeyThresholds, entityDatas, true);
        thresholdItems.addAll(latestThresholdItems);
        Map<String, List<TimeSeriesChartThreshold>> thresholdsByAlias = thresholds.stream().filter(t -> ValueSourceType.entity.equals((Object)t.getType())).collect(Collectors.groupingBy(ValueSourceConfig::getEntityAlias));
        thresholdsByAlias.forEach((alias, entityThresholds) -> {
            Optional<String> aliasId = ReportQueryUtils.resolveAliasId(ctx, alias);
            if (aliasId.isPresent()) {
                List<DataKey> dataKeys = entityThresholds.stream().map(ValueSourceConfig::toEntityDataKey).distinct().toList();
                DataSource thresholdDataSource = DataSource.builder().type(DataSourceType.ENTITY).entityAliasId(aliasId.get()).dataKeys(dataKeys).build();
                List<EntityData> foundEntities = this.fetchEntities(ctx, thresholdDataSource, stateEntity != null ? stateEntity.getEntityId() : null, ReportQueryUtils.DEFAULT_TS_CHART_SORT_ORDER);
                List<TsChartThresholdItem> entityThresholdItems = ReportUtils.collectThresholdItems(entityThresholds, foundEntities, false);
                thresholdItems.addAll(entityThresholdItems);
            }
        });
        List<TsChartRangeItem> rangeItems = new ArrayList<TsChartRangeItem>();
        if (ReportComponentSubType.RANGE_CHART == component.getSubType() && component.getTimeSeriesChartSettings() != null && (colorRanges = (rangeChartSettings = (ReportRangeChartSettings)component.getTimeSeriesChartSettings()).getRangeColors()) != null) {
            int decimals = rangeChartSettings.getRangeDecimals() != null ? rangeChartSettings.getRangeDecimals() : 2;
            String units = rangeChartSettings.getRangeUnits() != null ? rangeChartSettings.getRangeUnits() : "";
            NumberFormat valueFormat = ChartUtils.createValueFormatter(decimals, "");
            rangeItems = TsChartRangeItem.toRangeItems(colorRanges, valueFormat);
            if (rangeChartSettings.getShowRangeThresholds() == null || rangeChartSettings.getShowRangeThresholds().booleanValue()) {
                TimeSeriesChartThreshold rangeThreshold = new TimeSeriesChartThreshold(rangeChartSettings.getRangeThreshold());
                rangeThreshold.setType(ValueSourceType.constant);
                rangeThreshold.setYAxisId("default");
                rangeThreshold.setDecimals(Integer.valueOf(rangeThreshold.getDecimals() != null ? rangeThreshold.getDecimals() : decimals));
                rangeThreshold.setUnits(rangeThreshold.getUnits() != null ? rangeThreshold.getUnits() : units);
                thresholdItems.addAll(TsChartRangeItem.toMarkPoints(rangeItems).stream().map(item -> new TsChartThresholdItem(rangeThreshold, (Double)item)).toList());
            }
        }
        ArrayList<TsChartDataSource> chartData = new ArrayList<TsChartDataSource>();
        List<DataKey> dataKeys = ds.getDataKeys();
        List<String> keys = dataKeys.stream().map(DataKey::getName).distinct().toList();
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        String targetTimezone = StringUtils.isNotBlank((String)timeWindowConf.getTimezone()) ? timeWindowConf.getTimezone() : ctx.getTimeZone();
        TimeIntervalCalculator.TimeRange timeRange = TimeIntervalCalculator.getTimeRange((TimeWindowConfiguration)timeWindowConf, (String)targetTimezone);
        ZoneId zoneId = targetTimezone != null ? ZoneId.of(targetTimezone) : ZoneId.systemDefault();
        int dataIndex = 0;
        int startDataIndex = 0;
        boolean stateData = ReportComponentSubType.STATE_CHART == component.getSubType();
        DataPostProcessFunction dataPostProcessFunction = (dataKey, timestamp, value) -> this.postProcess(ctx, dataKey, timestamp, value, true);
        for (EntityData entityData : entityDatas) {
            Aggregation aggregation = timeWindowConf.getAggregation().getType();
            if (stateData) {
                aggregation = Aggregation.NONE;
            }
            List<TsKvEntry> tsKvEntries = this.dataService.getTimeseries(entityData.getEntityId(), keys, timeRange.startTs, timeRange.endTs, historyConf.getInterval(), targetTimezone, aggregation, SortOrder.Direction.ASC, timeWindowConf.getAggregation().getLimit(), false, ctx);
            if (stateData) {
                List<TsKvEntry> prevTsKvEntries = this.dataService.getTimeseries(entityData.getEntityId(), keys, timeRange.startTs - TimeUnit.DAYS.toMillis(365L), timeRange.startTs, historyConf.getInterval(), targetTimezone, aggregation, SortOrder.Direction.DESC, 1, false, ctx);
                if (!prevTsKvEntries.isEmpty()) {
                    TsKvEntry prev = prevTsKvEntries.get(0);
                    if (tsKvEntries.isEmpty() || ((TsKvEntry)tsKvEntries.get(0)).getTs() > timeRange.startTs) {
                        BasicTsKvEntry startEntry = new BasicTsKvEntry(timeRange.startTs, (KvEntry)prev);
                        tsKvEntries.add(0, (TsKvEntry)startEntry);
                    }
                }
                if (!tsKvEntries.isEmpty() && ((TsKvEntry)tsKvEntries.get(tsKvEntries.size() - 1)).getTs() < timeRange.endTs) {
                    TsKvEntry last = (TsKvEntry)tsKvEntries.get(tsKvEntries.size() - 1);
                    BasicTsKvEntry endEntry = new BasicTsKvEntry(timeRange.endTs, (KvEntry)last);
                    tsKvEntries.add((TsKvEntry)endEntry);
                }
            }
            TsChartDataSource chartDataSource = new TsChartDataSource(ds, entityData, tsKvEntries, timeRange, historyConf.getInterval(), aggregation, zoneId, false, null, dataPostProcessFunction, dataIndex, startDataIndex);
            chartData.add(chartDataSource);
            ++dataIndex;
            startDataIndex += chartDataSource.getDataKeys().size();
        }
        int keyIndex = 0;
        for (TsChartDataSource chartDataSource : chartData) {
            for (DataKey dataKey2 : chartDataSource.getDataKeys()) {
                if (chartDataSource.isGenerated()) {
                    dataKey2.setColor(ColorUtils.getMaterialColor(keyIndex));
                }
                ++keyIndex;
            }
        }
        Object var30_34 = null;
        if (comparisonEnabled) {
            ComparisonDuration timeForComparison = ComparisonDuration.previousInterval;
            Long comparisonCustomIntervalValue = 0x6DDD00L;
            if (component.getTimeSeriesChartSettings() != null) {
                if (component.getTimeSeriesChartSettings().getTimeForComparison() != null) {
                    timeForComparison = component.getTimeSeriesChartSettings().getTimeForComparison();
                }
                if (component.getTimeSeriesChartSettings().getComparisonCustomIntervalValue() != null) {
                    comparisonCustomIntervalValue = component.getTimeSeriesChartSettings().getComparisonCustomIntervalValue();
                }
            }
            TimeIntervalCalculator.TimeRange timeRange2 = TimeIntervalCalculator.getComparisonTimeRange((TimeIntervalCalculator.TimeRange)timeRange, (TimeWindowConfiguration)timeWindowConf, (String)targetTimezone, (ComparisonDuration)timeForComparison, (Long)comparisonCustomIntervalValue);
            var30_36 = timeRange2;
            List<DataKey> comparisionDataKeys = dataKeys.stream().filter(DataKey::isComparisonKey).toList();
            if (!comparisionDataKeys.isEmpty()) {
                List<String> comparisonKeys = comparisionDataKeys.stream().map(DataKey::getName).distinct().toList();
                ArrayList<TsChartDataSource> comparisonChartData = new ArrayList<TsChartDataSource>();
                for (EntityData entity : entityDatas) {
                    List<TsKvEntry> tsKvEntries = this.dataService.getTimeseries(entity.getEntityId(), comparisonKeys, timeRange2.startTs, timeRange2.endTs, historyConf.getInterval(), targetTimezone, timeWindowConf.getAggregation().getType(), SortOrder.Direction.ASC, timeWindowConf.getAggregation().getLimit(), false, ctx);
                    TsChartDataSource chartDataSource = new TsChartDataSource(ds, entity, tsKvEntries, timeRange2, historyConf.getInterval(), timeWindowConf.getAggregation().getType(), zoneId, true, timeForComparison, dataPostProcessFunction, dataIndex, startDataIndex);
                    comparisonChartData.add(chartDataSource);
                    ++dataIndex;
                    startDataIndex += chartDataSource.getDataKeys().size();
                }
                for (TsChartDataSource chartDataSource : comparisonChartData) {
                    for (DataKey dataKey3 : chartDataSource.getDataKeys()) {
                        String color = ColorUtils.getMaterialColor(keyIndex);
                        TimeSeriesChartKeySettings timeSeriesChartKeySettings = (TimeSeriesChartKeySettings)dataKey3.getSettings();
                        DataKeyComparisonSettings comparisonSettings = timeSeriesChartKeySettings.getComparisonSettings();
                        if (StringUtils.isNotBlank((String)comparisonSettings.getColor())) {
                            color = comparisonSettings.getColor();
                        }
                        dataKey3.setColor(color);
                        ++keyIndex;
                    }
                }
                chartData.addAll(comparisonChartData);
            }
        }
        TimeZone timeZone = TimeZone.getTimeZone(zoneId.getId());
        TsChartData tsChartData = new TsChartData(timeZone, timeRange, Aggregation.NONE.equals((Object)timeWindowConf.getAggregation().getType()), chartData, thresholdItems, rangeItems, comparisonEnabled, (TimeIntervalCalculator.TimeRange)var30_36);
        HashMap<String, String> variables = new HashMap<String, String>();
        if (stateEntity != null) {
            this.putEntityInfoData(stateEntity, variables);
        } else if (!entityDatas.isEmpty()) {
            this.putEntityInfoData(entityDatas.get(0), variables);
        }
        return new ComponentData(usablePageWidthPx, tsChartData, new HashMap<String, Object>(variables));
    }

    private ComponentData buildImageComponentData(int usablePageWidthPx, TbReportCtx ctx, ImageComponent component) {
        if (ImageSourceType.ENTITY_KEY == component.getSourceType()) {
            Optional<DataSource> dataSource = ReportUtils.getSingleDataSource((DataReportComponent)component);
            if (dataSource.isEmpty()) {
                return new ComponentData(usablePageWidthPx);
            }
            return this.buildSingleComponentData(usablePageWidthPx, ctx, dataSource.get(), null);
        }
        return new ComponentData(usablePageWidthPx);
    }

    private ComponentData buildSingleComponentData(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        return switch (dataSource.getType()) {
            case DEVICE, ENTITY -> this.buildEntityDataSource(usablePageWidthPx, ctx, dataSource, stateEntityId);
            case ENTITY_COUNT -> this.buildEntityCountDataSource(usablePageWidthPx, ctx, dataSource, stateEntityId);
            case ALARM_COUNT -> this.buildAlarmCountDataSource(usablePageWidthPx, ctx, dataSource, stateEntityId);
            default -> throw new IllegalArgumentException("Unknown data source type: " + String.valueOf(dataSource.getType()));
        };
    }

    private ComponentData buildEntityDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        List<Map<String, String>> entityDatas = this.collectEntityDatas(ctx, dataSource, stateEntityId);
        HashMap<String, Object> variables = new HashMap<String, Object>();
        if (stateEntityId == null && !entityDatas.isEmpty()) {
            variables.putAll(entityDatas.get(0));
        }
        return new ComponentData(usablePageWidthPx, dataSource, entityDatas, variables);
    }

    private ComponentData buildEntityCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String label = this.resolveSingleLabel(dataSource, "count");
        map.put(label, this.dataService.countEntitiesByQuery(ReportQueryUtils.toEntityCountQuery(dataSource, ctx, stateEntityId), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private ComponentData buildAlarmCountDataSource(int usablePageWidthPx, TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String label = this.resolveSingleLabel(dataSource, "count");
        map.put(label, this.dataService.countAlarmsByQuery(ReportQueryUtils.toAlarmCountQuery(dataSource, ctx, stateEntityId), ctx));
        return new ComponentData(usablePageWidthPx, map);
    }

    private String resolveSingleLabel(DataSource dataSource, String fallback) {
        String label = null;
        if (dataSource.getDataKeys() != null && !dataSource.getDataKeys().isEmpty()) {
            label = ((DataKey)dataSource.getDataKeys().get(0)).getLabel();
        }
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        return fallback;
    }

    private ComponentData buildDashboardComponentData(int usablePageWidthPx, TbReportCtx ctx, DashboardComponent component, EntityData stateEntity) {
        if (component.getConfig() == null) {
            return new ComponentData(usablePageWidthPx, "Dashboard report config is empty");
        }
        if (StringUtils.isBlank((String)component.getConfig().getBaseUrl())) {
            return new ComponentData(usablePageWidthPx, "Base URL is not configured for dashboard report");
        }
        if (StringUtils.isBlank((String)component.getConfig().getDashboardId())) {
            return new ComponentData(usablePageWidthPx, "Dashboard id is not configured for dashboard report");
        }
        SettableFuture futureToSet = SettableFuture.create();
        DashboardReportConfig config = component.getConfig();
        config.setType("png");
        if (stateEntity != null) {
            config.setState(ReportUtils.updateDashboardReportStateParamsWithEntity(config.getState(), stateEntity));
        }
        this.webReportClient.requestDashboardReport(config, null, ctx.getAccessToken(), ctx.getAccessTokenExpTs(), futureToSet::set, error -> {
            log.error("[{}] Failed to generate dashboard report", (Object)ctx.getTenantId(), error);
            futureToSet.setException(error);
        });
        try {
            return new ComponentData(usablePageWidthPx, ((DashboardReportData)futureToSet.get()).getData());
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private ComponentData buildSplitViewComponentData(int usablePageWidthPx, TbReportCtx ctx, SplitViewComponent component, EntityData stateEntity) {
        float splitPosition = component.getSplitPosition().floatValue();
        float splitGap = (float)component.getSplitGap().intValue() * 4.0f / 3.0f;
        int leftWidth = (int)((float)usablePageWidthPx * splitPosition / 100.0f - splitGap / 2.0f);
        int rightWidth = (int)((float)usablePageWidthPx * (100.0f - splitPosition) / 100.0f - splitGap / 2.0f);
        int centerWidth = (int)splitGap;
        String leftContent = "";
        if (component.getLeftView() != null) {
            leftContent = this.renderContent(leftWidth, ctx, Collections.singletonList(component.getLeftView()), stateEntity);
        }
        String rightContent = "";
        if (component.getRightView() != null) {
            rightContent = this.renderContent(rightWidth, ctx, Collections.singletonList(component.getRightView()), stateEntity);
        }
        HashMap<String, Object> variables = new HashMap<String, Object>();
        variables.put("leftWidth", leftWidth);
        variables.put("rightWidth", rightWidth);
        variables.put("centerWidth", centerWidth);
        variables.put("leftContent", leftContent);
        variables.put("rightContent", rightContent);
        return new ComponentData(usablePageWidthPx, variables);
    }

    private ComponentData buildMultipleDataSourceData(int usablePageWidthPx, TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        List<DataSource> dataSources = null;
        if (component instanceof DataReportComponent) {
            dataSources = ReportUtils.getMultipleDataSources((DataReportComponent)component);
        }
        if (dataSources == null || dataSources.isEmpty()) {
            return new ComponentData(usablePageWidthPx);
        }
        HashMap<String, String> variables = new HashMap<String, String>();
        if (stateEntity != null) {
            this.putEntityInfoData(stateEntity, variables);
        }
        ComponentData mainDataSource = new ComponentData(usablePageWidthPx, null, new ArrayList<Map<String, String>>(), new HashMap<String, Object>(variables));
        for (DataSource dataSource : dataSources) {
            ComponentData singleDataSource = this.buildSingleComponentData(usablePageWidthPx, ctx, dataSource, stateEntity != null ? stateEntity.getEntityId() : null);
            mainDataSource.merge(singleDataSource);
        }
        return mainDataSource;
    }

    private Dimension computePageSize(PdfReportTemplateConfig config) {
        PageSize pageSize = Optional.ofNullable(config.getPageSize()).orElse(PageSize.A4);
        return config.getPageOrientation() == PageOrientation.LANDSCAPE ? new Dimension(pageSize.getHeight(), pageSize.getWidth()) : new Dimension(pageSize.getWidth(), pageSize.getHeight());
    }

    private Insets computePageMargins(PdfReportTemplateConfig configuration) {
        if (configuration.getPageMargins() != null) {
            return configuration.getPageMargins();
        }
        return new Insets(20, 20, 20, 20);
    }

    private void fillPageLayoutVariables(Map<String, Object> reportVariables, PdfReportTemplateConfig configuration, HeaderFooterRenderLayout headerLayout, HeaderFooterRenderLayout footerLayout, Dimension pageSize, Insets pageMargins) {
        reportVariables.put("pageWidth", pageSize.getWidth() + "pt");
        reportVariables.put("pageHeight", pageSize.getHeight() + "pt");
        reportVariables.put("pageMarginLeft", pageMargins.getLeft() + "pt");
        reportVariables.put("pageMarginRight", pageMargins.getRight() + "pt");
        String pageBackground = configuration.getPageBackground() != null ? ColorUtils.normalizeCssColor(configuration.getPageBackground()) : "#fff";
        reportVariables.put("pageBackground", pageBackground);
        int minContentHeight = 100;
        int minHalfPageContentHeight = Math.max((pageSize.height - pageMargins.getTop() - pageMargins.getBottom() - minContentHeight) / 2, 0);
        int maxTopMargin = pageMargins.getTop() + minHalfPageContentHeight;
        int maxBottomMargin = pageMargins.getBottom() + minHalfPageContentHeight;
        int pageMarginTop = this.fillHeaderFooterVariables(reportVariables, headerLayout, pageMargins, maxTopMargin, true);
        reportVariables.put("pageMarginTop", pageMarginTop + "pt");
        int pageMarginBottom = this.fillHeaderFooterVariables(reportVariables, footerLayout, pageMargins, maxBottomMargin, false);
        reportVariables.put("pageMarginBottom", pageMarginBottom + "pt");
    }

    private int fillHeaderFooterVariables(Map<String, Object> reportVariables, HeaderFooterRenderLayout headerFooterLayout, Insets pageMargins, int maxMargin, boolean headerElseFooter) {
        int startMargin;
        String prefix = headerElseFooter ? "Header" : "Footer";
        String marginPrefix = headerElseFooter ? "Top" : "Bottom";
        reportVariables.put("enable" + prefix, headerFooterLayout.isEnabled());
        int margin = startMargin = headerElseFooter ? pageMargins.getTop() : pageMargins.getBottom();
        reportVariables.put("page" + prefix + "Padding", startMargin + "pt");
        if (headerFooterLayout.isEnabled()) {
            reportVariables.put("page" + prefix, headerFooterLayout.getHtmlContent());
            margin = Math.min((int)((float)startMargin + (float)headerFooterLayout.getHeightPx() * 3.0f / 4.0f), maxMargin);
            int height = margin - startMargin;
            reportVariables.put("page" + prefix + "Height", height + "pt");
        }
        reportVariables.put("enableFirstPage" + prefix, headerFooterLayout.isFirstPageEnabled());
        if (headerFooterLayout.isFirstPageEnabled()) {
            int firstPageMargin = Math.min((int)((float)startMargin + (float)headerFooterLayout.getFirstPageHeightPx() * 3.0f / 4.0f), maxMargin);
            reportVariables.put("firstPageMargin" + marginPrefix, firstPageMargin + "pt");
            reportVariables.put("firstPage" + prefix, headerFooterLayout.getFirstPageHtmlContent());
            int height = firstPageMargin - startMargin;
            reportVariables.put("firstPage" + prefix + "Height", height + "pt");
        }
        return margin;
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.PDF;
    }
}

