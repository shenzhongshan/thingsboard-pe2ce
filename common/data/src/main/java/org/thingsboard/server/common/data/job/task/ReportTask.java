/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.job.task;

import lombok.Generated;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.ReportTaskResult;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

public class ReportTask
extends Task<ReportTaskResult> {
    private CustomerId customerId;
    private ReportTemplateId reportTemplateId;
    private ReportTemplateConfig reportTemplateConfig;
    private String timezone;
    private UserId userId;
    private EntityId userOwnerId;
    private String accessToken;
    private long accessTokenExpirationTs;
    private EntityId originator;

    @Override
    public ReportTaskResult toFailed(Throwable error) {
        return ReportTaskResult.failed(this, error);
    }

    @Override
    public ReportTaskResult toDiscarded() {
        return ReportTaskResult.discarded(this);
    }

    @Override
    public EntityId getEntityId() {
        return this.getJobId();
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

    @Generated
    protected ReportTask(ReportTaskBuilder<?, ?> b) {
        super(b);
        this.customerId = b.customerId;
        this.reportTemplateId = b.reportTemplateId;
        this.reportTemplateConfig = b.reportTemplateConfig;
        this.timezone = b.timezone;
        this.userId = b.userId;
        this.userOwnerId = b.userOwnerId;
        this.accessToken = b.accessToken;
        this.accessTokenExpirationTs = b.accessTokenExpirationTs;
        this.originator = b.originator;
    }

    @Generated
    public static ReportTaskBuilder<?, ?> builder() {
        return new ReportTaskBuilderImpl();
    }

    @Generated
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @Generated
    public ReportTemplateId getReportTemplateId() {
        return this.reportTemplateId;
    }

    @Generated
    public ReportTemplateConfig getReportTemplateConfig() {
        return this.reportTemplateConfig;
    }

    @Generated
    public String getTimezone() {
        return this.timezone;
    }

    @Generated
    public UserId getUserId() {
        return this.userId;
    }

    @Generated
    public EntityId getUserOwnerId() {
        return this.userOwnerId;
    }

    @Generated
    public String getAccessToken() {
        return this.accessToken;
    }

    @Generated
    public long getAccessTokenExpirationTs() {
        return this.accessTokenExpirationTs;
    }

    @Generated
    public EntityId getOriginator() {
        return this.originator;
    }

    @Generated
    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @Generated
    public void setReportTemplateId(ReportTemplateId reportTemplateId) {
        this.reportTemplateId = reportTemplateId;
    }

    @Generated
    public void setReportTemplateConfig(ReportTemplateConfig reportTemplateConfig) {
        this.reportTemplateConfig = reportTemplateConfig;
    }

    @Generated
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Generated
    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    @Generated
    public void setUserOwnerId(EntityId userOwnerId) {
        this.userOwnerId = userOwnerId;
    }

    @Generated
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Generated
    public void setAccessTokenExpirationTs(long accessTokenExpirationTs) {
        this.accessTokenExpirationTs = accessTokenExpirationTs;
    }

    @Generated
    public void setOriginator(EntityId originator) {
        this.originator = originator;
    }

    @Generated
    public ReportTask() {
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportTask)) {
            return false;
        }
        ReportTask other = (ReportTask)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (this.getAccessTokenExpirationTs() != other.getAccessTokenExpirationTs()) {
            return false;
        }
        CustomerId this$customerId = this.getCustomerId();
        CustomerId other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !((Object)this$customerId).equals(other$customerId)) {
            return false;
        }
        ReportTemplateId this$reportTemplateId = this.getReportTemplateId();
        ReportTemplateId other$reportTemplateId = other.getReportTemplateId();
        if (this$reportTemplateId == null ? other$reportTemplateId != null : !((Object)this$reportTemplateId).equals(other$reportTemplateId)) {
            return false;
        }
        ReportTemplateConfig this$reportTemplateConfig = this.getReportTemplateConfig();
        ReportTemplateConfig other$reportTemplateConfig = other.getReportTemplateConfig();
        if (this$reportTemplateConfig == null ? other$reportTemplateConfig != null : !this$reportTemplateConfig.equals(other$reportTemplateConfig)) {
            return false;
        }
        String this$timezone = this.getTimezone();
        String other$timezone = other.getTimezone();
        if (this$timezone == null ? other$timezone != null : !this$timezone.equals(other$timezone)) {
            return false;
        }
        UserId this$userId = this.getUserId();
        UserId other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        EntityId this$userOwnerId = this.getUserOwnerId();
        EntityId other$userOwnerId = other.getUserOwnerId();
        if (this$userOwnerId == null ? other$userOwnerId != null : !this$userOwnerId.equals(other$userOwnerId)) {
            return false;
        }
        String this$accessToken = this.getAccessToken();
        String other$accessToken = other.getAccessToken();
        if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken)) {
            return false;
        }
        EntityId this$originator = this.getOriginator();
        EntityId other$originator = other.getOriginator();
        return !(this$originator == null ? other$originator != null : !this$originator.equals(other$originator));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportTask;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        long $accessTokenExpirationTs = this.getAccessTokenExpirationTs();
        result = result * 59 + (int)($accessTokenExpirationTs >>> 32 ^ $accessTokenExpirationTs);
        CustomerId $customerId = this.getCustomerId();
        result = result * 59 + ($customerId == null ? 43 : ((Object)$customerId).hashCode());
        ReportTemplateId $reportTemplateId = this.getReportTemplateId();
        result = result * 59 + ($reportTemplateId == null ? 43 : ((Object)$reportTemplateId).hashCode());
        ReportTemplateConfig $reportTemplateConfig = this.getReportTemplateConfig();
        result = result * 59 + ($reportTemplateConfig == null ? 43 : $reportTemplateConfig.hashCode());
        String $timezone = this.getTimezone();
        result = result * 59 + ($timezone == null ? 43 : $timezone.hashCode());
        UserId $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        EntityId $userOwnerId = this.getUserOwnerId();
        result = result * 59 + ($userOwnerId == null ? 43 : $userOwnerId.hashCode());
        String $accessToken = this.getAccessToken();
        result = result * 59 + ($accessToken == null ? 43 : $accessToken.hashCode());
        EntityId $originator = this.getOriginator();
        result = result * 59 + ($originator == null ? 43 : $originator.hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "ReportTask(super=" + super.toString() + ", customerId=" + String.valueOf(this.getCustomerId()) + ", reportTemplateId=" + String.valueOf(this.getReportTemplateId()) + ", timezone=" + this.getTimezone() + ", userId=" + String.valueOf(this.getUserId()) + ", userOwnerId=" + String.valueOf(this.getUserOwnerId()) + ", accessTokenExpirationTs=" + this.getAccessTokenExpirationTs() + ", originator=" + String.valueOf(this.getOriginator()) + ")";
    }

    @Generated
    public static abstract class ReportTaskBuilder<C extends ReportTask, B extends ReportTaskBuilder<C, B>>
    extends Task.TaskBuilder<ReportTaskResult, C, B> {
        @Generated
        private CustomerId customerId;
        @Generated
        private ReportTemplateId reportTemplateId;
        @Generated
        private ReportTemplateConfig reportTemplateConfig;
        @Generated
        private String timezone;
        @Generated
        private UserId userId;
        @Generated
        private EntityId userOwnerId;
        @Generated
        private String accessToken;
        @Generated
        private long accessTokenExpirationTs;
        @Generated
        private EntityId originator;

        @Generated
        public B customerId(CustomerId customerId) {
            this.customerId = customerId;
            return (B)this.self();
        }

        @Generated
        public B reportTemplateId(ReportTemplateId reportTemplateId) {
            this.reportTemplateId = reportTemplateId;
            return (B)this.self();
        }

        @Generated
        public B reportTemplateConfig(ReportTemplateConfig reportTemplateConfig) {
            this.reportTemplateConfig = reportTemplateConfig;
            return (B)this.self();
        }

        @Generated
        public B timezone(String timezone) {
            this.timezone = timezone;
            return (B)this.self();
        }

        @Generated
        public B userId(UserId userId) {
            this.userId = userId;
            return (B)this.self();
        }

        @Generated
        public B userOwnerId(EntityId userOwnerId) {
            this.userOwnerId = userOwnerId;
            return (B)this.self();
        }

        @Generated
        public B accessToken(String accessToken) {
            this.accessToken = accessToken;
            return (B)this.self();
        }

        @Generated
        public B accessTokenExpirationTs(long accessTokenExpirationTs) {
            this.accessTokenExpirationTs = accessTokenExpirationTs;
            return (B)this.self();
        }

        @Generated
        public B originator(EntityId originator) {
            this.originator = originator;
            return (B)this.self();
        }

        @Override
        @Generated
        protected abstract B self();

        @Override
        @Generated
        public abstract C build();

        @Override
        @Generated
        public String toString() {
            return "ReportTask.ReportTaskBuilder(super=" + super.toString() + ", customerId=" + String.valueOf(this.customerId) + ", reportTemplateId=" + String.valueOf(this.reportTemplateId) + ", reportTemplateConfig=" + String.valueOf(this.reportTemplateConfig) + ", timezone=" + this.timezone + ", userId=" + String.valueOf(this.userId) + ", userOwnerId=" + String.valueOf(this.userOwnerId) + ", accessToken=" + this.accessToken + ", accessTokenExpirationTs=" + this.accessTokenExpirationTs + ", originator=" + String.valueOf(this.originator) + ")";
        }
    }

    @Generated
    private static final class ReportTaskBuilderImpl
    extends ReportTaskBuilder<ReportTask, ReportTaskBuilderImpl> {
        @Generated
        private ReportTaskBuilderImpl() {
        }

        @Override
        @Generated
        protected ReportTaskBuilderImpl self() {
            return this;
        }

        @Override
        @Generated
        public ReportTask build() {
            return new ReportTask(this);
        }
    }
}

