/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.job.task.ReportTask
 *  org.thingsboard.server.common.data.report.ReportData
 *  org.thingsboard.server.common.data.report.TbReportFormat
 */
package org.thingsboard.server.report.service;

import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.report.context.TbReportCtx;

public interface ReportService {
    public ReportData generateReport(ReportTask var1, TbReportCtx var2) throws Exception;

    public TbReportFormat getFormat();
}

