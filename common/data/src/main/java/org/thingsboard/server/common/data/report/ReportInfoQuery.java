/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report;

import java.beans.ConstructorProperties;
import java.util.UUID;
import lombok.Generated;
import org.thingsboard.server.common.data.page.PageLink;

public class ReportInfoQuery {
    private PageLink pageLink;
    private UUID reportTemplateId;
    private UUID userId;
    private boolean includeCustomers;

    @Generated
    public static ReportInfoQueryBuilder builder() {
        return new ReportInfoQueryBuilder();
    }

    @Generated
    public PageLink getPageLink() {
        return this.pageLink;
    }

    @Generated
    public UUID getReportTemplateId() {
        return this.reportTemplateId;
    }

    @Generated
    public UUID getUserId() {
        return this.userId;
    }

    @Generated
    public boolean isIncludeCustomers() {
        return this.includeCustomers;
    }

    @Generated
    public void setPageLink(PageLink pageLink) {
        this.pageLink = pageLink;
    }

    @Generated
    public void setReportTemplateId(UUID reportTemplateId) {
        this.reportTemplateId = reportTemplateId;
    }

    @Generated
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Generated
    public void setIncludeCustomers(boolean includeCustomers) {
        this.includeCustomers = includeCustomers;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportInfoQuery)) {
            return false;
        }
        ReportInfoQuery other = (ReportInfoQuery)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isIncludeCustomers() != other.isIncludeCustomers()) {
            return false;
        }
        PageLink this$pageLink = this.getPageLink();
        PageLink other$pageLink = other.getPageLink();
        if (this$pageLink == null ? other$pageLink != null : !((Object)this$pageLink).equals(other$pageLink)) {
            return false;
        }
        UUID this$reportTemplateId = this.getReportTemplateId();
        UUID other$reportTemplateId = other.getReportTemplateId();
        if (this$reportTemplateId == null ? other$reportTemplateId != null : !((Object)this$reportTemplateId).equals(other$reportTemplateId)) {
            return false;
        }
        UUID this$userId = this.getUserId();
        UUID other$userId = other.getUserId();
        return !(this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportInfoQuery;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isIncludeCustomers() ? 79 : 97);
        PageLink $pageLink = this.getPageLink();
        result = result * 59 + ($pageLink == null ? 43 : ((Object)$pageLink).hashCode());
        UUID $reportTemplateId = this.getReportTemplateId();
        result = result * 59 + ($reportTemplateId == null ? 43 : ((Object)$reportTemplateId).hashCode());
        UUID $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ReportInfoQuery(pageLink=" + String.valueOf(this.getPageLink()) + ", reportTemplateId=" + String.valueOf(this.getReportTemplateId()) + ", userId=" + String.valueOf(this.getUserId()) + ", includeCustomers=" + this.isIncludeCustomers() + ")";
    }

    @ConstructorProperties(value={"pageLink", "reportTemplateId", "userId", "includeCustomers"})
    @Generated
    public ReportInfoQuery(PageLink pageLink, UUID reportTemplateId, UUID userId, boolean includeCustomers) {
        this.pageLink = pageLink;
        this.reportTemplateId = reportTemplateId;
        this.userId = userId;
        this.includeCustomers = includeCustomers;
    }

    @Generated
    public static class ReportInfoQueryBuilder {
        @Generated
        private PageLink pageLink;
        @Generated
        private UUID reportTemplateId;
        @Generated
        private UUID userId;
        @Generated
        private boolean includeCustomers;

        @Generated
        ReportInfoQueryBuilder() {
        }

        @Generated
        public ReportInfoQueryBuilder pageLink(PageLink pageLink) {
            this.pageLink = pageLink;
            return this;
        }

        @Generated
        public ReportInfoQueryBuilder reportTemplateId(UUID reportTemplateId) {
            this.reportTemplateId = reportTemplateId;
            return this;
        }

        @Generated
        public ReportInfoQueryBuilder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        @Generated
        public ReportInfoQueryBuilder includeCustomers(boolean includeCustomers) {
            this.includeCustomers = includeCustomers;
            return this;
        }

        @Generated
        public ReportInfoQuery build() {
            return new ReportInfoQuery(this.pageLink, this.reportTemplateId, this.userId, this.includeCustomers);
        }

        @Generated
        public String toString() {
            return "ReportInfoQuery.ReportInfoQueryBuilder(pageLink=" + String.valueOf(this.pageLink) + ", reportTemplateId=" + String.valueOf(this.reportTemplateId) + ", userId=" + String.valueOf(this.userId) + ", includeCustomers=" + this.includeCustomers + ")";
        }
    }
}

