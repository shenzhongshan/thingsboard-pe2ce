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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.sql.report.ReportInfoRepository;
import org.thingsboard.server.dao.sql.report.ReportRepository;
import org.thingsboard.server.dao.sql.report.ReportTemplateRepository;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseReportService implements ReportService {

    private static final int MAX_REPORT_SIZE_BYTES = 50 * 1024 * 1024;

    private final ReportRepository reportRepository;
    private final ReportInfoRepository reportInfoRepository;
    private final ReportTemplateRepository reportTemplateRepository;
    private final UserService userService;
    private final CustomerService customerService;

    @Override
    public Report createReport(TenantId tenantId, Report report, byte[] data) {
        log.trace("Executing createReport [{}]", report);
        if (data == null || data.length == 0) {
            throw new DataValidationException("Generated report has no content!");
        }
        if (data.length > MAX_REPORT_SIZE_BYTES) {
            throw new DataValidationException("Generated report exceeds the maximum allowed size!");
        }
        report.setTenantId(tenantId);
        if (report.getId() == null) {
            report.setCreatedTime(System.currentTimeMillis());
        }
        ReportEntity entity = new ReportEntity(report);
        entity.setData(data);
        return reportRepository.save(entity).toData();
    }

    @Override
    public Report findReportById(TenantId tenantId, ReportId reportId) {
        return DaoUtil.getData(reportRepository.findById(reportId.getId()));
    }

    @Override
    public byte[] getReportData(TenantId tenantId, ReportId reportId) {
        return reportRepository.findDataById(reportId.getId());
    }

    @Override
    // customerId 由控制层在客户用户场景传入，避免客户用户读取其他客户的报告。
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, ReportTemplateId templateId, UserId userId,
                                                CustomerId customerId, PageLink pageLink) {
        // Backed by report_info_view, which joins template/user/customer names in one query.
        // Previously this enriched each row with three separate lookups (N+1 on large pages).
        return DaoUtil.toPageData(reportInfoRepository.findByTenantId(
                tenantId.getId(),
                templateId != null ? templateId.getId() : null,
                userId != null ? userId.getId() : null,
                customerId != null ? customerId.getId() : null,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void deleteReport(TenantId tenantId, ReportId reportId) {
        reportRepository.deleteById(reportId.getId());
    }

    @Override
    public void deleteReportsByTenantId(TenantId tenantId) {
        for (UUID id : reportRepository.findIdsByTenantId(tenantId.getId())) {
            reportRepository.deleteById(id);
        }
    }

}
