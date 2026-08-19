/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.media.Schema$AccessMode
 *  io.swagger.v3.oas.annotations.media.Schema$RequiredMode
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

public class BaseReportTemplate
extends BaseData<ReportTemplateId>
implements HasName,
HasTenantId,
HasCustomerId,
ExportableEntity<ReportTemplateId>,
HasVersion {
    private static final long serialVersionUID = -1756737145862011794L;
    @Schema(description="JSON object with Tenant Id. Tenant Id of the report template can't be changed.", accessMode=Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description="JSON object with Customer Id", accessMode=Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @NoXss
    @Length(fieldName="name")
    @Schema(requiredMode=Schema.RequiredMode.REQUIRED, description="Report name", example="Weekly Report")
    private String name;
    @Schema(requiredMode=Schema.RequiredMode.REQUIRED, description="Report format", allowableValues={"PDF, CSV"})
    private TbReportFormat format;
    @Schema(requiredMode=Schema.RequiredMode.REQUIRED, description="Report template type", allowableValues={"REPORT, SUB_REPORT"})
    private ReportTemplateType type;
    @NoXss
    @Length(fieldName="description", max=1024)
    @Schema(description="Description")
    private String description;
    private ReportTemplateId externalId;
    private Long version;

    public BaseReportTemplate() {
    }

    public BaseReportTemplate(ReportTemplateId id) {
        super(id);
    }

    public BaseReportTemplate(BaseReportTemplate reportTemplate) {
        super(reportTemplate);
        this.tenantId = reportTemplate.getTenantId();
        this.customerId = reportTemplate.getCustomerId();
        this.name = reportTemplate.getName();
        this.format = reportTemplate.getFormat();
        this.type = reportTemplate.getType();
        this.description = reportTemplate.getDescription();
        this.externalId = reportTemplate.getExternalId();
        this.version = reportTemplate.getVersion();
    }

    @Override
    @Schema(description="JSON object with the report template Id. Specify this field to update the report. Referencing non-existing report template Id will cause error. Omit this field to create new report template")
    public ReportTemplateId getId() {
        return (ReportTemplateId)super.getId();
    }

    @Override
    @Schema(description="Timestamp of the report template creation, in milliseconds", example="1609459200000", accessMode=Schema.AccessMode.READ_ONLY)
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description="JSON object with Customer or Tenant Id", accessMode=Schema.AccessMode.READ_ONLY)
    public EntityId getOwnerId() {
        return this.customerId != null && !this.customerId.isNullUid() ? this.customerId : this.tenantId;
    }

    public void setOwnerId(EntityId entityId) {
        this.customerId = EntityType.CUSTOMER.equals((Object)entityId.getEntityType()) ? new CustomerId(entityId.getId()) : new CustomerId(CustomerId.NULL_UUID);
    }

    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

    @Override
    @Generated
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setFormat(TbReportFormat format) {
        this.format = format;
    }

    @Generated
    public void setType(ReportTemplateType type) {
        this.type = type;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @Generated
    public void setExternalId(ReportTemplateId externalId) {
        this.externalId = externalId;
    }

    @Override
    @Generated
    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    @Generated
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Override
    @Generated
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @Override
    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public TbReportFormat getFormat() {
        return this.format;
    }

    @Generated
    public ReportTemplateType getType() {
        return this.type;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Override
    @Generated
    public ReportTemplateId getExternalId() {
        return this.externalId;
    }

    @Override
    @Generated
    public Long getVersion() {
        return this.version;
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BaseReportTemplate)) {
            return false;
        }
        BaseReportTemplate other = (BaseReportTemplate)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$version = this.getVersion();
        Long other$version = other.getVersion();
        if (this$version == null ? other$version != null : !((Object)this$version).equals(other$version)) {
            return false;
        }
        TenantId this$tenantId = this.getTenantId();
        TenantId other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        CustomerId this$customerId = this.getCustomerId();
        CustomerId other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !((Object)this$customerId).equals(other$customerId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        TbReportFormat this$format = this.getFormat();
        TbReportFormat other$format = other.getFormat();
        if (this$format == null ? other$format != null : !((Object)((Object)this$format)).equals((Object)other$format)) {
            return false;
        }
        ReportTemplateType this$type = this.getType();
        ReportTemplateType other$type = other.getType();
        if (this$type == null ? other$type != null : !((Object)((Object)this$type)).equals((Object)other$type)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        ReportTemplateId this$externalId = this.getExternalId();
        ReportTemplateId other$externalId = other.getExternalId();
        return !(this$externalId == null ? other$externalId != null : !((Object)this$externalId).equals(other$externalId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof BaseReportTemplate;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : ((Object)$version).hashCode());
        TenantId $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        CustomerId $customerId = this.getCustomerId();
        result = result * 59 + ($customerId == null ? 43 : ((Object)$customerId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        TbReportFormat $format = this.getFormat();
        result = result * 59 + ($format == null ? 43 : ((Object)((Object)$format)).hashCode());
        ReportTemplateType $type = this.getType();
        result = result * 59 + ($type == null ? 43 : ((Object)((Object)$type)).hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        ReportTemplateId $externalId = this.getExternalId();
        result = result * 59 + ($externalId == null ? 43 : ((Object)$externalId).hashCode());
        return result;
    }

    @Override
    @Generated
    public String toString() {
        return "BaseReportTemplate(super=" + super.toString() + ", tenantId=" + String.valueOf(this.getTenantId()) + ", customerId=" + String.valueOf(this.getCustomerId()) + ", name=" + this.getName() + ", format=" + String.valueOf((Object)this.getFormat()) + ", type=" + String.valueOf((Object)this.getType()) + ", description=" + this.getDescription() + ", externalId=" + String.valueOf(this.getExternalId()) + ", version=" + this.getVersion() + ")";
    }
}

