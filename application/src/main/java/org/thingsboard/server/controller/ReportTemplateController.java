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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's report template API (PE path: /api/reportTemplate*). Dropped vs PE: version
 * control, edge sync. Configuration shape documented on {@link ReportTemplate}.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class ReportTemplateController extends BaseController {

    public static final String REPORT_TEMPLATE_ID = "reportTemplateId";

    private final ReportTemplateService reportTemplateService;

    @ApiOperation(value = "Get Report Template (getReportTemplateById)")
    // 客户用户可以读取本客户或租户公共模板，但不能通过此接口修改模板。
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/reportTemplate/{reportTemplateId}"})
    public ReportTemplate getReportTemplateById(
            @Parameter(description = "A string value representing the report template id", required = true)
            @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws ThingsboardException {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.READ);
        return checkAccess(checkNotNull(reportTemplateService.findReportTemplateById(
                getTenantId(), new ReportTemplateId(toUUID(strReportTemplateId)))));
    }

    @ApiOperation(value = "Save Report Template (saveReportTemplate)")
    // 模板创建和编辑仍然只允许租户管理员，客户用户仅能读取模板。
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/reportTemplate"})
    public ReportTemplate saveReportTemplate(
            @Parameter(description = "A JSON value representing the report template", required = true)
            @RequestBody ReportTemplate reportTemplate) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.WRITE);
        reportTemplate.setTenantId(getTenantId());
        if (reportTemplate.getId() != null) {
            checkAccess(checkNotNull(reportTemplateService.findReportTemplateById(getTenantId(), reportTemplate.getId())));
        }
        return reportTemplateService.saveReportTemplate(getTenantId(), reportTemplate);
    }

    @ApiOperation(value = "Get Report Template Infos (getReportTemplateInfos)")
    // 客户用户可读取自己的模板和租户公共模板；模板写入仍只允许租户管理员。
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/reportTemplate/infos"}, params = {"pageSize", "page"})
    public PageData<ReportTemplateInfo> getReportTemplateInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on report template name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "A string value representing the customer id, to filter customer-owned templates")
            @RequestParam(required = false) String customerId) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        CustomerId customerIdFilter = customerId != null ? new CustomerId(toUUID(customerId)) : null;
        if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
            // 正常客户用户一定关联客户；缺少客户归属时返回空结果，避免把租户模板泄露给孤立账号。
            if (getCurrentUser().getCustomerId() == null || getCurrentUser().getCustomerId().isNullUid()) {
                return new PageData<>(java.util.List.of(), 0, 0, false);
            }
            return reportTemplateService.findReportTemplateInfosForCustomer(
                    getTenantId(), getCurrentUser().getCustomerId(), pageLink);
        }
        return reportTemplateService.findReportTemplateInfos(getTenantId(), customerIdFilter, pageLink);
    }

    @ApiOperation(value = "Delete Report Template (deleteReportTemplate)",
            notes = "Previously generated reports produced from this template are preserved.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/reportTemplate/{reportTemplateId}"})
    public void deleteReportTemplate(
            @Parameter(description = "A string value representing the report template id", required = true)
            @PathVariable(REPORT_TEMPLATE_ID) String strReportTemplateId) throws ThingsboardException {
        checkParameter(REPORT_TEMPLATE_ID, strReportTemplateId);
        accessControlService.checkPermission(getCurrentUser(), Resource.REPORT_TEMPLATE, Operation.DELETE);
        ReportTemplateId reportTemplateId = new ReportTemplateId(toUUID(strReportTemplateId));
        checkAccess(checkNotNull(reportTemplateService.findReportTemplateById(getTenantId(), reportTemplateId)));
        reportTemplateService.deleteReportTemplate(getTenantId(), reportTemplateId);
    }

    private ReportTemplate checkAccess(ReportTemplate reportTemplate) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(reportTemplate.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        // 客户用户只能读取当前客户的模板，避免通过模板详情接口越权读取其他客户配置。
        if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())
                && reportTemplate.getCustomerId() != null
                && (getCurrentUser().getCustomerId() == null
                || !getCurrentUser().getCustomerId().equals(reportTemplate.getCustomerId()))) {
            throw new ThingsboardException("你无权访问其他客户的报告模板!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return reportTemplate;
    }

}
