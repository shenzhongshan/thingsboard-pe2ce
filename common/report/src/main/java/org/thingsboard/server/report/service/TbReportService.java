/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.annotation.PostConstruct
 *  jakarta.annotation.PreDestroy
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Lazy
 *  org.springframework.stereotype.Service
 *  org.thingsboard.common.util.ThingsBoardThreadFactory
 *  org.thingsboard.server.common.data.job.task.ReportTask
 *  org.thingsboard.server.common.data.report.Report
 *  org.thingsboard.server.common.data.report.ReportData
 *  org.thingsboard.server.common.data.report.TbReportFormat
 *  org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig
 */
package org.thingsboard.server.report.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.TbReportCtxProvider;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.report.service.ReportService;

@Service
public class TbReportService {
    private final Map<TbReportFormat, ReportService> reportServices = new EnumMap<TbReportFormat, ReportService>(TbReportFormat.class);
    private final TbReportCtxProvider contextProvider;
    private final ReportDataService dataService;
    private ExecutorService executor;
    @Value(value="${reports.test_report_pool_size:12}")
    private int testReportThreads;

    @PostConstruct
    private void init() {
        this.executor = Executors.newFixedThreadPool(this.testReportThreads, (ThreadFactory)ThingsBoardThreadFactory.forName((String)this.getClass().getSimpleName()));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

    private TbReportService(List<ReportService> reportServices, @Lazy TbReportCtxProvider contextProvider, @Lazy ReportDataService dataService) {
        reportServices.forEach(service -> {
            TbReportFormat format = service.getFormat();
            if (format != null) {
                this.reportServices.put(format, (ReportService)service);
            }
        });
        this.contextProvider = contextProvider;
        this.dataService = dataService;
    }

    public Future<ReportData> generateTestReport(ReportTask task) {
        return this.executor.submit(() -> {
            try (TbReportCtx ctx = this.contextProvider.newContext(task);){
                ReportData reportData = this.generateReport(task, ctx);
                return reportData;
            }
        });
    }

    public Report generateReport(ReportTask task) throws Exception {
        try (TbReportCtx ctx = this.contextProvider.newContext(task);){
            ReportData reportData = this.generateReport(task, ctx);
            Report report = new Report();
            report.setTenantId(task.getTenantId());
            report.setCustomerId(task.getCustomerId());
            report.setTemplateId(task.getReportTemplateId());
            report.setFormat(task.getReportTemplateConfig().getFormat());
            report.setName(reportData.getName());
            report.setUserId(task.getUserId());
            Report report2 = this.dataService.createReport(report, reportData.getData(), ctx);
            return report2;
        }
    }

    private ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception {
        ReportTemplateConfig configuration = task.getReportTemplateConfig();
        return this.reportServices.get(configuration.getFormat()).generateReport(task, ctx);
    }
}

