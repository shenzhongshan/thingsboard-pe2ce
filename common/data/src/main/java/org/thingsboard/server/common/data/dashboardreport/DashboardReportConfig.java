/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.media.Schema$RequiredMode
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.dashboardreport;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

public class DashboardReportConfig {
    @Schema(description="Base URL of ThingsBoard UI that should be accessible by Web Report Server.", example="https:thingsboard.cloud", requiredMode=Schema.RequiredMode.REQUIRED)
    String baseUrl;
    @Schema(description="A string value representing the dashboard id.", example="784f394c-42b6-435a-983c-b7beff2784f9", requiredMode=Schema.RequiredMode.REQUIRED)
    String dashboardId;
    @Schema(description="Target dashboard state for dashboard report generation.")
    String state;
    @Schema(description="Timezone in which target dashboard will be presented in dashboard report.", example="Europe/Kiev", requiredMode=Schema.RequiredMode.REQUIRED)
    String timezone;
    @Schema(description="If set, timewindow configured in the target dashboard will be used during dashboard report generation.", example="true")
    boolean useDashboardTimewindow;
    @Schema(description="Specific dashboard timewindow that will be used during dashboard report generation.")
    JsonNode timewindow;
    @Schema(description="If set, timewindow configured in the target dashboard will be used during dashboard report generation.", example="report-%d{yyyy-MM-dd_HH:mm:ss}", requiredMode=Schema.RequiredMode.REQUIRED)
    String namePattern;
    @Schema(description="Dashboard report file type, can be PDF | PNG | JPEG.", example="pdf")
    String type;
    @Schema(description="If set, credentials of user created this dashboard report configuration will be used to open dashboard UI during dashboard report generation.", example="true")
    boolean useCurrentUserCredentials;
    @Schema(description="A string value representing the user id.", example="784f394c-42b6-435a-983c-b7beff2784f9", requiredMode=Schema.RequiredMode.REQUIRED)
    String userId;

    @Generated
    public DashboardReportConfig() {
    }

    @Generated
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Generated
    public String getDashboardId() {
        return this.dashboardId;
    }

    @Generated
    public String getState() {
        return this.state;
    }

    @Generated
    public String getTimezone() {
        return this.timezone;
    }

    @Generated
    public boolean isUseDashboardTimewindow() {
        return this.useDashboardTimewindow;
    }

    @Generated
    public JsonNode getTimewindow() {
        return this.timewindow;
    }

    @Generated
    public String getNamePattern() {
        return this.namePattern;
    }

    @Generated
    public String getType() {
        return this.type;
    }

    @Generated
    public boolean isUseCurrentUserCredentials() {
        return this.useCurrentUserCredentials;
    }

    @Generated
    public String getUserId() {
        return this.userId;
    }

    @Generated
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Generated
    public void setDashboardId(String dashboardId) {
        this.dashboardId = dashboardId;
    }

    @Generated
    public void setState(String state) {
        this.state = state;
    }

    @Generated
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Generated
    public void setUseDashboardTimewindow(boolean useDashboardTimewindow) {
        this.useDashboardTimewindow = useDashboardTimewindow;
    }

    @Generated
    public void setTimewindow(JsonNode timewindow) {
        this.timewindow = timewindow;
    }

    @Generated
    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    @Generated
    public void setType(String type) {
        this.type = type;
    }

    @Generated
    public void setUseCurrentUserCredentials(boolean useCurrentUserCredentials) {
        this.useCurrentUserCredentials = useCurrentUserCredentials;
    }

    @Generated
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DashboardReportConfig)) {
            return false;
        }
        DashboardReportConfig other = (DashboardReportConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isUseDashboardTimewindow() != other.isUseDashboardTimewindow()) {
            return false;
        }
        if (this.isUseCurrentUserCredentials() != other.isUseCurrentUserCredentials()) {
            return false;
        }
        String this$baseUrl = this.getBaseUrl();
        String other$baseUrl = other.getBaseUrl();
        if (this$baseUrl == null ? other$baseUrl != null : !this$baseUrl.equals(other$baseUrl)) {
            return false;
        }
        String this$dashboardId = this.getDashboardId();
        String other$dashboardId = other.getDashboardId();
        if (this$dashboardId == null ? other$dashboardId != null : !this$dashboardId.equals(other$dashboardId)) {
            return false;
        }
        String this$state = this.getState();
        String other$state = other.getState();
        if (this$state == null ? other$state != null : !this$state.equals(other$state)) {
            return false;
        }
        String this$timezone = this.getTimezone();
        String other$timezone = other.getTimezone();
        if (this$timezone == null ? other$timezone != null : !this$timezone.equals(other$timezone)) {
            return false;
        }
        JsonNode this$timewindow = this.getTimewindow();
        JsonNode other$timewindow = other.getTimewindow();
        if (this$timewindow == null ? other$timewindow != null : !this$timewindow.equals(other$timewindow)) {
            return false;
        }
        String this$namePattern = this.getNamePattern();
        String other$namePattern = other.getNamePattern();
        if (this$namePattern == null ? other$namePattern != null : !this$namePattern.equals(other$namePattern)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$userId = this.getUserId();
        String other$userId = other.getUserId();
        return !(this$userId == null ? other$userId != null : !this$userId.equals(other$userId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DashboardReportConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isUseDashboardTimewindow() ? 79 : 97);
        result = result * 59 + (this.isUseCurrentUserCredentials() ? 79 : 97);
        String $baseUrl = this.getBaseUrl();
        result = result * 59 + ($baseUrl == null ? 43 : $baseUrl.hashCode());
        String $dashboardId = this.getDashboardId();
        result = result * 59 + ($dashboardId == null ? 43 : $dashboardId.hashCode());
        String $state = this.getState();
        result = result * 59 + ($state == null ? 43 : $state.hashCode());
        String $timezone = this.getTimezone();
        result = result * 59 + ($timezone == null ? 43 : $timezone.hashCode());
        JsonNode $timewindow = this.getTimewindow();
        result = result * 59 + ($timewindow == null ? 43 : $timewindow.hashCode());
        String $namePattern = this.getNamePattern();
        result = result * 59 + ($namePattern == null ? 43 : $namePattern.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "DashboardReportConfig(baseUrl=" + this.getBaseUrl() + ", dashboardId=" + this.getDashboardId() + ", state=" + this.getState() + ", timezone=" + this.getTimezone() + ", useDashboardTimewindow=" + this.isUseDashboardTimewindow() + ", timewindow=" + String.valueOf(this.getTimewindow()) + ", namePattern=" + this.getNamePattern() + ", type=" + this.getType() + ", useCurrentUserCredentials=" + this.isUseCurrentUserCredentials() + ", userId=" + this.getUserId() + ")";
    }
}

