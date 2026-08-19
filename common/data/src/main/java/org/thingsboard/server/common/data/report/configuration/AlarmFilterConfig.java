/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import java.beans.ConstructorProperties;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.UserId;

public class AlarmFilterConfig {
    private List<String> typeList;
    private List<AlarmSearchStatus> statusList;
    private List<AlarmSeverity> severityList;
    private UserId assigneeId;
    private boolean searchPropagatedAlarms;

    @Generated
    public List<String> getTypeList() {
        return this.typeList;
    }

    @Generated
    public List<AlarmSearchStatus> getStatusList() {
        return this.statusList;
    }

    @Generated
    public List<AlarmSeverity> getSeverityList() {
        return this.severityList;
    }

    @Generated
    public UserId getAssigneeId() {
        return this.assigneeId;
    }

    @Generated
    public boolean isSearchPropagatedAlarms() {
        return this.searchPropagatedAlarms;
    }

    @Generated
    public void setTypeList(List<String> typeList) {
        this.typeList = typeList;
    }

    @Generated
    public void setStatusList(List<AlarmSearchStatus> statusList) {
        this.statusList = statusList;
    }

    @Generated
    public void setSeverityList(List<AlarmSeverity> severityList) {
        this.severityList = severityList;
    }

    @Generated
    public void setAssigneeId(UserId assigneeId) {
        this.assigneeId = assigneeId;
    }

    @Generated
    public void setSearchPropagatedAlarms(boolean searchPropagatedAlarms) {
        this.searchPropagatedAlarms = searchPropagatedAlarms;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AlarmFilterConfig)) {
            return false;
        }
        AlarmFilterConfig other = (AlarmFilterConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isSearchPropagatedAlarms() != other.isSearchPropagatedAlarms()) {
            return false;
        }
        List<String> this$typeList = this.getTypeList();
        List<String> other$typeList = other.getTypeList();
        if (this$typeList == null ? other$typeList != null : !((Object)this$typeList).equals(other$typeList)) {
            return false;
        }
        List<AlarmSearchStatus> this$statusList = this.getStatusList();
        List<AlarmSearchStatus> other$statusList = other.getStatusList();
        if (this$statusList == null ? other$statusList != null : !((Object)this$statusList).equals(other$statusList)) {
            return false;
        }
        List<AlarmSeverity> this$severityList = this.getSeverityList();
        List<AlarmSeverity> other$severityList = other.getSeverityList();
        if (this$severityList == null ? other$severityList != null : !((Object)this$severityList).equals(other$severityList)) {
            return false;
        }
        UserId this$assigneeId = this.getAssigneeId();
        UserId other$assigneeId = other.getAssigneeId();
        return !(this$assigneeId == null ? other$assigneeId != null : !((Object)this$assigneeId).equals(other$assigneeId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AlarmFilterConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isSearchPropagatedAlarms() ? 79 : 97);
        List<String> $typeList = this.getTypeList();
        result = result * 59 + ($typeList == null ? 43 : ((Object)$typeList).hashCode());
        List<AlarmSearchStatus> $statusList = this.getStatusList();
        result = result * 59 + ($statusList == null ? 43 : ((Object)$statusList).hashCode());
        List<AlarmSeverity> $severityList = this.getSeverityList();
        result = result * 59 + ($severityList == null ? 43 : ((Object)$severityList).hashCode());
        UserId $assigneeId = this.getAssigneeId();
        result = result * 59 + ($assigneeId == null ? 43 : ((Object)$assigneeId).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "AlarmFilterConfig(typeList=" + String.valueOf(this.getTypeList()) + ", statusList=" + String.valueOf(this.getStatusList()) + ", severityList=" + String.valueOf(this.getSeverityList()) + ", assigneeId=" + String.valueOf(this.getAssigneeId()) + ", searchPropagatedAlarms=" + this.isSearchPropagatedAlarms() + ")";
    }

    @Generated
    public AlarmFilterConfig() {
    }

    @ConstructorProperties(value={"typeList", "statusList", "severityList", "assigneeId", "searchPropagatedAlarms"})
    @Generated
    public AlarmFilterConfig(List<String> typeList, List<AlarmSearchStatus> statusList, List<AlarmSeverity> severityList, UserId assigneeId, boolean searchPropagatedAlarms) {
        this.typeList = typeList;
        this.statusList = statusList;
        this.severityList = severityList;
        this.assigneeId = assigneeId;
        this.searchPropagatedAlarms = searchPropagatedAlarms;
    }
}

