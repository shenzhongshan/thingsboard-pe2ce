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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.scheduler.TbSchedulerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's scheduler event API. Dropped vs PE: edge assignment endpoints, scheduled
 * report endpoints, by-ids lookup. Tenant/customer scoping is enforced in-controller since
 * SchedulerEvent has no CE EntityType (no generic checkEntity support).
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class SchedulerEventController extends BaseController {

    public static final String SCHEDULER_EVENT_ID = "schedulerEventId";
    /** Scheduler event type used by report generation schedules. */
    private static final String SCHEDULED_REPORT_EVENT_TYPE = "generateReport";

    private final SchedulerEventService schedulerEventService;
    private final TbSchedulerService tbSchedulerService;

    @ApiOperation(value = "Get Scheduler Event With Customer Info (getSchedulerEventInfoById)",
            notes = "Fetch the scheduler event info (with the customer title) based on the provided id. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/schedulerEvent/info/{schedulerEventId}"})
    public SchedulerEventWithCustomerInfo getSchedulerEventInfoById(
            @Parameter(description = "A string value representing the scheduler event id", required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ);
        SchedulerEventWithCustomerInfo event = schedulerEventService.findSchedulerEventWithCustomerInfoById(
                getCurrentUser().getTenantId(), new SchedulerEventId(toUUID(strSchedulerEventId)));
        return checkAccess(checkNotNull(event));
    }

    @ApiOperation(value = "Get Scheduler Event (getSchedulerEventById)",
            notes = "Fetch the full scheduler event, including its configuration. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/schedulerEvent/{schedulerEventId}"})
    public SchedulerEvent getSchedulerEventById(
            @Parameter(description = "A string value representing the scheduler event id", required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.READ);
        return getExisting(strSchedulerEventId);
    }

    @ApiOperation(value = "Save Scheduler Event (saveSchedulerEvent)",
            notes = "Creates or updates a scheduler event. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = {"/schedulerEvent"})
    public SchedulerEvent saveSchedulerEvent(
            @Parameter(description = "A JSON value representing the scheduler event", required = true)
            @RequestBody SchedulerEvent schedulerEvent) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.WRITE);
        schedulerEvent.setTenantId(currentUser.getTenantId());
        if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())) {
            schedulerEvent.setCustomerId(currentUser.getCustomerId());
        }
        if (schedulerEvent.getId() != null) {
            checkAccess(getExisting(schedulerEvent.getId().toString()));
        }
        return tbSchedulerService.save(schedulerEvent, currentUser);
    }

    @ApiOperation(value = "Enable or disable Scheduler Event (enableSchedulerEvent)",
            notes = "Updates the scheduler event with enabled = true/false. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = {"/schedulerEvent/{schedulerEventId}/enabled/{enabledValue}"})
    public SchedulerEvent enableSchedulerEvent(
            @Parameter(description = "A string value representing the scheduler event id", required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId,
            @Parameter(description = "Enable or disable the scheduler event", required = true)
            @PathVariable("enabledValue") Boolean enabledValue) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.WRITE);
        SchedulerEvent schedulerEvent = checkAccess(getExisting(strSchedulerEventId));
        schedulerEvent.setEnabled(enabledValue);
        return tbSchedulerService.save(schedulerEvent, getCurrentUser());
    }

    @ApiOperation(value = "Delete Scheduler Event (deleteSchedulerEvent)",
            notes = "Deletes the scheduler event. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = {"/schedulerEvent/{schedulerEventId}"})
    @ResponseStatus(HttpStatus.OK)
    public void deleteSchedulerEvent(
            @Parameter(description = "A string value representing the scheduler event id", required = true)
            @PathVariable(SCHEDULER_EVENT_ID) String strSchedulerEventId) throws ThingsboardException {
        checkParameter(SCHEDULER_EVENT_ID, strSchedulerEventId);
        accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, Operation.DELETE);
        SchedulerEvent schedulerEvent = checkAccess(getExisting(strSchedulerEventId));
        tbSchedulerService.delete(schedulerEvent, getCurrentUser());
    }

    @ApiOperation(value = "Get scheduler events (getSchedulerEvents)",
            notes = "Returns a page of scheduler events (with customer info) owned by the tenant, or created by the customer for customer users. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/schedulerEvents"}, params = {"pageSize", "page"})
    public PageData<SchedulerEventWithCustomerInfo> getSchedulerEvents(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on event's name, type, or customer's name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "A string value representing the scheduler event type. For example, 'updateAttributes'")
            @RequestParam(required = false) String type) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SchedulerEventFilter filter = SchedulerEventFilter.builder()
                .customerId(currentUser.getCustomerId())
                .type(type)
                .build();
        return schedulerEventService.findSchedulerEventsByTenantIdAndFilter(currentUser.getTenantId(), filter, pageLink);
    }

    @ApiOperation(value = "Get scheduled report events (getScheduledReportEvents)",
            notes = "Returns a page of scheduler events that generate reports, optionally narrowed to a " +
                    "report template and/or the user the report runs as. Requires TENANT_ADMIN authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/scheduledReports"}, params = {"pageSize", "page"})
    public PageData<SchedulerEventWithCustomerInfo> getScheduledReportEvents(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on the event name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION)
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION)
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Optional report template id to filter by")
            @RequestParam(required = false) String reportTemplateId,
            @Parameter(description = "Optional user id to filter by")
            @RequestParam(required = false) String userId) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SchedulerEventFilter filter = SchedulerEventFilter.builder()
                .customerId(currentUser.getCustomerId())
                .type(SCHEDULED_REPORT_EVENT_TYPE)
                .build();
        PageData<SchedulerEventWithCustomerInfo> events =
                schedulerEventService.findSchedulerEventsByTenantIdAndFilter(currentUser.getTenantId(), filter, pageLink);
        if (reportTemplateId == null && userId == null) {
            return events;
        }
        // reportTemplateId/userId live inside the event configuration JSON, which the info
        // projection omits — so the full event is loaded for the rows of this page only
        // (bounded by pageSize). Totals still describe all scheduled reports.
        TenantId tenantId = currentUser.getTenantId();
        List<SchedulerEventWithCustomerInfo> filtered = events.getData().stream()
                .filter(event -> {
                    SchedulerEvent full = schedulerEventService.findSchedulerEventById(tenantId, event.getId());
                    return full != null
                            && matchesConfigId(full.getConfiguration(), "reportTemplateId", reportTemplateId)
                            && matchesConfigId(full.getConfiguration(), "userId", userId);
                })
                .toList();
        return new PageData<>(filtered, events.getTotalPages(), events.getTotalElements(), events.hasNext());
    }

    /** Matches an id nested at configuration.msgBody.{field}.id; a null filter always matches. */
    private boolean matchesConfigId(JsonNode configuration, String field, String expectedId) {
        if (expectedId == null) {
            return true;
        }
        if (configuration == null) {
            return false;
        }
        JsonNode node = configuration.path("msgBody").path(field).path("id");
        return node.isTextual() && expectedId.equals(node.asText());
    }

    @ApiOperation(value = "Get scheduler events by time range (getSchedulerEventsByTime)",
            notes = "Returns all scheduler events that fire within [startTime, endTime], each with the list of fire timestamps — used by the calendar view. Requires TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = {"/schedulerEvents"}, params = {"startTime", "endTime"})
    public List<SchedulerEventWithCustomerInfo> getSchedulerEventsByTime(
            @Parameter(description = "A string value representing the scheduler event type. For example, 'updateAttributes'")
            @RequestParam(required = false) String type,
            @Parameter(description = "Start time filter in milliseconds for scheduler event fire time", required = true)
            @RequestParam long startTime,
            @Parameter(description = "End time filter in milliseconds for scheduler event fire time", required = true)
            @RequestParam long endTime,
            @Parameter(description = "Case-insensitive 'substring' filter based on event's name, type, or customer's name")
            @RequestParam(required = false) String textSearch) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, Operation.READ);
        SchedulerEventTimeFilter filter = SchedulerEventTimeFilter.builder()
                .customerId(currentUser.getCustomerId())
                .type(type)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return schedulerEventService.findAllSchedulerEventsByTenantIdAndEventTimeFilter(currentUser.getTenantId(), filter, textSearch);
    }

    private SchedulerEvent getExisting(String strSchedulerEventId) throws ThingsboardException {
        SchedulerEvent event = schedulerEventService.findSchedulerEventById(
                getCurrentUser().getTenantId(), new SchedulerEventId(toUUID(strSchedulerEventId)));
        return checkNotNull(event, "Scheduler event with id [" + strSchedulerEventId + "] is not found");
    }

    // Tenant scoping (and customer scoping for customer users) — manual since there is no CE EntityType.
    private <T extends SchedulerEventInfo> T checkAccess(T event) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        if (!currentUser.getTenantId().equals(event.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())
                && !currentUser.getCustomerId().equals(event.getCustomerId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return event;
    }

}
