/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.job;

import lombok.Generated;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.ReportTaskResult;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.report.Report;

public class ReportJobResult
extends JobResult {
    private Report report;

    @Override
    public void processTaskResult(TaskResult taskResult) {
        super.processTaskResult(taskResult);
        ReportTaskResult reportTaskResult = (ReportTaskResult)taskResult;
        if (reportTaskResult.getReport() != null) {
            this.report = reportTaskResult.getReport();
        }
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

    @Generated
    public ReportJobResult() {
    }

    @Generated
    public Report getReport() {
        return this.report;
    }

    @Generated
    public void setReport(Report report) {
        this.report = report;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportJobResult(report=" + String.valueOf(this.getReport()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportJobResult)) {
            return false;
        }
        ReportJobResult other = (ReportJobResult)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Report this$report = this.getReport();
        Report other$report = other.getReport();
        return !(this$report == null ? other$report != null : !((Object)this$report).equals(other$report));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportJobResult;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Report $report = this.getReport();
        result = result * 59 + ($report == null ? 43 : ((Object)$report).hashCode());
        return result;
    }
}

