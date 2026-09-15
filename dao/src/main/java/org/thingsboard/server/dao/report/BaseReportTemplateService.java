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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.model.sql.ReportTemplateEntity;
import org.thingsboard.server.dao.sql.report.ReportTemplateRepository;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseReportTemplateService implements ReportTemplateService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final ReportTemplateRepository reportTemplateRepository;
    private final CustomerService customerService;

    @Override
    public ReportTemplate saveReportTemplate(TenantId tenantId, ReportTemplate reportTemplate) {
        log.trace("Executing saveReportTemplate [{}]", reportTemplate);
        if (StringUtils.isEmpty(reportTemplate.getName())) {
            throw new DataValidationException("Report template name should be specified!");
        }
        if (reportTemplate.getFormat() == null) {
            throw new DataValidationException("Report format should be specified!");
        }
        if (reportTemplate.getConfiguration() == null) {
            throw new DataValidationException("Report template configuration should be specified!");
        }
        reportTemplate.setTenantId(tenantId);
        ReportTemplateEntity existing = reportTemplateRepository.findByTenantIdAndName(tenantId.getId(), reportTemplate.getName());
        if (existing != null && (reportTemplate.getId() == null || !existing.getId().equals(reportTemplate.getId().getId()))) {
            throw new DataValidationException("Report template with such name already exists!");
        }
        if (reportTemplate.getId() == null) {
            reportTemplate.setCreatedTime(System.currentTimeMillis());
        }
        boolean created = reportTemplate.getId() == null;
        ReportTemplate saved = reportTemplateRepository.save(new ReportTemplateEntity(reportTemplate)).toData();
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(saved.getId())
                .entity(saved).created(created).broadcastEvent(true).build());
        return saved;
    }

    @Override
    public ReportTemplate findReportTemplateById(TenantId tenantId, ReportTemplateId reportTemplateId) {
        return DaoUtil.getData(reportTemplateRepository.findById(reportTemplateId.getId()));
    }

    @Override
    public ReportTemplateInfo findReportTemplateInfoById(TenantId tenantId, ReportTemplateId reportTemplateId) {
        ReportTemplate template = findReportTemplateById(tenantId, reportTemplateId);
        return template == null ? null : toInfo(template);
    }

    @Override
    public PageData<ReportTemplateInfo> findReportTemplateInfos(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        Page<ReportTemplateEntity> page = customerId == null || customerId.isNullUid()
                ? reportTemplateRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink))
                : reportTemplateRepository.findByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink));
        PageData<ReportTemplate> templates = DaoUtil.toPageData(page);
        java.util.List<ReportTemplateInfo> infos = templates.getData().stream().map(this::toInfo).toList();
        return new PageData<>(infos, templates.getTotalPages(), templates.getTotalElements(), templates.hasNext());
    }

    @Override
    public PageData<ReportTemplateInfo> findReportTemplateInfosForCustomer(TenantId tenantId, CustomerId customerId,
                                                                            PageLink pageLink) {
        Page<ReportTemplateEntity> page = reportTemplateRepository.findByTenantIdAndCustomerIdOrPublic(
                tenantId.getId(), customerId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink));
        PageData<ReportTemplate> templates = DaoUtil.toPageData(page);
        java.util.List<ReportTemplateInfo> infos = templates.getData().stream().map(this::toInfo).toList();
        return new PageData<>(infos, templates.getTotalPages(), templates.getTotalElements(), templates.hasNext());
    }

    private ReportTemplateInfo toInfo(ReportTemplate template) {
        ReportTemplateInfo info = new ReportTemplateInfo(template.getId());
        info.setCreatedTime(template.getCreatedTime());
        info.setTenantId(template.getTenantId());
        info.setCustomerId(template.getCustomerId());
        info.setName(template.getName());
        info.setFormat(template.getFormat());
        info.setType(template.getType());
        info.setDescription(template.getDescription());
        if (template.getCustomerId() != null && !template.getCustomerId().isNullUid()) {
            Customer customer = customerService.findCustomerById(template.getTenantId(), template.getCustomerId());
            info.setOwnerName(customer != null ? customer.getTitle() : null);
        }
        return info;
    }

    @Override
    public void deleteReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId) {
        log.trace("Executing deleteReportTemplate [{}]", reportTemplateId);
        // Previously generated reports are a historical record and are kept (matches PE's
        // ON DELETE SET NULL FK intent) — only the template row itself is removed.
        reportTemplateRepository.deleteById(reportTemplateId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(reportTemplateId).build());
    }

    @Override
    public void deleteReportTemplatesByTenantId(TenantId tenantId) {
        for (UUID id : reportTemplateRepository.findIdsByTenantId(tenantId.getId())) {
            reportTemplateRepository.deleteById(id);
        }
    }

}
