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
package org.thingsboard.server.service.report;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.info.GeneralNotificationInfo;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared "render + persist + notify" report-generation logic used by both the scheduler
 * (DefaultSchedulerService) and the "generate report" rule node (TbGenerateReportV2Node).
 * Synchronous, matching this CE port's simplification of PE's async Job-Manager-based generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final ReportTemplateService reportTemplateService;
    private final ReportService reportService;
    private final TbReportRenderService reportRenderService;
    private final NotificationCenter notificationCenter;

    public Report generateReport(TenantId tenantId, ReportConfig reportConfig) throws Exception {
        if (reportConfig == null || reportConfig.getReportTemplateId() == null) {
            throw new RuntimeException("Report template id is not specified!");
        }
        ReportTemplate template = reportTemplateService.findReportTemplateById(tenantId, reportConfig.getReportTemplateId());
        if (template == null) {
            throw new RuntimeException("Report template with id [" + reportConfig.getReportTemplateId() + "] not found!");
        }
        String timezone = reportConfig.getTimezone() != null ? reportConfig.getTimezone() : "UTC";
        ReportData rendered = reportRenderService.generateReport(tenantId, template, timezone);
        Report report = new Report();
        report.setTenantId(tenantId);
        report.setCustomerId(template.getCustomerId());
        report.setTemplateId(template.getId());
        report.setFormat(template.getFormat());
        report.setName(rendered.getName());
        report.setUserId(reportConfig.getUserId());
        Report saved = reportService.createReport(tenantId, report, rendered.getData());
        if (reportConfig.getTargets() != null && !reportConfig.getTargets().isEmpty() && reportConfig.getNotificationTemplateId() != null) {
            sendReportCompletionNotification(tenantId, template, saved, reportConfig);
        }
        return saved;
    }

    private void sendReportCompletionNotification(TenantId tenantId, ReportTemplate template, Report report, ReportConfig reportConfig) {
        Map<String, String> data = new HashMap<>();
        data.put("reportName", report.getName());
        data.put("templateName", template.getName());
        data.put("reportId", report.getId().toString());
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targets(reportConfig.getTargets())
                .templateId(reportConfig.getNotificationTemplateId())
                .info(new GeneralNotificationInfo(data))
                .additionalConfig(new NotificationRequestConfig())
                .originatorEntityId(template.getId())
                .build();
        notificationCenter.processNotificationRequest(tenantId, notificationRequest, new FutureCallback<NotificationRequestStats>() {
            @Override
            public void onSuccess(NotificationRequestStats stats) {
                log.debug("[{}] Sent report-generation completion notification for report [{}]", tenantId, report.getId());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to send report-generation completion notification for report [{}]", tenantId, report.getId(), t);
            }
        });
    }

}
