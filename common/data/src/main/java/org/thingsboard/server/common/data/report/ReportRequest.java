/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Generated;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

public class ReportRequest {
    @Schema(description="Json object representing the report template id.")
    private ReportTemplateId reportTemplateId;
    @Schema(description="Json object representing the report template config.")
    private ReportTemplateConfig reportTemplateConfig;
    @Schema(description="Timezone used for report generation.", example="Europe/Kiev")
    private String timezone;
    @Schema(description="A string value representing the user id.", example="784f394c-42b6-435a-983c-b7beff2784f9")
    private String userId;
    @Schema(description="Json object representing the originator id.")
    private EntityId originator;
    private List<UUID> targets;
    private NotificationTemplateId notificationTemplateId;

    @Generated
    public ReportRequest() {
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
    public String getUserId() {
        return this.userId;
    }

    @Generated
    public EntityId getOriginator() {
        return this.originator;
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
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Generated
    public void setOriginator(EntityId originator) {
        this.originator = originator;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportRequest)) {
            return false;
        }
        ReportRequest other = (ReportRequest)o;
        if (!other.canEqual(this)) {
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
        String this$userId = this.getUserId();
        String other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) {
            return false;
        }
        EntityId this$originator = this.getOriginator();
        EntityId other$originator = other.getOriginator();
        if (this$originator == null ? other$originator != null : !this$originator.equals(other$originator)) {
            return false;
        }
        List<UUID> this$targets = this.getTargets();
        List<UUID> other$targets = other.getTargets();
        if (this$targets == null ? other$targets != null : !((Object)this$targets).equals(other$targets)) {
            return false;
        }
        NotificationTemplateId this$notificationTemplateId = this.getNotificationTemplateId();
        NotificationTemplateId other$notificationTemplateId = other.getNotificationTemplateId();
        return !(this$notificationTemplateId == null ? other$notificationTemplateId != null : !((Object)this$notificationTemplateId).equals(other$notificationTemplateId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        ReportTemplateId $reportTemplateId = this.getReportTemplateId();
        result = result * 59 + ($reportTemplateId == null ? 43 : ((Object)$reportTemplateId).hashCode());
        ReportTemplateConfig $reportTemplateConfig = this.getReportTemplateConfig();
        result = result * 59 + ($reportTemplateConfig == null ? 43 : $reportTemplateConfig.hashCode());
        String $timezone = this.getTimezone();
        result = result * 59 + ($timezone == null ? 43 : $timezone.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        EntityId $originator = this.getOriginator();
        result = result * 59 + ($originator == null ? 43 : $originator.hashCode());
        List<UUID> $targets = this.getTargets();
        result = result * 59 + ($targets == null ? 43 : ((Object)$targets).hashCode());
        NotificationTemplateId $notificationTemplateId = this.getNotificationTemplateId();
        result = result * 59 + ($notificationTemplateId == null ? 43 : ((Object)$notificationTemplateId).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ReportRequest(reportTemplateId=" + String.valueOf(this.getReportTemplateId()) + ", reportTemplateConfig=" + String.valueOf(this.getReportTemplateConfig()) + ", timezone=" + this.getTimezone() + ", userId=" + this.getUserId() + ", originator=" + String.valueOf(this.getOriginator()) + ", targets=" + String.valueOf(this.getTargets()) + ", notificationTemplateId=" + String.valueOf(this.getNotificationTemplateId()) + ")";
    }
}

