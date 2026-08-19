/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.job;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.UUID;
import lombok.Generated;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.JobConfiguration;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.rule.RuleNode;

public class ReportJobConfiguration
extends JobConfiguration {
    private ReportTemplateId reportTemplateId;
    private UserId userId;
    private String timezone;
    private List<UUID> targets;
    private NotificationTemplateId notificationTemplateId;
    private List<NotificationRequest> notificationRequests;
    private EntityId originator;
    private RuleNode ruleNode;
    private String outputTbMsgProto;
    private String queueName;
    private EntityInfo schedulerEventInfo;

    @Override
    public JobType getType() {
        return JobType.REPORT;
    }

    @Generated
    public static ReportJobConfigurationBuilder builder() {
        return new ReportJobConfigurationBuilder();
    }

    @Generated
    public ReportTemplateId getReportTemplateId() {
        return this.reportTemplateId;
    }

    @Generated
    public UserId getUserId() {
        return this.userId;
    }

    @Generated
    public String getTimezone() {
        return this.timezone;
    }

    @Generated
    public List<UUID> getTargets() {
        return this.targets;
    }

    @Generated
    public NotificationTemplateId getNotificationTemplateId() {
        return this.notificationTemplateId;
    }

    @Generated
    public List<NotificationRequest> getNotificationRequests() {
        return this.notificationRequests;
    }

    @Generated
    public EntityId getOriginator() {
        return this.originator;
    }

    @Generated
    public RuleNode getRuleNode() {
        return this.ruleNode;
    }

    @Generated
    public String getOutputTbMsgProto() {
        return this.outputTbMsgProto;
    }

    @Generated
    public String getQueueName() {
        return this.queueName;
    }

    @Generated
    public EntityInfo getSchedulerEventInfo() {
        return this.schedulerEventInfo;
    }

    @Generated
    public void setReportTemplateId(ReportTemplateId reportTemplateId) {
        this.reportTemplateId = reportTemplateId;
    }

    @Generated
    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    @Generated
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Generated
    public void setTargets(List<UUID> targets) {
        this.targets = targets;
    }

    @Generated
    public void setNotificationTemplateId(NotificationTemplateId notificationTemplateId) {
        this.notificationTemplateId = notificationTemplateId;
    }

    @Generated
    public void setNotificationRequests(List<NotificationRequest> notificationRequests) {
        this.notificationRequests = notificationRequests;
    }

    @Generated
    public void setOriginator(EntityId originator) {
        this.originator = originator;
    }

    @Generated
    public void setRuleNode(RuleNode ruleNode) {
        this.ruleNode = ruleNode;
    }

    @Generated
    public void setOutputTbMsgProto(String outputTbMsgProto) {
        this.outputTbMsgProto = outputTbMsgProto;
    }

    @Generated
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Generated
    public void setSchedulerEventInfo(EntityInfo schedulerEventInfo) {
        this.schedulerEventInfo = schedulerEventInfo;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportJobConfiguration)) {
            return false;
        }
        ReportJobConfiguration other = (ReportJobConfiguration)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ReportTemplateId this$reportTemplateId = this.getReportTemplateId();
        ReportTemplateId other$reportTemplateId = other.getReportTemplateId();
        if (this$reportTemplateId == null ? other$reportTemplateId != null : !((Object)this$reportTemplateId).equals(other$reportTemplateId)) {
            return false;
        }
        UserId this$userId = this.getUserId();
        UserId other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        String this$timezone = this.getTimezone();
        String other$timezone = other.getTimezone();
        if (this$timezone == null ? other$timezone != null : !this$timezone.equals(other$timezone)) {
            return false;
        }
        List<UUID> this$targets = this.getTargets();
        List<UUID> other$targets = other.getTargets();
        if (this$targets == null ? other$targets != null : !((Object)this$targets).equals(other$targets)) {
            return false;
        }
        NotificationTemplateId this$notificationTemplateId = this.getNotificationTemplateId();
        NotificationTemplateId other$notificationTemplateId = other.getNotificationTemplateId();
        if (this$notificationTemplateId == null ? other$notificationTemplateId != null : !((Object)this$notificationTemplateId).equals(other$notificationTemplateId)) {
            return false;
        }
        List<NotificationRequest> this$notificationRequests = this.getNotificationRequests();
        List<NotificationRequest> other$notificationRequests = other.getNotificationRequests();
        if (this$notificationRequests == null ? other$notificationRequests != null : !((Object)this$notificationRequests).equals(other$notificationRequests)) {
            return false;
        }
        EntityId this$originator = this.getOriginator();
        EntityId other$originator = other.getOriginator();
        if (this$originator == null ? other$originator != null : !this$originator.equals(other$originator)) {
            return false;
        }
        RuleNode this$ruleNode = this.getRuleNode();
        RuleNode other$ruleNode = other.getRuleNode();
        if (this$ruleNode == null ? other$ruleNode != null : !((Object)this$ruleNode).equals(other$ruleNode)) {
            return false;
        }
        String this$outputTbMsgProto = this.getOutputTbMsgProto();
        String other$outputTbMsgProto = other.getOutputTbMsgProto();
        if (this$outputTbMsgProto == null ? other$outputTbMsgProto != null : !this$outputTbMsgProto.equals(other$outputTbMsgProto)) {
            return false;
        }
        String this$queueName = this.getQueueName();
        String other$queueName = other.getQueueName();
        if (this$queueName == null ? other$queueName != null : !this$queueName.equals(other$queueName)) {
            return false;
        }
        EntityInfo this$schedulerEventInfo = this.getSchedulerEventInfo();
        EntityInfo other$schedulerEventInfo = other.getSchedulerEventInfo();
        return !(this$schedulerEventInfo == null ? other$schedulerEventInfo != null : !((Object)this$schedulerEventInfo).equals(other$schedulerEventInfo));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportJobConfiguration;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        ReportTemplateId $reportTemplateId = this.getReportTemplateId();
        result = result * 59 + ($reportTemplateId == null ? 43 : ((Object)$reportTemplateId).hashCode());
        UserId $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        String $timezone = this.getTimezone();
        result = result * 59 + ($timezone == null ? 43 : $timezone.hashCode());
        List<UUID> $targets = this.getTargets();
        result = result * 59 + ($targets == null ? 43 : ((Object)$targets).hashCode());
        NotificationTemplateId $notificationTemplateId = this.getNotificationTemplateId();
        result = result * 59 + ($notificationTemplateId == null ? 43 : ((Object)$notificationTemplateId).hashCode());
        List<NotificationRequest> $notificationRequests = this.getNotificationRequests();
        result = result * 59 + ($notificationRequests == null ? 43 : ((Object)$notificationRequests).hashCode());
        EntityId $originator = this.getOriginator();
        result = result * 59 + ($originator == null ? 43 : $originator.hashCode());
        RuleNode $ruleNode = this.getRuleNode();
        result = result * 59 + ($ruleNode == null ? 43 : ((Object)$ruleNode).hashCode());
        String $outputTbMsgProto = this.getOutputTbMsgProto();
        result = result * 59 + ($outputTbMsgProto == null ? 43 : $outputTbMsgProto.hashCode());
        String $queueName = this.getQueueName();
        result = result * 59 + ($queueName == null ? 43 : $queueName.hashCode());
        EntityInfo $schedulerEventInfo = this.getSchedulerEventInfo();
        result = result * 59 + ($schedulerEventInfo == null ? 43 : ((Object)$schedulerEventInfo).hashCode());
        return result;
    }

    @ConstructorProperties(value={"reportTemplateId", "userId", "timezone", "targets", "notificationTemplateId", "notificationRequests", "originator", "ruleNode", "outputTbMsgProto", "queueName", "schedulerEventInfo"})
    @Generated
    public ReportJobConfiguration(ReportTemplateId reportTemplateId, UserId userId, String timezone, List<UUID> targets, NotificationTemplateId notificationTemplateId, List<NotificationRequest> notificationRequests, EntityId originator, RuleNode ruleNode, String outputTbMsgProto, String queueName, EntityInfo schedulerEventInfo) {
        this.reportTemplateId = reportTemplateId;
        this.userId = userId;
        this.timezone = timezone;
        this.targets = targets;
        this.notificationTemplateId = notificationTemplateId;
        this.notificationRequests = notificationRequests;
        this.originator = originator;
        this.ruleNode = ruleNode;
        this.outputTbMsgProto = outputTbMsgProto;
        this.queueName = queueName;
        this.schedulerEventInfo = schedulerEventInfo;
    }

    @Generated
    public ReportJobConfiguration() {
    }

    @Override
    @Generated
    public String toString() {
        return "ReportJobConfiguration(super=" + super.toString() + ", reportTemplateId=" + String.valueOf(this.getReportTemplateId()) + ", userId=" + String.valueOf(this.getUserId()) + ", timezone=" + this.getTimezone() + ", targets=" + String.valueOf(this.getTargets()) + ", notificationTemplateId=" + String.valueOf(this.getNotificationTemplateId()) + ", notificationRequests=" + String.valueOf(this.getNotificationRequests()) + ", originator=" + String.valueOf(this.getOriginator()) + ", ruleNode=" + String.valueOf(this.getRuleNode()) + ", outputTbMsgProto=" + this.getOutputTbMsgProto() + ", queueName=" + this.getQueueName() + ", schedulerEventInfo=" + String.valueOf(this.getSchedulerEventInfo()) + ")";
    }

    @Generated
    public static class ReportJobConfigurationBuilder {
        @Generated
        private ReportTemplateId reportTemplateId;
        @Generated
        private UserId userId;
        @Generated
        private String timezone;
        @Generated
        private List<UUID> targets;
        @Generated
        private NotificationTemplateId notificationTemplateId;
        @Generated
        private List<NotificationRequest> notificationRequests;
        @Generated
        private EntityId originator;
        @Generated
        private RuleNode ruleNode;
        @Generated
        private String outputTbMsgProto;
        @Generated
        private String queueName;
        @Generated
        private EntityInfo schedulerEventInfo;

        @Generated
        ReportJobConfigurationBuilder() {
        }

        @Generated
        public ReportJobConfigurationBuilder reportTemplateId(ReportTemplateId reportTemplateId) {
            this.reportTemplateId = reportTemplateId;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder targets(List<UUID> targets) {
            this.targets = targets;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder notificationTemplateId(NotificationTemplateId notificationTemplateId) {
            this.notificationTemplateId = notificationTemplateId;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder notificationRequests(List<NotificationRequest> notificationRequests) {
            this.notificationRequests = notificationRequests;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder originator(EntityId originator) {
            this.originator = originator;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder ruleNode(RuleNode ruleNode) {
            this.ruleNode = ruleNode;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder outputTbMsgProto(String outputTbMsgProto) {
            this.outputTbMsgProto = outputTbMsgProto;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder queueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        @Generated
        public ReportJobConfigurationBuilder schedulerEventInfo(EntityInfo schedulerEventInfo) {
            this.schedulerEventInfo = schedulerEventInfo;
            return this;
        }

        @Generated
        public ReportJobConfiguration build() {
            return new ReportJobConfiguration(this.reportTemplateId, this.userId, this.timezone, this.targets, this.notificationTemplateId, this.notificationRequests, this.originator, this.ruleNode, this.outputTbMsgProto, this.queueName, this.schedulerEventInfo);
        }

        @Generated
        public String toString() {
            return "ReportJobConfiguration.ReportJobConfigurationBuilder(reportTemplateId=" + String.valueOf(this.reportTemplateId) + ", userId=" + String.valueOf(this.userId) + ", timezone=" + this.timezone + ", targets=" + String.valueOf(this.targets) + ", notificationTemplateId=" + String.valueOf(this.notificationTemplateId) + ", notificationRequests=" + String.valueOf(this.notificationRequests) + ", originator=" + String.valueOf(this.originator) + ", ruleNode=" + String.valueOf(this.ruleNode) + ", outputTbMsgProto=" + this.outputTbMsgProto + ", queueName=" + this.queueName + ", schedulerEventInfo=" + String.valueOf(this.schedulerEventInfo) + ")";
        }
    }
}

