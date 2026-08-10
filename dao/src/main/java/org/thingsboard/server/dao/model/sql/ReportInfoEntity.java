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
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

/**
 * Read-only projection over {@code report_info_view}: a report joined with its template,
 * user and customer names. Avoids the per-row lookups a plain report listing would need.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = "report_info_view")
public class ReportInfoEntity extends BaseSqlEntity<ReportInfo> implements BaseEntity<ReportInfo> {

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
    @Column(name = "customer_title")
    private String customerTitle;
    @Column(name = "report_template_name")
    private String reportTemplateName;
    @Column(name = "user_name")
    private String userName;

    public ReportInfoEntity() {
        super();
    }

    @Override
    public ReportInfo toData() {
        ReportInfo info = new ReportInfo();
        info.setId(new ReportId(getUuid()));
        info.setCreatedTime(createdTime);
        if (tenantId != null) {
            info.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            info.setCustomerId(new CustomerId(customerId));
        }
        if (templateId != null) {
            info.setTemplateId(new ReportTemplateId(templateId));
        }
        info.setFormat(format);
        info.setName(name);
        if (userId != null) {
            info.setUserId(new UserId(userId));
        }
        info.setCustomerTitle(customerTitle);
        info.setTemplateName(reportTemplateName);
        info.setUserName(userName);
        return info;
    }

}
