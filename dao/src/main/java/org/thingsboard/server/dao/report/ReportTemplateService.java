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

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;

public interface ReportTemplateService {

    ReportTemplate saveReportTemplate(TenantId tenantId, ReportTemplate reportTemplate);

    ReportTemplate findReportTemplateById(TenantId tenantId, ReportTemplateId reportTemplateId);

    ReportTemplateInfo findReportTemplateInfoById(TenantId tenantId, ReportTemplateId reportTemplateId);

    PageData<ReportTemplateInfo> findReportTemplateInfos(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /** 查询客户自己的模板以及租户级公共模板，供客户用户生成报告。 */
    PageData<ReportTemplateInfo> findReportTemplateInfosForCustomer(TenantId tenantId, CustomerId customerId,
                                                                     PageLink pageLink);

    void deleteReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId);

    void deleteReportTemplatesByTenantId(TenantId tenantId);

}
