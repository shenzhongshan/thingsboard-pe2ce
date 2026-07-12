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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

/**
 * A generated report instance/artifact. The actual rendered bytes are NOT a field here — stored
 * separately (bytea column, streamed on demand), matching PE's design; see ReportDao.
 */
@Schema
@EqualsAndHashCode(callSuper = true)
public class Report extends BaseData<ReportId> implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 1L;

    @Getter @Setter
    private TenantId tenantId;

    @Getter @Setter
    private CustomerId customerId;

    @Getter @Setter
    @Schema(description = "JSON object with the source report template Id")
    private ReportTemplateId templateId;

    @Getter @Setter
    private TbReportFormat format;

    @Getter @Setter
    private String name;

    @Getter @Setter
    @Schema(description = "JSON object with the id of the user the report was generated for/by")
    private UserId userId;

    public Report() {
        super();
    }

    public Report(ReportId id) {
        super(id);
    }

    public Report(Report report) {
        super(report);
        this.tenantId = report.getTenantId();
        this.customerId = report.getCustomerId();
        this.templateId = report.getTemplateId();
        this.format = report.getFormat();
        this.name = report.getName();
        this.userId = report.getUserId();
    }

    @Override
    public String getName() {
        return name;
    }

}
