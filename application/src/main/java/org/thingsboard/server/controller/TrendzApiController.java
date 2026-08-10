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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.trendz.TrendzApiService;

import java.util.UUID;

/**
 * Reads Trendz content for the current user. Every call requires Trendz to be synchronized; the
 * user's identity is forwarded so Trendz returns only what that user may see.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/trendz")
@RequiredArgsConstructor
@Slf4j
public class TrendzApiController extends BaseController {

    private static final String TRENDZ_VIEW_ID = "viewId";

    private final TrendzApiService trendzApiService;

    @ApiOperation(value = "Get Trendz Views (getTrendzViews)",
            notes = "Returns a page of Trendz views available to the current user. " +
                    "Available for users with TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(path = "/view/all", params = {"pageSize", "page"})
    public PageData<TrendzViewConfigLite> getTrendzViews(
            @Parameter(description = "Maximum amount of entities in a one page", required = true) @RequestParam int pageSize,
            @Parameter(description = "Sequence number of page starting from 0", required = true) @RequestParam int page,
            @Parameter(description = "Case insensitive substring filter on the Trendz view name.") @RequestParam(required = false) String textSearch,
            @Parameter(description = "Property to sort by", schema = @Schema(allowableValues = {"name", "createdAt", "updatedAt", "favorite"})) @RequestParam(required = false) String sortProperty,
            @Parameter(description = "Sort order", schema = @Schema(allowableValues = {"ASC", "DESC"})) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return trendzApiService.getAllViews(getCurrentUser(), pageLink);
    }

    @ApiOperation(value = "Get Trendz View by Id (getTrendzViewById)",
            notes = "Fetches one Trendz view configuration. " +
                    "Available for users with TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/view/{viewId}")
    public TrendzViewConfig getTrendzViewById(
            @Parameter(description = "Trendz view id, e.g. 784f394c-42b6-435a-983c-b7beff2784f9", required = true)
            @PathVariable(TRENDZ_VIEW_ID) String strViewId) throws ThingsboardException {
        checkParameter(TRENDZ_VIEW_ID, strViewId);
        UUID viewId = toUUID(strViewId);
        return trendzApiService.getViewById(getCurrentUser(), viewId);
    }

    @ApiOperation(value = "Get Trendz Summary (getTrendzSummary)",
            notes = "Fetches the Trendz summary shown on the home page. " +
                    "Available for users with TENANT_ADMIN or CUSTOMER_USER authority.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/summary")
    public TrendzSummary getTrendzSummary() throws ThingsboardException {
        return trendzApiService.getTrendzSummary(getCurrentUser());
    }

    @ApiOperation(value = "Get Trendz Usage (getTrendzUsage)",
            notes = "Fetches Trendz feature usage counters. " +
                    "Available for users with SYS_ADMIN or TENANT_ADMIN authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/usage")
    public TrendzUsage getTrendzUsage() throws ThingsboardException {
        return trendzApiService.getTrendzUsage(getCurrentUser());
    }

}
