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
package org.thingsboard.server.dao.report;

import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportInfo;

public interface ReportService {

    /** Persists the report metadata and its rendered bytes together. */
    Report createReport(TenantId tenantId, Report report, byte[] data);

    Report findReportById(TenantId tenantId, ReportId reportId);

    byte[] getReportData(TenantId tenantId, ReportId reportId);

    /** 按模板、生成用户及客户范围分页查询报告元数据。customerId 为空时不限制客户。 */
    PageData<ReportInfo> findReportInfos(TenantId tenantId, ReportTemplateId templateId, UserId userId,
                                         CustomerId customerId, PageLink pageLink);

    void deleteReport(TenantId tenantId, ReportId reportId);

    void deleteReportsByTenantId(TenantId tenantId);

}
