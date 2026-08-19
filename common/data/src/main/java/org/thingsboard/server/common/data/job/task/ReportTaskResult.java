/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.job.task;

import lombok.Generated;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.report.Report;

public class ReportTaskResult
extends TaskResult {
    private Report report;
    private String error;

    private ReportTaskResult(boolean success, boolean discarded, Report report, String error) {
        super(success, discarded);
        this.report = report;
        this.error = error;
    }

    public static ReportTaskResult success(ReportTask task, Report report) {
        return ReportTaskResult.builder().success(true).report(report).build();
    }

    public static ReportTaskResult failed(ReportTask task, Throwable error) {
        return ReportTaskResult.builder().error(error.getMessage()).build();
    }

    public static ReportTaskResult discarded(ReportTask task) {
        return ReportTaskResult.builder().discarded(true).build();
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

    @Generated
    public static ReportTaskResultBuilder builder() {
        return new ReportTaskResultBuilder();
    }

    @Generated
    public Report getReport() {
        return this.report;
    }

    @Generated
    public String getError() {
        return this.error;
    }

    @Generated
    public void setReport(Report report) {
        this.report = report;
    }

    @Generated
    public void setError(String error) {
        this.error = error;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportTaskResult)) {
            return false;
        }
        ReportTaskResult other = (ReportTaskResult)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Report this$report = this.getReport();
        Report other$report = other.getReport();
        if (this$report == null ? other$report != null : !((Object)this$report).equals(other$report)) {
            return false;
        }
        String this$error = this.getError();
        String other$error = other.getError();
        return !(this$error == null ? other$error != null : !this$error.equals(other$error));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportTaskResult;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Report $report = this.getReport();
        result = result * 59 + ($report == null ? 43 : ((Object)$report).hashCode());
        String $error = this.getError();
        result = result * 59 + ($error == null ? 43 : $error.hashCode());
        return result;
    }

    @Generated
    public ReportTaskResult() {
    }

    @Override
    @Generated
    public String toString() {
        return "ReportTaskResult(super=" + super.toString() + ", report=" + String.valueOf(this.getReport()) + ", error=" + this.getError() + ")";
    }

    @Generated
    public static class ReportTaskResultBuilder {
        @Generated
        private boolean success;
        @Generated
        private boolean discarded;
        @Generated
        private Report report;
        @Generated
        private String error;

        @Generated
        ReportTaskResultBuilder() {
        }

        @Generated
        public ReportTaskResultBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        @Generated
        public ReportTaskResultBuilder discarded(boolean discarded) {
            this.discarded = discarded;
            return this;
        }

        @Generated
        public ReportTaskResultBuilder report(Report report) {
            this.report = report;
            return this;
        }

        @Generated
        public ReportTaskResultBuilder error(String error) {
            this.error = error;
            return this;
        }

        @Generated
        public ReportTaskResult build() {
            return new ReportTaskResult(this.success, this.discarded, this.report, this.error);
        }

        @Generated
        public String toString() {
            return "ReportTaskResult.ReportTaskResultBuilder(success=" + this.success + ", discarded=" + this.discarded + ", report=" + String.valueOf(this.report) + ", error=" + this.error + ")";
        }
    }
}

