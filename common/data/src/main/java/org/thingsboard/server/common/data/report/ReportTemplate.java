/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * A report template. The configuration is PE's typed model ({@link ReportTemplateConfig}), which is
 * polymorphic on its own {@code format} property - so the persisted JSON carries {@code "format"}
 * in addition to the {@code format} column, and the two must agree.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportTemplate extends BaseData<ReportTemplateId> implements HasName, HasTenantId, HasCustomerId,
        HasVersion, ExportableEntity<ReportTemplateId> {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Getter @Setter
    @Schema(description = "JSON object with Customer Id")
    private CustomerId customerId;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Report template name", example = "Weekly Report")
    private String name;

    @Getter @Setter
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Report format", allowableValues = {"PDF", "CSV"})
    private TbReportFormat format;

    @Getter @Setter
    @Schema(description = "Report template type", allowableValues = {"REPORT", "SUB_REPORT"})
    private ReportTemplateType type;

    @Getter @Setter
    @NoXss
    @Length(fieldName = "description", max = 1024)
    @Schema(description = "Description")
    private String description;

    @Getter @Setter
    @Schema(description = "Report template configuration: page layout plus the ordered list of report components.")
    private ReportTemplateConfig configuration;

    @Getter @Setter
    private ReportTemplateId externalId;

    @Getter @Setter
    private Long version;

    public ReportTemplate() {
        super();
    }

    public ReportTemplate(ReportTemplateId id) {
        super(id);
    }

    public ReportTemplate(ReportTemplate reportTemplate) {
        super(reportTemplate);
        this.tenantId = reportTemplate.getTenantId();
        this.customerId = reportTemplate.getCustomerId();
        this.name = reportTemplate.getName();
        this.format = reportTemplate.getFormat();
        this.type = reportTemplate.getType();
        this.description = reportTemplate.getDescription();
        this.configuration = reportTemplate.getConfiguration();
        this.externalId = reportTemplate.getExternalId();
        this.version = reportTemplate.getVersion();
    }

    @Override
    @Schema(description = "JSON object with the report template Id. Omit this field to create a new report template.")
    public ReportTemplateId getId() {
        return super.getId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ReportTemplate [tenantId=" + tenantId + ", customerId=" + customerId + ", name=" + name
                + ", format=" + format + ", type=" + type + "]";
    }

}
