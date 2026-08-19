/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report;

import java.beans.ConstructorProperties;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;

public class ReportTemplateQuery {
    private PageLink pageLink;
    private boolean includeCustomers;
    private List<TbReportFormat> formatList;
    private List<ReportTemplateType> typeList;

    @Generated
    public static ReportTemplateQueryBuilder builder() {
        return new ReportTemplateQueryBuilder();
    }

    @Generated
    public PageLink getPageLink() {
        return this.pageLink;
    }

    @Generated
    public boolean isIncludeCustomers() {
        return this.includeCustomers;
    }

    @Generated
    public List<TbReportFormat> getFormatList() {
        return this.formatList;
    }

    @Generated
    public List<ReportTemplateType> getTypeList() {
        return this.typeList;
    }

    @Generated
    public void setPageLink(PageLink pageLink) {
        this.pageLink = pageLink;
    }

    @Generated
    public void setIncludeCustomers(boolean includeCustomers) {
        this.includeCustomers = includeCustomers;
    }

    @Generated
    public void setFormatList(List<TbReportFormat> formatList) {
        this.formatList = formatList;
    }

    @Generated
    public void setTypeList(List<ReportTemplateType> typeList) {
        this.typeList = typeList;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportTemplateQuery)) {
            return false;
        }
        ReportTemplateQuery other = (ReportTemplateQuery)o;
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
        List<TbReportFormat> this$formatList = this.getFormatList();
        List<TbReportFormat> other$formatList = other.getFormatList();
        if (this$formatList == null ? other$formatList != null : !((Object)this$formatList).equals(other$formatList)) {
            return false;
        }
        List<ReportTemplateType> this$typeList = this.getTypeList();
        List<ReportTemplateType> other$typeList = other.getTypeList();
        return !(this$typeList == null ? other$typeList != null : !((Object)this$typeList).equals(other$typeList));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ReportTemplateQuery;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isIncludeCustomers() ? 79 : 97);
        PageLink $pageLink = this.getPageLink();
        result = result * 59 + ($pageLink == null ? 43 : ((Object)$pageLink).hashCode());
        List<TbReportFormat> $formatList = this.getFormatList();
        result = result * 59 + ($formatList == null ? 43 : ((Object)$formatList).hashCode());
        List<ReportTemplateType> $typeList = this.getTypeList();
        result = result * 59 + ($typeList == null ? 43 : ((Object)$typeList).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ReportTemplateQuery(pageLink=" + String.valueOf(this.getPageLink()) + ", includeCustomers=" + this.isIncludeCustomers() + ", formatList=" + String.valueOf(this.getFormatList()) + ", typeList=" + String.valueOf(this.getTypeList()) + ")";
    }

    @ConstructorProperties(value={"pageLink", "includeCustomers", "formatList", "typeList"})
    @Generated
    public ReportTemplateQuery(PageLink pageLink, boolean includeCustomers, List<TbReportFormat> formatList, List<ReportTemplateType> typeList) {
        this.pageLink = pageLink;
        this.includeCustomers = includeCustomers;
        this.formatList = formatList;
        this.typeList = typeList;
    }

    @Generated
    public static class ReportTemplateQueryBuilder {
        @Generated
        private PageLink pageLink;
        @Generated
        private boolean includeCustomers;
        @Generated
        private List<TbReportFormat> formatList;
        @Generated
        private List<ReportTemplateType> typeList;

        @Generated
        ReportTemplateQueryBuilder() {
        }

        @Generated
        public ReportTemplateQueryBuilder pageLink(PageLink pageLink) {
            this.pageLink = pageLink;
            return this;
        }

        @Generated
        public ReportTemplateQueryBuilder includeCustomers(boolean includeCustomers) {
            this.includeCustomers = includeCustomers;
            return this;
        }

        @Generated
        public ReportTemplateQueryBuilder formatList(List<TbReportFormat> formatList) {
            this.formatList = formatList;
            return this;
        }

        @Generated
        public ReportTemplateQueryBuilder typeList(List<ReportTemplateType> typeList) {
            this.typeList = typeList;
            return this;
        }

        @Generated
        public ReportTemplateQuery build() {
            return new ReportTemplateQuery(this.pageLink, this.includeCustomers, this.formatList, this.typeList);
        }

        @Generated
        public String toString() {
            return "ReportTemplateQuery.ReportTemplateQueryBuilder(pageLink=" + String.valueOf(this.pageLink) + ", includeCustomers=" + this.includeCustomers + ", formatList=" + String.valueOf(this.formatList) + ", typeList=" + String.valueOf(this.typeList) + ")";
        }
    }
}

