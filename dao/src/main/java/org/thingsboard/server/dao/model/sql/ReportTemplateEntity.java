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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "report_template")
public class ReportTemplateEntity extends BaseSqlEntity<ReportTemplate> implements BaseEntity<ReportTemplate> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    private TbReportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ReportTemplateType type;

    @Column(name = "description")
    private String description;

    @Convert(converter = JsonConverter.class)
    @Column(name = "configuration")
    private JsonNode configuration;

    public ReportTemplateEntity() {
        super();
    }

    public ReportTemplateEntity(ReportTemplate reportTemplate) {
        if (reportTemplate.getId() != null) {
            this.setUuid(reportTemplate.getId().getId());
        }
        this.setCreatedTime(reportTemplate.getCreatedTime());
        if (reportTemplate.getTenantId() != null) {
            this.tenantId = reportTemplate.getTenantId().getId();
        }
        if (reportTemplate.getCustomerId() != null) {
            this.customerId = reportTemplate.getCustomerId().getId();
        }
        this.name = reportTemplate.getName();
        this.format = reportTemplate.getFormat();
        this.type = reportTemplate.getType();
        this.description = reportTemplate.getDescription();
        this.configuration = reportTemplate.getConfiguration();
    }

    @Override
    public ReportTemplate toData() {
        ReportTemplate reportTemplate = new ReportTemplate(new ReportTemplateId(id));
        reportTemplate.setCreatedTime(createdTime);
        if (tenantId != null) {
            reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            reportTemplate.setCustomerId(new CustomerId(customerId));
        }
        reportTemplate.setName(name);
        reportTemplate.setFormat(format);
        reportTemplate.setType(type);
        reportTemplate.setDescription(description);
        reportTemplate.setConfiguration(configuration);
        return reportTemplate;
    }

}
