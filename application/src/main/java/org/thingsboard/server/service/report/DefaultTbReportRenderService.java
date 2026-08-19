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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.service.ReportService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapts the platform's render request to PE's report engine.
 * <p>
 * The engine is task-driven ({@link ReportTask}) because in PE it is fed from a queue; here the
 * request arrives synchronously from a controller or the scheduler, so the task is built on the
 * spot. No access token is set - {@link InProcessReportDataService} talks to the services directly.
 */
@Slf4j
@Service
@TbCoreComponent
public class DefaultTbReportRenderService implements TbReportRenderService {

    private final Map<TbReportFormat, ReportService> reportServices = new EnumMap<>(TbReportFormat.class);
    private final InProcessTbReportCtxProvider ctxProvider;

    public DefaultTbReportRenderService(List<ReportService> reportServices, InProcessTbReportCtxProvider ctxProvider) {
        // Same registration TbReportService does; done here rather than reaching into that class,
        // which is decompiled PE and keeps its format map private.
        reportServices.forEach(service -> {
            if (service.getFormat() != null) {
                this.reportServices.put(service.getFormat(), service);
            }
        });
        this.ctxProvider = ctxProvider;
    }

    @Override
    public ReportData generateReport(TenantId tenantId, ReportTemplate template, String timezone) throws Exception {
        if (template.getConfiguration() == null) {
            throw new ThingsboardException("Report template has no configuration!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        ReportTask task = toTask(tenantId, template, timezone);
        // The column is authoritative: a configuration posted without the redundant "format" key
        // binds as the default subtype, so its getFormat() cannot be trusted to pick the renderer.
        TbReportFormat format = template.getFormat() != null
                ? template.getFormat() : task.getReportTemplateConfig().getFormat();
        ReportService reportService = reportServices.get(format);
        if (reportService == null) {
            throw new ThingsboardException("Unsupported report format: " + format, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        try (TbReportCtx ctx = ctxProvider.newContext(task)) {
            return reportService.generateReport(task, ctx);
        }
    }

    private ReportTask toTask(TenantId tenantId, ReportTemplate template, String timezone) {
        ReportTask task = new ReportTask();
        task.setTenantId(tenantId);
        task.setCustomerId(template.getCustomerId());
        task.setReportTemplateId(template.getId());
        task.setReportTemplateConfig(template.getConfiguration());
        task.setTimezone(timezone);
        currentUser().ifPresent(user -> {
            task.setUserId(user.getId());
            task.setUserOwnerId(user.getOwnerId());
        });
        return task;
    }

    /**
     * The report runs with the requesting user's permissions where there is one. Scheduled runs have
     * no security context; those supply the user on the task themselves.
     */
    private Optional<SecurityUser> currentUser() {
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        if (context.getAuthentication() != null
                && context.getAuthentication().getPrincipal() instanceof SecurityUser securityUser) {
            return Optional.of(securityUser);
        }
        return Optional.empty();
    }

}
