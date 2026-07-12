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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.report.TbReportRenderService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's generated-report API (PE path: /api/v2/report*). Report generation here is
 * synchronous (render + persist in the request thread) rather than PE's async job queue —
 * acceptable given CE's report sizes/volume; the scheduler (see REPORT-B6) uses the same
 * {@link TbReportRenderService} off the request thread for scheduled generation.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class ReportController extends BaseController {

    public static final String REPORT_ID = "reportId";
    public static final String REPORT_TEMPLATE_ID = "reportTemplateId";

    private final ReportService reportService;
    private final ReportTemplateService reportTemplateService;
    private final TbReportRenderService reportRenderService;

    @ApiOperation(value = "Get Report (getReportById)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/report/{reportId}"})
    public Report getReportById(
            @Parameter(description = "A string value representing the report id", required = true)
            @PathVariable(REPORT_ID) String strReportId) throws ThingsboardException {
        checkParameter(REPORT_ID, strReportId);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT, Operation.READ);
        return checkAccess(checkNotNull(reportService.findReportById(
                getTenantId(), new ReportId(toUUID(strReportId)))));
    }

    @ApiOperation(value = "Download Report (downloadReport)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/report/{reportId}/download"})
    public ResponseEntity<ByteArrayResource> downloadReport(
            @Parameter(description = "A string value representing the report id", required = true)
            @PathVariable(REPORT_ID) String strReportId) throws ThingsboardException {
        checkParameter(REPORT_ID, strReportId);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT, Operation.READ);
        ReportId reportId = new ReportId(toUUID(strReportId));
        Report report = checkAccess(checkNotNull(reportService.findReportById(getTenantId(), reportId)));
        byte[] data = reportService.getReportData(getTenantId(), reportId);
        checkNotNull(data);
        String encodedName = URLEncoder.encode(report.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType(report.getFormat().getContentType()))
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @ApiOperation(value = "Get Reports (getReportInfos)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/reports"}, params = {"pageSize", "page"})
    public PageData<ReportInfo> getReportInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on report name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "A string value representing the report template id, to filter reports by source template")
            @RequestParam(required = false) String reportTemplateId,
            @Parameter(description = "A string value representing the user id, to filter reports by generating user")
            @RequestParam(required = false) String userId) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        ReportTemplateId templateIdFilter = reportTemplateId != null ? new ReportTemplateId(toUUID(reportTemplateId)) : null;
        UserId userIdFilter = userId != null ? new UserId(toUUID(userId)) : null;
        return reportService.findReportInfos(getTenantId(), templateIdFilter, userIdFilter, pageLink);
    }

    @ApiOperation(value = "Generate Report (generateReport)",
            notes = "Renders the given report template synchronously and persists the resulting report.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/report/{reportTemplateId}/generate"})
    public Report generateReport(
            @Parameter(description = "A string value representing the report template id", required = true)
            @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId,
            @Parameter(description = "Timezone used to resolve relative time windows in the report, e.g. 'UTC', 'Europe/Kyiv'")
            @RequestParam(required = false, defaultValue = "UTC") String timezone) throws Exception {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT, Operation.WRITE);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        ReportTemplate template = checkNotNull(reportTemplateService.findReportTemplateById(getTenantId(), reportTemplateId));
        if (!getCurrentUser().getTenantId().equals(template.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        ReportData rendered = reportRenderService.generateReport(getTenantId(), template, timezone);
        Report report = new Report();
        report.setTenantId(getTenantId());
        report.setCustomerId(template.getCustomerId());
        report.setTemplateId(reportTemplateId);
        report.setFormat(template.getFormat());
        report.setName(rendered.getName());
        report.setUserId(getCurrentUser().getId());
        return reportService.createReport(getTenantId(), report, rendered.getData());
    }

    @ApiOperation(value = "Delete Report (deleteReport)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/report/{reportId}"})
    public void deleteReport(
            @Parameter(description = "A string value representing the report id", required = true)
            @PathVariable(REPORT_ID) String strReportId) throws ThingsboardException {
        checkParameter(REPORT_ID, strReportId);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT, Operation.DELETE);
        ReportId reportId = new ReportId(toUUID(strReportId));
        checkAccess(checkNotNull(reportService.findReportById(getTenantId(), reportId)));
        reportService.deleteReport(getTenantId(), reportId);
    }

    private Report checkAccess(Report report) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(report.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return report;
    }

}
