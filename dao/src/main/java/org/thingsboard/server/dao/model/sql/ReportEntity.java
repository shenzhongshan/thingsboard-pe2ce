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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

/**
 * The generated PDF/CSV bytes live in {@link #data} but are deliberately excluded from
 * {@link #toData()} — list/metadata queries should never pull the blob into memory; use
 * {@link org.thingsboard.server.dao.sql.report.ReportRepository#findDataById} to fetch it on demand.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "report")
public class ReportEntity extends BaseSqlEntity<Report> implements BaseEntity<Report> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "template_id")
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    private TbReportFormat format;

    @Column(name = "name")
    private String name;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "data")
    private byte[] data;

    public ReportEntity() {
        super();
    }

    public ReportEntity(Report report) {
        if (report.getId() != null) {
            this.setUuid(report.getId().getId());
        }
        this.setCreatedTime(report.getCreatedTime());
        if (report.getTenantId() != null) {
            this.tenantId = report.getTenantId().getId();
        }
        if (report.getCustomerId() != null) {
            this.customerId = report.getCustomerId().getId();
        }
        if (report.getTemplateId() != null) {
            this.templateId = report.getTemplateId().getId();
        }
        this.format = report.getFormat();
        this.name = report.getName();
        if (report.getUserId() != null) {
            this.userId = report.getUserId().getId();
        }
    }

    @Override
    public Report toData() {
        Report report = new Report(new ReportId(id));
        report.setCreatedTime(createdTime);
        if (tenantId != null) {
            report.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            report.setCustomerId(new CustomerId(customerId));
        }
        if (templateId != null) {
            report.setTemplateId(new ReportTemplateId(templateId));
        }
        report.setFormat(format);
        report.setName(name);
        if (userId != null) {
            report.setUserId(new UserId(userId));
        }
        return report;
    }

}
