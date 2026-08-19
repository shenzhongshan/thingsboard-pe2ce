/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.job.task.ReportTask
 */
package org.thingsboard.server.report.context;

import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.report.context.TbReportCtx;

public interface TbReportCtxProvider {
    public TbReportCtx newContext(ReportTask var1);
}

